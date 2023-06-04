package builderb0y.bigglobe.codecs;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class CoderRegistry<E extends CoderRegistryTyped> extends NamedCoder<E> {

	public final Identifier registryID;
	public final String typeKey;
	public final Map<Identifier, Entry<? extends E>> byID;
	public final Map<ReifiedType<?>, Entry<? extends E>> byType;

	public CoderRegistry(Identifier registryID) {
		this(registryID, "type");
	}

	public CoderRegistry(Identifier registryID, String key) {
		super("CoderRegistry<" + registryID + '>');
		this.registryID = registryID;
		this.typeKey    = key;
		this.byID       = new HashMap<>(64);
		this.byType     = new Object2ObjectOpenCustomHashMap<>(64, ReifiedType.GENERIC_TYPE_STRATEGY);
	}

	public void register(Entry<? extends E> entry) {
		this.byID.put(entry.id, entry);
		this.byType.put(entry.type, entry);
	}

	public <E2 extends E> void register(Identifier id, ReifiedType<E2> type, AutoCoder<E2> coder) {
		this.register(new Entry<>(id, type, coder));
	}

	public <E2 extends E> void registerAuto(Identifier id, ReifiedType<E2> type) {
		this.register(id, type, BigGlobeAutoCodec.AUTO_CODEC.createCoder(type));
	}

	public <E2 extends E> void registerAuto(Identifier id, Class<E2> clazz) {
		this.registerAuto(id, ReifiedType.from(clazz));
	}

	public Identifier toID(String string) {
		String namespace, path;
		int colon = string.indexOf(':');
		if (colon >= 0) {
			namespace = string.substring(0, colon);
			path = string.substring(colon + 1);
		}
		else {
			namespace = this.registryID.getNamespace();
			path = string;
		}
		return new Identifier(namespace, path);
	}

	public String toString(Identifier id) {
		return id.getNamespace().equals(this.registryID.getNamespace()) ? id.getPath() : id.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, E> context) throws EncodeException {
		E object = context.input;
		if (object == null) return context.empty();
		Entry<? extends E> entry = this.byType.get(object.getType());
		if (entry == null) throw new EncodeException("Unregistered object: " + object);
		return context.addToStringMap(
			context.encodeWith((AutoEncoder<E>)(entry.coder)),
			this.typeKey,
			context.createString(this.toString(entry.id))
		);
	}

	@Override
	public <T_Encoded> @Nullable E decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Identifier id = this.toID(context.getMember(this.typeKey).forceAsString());
		Entry<? extends E> entry = this.byID.get(id);
		if (entry == null) throw new DecodeException("Unregistered ID: " + id);
		return context.removeMember(this.typeKey).decodeWith(entry.coder);
	}

	public static record Entry<E extends CoderRegistryTyped>(Identifier id, ReifiedType<E> type, AutoCoder<E> coder) {}
}