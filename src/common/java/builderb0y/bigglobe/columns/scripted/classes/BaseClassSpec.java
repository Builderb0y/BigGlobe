package builderb0y.bigglobe.columns.scripted.classes;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.classes.BasePropertySpec.PropertyCompileContext;
import builderb0y.bigglobe.columns.scripted.classes.OverrideTracker.TrackedField;
import builderb0y.bigglobe.columns.scripted.classes.OverrideTracker.TrackedProperty;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.bytecode.tree.instructions.update.ReceiverObjectUpdaterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class BaseClassSpec extends TypeSpec {

	public final @IdentifierName String name;
	public final @UseName("abstract")
	@DefaultBoolean(false) boolean isAbstract;
	public final @UseName("extends")
	@VerifyNullable Holder<ElementSpec> parent;
	public final DelayedEntryList<ElementSpec> members;
	public transient TypeInfo typeInfo;
	public transient ClassCompileContext classCompileContext;
	public transient MethodCompileContext primaryConstructor;
	public transient MutableDependencyView primaryConstructorDependencies;
	public transient Map<MemberSpec, Object> memberCompileContexts;
	public transient OverrideTracker overrideTracker;

	public BaseClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent,
		DelayedEntryList<ElementSpec> members
	) {
		this.name = name;
		this.isAbstract = isAbstract;
		this.parent = parent;
		this.members = members;
		this.memberCompileContexts = new Reference2ReferenceOpenHashMap<>();
		Set<Holder<? extends DependencyView>> primaryConstructorDependencies = new HashSet<>();
		this.primaryConstructorDependencies = (SetBasedMutableDependencyView)(() -> primaryConstructorDependencies);
	}

	public void checkField(ClassHierarchy hierarchy, FieldSpec spec) throws CustomClassFormatException {
	}

	public void checkProperty(ClassHierarchy hierarchy, BasePropertySpec spec) throws CustomClassFormatException {
	}

	public void checkMethod(ClassHierarchy hierarchy, BaseMethodSpec spec) throws CustomClassFormatException {
	}

	public void checkConstructor(ClassHierarchy hierarchy, ConstructorSpec spec) throws CustomClassFormatException {
		if (this.isAbstract) {
			throw new CustomClassFormatException("Can't add constructor " + hierarchy.idOf(spec) + " to abstract class " + hierarchy.idOf(this));
		}
	}

	public void checkEnumField(ClassHierarchy hierarchy, EnumValueSpec spec) throws CustomClassFormatException {
		throw new CustomClassFormatException("Can't add enum field " + hierarchy.idOf(spec) + " to class " + hierarchy.idOf(this));
	}

	@SuppressWarnings("unchecked")
	public <T> T getCompileContext(MemberSpec spec) {
		return (T)(this.memberCompileContexts.get(spec));
	}

	public void setCompileContext(MemberSpec spec, Object value) {
		this.memberCompileContexts.put(spec, value);
	}

	public void applyDefaultFields(ClassHierarchy hierarchy, LoadInsnTree loadSelf) throws ScriptParsingException {
		for (TrackedField trackedField : this.overrideTracker.fields.values()) {
			FieldSpec fieldSpec = (FieldSpec)(trackedField.declaration().value());
			if (fieldSpec.defaultValue != null) {
				putField(
					loadSelf,
					this.<FieldCompileContext>getCompileContext(fieldSpec).info,
					scoped(
						hierarchy.registry.parseCode(
							this.primaryConstructor,
							fieldSpec.defaultValue,
							null,
							null,
							loadSelf,
							this.primaryConstructorDependencies,
							MemberSpec.NO_EXTRAS
						)
					)
				)
					.emitBytecode(this.primaryConstructor);
			}
		}
	}

	public InsnTree applyFields(ClassHierarchy hierarchy, InsnTree loadColumn, MapData map, InsnTree result) throws ConstantFormatException {
		for (Map.Entry<Data, Data> entry : map.value.entrySet()) {
			StringData name = entry.getKey().tryAsString();
			if (name == null) throw new IllegalArgumentException("Field or property name is non-string: " + entry.getKey());
			TrackedField field = this.overrideTracker.fields.get(name.value);
			if (field != null) {
				FieldSpec fieldSpec = (FieldSpec)(field.declaration().value());
				FieldCompileContext context = this.getCompileContext(fieldSpec);
				InsnTree fieldConstant = asType(fieldSpec.field_type).parseConstant(hierarchy, entry.getValue(), loadColumn);
				result = new ReceiverObjectUpdaterInsnTree(
					CombinedMode.VOID_ASSIGN,
					ObjectUpdaterEmitters.forField(
						result,
						context.info,
						fieldConstant
					)
				);
				continue;
			}
			TrackedProperty property = this.overrideTracker.properties.get(name.value);
			if (property != null) {
				BasePropertySpec propertySpec = (BasePropertySpec)(property.declaration().value());
				if (!propertySpec.isSettable()) {
					throw new IllegalArgumentException("Property " + name.value + " is not settable");
				}
				PropertyCompileContext context = this.getCompileContext(propertySpec);
				InsnTree propertyConstant = asType(propertySpec.getPropertyType()).parseConstant(hierarchy, entry.getValue(), loadColumn);
				result = new ReceiverObjectUpdaterInsnTree(
					CombinedMode.VOID_ASSIGN,
					ObjectUpdaterEmitters.forGetterSetter(
						result,
						context.get.info,
						context.set.info,
						propertyConstant
					)
				);
				continue;
			}
			throw new IllegalArgumentException("Can't find field or property named " + name.value + " in class " + hierarchy.idOf(this));
		}
		return result;
	}

	@Override
	public TypeInfo getTypeInfo() {
		if (this.typeInfo == null) {
			throw new IllegalStateException("Must progress to CREATE_TYPE_INFO before type info can be queried!");
		}
		return this.typeInfo;
	}

	@Override
	public @NotNull OverrideTracker getOverrideTracker() {
		if (this.overrideTracker == null) {
			throw new IllegalStateException("Must progress to CREATE_TYPE_INFO before override tracker can be queried!");
		}
		return this.overrideTracker;
	}

	@Override
	public boolean isFinal() {
		return false;
	}

	@Override
	public String name() {
		return this.name;
	}

	public abstract TypeInfo defaultSuperClass();

	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		this.overrideTracker.addReservedMethod("getClass");
		this.overrideTracker.addReservedMethod("clone");
		this.overrideTracker.addReservedMethod("notify");
		this.overrideTracker.addReservedMethod("notifyAll");
		this.overrideTracker.addReservedMethod("wait");
		this.overrideTracker.addReservedMethod("wait", TypeInfos.LONG);
		this.overrideTracker.addReservedMethod("wait", TypeInfos.LONG, TypeInfos.INT);
		this.overrideTracker.addReservedMethod("finalize");
	}

	@Override
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<Holder<ElementSpec>> cyclicDetector) throws CustomClassFormatException {
		Holder<ElementSpec> entry = hierarchy.entryOf(this);
		if (cyclicDetector.add(entry)) try {
			TypeInfo superClass;
			if (this.parent != null) {
				TypeSpec superSpec = asType(this.parent);
				if (superSpec.canProgressTo(CompileStep.CREATE_TYPE_INFO)) {
					superSpec.createTypeInfo(hierarchy, cyclicDetector);
				}
				superClass = superSpec.getTypeInfo();
				this.overrideTracker = new OverrideTracker(hierarchy, hierarchy.entryOf(this), superSpec.getOverrideTracker());
			}
			else {
				superClass = this.defaultSuperClass();
				this.overrideTracker = new OverrideTracker(hierarchy, hierarchy.entryOf(this));
				this.addReservedMembers();
			}
			this.typeInfo = TypeInfo.makeClass(
				Type.getObjectType(ScriptObject.TYPE.getInternalName() + '$' + this.name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement()),
				superClass,
				TypeInfo.ARRAY_FACTORY.empty(),
				false
			);
		}
		finally {
			cyclicDetector.remove(entry);
		}
		else {
			throw new CustomClassFormatException("Cyclic inheritance chain: " + cyclicDetector.stream().dropWhile((Holder<ElementSpec> e) -> e != entry).map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(" -> ")) + " -> " + UnregisteredObjectException.getID(entry));
		}
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws CustomClassFormatException {
		if (this.parent != null && asType(this.parent).isFinal()) {
			throw new CustomClassFormatException("Class " + hierarchy.idOf(this) + " cannot extend " + UnregisteredObjectException.getID(this.parent));
		}
		for (Holder<ElementSpec> element : this.members.entryList()) {
			MemberSpec member = asMember(element);
			member.verify(hierarchy, this);
			member.track(this.overrideTracker);
		}
		boolean isAbstract = this.overrideTracker.hasAnyAbstractMethods();
		if (this.isAbstract && !isAbstract) {
			BigGlobeMod.LOGGER.warn("Custom class " + hierarchy.idOf(this) + " is marked as abstract, but has no abstract methods. This may be a mistake.");
		}
		else if (!this.isAbstract && isAbstract) {
			throw new CustomClassFormatException("Custom class " + hierarchy.idOf(this) + " is not marked as abstract, but contains or inherits abstract members: " + this.overrideTracker.getAbstractMembers().map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(", ", "[", "]")));
		}
	}

	@Override
	public void createClass(ClassHierarchy hierarchy) {
		this.classCompileContext = new ClassCompileContext(
			this.isAbstract ? ACC_PUBLIC | ACC_ABSTRACT | ACC_SUPER : ACC_PUBLIC | ACC_SUPER,
			this.getTypeInfo()
		);
	}

	@Override
	public void createMembers(ClassHierarchy hierarchy) {
		for (Holder<ElementSpec> member : this.members.entryList()) {
			asMember(member).create(hierarchy, this);
		}
	}

	@Override
	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {
		for (Holder<ElementSpec> member : this.members.entryList()) {
			asMember(member).compile(hierarchy, this);
		}
	}

	@Override
	public void link(ScriptClassLoader loader) {
		loader.recursiveAddClasses(this.classCompileContext, DUMP_DIRECTORY, null);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, @Nullable InsnTree loadCustomClass) {
		super.setupEnvironment(environment, loadCustomClass);
		for (Holder<ElementSpec> member : this.members.entryList()) {
			asMember(member).setupEnvironment(environment, this, loadCustomClass);
		}
	}
}