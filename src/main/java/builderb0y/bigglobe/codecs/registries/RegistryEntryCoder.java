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

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class RegistryEntryCoder<T> extends AbstractRegistryCoder<T, RegistryEntry<T>> {

	public final @Nullable AutoCoder<T> inlineCoder;

	public RegistryEntryCoder(@NotNull ReifiedType<RegistryEntry<T>> handledType, RegistryKey<Registry<T>> key, @Nullable AutoCoder<T> coder) {
		super(handledType, key);
		this.inlineCoder = coder;
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		if (context.isString()) return this.registry(context).getByName(context.forceAsString());
		if (this.inlineCoder != null) return RegistryEntry.of(context.decodeWith(this.inlineCoder));
		throw new DecodeException(() -> context.pathToStringBuilder().append(" is not a string, and inline definitions are not allowed.").toString());
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<T>> context) throws EncodeException {
		RegistryEntry<T> entry = context.object;
		if (entry == null) return context.empty();
		RegistryKey<T> key = entry.getKey().orElse(null);
		if (key != null) return context.createString(key.getValue().toString());
		if (this.inlineCoder != null) return context.object(entry.value()).encodeWith(this.inlineCoder);
		throw new EncodeException(() -> "Unregistered object and inline definitions are not allowed: " + entry);
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Inlinable {}

	public static class Factory extends NamedCoderFactory {

		public static final Factory INSTANCE = new Factory();

		public final Map<ReifiedType<?>, RegistryKey<? extends Registry<?>>> keys = new HashMap<>();

		public <T> void register(ReifiedType<T> type, RegistryKey<Registry<T>> key) {
			this.keys.put(type, key);
		}

		@Override
		@OverrideOnly
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <T_HandledType> @Nullable AutoCoder<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
			ReifiedType<?> objectType = context.type.resolveParameter(RegistryEntry.class);
			if (objectType != null) {
				RegistryKey<? extends Registry<?>> key = this.keys.get(objectType);
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