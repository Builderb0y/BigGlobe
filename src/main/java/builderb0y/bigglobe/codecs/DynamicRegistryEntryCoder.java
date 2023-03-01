package builderb0y.bigglobe.codecs;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class DynamicRegistryEntryCoder<T> extends NamedCoder<RegistryEntry<T>> {

	public final DynamicRegistryCoder<T> registryCoder;

	public DynamicRegistryEntryCoder(@NotNull ReifiedType<RegistryEntry<T>> handledType, DynamicRegistryCoder<T> registryCoder) {
		super(handledType);
		this.registryCoder = registryCoder;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Registry<T> registry = context.decodeWith(this.registryCoder);
		Identifier id = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		RegistryKey<T> key = RegistryKey.of(registry.getKey(), id);
		return registry.getOrCreateEntry(key);
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		Registry<T> registry = this.registryCoder.getRegistry(context.ops, EncodeException::new);
		RegistryKey<T> key = context.input.getKeyOrValue().map(
			Function.identity(),
			object -> registry.getKey(object).orElse(null)
		);
		if (key == null) throw new EncodeException("Unregistered entry: " + context.input + " (" + context.input.value() + ") in registry " + registry);
		return context.createString(key.getValue().toString());
	}
}