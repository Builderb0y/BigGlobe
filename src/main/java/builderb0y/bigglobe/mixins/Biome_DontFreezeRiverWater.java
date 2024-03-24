package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.blocks.RiverWaterBlock;

@Mixin(Biome.class)
public class Biome_DontFreezeRiverWater {

	@Redirect(method = "canSetIce(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Z)Z", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/block/FluidBlock"))
	private boolean bigglobe_checkRiverWater(Object block, Class<?> clazz) {
		return clazz.isInstance(block) && !(block instanceof RiverWaterBlock);
	}
}