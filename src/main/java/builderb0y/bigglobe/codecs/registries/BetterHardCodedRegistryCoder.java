package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.registry.Registry;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BetterHardCodedRegistryCoder<T> extends NamedCoder<BetterRegistry<T>> {

	public final BetterRegistry<T> registry;

	public BetterHardCodedRegistryCoder(Registry<T> registry) {
		super("BetterHardCodedRegistryCoder<" + RegistryVersions.getRegistryKey(registry) + '>');
		this.registry = new BetterHardCodedRegistry<>(registry);
	}

	@Override
	public <T_Encoded> @Nullable BetterRegistry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return this.registry;
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BetterRegistry<T>> context) throws EncodeException {
		return context.empty();
	}
}