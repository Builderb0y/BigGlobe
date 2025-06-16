package builderb0y.bigglobe.columns.scripted.classes;

import it.unimi.dsi.fastutil.Hash;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FieldSpec extends MemberSpec {

	public static final Hash.Strategy<FieldSpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), (FieldSpec field) -> field.type),
		FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> type;

	public FieldSpec(@IdentifierName String name, RegistryEntry<ElementSpec> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addField(this);
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		if (asType(this.type).getTypeInfo().isVoid()) {
			throw new CustomClassFormatException("Void-typed field: " );
		}
	}

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		super.create(hierarchy, owner);
		owner.setCompileContext(
			this,
			owner.classCompileContext.newField(
				ACC_PUBLIC,
				this.name,
				((TypeSpec)(this.type.value())).getTypeInfo()
			)
		);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller) {
		FieldCompileContext fieldCompileContext = owner.getCompileContext(this);
		environment.addFieldGet(fieldCompileContext.info);
		if (caller.info.extendsOrImplements(fieldCompileContext.clazz.info)) {
			environment.addVariableGetField(load("this", caller.info), fieldCompileContext.info);
		}
	}

	@Override
	public String name() {
		return this.name;
	}

	public void compile(ClassHierarchy hierarchy) {}

	@Override
	public String toString() {
		return "FieldSpec " + this.name + " [" + UnregisteredObjectException.getID(this.type) + ']';
	}
}