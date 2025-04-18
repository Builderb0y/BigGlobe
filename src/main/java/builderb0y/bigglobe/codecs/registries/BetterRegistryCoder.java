package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;

public class BetterRegistryCoder<T> extends AbstractRegistryCoder<T, BetterRegistry<T>> {

	public BetterRegistryCoder(@NotNull ReifiedType<BetterRegistry<T>> handledType, RegistryKey<Registry<T>> key) {
		super(handledType, key);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable BetterRegistry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return this.registry(context);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, BetterRegistry<T>> context) throws EncodeException {
		return EmptyData.INSTANCE;
	}
}