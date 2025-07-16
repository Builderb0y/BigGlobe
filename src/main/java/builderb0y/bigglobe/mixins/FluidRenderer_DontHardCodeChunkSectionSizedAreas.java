package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.render.block.FluidRenderer;

import builderb0y.bigglobe.rendering.lods.LodGenerator;

@Mixin(FluidRenderer.class)
public class FluidRenderer_DontHardCodeChunkSectionSizedAreas {

	@ModifyConstant(method = "render", constant = @Constant(intValue = 15), expect = 3)
	private int bigglobe_dontHardCodeChunkSectionSizedAreas(int oldValue) {
		return LodGenerator.RENDERING_LODS.get() ? -1 : oldValue;
	}
}