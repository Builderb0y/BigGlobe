package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;

public class BetterRegistryLookupCoder extends NamedCoder<RegistryAdapter.Lookup> {

	public static final BetterRegistryLookupCoder INSTANCE = new BetterRegistryLookupCoder("BetterRegistryLookupCoder.INSTANCE");

	public BetterRegistryLookupCoder(@NotNull String toString) {
		super(toString);
	}

	@Override
	public <T_Encoded> RegistryAdapter.@Nullable Lookup decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			return fromOps(registryOps);
		}
		else {
			throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	public static RegistryAdapter.Lookup fromOps(RegistryOps<?> registryOps) {
		return new RegistryAdapter.Lookup() {

			@Override
			public <T> RegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key) {
				HolderGetter<T> getter = registryOps.getter(key).orElse(null);
				if (getter == null) {
					throw new IllegalStateException("Missing registry: " + key.identifier());
				}
				return new RegistryAdapter.BasicImpl<>(key, getter);
			}
		};
	}

	@Override
	public <T_Encoded> Data encode(@NotNull EncodeContext<T_Encoded, RegistryAdapter.Lookup> context) throws EncodeException {
		return EmptyData.INSTANCE;
	}
}