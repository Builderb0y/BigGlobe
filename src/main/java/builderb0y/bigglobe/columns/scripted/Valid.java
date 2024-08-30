package builderb0y.bigglobe.columns.scripted;

import com.mojang.datafixers.util.Unit;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.Any;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptUsage;

public record Valid(
	@VerifyNullable ScriptUsage where,
	@VerifyNullable ScriptUsage min_y,
	@VerifyNullable ScriptUsage max_y,
	@VerifyNullable @Any Object fallback
) {

	public boolean isUseful(boolean _3D) {
		return (
			_3D
			? (this.where != null || this.min_y != null || this.max_y != null)
			: (this.where != null)
		);
	}

	public InsnTree getFallback(ColumnValueType type, ColumnCompileContext context) {
		return type.createConstant(this.fallback == Unit.INSTANCE ? null : this.fallback, context);
	}
}