package builderb0y.bigglobe.classes.spec;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.ScriptEnum;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.NewArrayWithContentsInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EnumClassSpec extends BaseClassSpec {

	@Override
	public EnumClassSpec parent(ClassHierarchy hierarchy) {
		return requireType(this.parent, EnumClassSpec.class, () -> hierarchy.idOf(this) + " > extends");
	}

	public transient MethodCompileContext staticInitializer;
	public transient FieldCompileContext valueSet, valueMap;
	public transient MethodCompileContext valueOf;
	public final transient Map<String, Holder<ElementSpec>> values = new HashMap<>();

	public EnumClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent
	) {
		super(name, isAbstract, parent);
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), this.values.values().stream());
	}

	@Override
	public void tryProgressTo(CompileStep step, ClassHierarchy context) throws DetailedException {
		super.tryProgressTo(step, context);
		if (step != CompileStep.REFERENCE) {
			context.catchAll(this.values.values(), step);
		}
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		if (this.parent != null && !(this.parent.value() instanceof EnumClassSpec)) {
			throw new CustomClassFormatException("Enum " + hierarchy.idOf(this) + " cannot extend non-enum " + UnregisteredObjectException.getID(this.parent));
		}
		super.verify(hierarchy);
	}

	@Override
	public TypeInfo defaultSuperClass() {
		return ScriptEnum.$CONSTRUCTOR_INFO.typeInfo;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedMethod("hashCode");
		this.overrideTracker.addReservedMethod("equals", TypeInfos.OBJECT);
		//toString() can be overridden.
	}

	@Override
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.primaryConstructor = this.classCompileContext.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			Stream.concat(
				Stream.of(new LazyVarInfo("name", TypeInfos.STRING)),
				this
				.getMembers(ConstantFieldSpec.class, true)
				.map((ConstantFieldSpec field) -> new LazyVarInfo(field.name, field.fieldType(hierarchy).getTypeInfo()))
			)
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
		this.staticInitializer = this.classCompileContext.newMethod(
			ACC_PRIVATE | ACC_STATIC,
			"<clinit>",
			TypeInfos.VOID
		);
		this.valueSet = this.classCompileContext.newField(
			ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
			"$valueSet",
			type(Set.class)
		);
		this.valueMap = this.classCompileContext.newField(
			ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
			"$valueMap",
			type(Map.class)
		);
		this.valueOf = this.classCompileContext.newMethod(
			ACC_PUBLIC | ACC_STATIC,
			"$valueOf",
			this.getTypeInfo(),
			new LazyVarInfo("name", TypeInfos.STRING)
		);
	}

	@Override
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		invokeInstance(
			load("this", this.getTypeInfo()),
			new MethodInfo(
				ACC_PUBLIC,
				this.getParentTypeInfo(hierarchy),
				"<init>",
				TypeInfos.VOID,
				this.parent != null
				? Stream.concat(
					Stream.of(TypeInfos.STRING),
					this
					.parent(hierarchy)
					.getMembers(ConstantFieldSpec.class, true)
					.map((ConstantFieldSpec constant) -> constant.fieldType(hierarchy).getTypeInfo())
				)
				.toArray(TypeInfo.ARRAY_FACTORY)
				: new TypeInfo[] { TypeInfos.STRING }
			),
			this.parent != null
			? Stream.concat(
				Stream.of(load("name", TypeInfos.STRING)),
				this
				.parent(hierarchy)
				.getMembers(ConstantFieldSpec.class, true)
				.map((ConstantFieldSpec constant) -> load(constant.name, constant.fieldType(hierarchy).getTypeInfo()))
			)
			.toArray(InsnTree.ARRAY_FACTORY)
			: new InsnTree[] {
				load("name", TypeInfos.STRING)
			}
		)
		.emitBytecode(this.primaryConstructor);
		super.compile(hierarchy);
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
		if (this.values != null) {
			new NewArrayWithContentsInsnTree(
				TypeInfo.makeArray(this.getTypeInfo()),
				this
				.values
				.values()
				.stream()
				.map(Holder<ElementSpec>::value)
				.map(EnumValueSpec.class::cast)
				.sorted(Comparator.comparing((EnumValueSpec spec) -> spec.name))
				.map((EnumValueSpec spec) -> getStatic(spec.context.info))
				.toArray(InsnTree.ARRAY_FACTORY)
			)
			.emitBytecode(this.staticInitializer);
			//array on stack.
			this.staticInitializer.node.visitInsn(DUP);
			ScriptEnum.$INFO.$createSet.emitBytecode(this.staticInitializer);
			this.valueSet.info.emitPut(this.staticInitializer);
			ScriptEnum.$INFO.$createMap.emitBytecode(this.staticInitializer);
			this.valueMap.info.emitPut(this.staticInitializer);
			this.staticInitializer.node.visitInsn(RETURN);
			this.staticInitializer.endCode();
			return_(
				new DirectCastInsnTree(
					invokeInstance(
						getStatic(this.valueMap.info),
						MethodInfo.findMethod(Map.class, "get", Object.class, Object.class),
						load("name", TypeInfos.STRING)
					),
					this.getTypeInfo(),
					false
				)
			)
			.emitBytecode(this.valueOf);
			this.valueOf.endCode();
		}
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, ExpressionParser parser, ExternalEnvironmentParams params) {
		super.setupEnvironment(self, parser, params);
		MutableScriptEnvironment environment = parser.environment.mutable();
		if (this.valueSet != null) environment.addQualifiedVariableGetStatic("valueSet", this.valueSet.info);
		if (this.valueMap != null) environment.addQualifiedVariableGetStatic("valueMap", this.valueMap.info);
		if (this.valueOf  != null) environment.addQualifiedFunctionInvokeStatic("valueOf", this.valueOf.info);
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data) throws ConstantFormatException {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		String name = BuiltinType.asString(data).value;
		if (this.values.get(name) != null) {
			return getStatic(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, this.getTypeInfo(), name, this.getTypeInfo());
		}
		else {
			throw new ConstantFormatException("Unknown enum " + name + " of type " + hierarchy.idOf(this));
		}
	}
}