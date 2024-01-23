package builderb0y.bigglobe.columns.scripted;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.ObjectOps;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public record Valid(
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where,
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y,
	@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y,
	@UseCoder(name = "code", in = Valid.class, usage = MemberUsage.METHOD_IS_HANDLER)
	@VerifyNullable Object fallback
) {

	public boolean isUseful(boolean _3D) {
		return (
			_3D
			? (this.where != null || this.min_y != null || this.max_y != null)
			: (this.where != null)
		);
	}

	public ConstantValue getFallback(TypeInfo type) {
		return ConstantValue.of(this.fallback, type);
	}

	public static <T_Encoded> Object code(DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		return context.ops.convertTo(ObjectOps.INSTANCE, context.input);
	}

	public static <T_Encoded> T_Encoded code(EncodeContext<T_Encoded, Object> context) throws EncodeException {
		Object input = context.input;
		if (input == null) return context.empty();
		return ObjectOps.INSTANCE.convertTo(context.ops, input);
	}
}