package builderb0y.bigglobe.columns.scripted.classes;

import it.unimi.dsi.fastutil.Hash;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FieldSpec extends MemberSpec {

	public static final Hash.Strategy<FieldSpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), (FieldSpec field) -> field.field_type),
		FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

	public final @IdentifierName String name;
	public final RegistryEntry<ElementSpec> field_type;
	public final @VerifyNullable @UseName("default") ScriptUsage defaultValue;

	public FieldSpec(@IdentifierName String name, RegistryEntry<ElementSpec> field_type, @VerifyNullable ScriptUsage defaultValue) {
		this.name = name;
		this.field_type = field_type;
		this.defaultValue = defaultValue;
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addNormalField(this);
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		if (asType(this.field_type).getTypeInfo().isVoid()) {
			throw new CustomClassFormatException("Void-typed field: " + this);
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
				((TypeSpec)(this.field_type.value())).getTypeInfo()
			)
		);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		FieldCompileContext fieldCompileContext = owner.getCompileContext(this);
		environment.addFieldGet(fieldCompileContext.info);
		if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(fieldCompileContext.clazz.info)) {
			environment.addVariableGetField(loadCustomClass, fieldCompileContext.info);
		}
	}

	@Override
	public String name() {
		return this.name;
	}

	public void compile(ClassHierarchy hierarchy) {}

	@Override
	public String toString() {
		return "FieldSpec " + this.name + " [" + UnregisteredObjectException.getID(this.field_type) + ']';
	}
}