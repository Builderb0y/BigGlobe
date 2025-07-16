package builderb0y.bigglobe.rendering.lods;

/**
workaround for the fact that in MC 1.21.6 and later,
fabric uses a BlockVertexConsumerProvider,
but in earlier versions, it uses a VertexConsumerProvider.
*/
public interface
	VersionedVertexConsumerProvider
extends
	#if MC_VERSION >= MC_1_21_6
		net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider
	#else
		net.minecraft.client.render.VertexConsumerProvider
	#endif
{}