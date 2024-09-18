package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryVersions;

public class HardCodedObjectCoder<T> extends NamedCoder<T> {

	public final Registry<T> registry;

	public HardCodedObjectCoder(Registry<T> registry) {
		super("HardCodedObjectCoder<" + RegistryVersions.getRegistryKey(registry).getValue() + '>');
		this.registry = registry;
	}

	@Override
	public <T_Encoded> @Nullable T decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Identifier identifier = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		if (this.registry.containsId(identifier)) {
			return this.registry.get(identifier);
		}
		else {
			throw new DecodeException(() -> "Registry " + RegistryVersions.getRegistryKey(this.registry).getValue() + " does not contain ID " + identifier);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, T> context) throws EncodeException {
		T object = context.object;
		if (object == null) return context.empty();
		Identifier id = this.registry.getId(object);
		if (id != null) {
			return context.object(id).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		}
		else {
			throw new EncodeException(() -> "Registry " + RegistryVersions.getRegistryKey(this.registry).getValue() + " does not contain object " + object);
		}
	}
}