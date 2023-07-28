package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

public class BetterDynamicRegistryCoder<T> extends NamedCoder<BetterRegistry<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public BetterDynamicRegistryCoder(RegistryKey<Registry<T>> key) {
		super("BetterDynamicRegistryCoder<" + key.getValue() + '>');
		this.registryKey = key;
	}

	@Override
	public <T_Encoded> @Nullable BetterRegistry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			Registry<T> registry = registryOps.getRegistry(this.registryKey).orElse(null);
			if (registry != null) {
				return new BetterHardCodedRegistry<>(registry);
			}
			else {
				throw new DecodeException(() -> "Registry " + this.registryKey.getValue() + " not present in RegistryOps");
			}
		}
		else {
			throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BetterRegistry<T>> context) throws EncodeException {
		return context.empty();
	}
}