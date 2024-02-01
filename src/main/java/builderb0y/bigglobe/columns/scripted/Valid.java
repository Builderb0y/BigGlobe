package builderb0y.bigglobe.columns.scripted;

import com.mojang.datafixers.util.Unit;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.Any;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public record Valid(
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y,
	@VerifyNullable @Any Object fallback
) {

	public boolean isUseful(boolean _3D) {
		return (
			_3D
			? (this.where != null || this.min_y != null || this.max_y != null)
			: (this.where != null)
		);
	}

	public ConstantValue getFallback(TypeInfo type) {
		return ConstantValue.of(this.fallback == Unit.INSTANCE ? null : this.fallback, type);
	}
}