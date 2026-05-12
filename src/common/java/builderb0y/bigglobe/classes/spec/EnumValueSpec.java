package builderb0y.bigglobe.classes.spec;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.OverrideTracker;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import net.minecraft.core.Holder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EnumValueSpec extends MemberSpec {

	public final Holder<ElementSpec> enum_type;
	public final @IdentifierName String name;

	public EnumValueSpec(Holder<ElementSpec> enum_type, @IdentifierName String name) {
		this.enum_type = enum_type;
		this.name = name;
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		super.verify(hierarchy, owner);
		owner.checkEnumField(hierarchy, this);
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addEnumField(this);
	}

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		super.create(hierarchy, owner);
		owner.setCompileContext(
			this,
			owner.classCompileContext.newField(
				ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
				this.name,
				asType(this.enum_type).getTypeInfo()
			)
		);
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {
		super.compile(hierarchy, owner);
		FieldCompileContext context = owner.getCompileContext(this);
		putStatic(
			context.info,
			newInstance(
				((EnumClassSpec)(this.enum_type)).primaryConstructor.info,
				ldc(this.name)
			)
		)
			.emitBytecode(((EnumClassSpec)(owner)).staticInitializer);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		environment.addQualifiedVariableGetStatic(owner.getTypeInfo(), this.name, owner.<FieldCompileContext>getCompileContext(this).info);
	}

	@Override
	public String toString() {
		return "EnumValueSpec " + this.name + " [" + UnregisteredObjectException.getID(this.enum_type) + ']';
	}

	@Override
	public String name() {
		return this.name;
	}
}