package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted2.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FieldSpec extends BaseFieldSpec {

	@Override
	public ClassSpec owner(ClassHierarchy hierarchy) {
		return requireType(this.owner, ClassSpec.class, () -> hierarchy.idOf(this) + " > owner");
	}

	public final @VerifyNullable @UseName("default") ScriptUsage defaultValue;
	public final transient SetBasedMutableDependencyView dependencies = SetBasedMutableDependencyView.from(new HashSet<>());

	public FieldSpec(Holder<ElementSpec> owner, @IdentifierName String name, Holder<ElementSpec> field_type, @VerifyNullable ScriptUsage defaultValue) {
		super(owner, name, field_type);
		this.defaultValue = defaultValue;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), this.dependencies.streamDirectDependencies());
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		if (this.defaultValue != null) {
			this.context.defaultValueMethod = this.owner(hierarchy).classCompileContext.newMethod(
				ACC_PRIVATE | ACC_STATIC,
				"$default_" + ColumnCompileContext.internalName(hierarchy.idOf(this), this.owner(hierarchy).classCompileContext.memberUniquifier++),
				this.fieldType(hierarchy).getTypeInfo()
			);
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		if (this.defaultValue != null) {
			compile(
				hierarchy,
				this.context.defaultValueMethod,
				this.defaultValue,
				null,
				this.dependencies,
				NO_EXTRAS
			);
			InsnTree value = this.getDefaultValue(hierarchy);
			if (value != null) {
				BaseClassSpec owner = this.owner(hierarchy);
				putField(
					load("this", owner.getTypeInfo()),
					this.context.field.info,
					value
				)
				.emitBytecode(owner.primaryConstructor);
			}
		}
	}

	@Override
	public @Nullable InsnTree getDefaultValue(ClassHierarchy hierarchy) throws ConstantFormatException {
		if (this.defaultValue != null) {
			return invokeStatic(this.context.defaultValueMethod.info);
		}
		else {
			return null;
		}
	}

	@Override
	public int getAccessFlags() {
		return ACC_PUBLIC;
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.owner(hierarchy).overrideTracker.addField(this);
	}
}