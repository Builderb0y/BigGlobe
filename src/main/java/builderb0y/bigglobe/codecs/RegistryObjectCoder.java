package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.registry.BetterRegistry;

public class RegistryObjectCoder<T> extends NamedCoder<T> {

	public final BetterRegistryCoder<T> registryCoder;

	public RegistryObjectCoder(@NotNull ReifiedType<T> handledType, BetterRegistryCoder<T> registryCoder) {
		super(handledType);
		this.registryCoder = registryCoder;
	}

	@Override
	public <T_Encoded> @Nullable T decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		BetterRegistry<T> registry = context.decodeWith(this.registryCoder);
		Identifier id = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		return registry.getEntry(id).object();
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T> context) throws EncodeException {
		if (context.input == null) return context.empty();
		BetterRegistry<T> registry = this.registryCoder.getRegistry(context.ops, EncodeException::new);
		return context.createString(registry.getID(context.input).toString());
	}
}