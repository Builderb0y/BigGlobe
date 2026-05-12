package builderb0y.bigglobe.classes.spec;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.*;
import builderb0y.bigglobe.classes.compile.OverrideTracker.TrackedField;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.NewArrayWithContentsInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EnumClassSpec extends BaseClassSpec {

	public final @VerifyNullable DelayedEntryList<ElementSpec> values;
	public transient @Nullable MethodCompileContext staticInitializer;
	public transient @Nullable FieldCompileContext valueSet, valueMap;
	public transient @Nullable MethodCompileContext valueOf;

	public EnumClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent,
		DelayedEntryList<ElementSpec> members,
		@Nullable DelayedEntryList<ElementSpec> values
	) {
		super(name, isAbstract, parent, members);
		this.values = values;
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws CustomClassFormatException {
		super.verify(hierarchy);
		if (this.parent == null) {
			if (this.values == null) {
				throw new CustomClassFormatException("Enum class " + hierarchy.idOf(this) + " does not extend anything, and therefore must specify values.");
			}
		}
		else {
			if (this.values != null) {
				throw new CustomClassFormatException("Enum class " + hierarchy.idOf(this) + " extends something, and therefore cannot specify values.");
			}
		}
		for (ElementSpec spec : this.members.objectList()) {
			if (spec instanceof EnumValueSpec) {
				throw new CustomClassFormatException("All elements of type 'field/enum' must go in the 'values' tag of an enum class, not the 'members' tag. (found " + hierarchy.idOf(spec) + " inside members of " + hierarchy.idOf(this) + ')');
			}
		}
		if (this.values != null) for (ElementSpec spec : this.values.objectList()) {
			if (!(spec instanceof EnumValueSpec)) {
				throw new CustomClassFormatException("All values inside 'values' tag of enum class must be of type 'field/enum' (found " + hierarchy.idOf(spec) + " of type " + ElementSpec.REGISTRY.getType(spec) + " inside values of " + hierarchy.idOf(this) + ')');
			}
		}
	}

	@Override
	public void checkConstructor(ClassHierarchy hierarchy, ConstructorSpec spec) throws CustomClassFormatException {
		throw new CustomClassFormatException("Can't add custom constructor " + hierarchy.idOf(spec) + " to enum class " + hierarchy.idOf(this) + " because enums are constructed automatically.");
	}

	@Override
	public void checkEnumField(ClassHierarchy hierarchy, EnumValueSpec spec) throws CustomClassFormatException {
		if (this.parent != null) {
			throw new CustomClassFormatException("Can't add enum field " + hierarchy.idOf(spec) + " to enum class " + hierarchy.idOf(this) + " because this enum class extends something.");
		}
	}

	@Override
	public TypeInfo defaultSuperClass() {
		return EnumBase.$CONSTRUCTOR_INFO.typeInfo;
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
	public void createClass(ClassHierarchy hierarchy) {
		super.createClass(hierarchy);
		this.primaryConstructor = this.classCompileContext.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			EnumBase.$CONSTRUCTOR_INFO.parameterVarInfos
		);
		if (this.values != null) {
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
	}

	@Override
	public void createMembers(ClassHierarchy hierarchy) {
		if (this.values != null) {
			for (Holder<ElementSpec> spec : this.values.entryList()) {
				asMember(spec).create(hierarchy, this);
			}
		}
		super.createMembers(hierarchy);
	}

	@Override
	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {
		return_(
			invokeInstance(
				load("this", this.getTypeInfo()),
				new MethodInfo(
					ACC_PUBLIC,
					this.parent != null
						? asType(this.parent).getTypeInfo()
						: EnumBase.$CONSTRUCTOR_INFO.typeInfo,
					"<init>",
					TypeInfos.VOID,
					EnumBase.$CONSTRUCTOR_INFO.parameterTypeInfos
				),
				EnumBase.$CONSTRUCTOR_INFO.loaders
			)
		)
			.emitBytecode(this.primaryConstructor);
		if (this.values != null) {
			for (Holder<ElementSpec> spec : this.values.entryList()) {
				asMember(spec).compile(hierarchy, this);
			}
		}
		super.compileMembers(hierarchy);
		for (TrackedField field : this.getOverrideTracker().fields.values()) {
			FieldSpec spec = (FieldSpec)(field.declaration().value());
			FieldCompileContext context = this.getCompileContext(spec);
			TypeInfo fieldType = asType(spec.field_type).getTypeInfo();
			InsnTree getFromSupplier = InsnTrees.invokeInstance(
				load("constants", EnumConstantSupplier.INFO.type),
				switch (fieldType.getSort()) {
					case VOID -> throw new IllegalStateException("Void-typed field");
					case BOOLEAN -> EnumConstantSupplier.INFO.getZ;
					case BYTE -> EnumConstantSupplier.INFO.getB;
					case CHAR -> EnumConstantSupplier.INFO.getC;
					case SHORT -> EnumConstantSupplier.INFO.getS;
					case INT -> EnumConstantSupplier.INFO.getI;
					case LONG -> EnumConstantSupplier.INFO.getL;
					case FLOAT -> EnumConstantSupplier.INFO.getF;
					case DOUBLE -> EnumConstantSupplier.INFO.getD;
					case OBJECT, ARRAY -> EnumConstantSupplier.INFO.getA;
				},
				ldc(this.name)
			);
			if (fieldType.isObject()) {
				getFromSupplier = new DirectCastInsnTree(getFromSupplier, fieldType, false);
			}
			putField(
				load("this", this.getTypeInfo()),
				context.info,
				getFromSupplier
			)
				.emitBytecode(this.primaryConstructor);
		}
		if (this.values != null) {
			new NewArrayWithContentsInsnTree(
				this.getTypeInfo(),
				this
					.values
					.objectList()
					.stream()
					.map(EnumValueSpec.class::cast)
					.map(this::getCompileContext)
					.map(FieldCompileContext.class::cast)
					.map((FieldCompileContext context) -> getStatic(context.info))
					.toArray(InsnTree.ARRAY_FACTORY)
			)
				.emitBytecode(this.staticInitializer);
			//array on stack.
			this.staticInitializer.node.visitInsn(DUP);
			EnumBase.$INFO.$createSet.emitBytecode(this.staticInitializer);
			this.valueSet.info.emitPut(this.staticInitializer);
			EnumBase.$INFO.$createMap.emitBytecode(this.staticInitializer);
			this.valueMap.info.emitPut(this.staticInitializer);
			this.staticInitializer.node.visitInsn(RETURN);
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
		}
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn) throws ConstantFormatException {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		String name = BuiltinTypeSpec.asString(data).value;
		if (this.members.objectStream().filter(EnumValueSpec.class::isInstance).map(ElementSpec::name).noneMatch(name::equals)) {
			throw new ConstantFormatException("Unknown enum " + name + " of type " + UnregisteredObjectException.getID(hierarchy.entryOf(this)));
		}
		return getStatic(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, this.getTypeInfo(), name, this.getTypeInfo());
	}
}