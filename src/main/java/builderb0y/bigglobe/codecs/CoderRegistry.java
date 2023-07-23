package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;

import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder.DispatchCoder;
import builderb0y.autocodec.coders.LookupCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class CoderRegistry<E extends CoderRegistryTyped<E>> extends DispatchCoder<E> {

	public final Identifier registryID;

	public CoderRegistry(Identifier registryID) {
		this(registryID, "type");
	}

	public CoderRegistry(Identifier registryID, String key) {
		super("CoderRegistry<" + registryID + '>', new IdentifierLookupCoder<>(registryID), key);
		this.registryID = registryID;
	}

	public IdentifierLookupCoder<E> lookup() {
		return (IdentifierLookupCoder<E>)(this.keyCoder);
	}

	public <E2 extends E> void register(Identifier id, AutoCoder<E2> coder) {
		this.lookup().add(id, coder);
	}

	public <E2 extends E> void registerAuto(Identifier id, ReifiedType<E2> type) {
		this.register(id, BigGlobeAutoCodec.AUTO_CODEC.createCoder(type));
	}

	public <E2 extends E> void registerAuto(Identifier id, Class<E2> clazz) {
		this.registerAuto(id, ReifiedType.from(clazz));
	}

	public String toString(Identifier id) {
		return id.getNamespace().equals(this.registryID.getNamespace()) ? id.getPath() : id.toString();
	}

	public static class IdentifierLookupCoder<E> extends LookupCoder<Identifier, AutoCoder<? extends E>> {

		public IdentifierLookupCoder(@NotNull Identifier registryKey) {
			super("IdentifierLookupCoder<" + registryKey + '>', BigGlobeAutoCodec.createNamespacedIdentifierCodec(registryKey.getNamespace()));
		}
	}
}