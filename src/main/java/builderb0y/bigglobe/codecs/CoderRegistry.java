package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder.DispatchCoder;
import builderb0y.autocodec.coders.LookupCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import net.minecraft.resources.ResourceLocation;

public class CoderRegistry<E extends CoderRegistryTyped<E>> extends DispatchCoder<E> {

	public final ResourceLocation registryID;

	public CoderRegistry(ResourceLocation registryID) {
		this(registryID, "type");
	}

	public CoderRegistry(ResourceLocation registryID, String key) {
		super("CoderRegistry<" + registryID + '>', new IdentifierLookupCoder<>(registryID), key);
		this.registryID = registryID;
	}

	public IdentifierLookupCoder<E> lookup() {
		return (IdentifierLookupCoder<E>)(this.keyCoder);
	}

	public ResourceLocation getType(E value) {
		return this.lookup().encode.get(value.getCoder());
	}

	public <E2 extends E> void register(ResourceLocation id, AutoCoder<E2> coder) {
		this.lookup().add(id, coder);
	}

	public <E2 extends E> void registerAuto(ResourceLocation id, ReifiedType<E2> type) {
		this.register(id, BigGlobeAutoCodec.AUTO_CODEC.createCoder(type));
	}

	public <E2 extends E> void registerAuto(ResourceLocation id, Class<E2> clazz) {
		this.registerAuto(id, ReifiedType.from(clazz));
	}

	public String toString(ResourceLocation id) {
		return id.getNamespace().equals(this.registryID.getNamespace()) ? id.getPath() : id.toString();
	}

	public static class IdentifierLookupCoder<E> extends LookupCoder<ResourceLocation, AutoCoder<? extends E>> {

		public IdentifierLookupCoder(@NotNull ResourceLocation registryKey) {
			super(
				"IdentifierLookupCoder<" + registryKey + '>',
				BigGlobeAutoCodec.createNamespacedIdentifierCodec(registryKey.getNamespace())
			);
		}
	}
}