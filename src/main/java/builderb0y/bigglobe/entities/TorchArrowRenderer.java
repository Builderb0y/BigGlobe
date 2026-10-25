package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class TorchArrowRenderer extends ArrowRenderer<TorchArrowEntity> {

	public static final ResourceLocation TEXTURE = BigGlobeMod.modID("textures/entity/projectiles/torch_arrow.png");

	public TorchArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public int getBlockLightLevel(TorchArrowEntity entity, BlockPos pos) {
		return 15;
	}

	@Override
	public ResourceLocation getTextureLocation(TorchArrowEntity entity) {
		return TEXTURE;
	}
}
