package builderb0y.bigglobe.codecs.registries;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.StringData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class RegistryEntryCoder<T> extends AbstractRegistryCoder<T, Holder<T>> {

	public final @Nullable AutoCoder<T> inlineCoder;

	public RegistryEntryCoder(@NotNull ReifiedType<Holder<T>> handledType, ResourceKey<Registry<T>> key, @Nullable AutoCoder<T> coder) {
		super(handledType, key);
		this.inlineCoder = coder;
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable Holder<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		if (context.isString()) return this.registry(context).requireById(BigGlobeAutoCodec.toID(context.forceAsString().value, this.key.identifier().getNamespace()));
		if (this.inlineCoder != null) return Holder.direct(context.decodeWith(this.inlineCoder));
		throw new DecodeException(() -> context.pathToStringBuilder().append(" is not a string, and inline definitions are not allowed.").toString());
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, Holder<T>> context) throws EncodeException {
		Holder<T> entry = context.object;
		if (entry == null) return EmptyData.INSTANCE;
		ResourceKey<T> key = entry.unwrapKey().orElse(null);
		if (key != null) return new StringData(BigGlobeAutoCodec.toString(key.identifier(), this.key.identifier().getNamespace()));
		if (this.inlineCoder != null) return context.object(entry.value()).encodeWith(this.inlineCoder);
		throw new EncodeException(() -> "Unregistered object and inline definitions are not allowed: " + entry);
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Inlinable {}

	public static class Factory extends NamedCoderFactory {

		public static final Factory INSTANCE = new Factory();

		public final Map<ReifiedType<?>, ResourceKey<? extends Registry<?>>> keys = new HashMap<>();

		public <T> void register(ReifiedType<T> type, ResourceKey<Registry<T>> key) {
			this.keys.put(type, key);
		}

		@Override
		@OverrideOnly
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T_HandledType> @Nullable AutoCoder<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
			ReifiedType<?> objectType = context.type.resolveParameter(Holder.class);
			if (objectType != null) {
				ResourceKey<? extends Registry<?>> key = this.keys.get(objectType);
				if (key == null) throw new FactoryException("Missing registry key for type RegistryEntry<" + objectType + '>');
				AutoCoder<?> inlineCoder = null;
				if (context.type.getAnnotations().has(Inlinable.class)) {
					inlineCoder = context.type(objectType).forceCreateCoder();
				}
				return new RegistryEntryCoder(context.type, key, inlineCoder);
			}
			return null;
		}
	}
}