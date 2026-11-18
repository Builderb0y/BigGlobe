package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;

public class BetterRegistryCoder<T> extends AbstractRegistryCoder<T, RegistryAdapter<T>> {

	public BetterRegistryCoder(@NotNull ReifiedType<RegistryAdapter<T>> handledType, ResourceKey<Registry<T>> key) {
		super(handledType, key);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable RegistryAdapter<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return this.registry(context);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, RegistryAdapter<T>> context) throws EncodeException {
		return EmptyData.INSTANCE;
	}
}