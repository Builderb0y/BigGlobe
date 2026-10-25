package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import builderb0y.bigglobe.rendering.lods.LodGenerator;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;

@Mixin(LiquidBlockRenderer.class)
public class FluidRenderer_DontHardCodeChunkSectionSizedAreas {

	@ModifyConstant(method = "tesselate", constant = @Constant(intValue = 15), expect = 3)
	private int bigglobe_dontHardCodeChunkSectionSizedAreas(int oldValue) {
		return LodGenerator.RENDERING_LODS.get() ? -1 : oldValue;
	}
}