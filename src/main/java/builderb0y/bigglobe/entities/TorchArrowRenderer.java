package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;

#if MC_VERSION >= MC_1_21_2
	import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
	import net.minecraft.util.math.BlockPos;
#endif

@Environment(EnvType.CLIENT)
public class TorchArrowRenderer extends ProjectileEntityRenderer<TorchArrowEntity #if MC_VERSION >= MC_1_21_2 , ProjectileEntityRenderState #endif> {

	public static final Identifier TEXTURE = BigGlobeMod.modID("textures/entity/projectiles/torch_arrow.png");

	public TorchArrowRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	#if MC_VERSION >= MC_1_21_9

		@Override
		public int getBlockLight(TorchArrowEntity entity, BlockPos pos) {
			return 16;
		}

	#else

		@Override
		public void render(
			#if MC_VERSION >= MC_1_21_2
				ProjectileEntityRenderState state,
			#else
				TorchArrowEntity entity,
				float yaw,
				float partialTicks,
			#endif
			MatrixStack matrixStack,
			VertexConsumerProvider vertexConsumerProvider,
			int light
		) {
			int skylight = LightmapTextureManager.getSkyLightCoordinates(light);
			int blockLight = 15;
			light = LightmapTextureManager.pack(blockLight, skylight);
			super.render(
				#if MC_VERSION >= MC_1_21_2
					state,
				#else
					entity,
					yaw,
					partialTicks,
				#endif
				matrixStack,
				vertexConsumerProvider,
				light
			);
		}

	#endif

	#if MC_VERSION >= MC_1_21_2

		@Override
		public ProjectileEntityRenderState createRenderState() {
			return new ProjectileEntityRenderState();
		}

	#endif

	@Override
	public Identifier getTexture(
		#if MC_VERSION >= MC_1_21_2
			ProjectileEntityRenderState state
		#else
			TorchArrowEntity entity
		#endif
	) {
		return TEXTURE;
	}
}