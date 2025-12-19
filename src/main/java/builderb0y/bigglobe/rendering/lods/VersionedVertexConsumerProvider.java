package builderb0y.bigglobe.rendering.lods;

#if MC_VERSION >= MC_1_21_6
	public interface VersionedVertexConsumerProvider extends net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider {}
#else
	public interface VersionedVertexConsumerProvider extends net.minecraft.client.render.VertexConsumerProvider {}
#endif