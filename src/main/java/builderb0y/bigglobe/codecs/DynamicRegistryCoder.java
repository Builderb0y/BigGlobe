package builderb0y.bigglobe.codecs;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.registry.BetterRegistry;

public class DynamicRegistryCoder<T> implements AutoCoder<@NotNull BetterRegistry<T>> {

	public static final WeakHashMap<RegistryOps<?>, Map<RegistryKey<? extends Registry<?>>, BetterRegistry<?>>> CACHE = new WeakHashMap<>(1);
	public final RegistryKey<Registry<T>> key;

	public DynamicRegistryCoder(RegistryKey<Registry<T>> key) {
		this.key = key;
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BetterRegistry<T>> context) throws EncodeException {
		return context.emptyMap();
	}

	@Override
	public <T_Encoded> @NotNull BetterRegistry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return this.getRegistry(context.ops, DecodeException::new);
	}

	@SuppressWarnings("unchecked")
	public <X extends Throwable> @NotNull BetterRegistry<T> getRegistry(DynamicOps<?> ops, Function<String, X> exceptionFactory) throws X {
		if (ops instanceof RegistryOps<?> registryOps) {
			return (BetterRegistry<T>)(
				CACHE
				.computeIfAbsent(registryOps, $ -> new IdentityHashMap<>(32))
				.computeIfAbsent(this.key, key -> {
					try {
						return new BetterRegistry<>(this.key, this.getRegistryWrapper(registryOps, exceptionFactory));
					}
					catch (Throwable throwable) {
						throw AutoCodecUtil.rethrow(throwable);
					}
				})
			);
		}
		else {
			throw exceptionFactory.apply("Not a RegistryOps: " + ops);
		}
	}

	@SuppressWarnings("unchecked")
	public <X extends Throwable> @NotNull RegistryWrapper<T> getRegistryWrapper(RegistryOps<?> registryOps, Function<String, X> exceptionFactory) throws X {
		if (registryOps.getEntryLookup(this.key).orElse(null) instanceof RegistryWrapper<T> wrapper) {
			return wrapper;
		}
		if (registryOps.getOwner(this.key).orElse(null) instanceof RegistryWrapper<?> wrapper) {
			return (RegistryWrapper<T>)(wrapper);
		}
		throw exceptionFactory.apply("Unable to access registry " + this.key.getValue() + " in " + registryOps);
	}
}