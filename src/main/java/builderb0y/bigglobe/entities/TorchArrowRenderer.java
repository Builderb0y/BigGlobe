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

@Environment(EnvType.CLIENT)
public class TorchArrowRenderer extends ProjectileEntityRenderer<TorchArrowEntity> {

	public static final Identifier TEXTURE = BigGlobeMod.modID("textures/entity/projectiles/torch_arrow.png");

	public TorchArrowRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(TorchArrowEntity persistentProjectileEntity, float yaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
		int skylight = LightmapTextureManager.getSkyLightCoordinates(light);
		int blockLight = 15;
		light = LightmapTextureManager.pack(blockLight, skylight);
		super.render(persistentProjectileEntity, yaw, partialTicks, matrixStack, vertexConsumerProvider, light);
	}

	@Override
	public Identifier getTexture(TorchArrowEntity entity) {
		return TEXTURE;
	}
}