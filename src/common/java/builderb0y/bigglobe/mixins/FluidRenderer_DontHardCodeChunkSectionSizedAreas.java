package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.renderer.block.FluidRenderer;

import builderb0y.bigglobe.rendering2.lods.LodMesher;

@Mixin(FluidRenderer.class)
public class FluidRenderer_DontHardCodeChunkSectionSizedAreas {

	@ModifyConstant(method = "tesselate", constant = @Constant(intValue = 15), expect = 3)
	private int bigglobe_dontHardCodeChunkSectionSizedAreas(int oldValue) {
		return LodMesher.isMeshing() ? -1 : oldValue;
	}
}