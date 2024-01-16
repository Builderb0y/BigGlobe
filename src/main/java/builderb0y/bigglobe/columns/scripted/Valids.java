package builderb0y.bigglobe.columns.scripted;

import builderb0y.autocodec.annotations.*;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

public class Valids {

	public static interface _2DValid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> where();

		public abstract ConstantValue getFallback(TypeInfo type);
	}

	public static interface _3DValid extends _2DValid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> min_y();

		public abstract ScriptUsage<GenericScriptTemplateUsage> max_y();
	}

	public static interface IntValid extends _2DValid {

		public abstract int fallback();

		@Override
		public default ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(this.fallback());
		}
	}

	public static interface LongValid extends _2DValid {

		public abstract long fallback();

		@Override
		public default ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(this.fallback());
		}
	}

	public static interface FloatValid extends _2DValid {

		public abstract float fallback();

		@Override
		public default ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(this.fallback());
		}
	}

	public static interface DoubleValid extends _2DValid {

		public abstract double fallback();

		@Override
		public default ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(this.fallback());
		}
	}

	public static interface BooleanValid extends _2DValid {

		public abstract boolean fallback();

		@Override
		public default ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(this.fallback());
		}
	}

	public static record Int2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultInt(0) int fallback) implements _2DValid, IntValid {}
	public static record Long2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultLong(0L) long fallback) implements _2DValid, LongValid {}
	public static record Float2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultFloat(Float.NaN) float fallback) implements _2DValid, FloatValid {}
	public static record Double2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultDouble(Double.NaN) double fallback) implements _2DValid, DoubleValid {}
	public static record Boolean2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultBoolean(false) boolean fallback) implements _2DValid, BooleanValid {}
	public static record NullObject2DValid(ScriptUsage<GenericScriptTemplateUsage> where) implements _2DValid {

		@Override
		public ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(null, type);
		}
	}

	public static record Int3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultInt(0) int fallback) implements _3DValid, IntValid {}
	public static record Long3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultLong(0L) long fallback) implements _3DValid, LongValid {}
	public static record Float3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultFloat(Float.NaN) float fallback) implements _3DValid, FloatValid {}
	public static record Double3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultDouble(Double.NaN) double fallback) implements _3DValid, DoubleValid {}
	public static record Boolean3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultBoolean(false) boolean fallback) implements _3DValid, BooleanValid {}

}