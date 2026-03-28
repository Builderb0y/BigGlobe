package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import builderb0y.bigglobe.BigGlobeMod;

@Environment(EnvType.CLIENT)
public class TorchArrowRenderer extends ArrowRenderer<TorchArrowEntity, ArrowRenderState> {

	public static final Identifier TEXTURE = BigGlobeMod.modID("textures/entity/projectiles/torch_arrow.png");

	public TorchArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public int getBlockLightLevel(TorchArrowEntity entity, BlockPos pos) {
		return 15;
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	public Identifier getTextureLocation(

		ArrowRenderState state

	) {
		return TEXTURE;
	}
}