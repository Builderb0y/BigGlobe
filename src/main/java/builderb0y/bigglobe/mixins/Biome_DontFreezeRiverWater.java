package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.blocks.RiverWaterBlock;

@Mixin(Biome.class)
public class Biome_DontFreezeRiverWater {

	/**
	the goal: make it so that `state.getBlock() instanceof FluidBlock`
	returns false when the block is *also* an instance of RiverWaterBlock.
	originally, I tried to redirect the instanceof operation,
	and this worked in a dev environment,
	but it turns out the refmap doesn't correctly map FluidBlock
	to intermediary mappings inside the @At args.
	as such, the injection failed at runtime in production environments.
	my workaround is to spoof the block when it's an instance of RiverWaterBlock,
	and leave the instanceof check as-is.
	*/
	@Redirect(method = "canSetIce(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"))
	private Block bigglobe_checkRiverWater(BlockState state) {
		if (state.getBlock() instanceof RiverWaterBlock) return Blocks.AIR; //or any other non-fluid block.
		else return state.getBlock();
	}
}