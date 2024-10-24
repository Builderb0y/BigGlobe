package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mojang.datafixers.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.ObjectOps;

/**
when applied to type Object, indicates that the decoded
object is allowed to be "any" kind of object. hence the name.
intended for cases where you need to store raw JSON data for further processing later.
*/
@Mirror(UseCoder.class)
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@UseCoder(name = "CODER", in = Any.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public @interface Any {

	public static final AutoCoder<Object> CODER = new NamedCoder<>("Any.CODER") {

		@Override
		public @Nullable <T_Encoded> Object decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return Unit.INSTANCE;
			return context.ops.convertTo(ObjectOps.INSTANCE, context.input);
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, Object> context) throws EncodeException {
			Object object = context.object;
			if (object == null) return context.empty();
			return ObjectOps.INSTANCE.convertTo(context.ops, object);
		}
	};
}