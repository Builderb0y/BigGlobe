package builderb0y.bigglobe.columns.scripted2;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.input.ScriptUsage;
import net.minecraft.core.Holder;

public record Valid(
	@VerifyNullable ScriptUsage where,
	@VerifyNullable ScriptUsage min_y,
	@VerifyNullable ScriptUsage max_y,
	Data fallback
) {

	public boolean isUseful(boolean _3D) {
		return (
			_3D
				? (this.where != null || this.min_y != null || this.max_y != null)
				: (this.where != null)
		);
	}

	public InsnTree getFallback(ClassHierarchy hierarchy, TypeSpec type, InsnTree loadColumn) {
		try {
			return type.parseConstant(hierarchy, this.fallback, loadColumn);
		}
		catch (ConstantFormatException e) {
			throw new RuntimeException(e);
		}
	}

	public InsnTree getFallback(ClassHierarchy hierarchy, Holder<ElementSpec> type, InsnTree loadColumn) {
		return this.getFallback(hierarchy, ElementSpec.asType(type), loadColumn);
	}
}