package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class DynamicRegistryWrapperCoder<T> extends NamedCoder<RegistryWrapper<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public DynamicRegistryWrapperCoder(RegistryKey<Registry<T>> registryKey) {
		super("DynamicRegistryWrapperCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable <T_Encoded> RegistryWrapper<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			if (registryOps.getEntryLookup(this.registryKey).orElse(null) instanceof RegistryWrapper<T> wrapper) {
				return wrapper;
			}
			else if (registryOps.getOwner(this.registryKey).orElse(null) instanceof RegistryWrapper<?> wrapper) {
				return (RegistryWrapper<T>)(wrapper);
			}
			else {
				throw new DecodeException(() -> "Unable to access registry " + this.registryKey.getValue() + " in " + registryOps);
			}
		}
		else {
			throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryWrapper<T>> context) throws EncodeException {
		return context.empty();
	}
}