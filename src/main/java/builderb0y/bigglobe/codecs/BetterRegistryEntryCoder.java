package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.registry.BetterRegistry;
import builderb0y.bigglobe.registry.BetterRegistryEntry;

public class BetterRegistryEntryCoder<T> extends NamedCoder<BetterRegistryEntry<T>> {

	public final BetterRegistryCoder<T> registryCoder;
	public final RegistryKey<Registry<T>> registryKey;

	public BetterRegistryEntryCoder(@NotNull ReifiedType<BetterRegistryEntry<T>> handledType, BetterRegistryCoder<T> registryCoder, RegistryKey<Registry<T>> key) {
		super(handledType);
		this.registryCoder = registryCoder;
		this.registryKey = key;
	}

	@Override
	public <T_Encoded> @Nullable BetterRegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		BetterRegistry<T> registry = context.decodeWith(this.registryCoder);
		Identifier id = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		return registry.getEntry(id);
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BetterRegistryEntry<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		return context.createString(context.input.id().toString());
	}
}