package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class DynamicRegistryCoder<T> extends NamedCoder<RegistryEntryLookup<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public DynamicRegistryCoder(RegistryKey<Registry<T>> registryKey) {
		super("DynamicRegistryCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntryLookup<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			RegistryEntryLookup<T> lookup = registryOps.getEntryLookup(this.registryKey).orElse(null);
			if (lookup != null) {
				return lookup;
			}
			else {
				throw AutoCodecVersions.newDecodeExceptions(() -> "Registry " + this.registryKey.getValue() + " not present in RegistryOps");
			}
		}
		else {
			throw AutoCodecVersions.newDecodeExceptions(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntryLookup<T>> context) throws EncodeException {
		return context.empty();
	}
}