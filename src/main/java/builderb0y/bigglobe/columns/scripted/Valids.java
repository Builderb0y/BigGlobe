package builderb0y.bigglobe.columns.scripted;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.*;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Valids {

	public static interface _2DValid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> where();

		public abstract ConstantValue getFallback(TypeInfo type);
	}

	public static interface _3DValid extends _2DValid {

		public abstract ScriptUsage<GenericScriptTemplateUsage> min_y();

		public abstract ScriptUsage<GenericScriptTemplateUsage> max_y();
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

	public static record Float2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultFloat(Float.NaN) float fallback) implements _2DValid, FloatValid {}
	public static record Double2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @DefaultDouble(Double.NaN) double fallback) implements _2DValid, DoubleValid {}
	public static record BlockState2DValid(ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable BlockState fallback) implements _2DValid {

		@Override
		public ConstantValue getFallback(TypeInfo type) {
			if (!type.extendsOrImplements(type(BlockState.class))) {
				throw new IllegalArgumentException("Expected BlockState, got " + type);
			}
			return ConstantValue.ofManual(this.fallback, type);
		}
	}
	public static record NullObject2DValid(ScriptUsage<GenericScriptTemplateUsage> where) implements _2DValid {

		@Override
		public ConstantValue getFallback(TypeInfo type) {
			return ConstantValue.of(null, type);
		}
	}

	public static record Float3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @DefaultFloat(Float.NaN) float fallback) implements _3DValid, FloatValid {}
	public static record BlockState3DValid(@VerifyNullable ScriptUsage<GenericScriptTemplateUsage> where, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> min_y, @VerifyNullable ScriptUsage<GenericScriptTemplateUsage> max_y, @VerifyNullable BlockState fallback) implements _3DValid {

		@Override
		public ConstantValue getFallback(TypeInfo type) {
			if (!type.extendsOrImplements(type(BlockState.class))) {
				throw new IllegalArgumentException("Expected BlockState, got " + type);
			}
			return ConstantValue.ofManual(this.fallback, type);
		}
	}
}