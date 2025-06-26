package builderb0y.bigglobe.columns.scripted.classes;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.classes.BaseMethodSpec.MethodSpecDesc;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

public abstract class BaseClassSpec extends TypeSpec {

	public final @IdentifierName String name;
	public final @UseName("abstract") @DefaultBoolean(false) boolean isAbstract;
	public final @UseName("extends") @VerifyNullable RegistryEntry<ElementSpec> parent;
	public final DelayedEntryList<ElementSpec> members;
	public transient TypeInfo typeInfo;
	public transient ClassCompileContext classCompileContext;
	public transient Map<MemberSpec, Object> memberCompileContexts;
	public transient OverrideTracker overrideTracker;

	public BaseClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable RegistryEntry<ElementSpec> parent,
		DelayedEntryList<ElementSpec> members
	) {
		this.name = name;
		this.isAbstract = isAbstract;
		this.parent = parent;
		this.members = members;
		this.memberCompileContexts = new Reference2ReferenceOpenHashMap<>();
	}

	@SuppressWarnings("unchecked")
	public <T> T getCompileContext(MemberSpec spec) {
		return (T)(this.memberCompileContexts.get(spec));
	}

	public void setCompileContext(MemberSpec spec, Object value) {
		this.memberCompileContexts.put(spec, value);
	}

	@Override
	public TypeInfo getTypeInfo() {
		if (this.typeInfo == null) {
			throw new IllegalStateException("Must progress to CREATE_TYPE_INFO before type info can be queried!");
		}
		return this.typeInfo;
	}

	@Override
	public @Nullable OverrideTracker getOverrideTracker() {
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

	public abstract FieldInfo baseColumnField();

	public TypeInfo defaultSuperClass() {
		return this.baseColumnField().owner;
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
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<RegistryEntry<ElementSpec>> cyclicDetector) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = hierarchy.entryOf(this);
		if (cyclicDetector.add(entry)) try {
			TypeInfo superClass;
			if (this.parent != null) {
				TypeSpec superSpec = asType(this.parent);
				if (superSpec.canProgressTo(CompileState.CREATE_TYPE_INFO)) {
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
				Type.getObjectType(ObjectBase.INFO.type.getInternalName() + '$' + this.name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement()),
				superClass,
				TypeInfo.ARRAY_FACTORY.empty(),
				false
			);
		}
		finally {
			cyclicDetector.remove(entry);
		}
		else {
			throw new CustomClassFormatException("Cyclic inheritance chain: " + cyclicDetector.stream().dropWhile((RegistryEntry<ElementSpec> e) -> e != entry).map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(" -> ")) + " -> " + UnregisteredObjectException.getID(entry));
		}
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws CustomClassFormatException {
		if (this.parent != null && asType(this.parent).isFinal()) {
			throw new CustomClassFormatException("Class " + UnregisteredObjectException.getID(this.parent) + " cannot be extended.");
		}
		for (RegistryEntry<ElementSpec> element : this.members.entryList()) {
			MemberSpec member = asMember(element);
			member.verify(hierarchy, this);
			member.track(this.overrideTracker);
		}
		boolean isAbstract = this.overrideTracker.hasAnyAbstractMethods();
		if (this.isAbstract && !isAbstract) {
			BigGlobeMod.LOGGER.warn("Custom class " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " is marked as abstract, but has no abstract methods. This may be a mistake.");
		}
		else if (!this.isAbstract && isAbstract) {
			throw new CustomClassFormatException("Custom class " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " is not marked as abstract, but contains or inherits abstract members: " + this.overrideTracker.getAbstractMembers().map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(", ", "[", "]")));
		}
	}

	@Override
	public void createClass(ClassHierarchy hierarchy) {
		this.classCompileContext = new ClassCompileContext(
			this.isAbstract ? ACC_PUBLIC | ACC_ABSTRACT : ACC_PUBLIC,
			this.typeInfo
		);
	}

	@Override
	public void createMembers(ClassHierarchy hierarchy) {
		for (RegistryEntry<ElementSpec> member : this.members.entryList()) {
			asMember(member).create(hierarchy, this);
		}
	}

	@Override
	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {
		for (RegistryEntry<ElementSpec> member : this.members.entryList()) {
			asMember(member).compile(hierarchy, this);
		}
	}

	@Override
	public void link(ScriptClassLoader loader) {
		super.link(loader);
		loader.recursiveAddClasses(this.classCompileContext, DUMP_DIRECTORY, null);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, ClassCompileContext caller) {
		super.setupEnvironment(environment, caller);
		for (RegistryEntry<ElementSpec> member : this.members.entryList()) {
			asMember(member).setupEnvironment(environment, this, caller);
		}
	}
}