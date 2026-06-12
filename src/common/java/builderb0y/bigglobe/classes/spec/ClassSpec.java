package builderb0y.bigglobe.classes.spec;

import java.lang.invoke.*;
import java.util.*;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.HashCodes;
import builderb0y.bigglobe.classes.ScriptObject;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.DynamicConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.HandleConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.*;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.AddInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassSpec extends BaseClassSpec {

	@Override
	public ClassSpec parent(ClassHierarchy hierarchy) {
		return requireType(this.parent, ClassSpec.class, () -> hierarchy.idOf(this) + " > extends");
	}

	public Set<Holder<ElementSpec>>
		fieldsForEquals   = new HashSet<>(),
		fieldsForHashCode = new HashSet<>(),
		fieldsForToString = new HashSet<>();

	public ClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent
	) {
		super(name, isAbstract, parent);
	}

	public Stream<Holder<ElementSpec>> fieldsForEquals(ClassHierarchy hierarchy) {
		Stream<Holder<ElementSpec>> stream = this.fieldsForEquals.stream();
		if (this.parent != null) stream = Stream.concat(this.parent(hierarchy).fieldsForEquals(hierarchy), stream);
		return stream;
	}

	public Stream<Holder<ElementSpec>> fieldsForHashCode(ClassHierarchy hierarchy) {
		Stream<Holder<ElementSpec>> stream = this.fieldsForHashCode.stream();
		if (this.parent != null) stream = Stream.concat(this.parent(hierarchy).fieldsForHashCode(hierarchy), stream);
		return stream;
	}

	public Stream<Holder<ElementSpec>> fieldsForToString(ClassHierarchy hierarchy) {
		Stream<Holder<ElementSpec>> stream = this.fieldsForToString.stream();
		if (this.parent != null) stream = Stream.concat(this.parent(hierarchy).fieldsForToString(hierarchy), stream);
		return stream;
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		if (this.parent != null) this.parent(hierarchy);
		super.verify(hierarchy);
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.primaryConstructor = this.classCompileContext.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID
		);
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		invokeInstance(
			load("this", this.getTypeInfo()),
			new MethodInfo(
				ACC_PUBLIC,
				this.getParentTypeInfo(hierarchy),
				"<init>",
				TypeInfos.VOID
			)
		)
		.emitBytecode(this.primaryConstructor);
		super.compile(hierarchy);
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();

		if (!this.fieldsForEquals.isEmpty()) {
			MethodCompileContext equals = this.classCompileContext.newMethod(
				ACC_PUBLIC,
				"equals",
				TypeInfos.BOOLEAN,
				new LazyVarInfo("object", TypeInfos.OBJECT)
			);
			InsnTree loadThis = load("this", this.classCompileContext.info);
			InsnTree loadObj  = load("object", TypeInfos.OBJECT);
			MethodInfo getClass = MethodInfo.findMethod(Object.class, "getClass", Class.class);
			ifThen(new IdentityEqualityConditionTree(loadThis, loadObj, IF_ACMPEQ), return_(ldc(true))).emitBytecode(equals);
			ifThen(NullCompareConditionTree.isNull(loadObj), return_(ldc(false))).emitBytecode(equals);
			ifThen(new IdentityEqualityConditionTree(invokeInstance(loadThis, getClass), invokeInstance(loadObj, getClass), IF_ACMPNE), return_(ldc(false))).emitBytecode(equals);
			LazyVarInfo that = equals.scopes.addVariable("that", this.classCompileContext.info);
			store(that, new DirectCastInsnTree(loadObj, that.type, false)).emitBytecode(equals);
			this
			.fieldsForEquals(hierarchy)
			.map(Holder<ElementSpec>::value)
			.map(FieldSpec.class::cast)
			.map((FieldSpec field) -> {
				InsnTree loadThisField = getField(loadThis, field.context.field.info);
				InsnTree loadThatField = getField(load(that), field.context.field.info);
				ConditionTree notEqual = switch (field.fieldType(hierarchy).getTypeInfo().getSort()) {
					case VOID -> throw new IllegalStateException("void field");
					case BOOLEAN, BYTE, SHORT, CHAR, INT -> new IntCompareConditionTree(loadThisField, loadThatField, IF_ICMPNE);
					case LONG -> new LongCompareConditionTree(loadThisField, loadThatField, IFNE);
					case FLOAT -> new FloatCompareConditionTree(loadThisField, loadThatField, IFNE, FCMPL);
					case DOUBLE -> new DoubleCompareConditionTree(loadThisField, loadThatField, IFNE, DCMPL);
					case OBJECT, ARRAY -> new ObjectCompareConditionTree(invokeStatic(ArrayExtensions.OBJECTS_EQUALS, loadThisField, loadThatField), IFNE);
				};
				return ifThen(notEqual, return_(ldc(false)));
			})
			.forEachOrdered((InsnTree tree) -> tree.emitBytecode(equals));
			return_(ldc(true)).emitBytecode(equals);
			equals.endCode();
		}

		if (!this.fieldsForHashCode.isEmpty()) {
			MethodCompileContext hashCode = this.classCompileContext.newMethod(
				ACC_PUBLIC,
				"hashCode",
				TypeInfos.INT
			);
			Iterator<InsnTree> iterator = (
				this
				.fieldsForHashCode(hierarchy)
				.map(Holder<ElementSpec>::value)
				.map(FieldSpec.class::cast)
				.map((FieldSpec field) -> {
					InsnTree load = getField(
						load("this", this.getTypeInfo()),
						field.context.field.info
					);
					MethodInfo hasher = switch (field.fieldType(hierarchy).getTypeInfo().getSort()) {
						case VOID          -> throw new IllegalStateException("void field");
						case BOOLEAN       -> HashCodes.INFO.hashZ;
						case BYTE          -> HashCodes.INFO.hashB;
						case CHAR          -> HashCodes.INFO.hashC;
						case SHORT         -> HashCodes.INFO.hashS;
						case INT           -> HashCodes.INFO.hashI;
						case LONG          -> HashCodes.INFO.hashL;
						case FLOAT         -> HashCodes.INFO.hashF;
						case DOUBLE        -> HashCodes.INFO.hashD;
						case OBJECT, ARRAY -> HashCodes.INFO.hashA;
					};
					return invokeStatic(hasher, load);
				})
				.iterator()
			);
			InsnTree accumulator = iterator.next();
			while (iterator.hasNext()) {
				accumulator = new AddInsnTree(accumulator, iterator.next(), IADD);
			}
			return_(accumulator).emitBytecode(hashCode);
			hashCode.endCode();
		}

		if (!this.fieldsForToString.isEmpty()) {
			MethodCompileContext toString = this.classCompileContext.newMethod(
				ACC_PUBLIC,
				"toString",
				TypeInfos.STRING
			);
			LoadInsnTree loadSelf = load("this", this.getTypeInfo());
			StringBuilder template = new StringBuilder();
			ArrayBuilder<InsnTree> arguments = new ArrayBuilder<>();
			template.append(this.name).append('(');
			this
			.fieldsForToString(hierarchy)
			.map(Holder<ElementSpec>::value)
			.map(FieldSpec.class::cast)
			.sorted(Comparator.comparing(FieldSpec::name, String.CASE_INSENSITIVE_ORDER))
			.forEachOrdered((FieldSpec field) -> {
				template.append(field.name).append(": ").append('\u0001').append(", ");
				arguments.add(getField(loadSelf, field.context.field.info));
			});
			return_(
				invokeDynamic(
					STRING_CONCAT,
					new MethodInfo(
						ACC_PUBLIC | ACC_STATIC,
						TypeInfos.OBJECT, //ignored
						"concat",
						TypeInfos.STRING,
						arguments
						.stream()
						.map(InsnTree::getTypeInfo)
						.toArray(TypeInfo.ARRAY_FACTORY)
					),
					new ConstantValue[] {
						constant(template.toString())
					},
					arguments.toArray(InsnTree.ARRAY_FACTORY)
				)
			)
			.emitBytecode(toString);
			toString.endCode();
		}
	}

	public static final MethodInfo STRING_CONCAT = MethodInfo.findMethod(
		StringConcatFactory.class,
		"makeConcatWithConstants",
		CallSite.class,
		MethodHandles.Lookup.class,
		String.class,
		MethodType.class,
		String.class,
		Object[].class
	);

	public static final MethodInfo CONSTANT_INVOKER = MethodInfo.findMethod(
		ConstantBootstraps.class,
		"invoke",
		Object.class,
		MethodHandles.Lookup.class,
		String.class,
		Class.class,
		MethodHandle.class,
		Object[].class
	);

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data) throws ConstantFormatException {
		if (data.isEmpty()) {
			return ldc(null, this.getTypeInfo());
		}
		else if (this.getMembers(BaseFieldSpec.class, true).findAny().isEmpty()) {
			return ldc(
				new DynamicConstantValue(
					this.getTypeInfo(),
					CONSTANT_INVOKER,
					new HandleConstantValue(this.getTypeInfo(), this.primaryConstructor.info)
				)
			);
		}
		else {
			throw new ConstantFormatException("Can't create constant for class " + hierarchy.idOf(this) + " because this class has at least one field.");
		}
	}

	@Override
	public TypeInfo defaultSuperClass() {
		return ScriptObject.TYPE;
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, ExpressionParser parser, ExternalEnvironmentParams params) {
		super.setupEnvironment(self, parser, params);
		parser.environment.mutable().addQualifiedConstructor(this.primaryConstructor.info);
	}
}