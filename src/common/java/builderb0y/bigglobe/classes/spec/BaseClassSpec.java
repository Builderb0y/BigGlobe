package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.compile.OverrideTracker;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.Grouper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.TypeHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class BaseClassSpec extends TypeSpec {

	public final @IdentifierName String name;
	public final @UseName("abstract") @DefaultBoolean(false) boolean isAbstract;
	public final @UseName("extends") @VerifyNullable Holder<ElementSpec> parent;
	public BaseClassSpec parent(ClassHierarchy hierarchy) {
		return requireType(this.parent, BaseClassSpec.class, () -> hierarchy.idOf(this) + " > extends");
	}

	public transient TypeInfo typeInfo;
	public transient ClassCompileContext classCompileContext;
	public transient MethodCompileContext primaryConstructor;
	public transient MutableDependencyView primaryConstructorDependencies;
	public transient OverrideTracker overrideTracker;
	public transient Set<Holder<ElementSpec>> subTypes;
	public transient Set<Holder<ElementSpec>> members;

	public <T extends MemberSpec> Stream<T> getMembers(Class<T> clazz, boolean inherit) {
		Stream<T> stream = Grouper.filterByClass(this.members.stream().map(Holder<ElementSpec>::value), clazz);
		if (inherit && this.parent != null) stream = Stream.concat(((BaseClassSpec)(this.parent.value())).getMembers(clazz, true), stream);
		return stream;
	}

	public BaseClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent
	) {
		this.name = name;
		this.isAbstract = isAbstract;
		this.parent = parent;
		Set<Holder<? extends DependencyView>> primaryConstructorDependencies = new HashSet<>();
		this.primaryConstructorDependencies = (SetBasedMutableDependencyView)(() -> primaryConstructorDependencies);
		this.subTypes = new HashSet<>();
		this.members = new HashSet<>();
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.ofNullable(this.parent);
	}

	@Override
	public void tryProgressTo(CompileStep step, ClassHierarchy context) throws DetailedException {
		super.tryProgressTo(step, context);
		if (step != CompileStep.REFERENCE) {
			context.catchAll(this.subTypes, step);
		}
	}

	/*
	public InsnTree applyFields(ClassHierarchy hierarchy, MapData map, InsnTree result) throws ConstantFormatException {
		for (Map.Entry<Data, Data> entry : map.value.entrySet()) {
			StringData name = entry.getKey().tryAsString();
			if (name == null) throw new IllegalArgumentException("Field or property name is non-string: " + entry.getKey());
			TrackedField field = this.overrideTracker.fields.get(name.value);
			if (field != null) {
				FieldSpec fieldSpec = (FieldSpec)(field.declaration().value());
				FieldCompileContext context = this.getCompileContext(fieldSpec);
				InsnTree fieldConstant = asType(fieldSpec.field_type).parseConstant(hierarchy, entry.getValue());
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
				InsnTree propertyConstant = asType(propertySpec.getPropertyType()).parseConstant(hierarchy, entry.getValue());
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
	*/

	@Override
	public TypeInfo getTypeInfo() {
		if (this.typeInfo == null) {
			throw new IllegalStateException("Must progress to RESOLVE before type info can be queried!");
		}
		return this.typeInfo;
	}

	public @NotNull OverrideTracker getOverrideTracker() {
		if (this.overrideTracker == null) {
			throw new IllegalStateException("Must progress to VERIFY before override tracker can be queried!");
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

	public TypeInfo getParentTypeInfo(ClassHierarchy hierarchy) {
		return this.parent != null ? this.parent(hierarchy).getTypeInfo() : this.defaultSuperClass();
	}

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
	@MustBeInvokedByOverriders
	public void reference(ClassHierarchy hierarchy) throws DetailedException {
		super.reference(hierarchy);
		if (this.parent != null) {
			this.parent(hierarchy).subTypes.add(hierarchy.entryOf(this));
		}
		else {
			hierarchy.rootTypes.add(hierarchy.entryOf(this));
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<Holder<ElementSpec>> cyclicDetector) throws DetailedException {
		super.createTypeInfo(hierarchy, cyclicDetector);
		Holder<ElementSpec> entry = hierarchy.entryOf(this);
		if (cyclicDetector.add(entry)) try {
			TypeInfo superClass = this.getParentTypeInfo(hierarchy);
			this.typeInfo = TypeInfo.makeClass(
				Type.getObjectType(this.defaultSuperClass().getInternalName() + '$' + this.name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement()),
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
		hierarchy.catchAll(this.members, CompileStep.RESOLVE);
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		if (this.parent != null && this.parent(hierarchy).isFinal()) {
			throw new CustomClassFormatException("Class " + hierarchy.idOf(this) + " cannot extend " + UnregisteredObjectException.getID(this.parent));
		}
		if (this.parent != null) {
			this.overrideTracker = new OverrideTracker(hierarchy, hierarchy.entryOf(this), this.parent(hierarchy).getOverrideTracker());
		}
		else {
			this.overrideTracker = new OverrideTracker(hierarchy, hierarchy.entryOf(this));
			this.addReservedMembers();
		}
		hierarchy.catchAll(this.members, CompileStep.VERIFY);
		boolean isAbstract = this.overrideTracker.hasAnyAbstractMethods();
		if (this.isAbstract && !isAbstract) {
			BigGlobeMod.LOGGER.warn("Custom class " + hierarchy.idOf(this) + " is marked as abstract, but has no abstract methods. This may be a mistake.");
		}
		else if (!this.isAbstract && isAbstract) {
			throw new CustomClassFormatException("Custom class " + hierarchy.idOf(this) + " is not marked as abstract, but contains or inherits abstract members: " + this.overrideTracker.getAbstractMembers().map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(", ", "[", "]")));
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.classCompileContext = new ClassCompileContext(
			this.isAbstract ? ACC_PUBLIC | ACC_ABSTRACT | ACC_SUPER : ACC_PUBLIC | ACC_SUPER,
			this.typeInfo
		);
		hierarchy.catchAll(this.members, CompileStep.REPRESENT);
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		hierarchy.catchAll(this.members, CompileStep.COMPILE);
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		environment.addType(this.name(), params.dependencyCallback(self), this.getTypeInfo());
	}

	public void define(ScriptClassLoader loader) {
		loader.recursiveAddClasses(this.classCompileContext, DUMP_DIRECTORY, null);
		for (Holder<ElementSpec> subType : this.subTypes) {
			((BaseClassSpec)(subType.value())).define(loader);
		}
	}
}