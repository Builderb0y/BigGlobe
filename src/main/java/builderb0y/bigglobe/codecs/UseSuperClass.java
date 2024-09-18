package builderb0y.bigglobe.codecs;

import java.lang.annotation.*;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoder;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoderFactory;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoderFactory;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

/**
when applied to type Sub and type Super is specified in {@link #value()},
the value will first be decoded with Super's decoder.
then, it will be cast to type Sub.
when encoding, Super's encoder will be used.

this annotation is intended to be used with {@link KeyDispatchCoder},
in cases where the KeyDispatchCoder can create a wide variety of subclasses,
but only some of them are applicable in certain contexts.
*/
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseSuperClass {

	public abstract Class<?> value();

	public static class Coder<T> extends NamedCoder<T> {

		public final AutoCoder<? super T> delegate;
		public final Class<T> subclass;

		@SuppressWarnings("unchecked")
		public Coder(@NotNull ReifiedType<T> handledType, AutoCoder<? super T> delegate) {
			super(handledType);
			this.delegate = delegate;
			this.subclass = (Class<T>)(handledType.requireRawClass());
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T_Encoded> @Nullable T decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			Object object = context.decodeWith(this.delegate);
			if (object == null || this.subclass.isInstance(object)) {
				return (T)(object);
			}
			else {
				throw new DecodeException(() -> (
					context
					.pathToStringBuilder()
					.append(" was decoded into an instance of ")
					.append(object.getClass().getName())
					.append(", but an instance of ")
					.append(this.subclass.getName())
					.append(" was expected.")
					.toString()
				));
			}
		}

		@Override
		@OverrideOnly
		@SuppressWarnings("unchecked")
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T> context) throws EncodeException {
			return context.encodeWith((AutoCoder<T>)(this.delegate));
		}

		public static class Factory extends NamedCoderFactory {

			public static final Factory INSTANCE = new Factory();

			@Override
			@OverrideOnly
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public <T_HandledType> @Nullable AutoCoder<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
				UseSuperClass annotation = context.type.getAnnotations().getFirst(UseSuperClass.class);
				if (annotation != null) {
					ReifiedType<?> ancestor = context.type.resolveAncestor(annotation.value());
					if (ancestor != null) {
						return new Coder(
							context.type,
							context.type(
								ancestor.addAnnotations(
									context
									.type
									.getAnnotations()
									.getAll((Annotation a) -> !a.equals(annotation))
								)
							)
							.forceCreateCoder()
						);
					}
					else {
						throw new FactoryException("Invalid usage of @UseSuperClass on type " + context.type);
					}
				}
				return null;
			}
		}
	}
}