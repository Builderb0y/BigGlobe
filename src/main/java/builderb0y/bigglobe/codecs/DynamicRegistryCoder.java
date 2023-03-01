package builderb0y.bigglobe.codecs;

import java.util.function.Function;

import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class DynamicRegistryCoder<T> implements AutoCoder<Registry<T>> {

	public final RegistryKey<Registry<T>> key;

	public DynamicRegistryCoder(RegistryKey<Registry<T>> key) {
		this.key = key;
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, Registry<T>> context) throws EncodeException {
		return context.emptyMap();
	}

	@Override
	public <T_Encoded> @Nullable Registry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return this.getRegistry(context.ops, DecodeException::new);
	}

	public <X extends Throwable> @NotNull Registry<T> getRegistry(DynamicOps<?> ops, Function<String, X> exceptionFactory) throws X {
		if (ops instanceof RegistryOps<?> registryOps) {
			Registry<T> registry = registryOps.getRegistry(this.key).orElse(null);
			if (registry != null) {
				return registry;
			}
			else {
				throw exceptionFactory.apply("No such registry " + this.key + " in " + registryOps);
			}
		}
		else {
			throw exceptionFactory.apply("Not a RegistryOps: " + ops);
		}
	}
}