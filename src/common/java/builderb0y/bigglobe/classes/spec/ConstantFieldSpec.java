package builderb0y.bigglobe.classes.spec;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantFieldSpec extends BaseFieldSpec {

	@Override
	public EnumClassSpec owner(ClassHierarchy hierarchy) {
		return requireType(this.owner, EnumClassSpec.class, () -> hierarchy.idOf(this) + " > owner");
	}

	public final @VerifyNullable @UseName("default") Data defaultValue;

	public ConstantFieldSpec(
		Holder<ElementSpec> owner,
		@IdentifierName String name,
		Holder<ElementSpec> field_type,
		@VerifyNullable Data defaultValue
	) {
		super(owner, name, field_type);
		this.defaultValue = defaultValue;
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		EnumClassSpec owner = this.owner(hierarchy);
		putField(
			load("this", owner.getTypeInfo()),
			this.context.field.info,
			load(this.name, this.fieldType(hierarchy).getTypeInfo())
		)
		.emitBytecode(owner.primaryConstructor);
	}

	@Override
	public @Nullable InsnTree getDefaultValue(ClassHierarchy hierarchy) throws DetailedException {
		return this.fieldType(hierarchy).parseConstant(hierarchy, this.defaultValue);
	}

	@Override
	public int getAccessFlags() {
		return ACC_PUBLIC | ACC_FINAL;
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.owner(hierarchy).overrideTracker.addField(this);
	}
}