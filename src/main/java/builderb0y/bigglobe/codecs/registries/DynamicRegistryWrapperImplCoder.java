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
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class DynamicRegistryWrapperImplCoder<T> extends NamedCoder<RegistryWrapper.Impl<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public DynamicRegistryWrapperImplCoder(RegistryKey<Registry<T>> registryKey) {
		super("DynamicRegistryWrapperImplCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable <T_Encoded> RegistryWrapper.Impl<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			if (registryOps.getEntryLookup(this.registryKey).orElse(null) instanceof RegistryWrapper.Impl<T> wrapper) {
				return wrapper;
			}
			else if (registryOps.getOwner(this.registryKey).orElse(null) instanceof RegistryWrapper.Impl<?> wrapper) {
				return (RegistryWrapper.Impl<T>)(wrapper);
			}
			else {
				throw AutoCodecVersions.newDecodeExceptions(() -> "Unable to access registry " + this.registryKey.getValue() + " in " + registryOps);
			}
		}
		else {
			throw AutoCodecVersions.newDecodeExceptions(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryWrapper.Impl<T>> context) throws EncodeException {
		return context.empty();
	}
}