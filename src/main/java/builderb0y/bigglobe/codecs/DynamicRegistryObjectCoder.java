package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class DynamicRegistryObjectCoder<T> extends NamedCoder<T> {

	public final DynamicRegistryCoder<T> registryCoder;

	public DynamicRegistryObjectCoder(@NotNull ReifiedType<T> handledType, DynamicRegistryCoder<T> registryCoder) {
		super(handledType);
		this.registryCoder = registryCoder;
	}

	@Override
	public <T_Encoded> @Nullable T decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Registry<T> registry = context.decodeWith(this.registryCoder);
		Identifier id = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		if (registry.containsId(id)) return registry.get(id);
		else throw new DecodeException("No such object with ID " + id + " in " + registry);
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T> context) throws EncodeException {
		if (context.input == null) return context.empty();
		Registry<T> registry = this.registryCoder.getRegistry(context.ops, EncodeException::new);
		Identifier id = registry.getId(context.input);
		if (id != null) return context.createString(id.toString());
		else throw new EncodeException("Unregistered object " + context.input + " in registry " + registry);
	}
}