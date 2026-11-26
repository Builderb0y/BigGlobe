package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import builderb0y.bigglobe.items.SignColorizer;
import builderb0y.bigglobe.util.WorldUtil;

@Mixin(BlockItem.class)
public class BlockItem_CharredSignColorHook {

	@Inject(method = "updateCustomBlockEntityTag", at = @At(value = "HEAD"))
	private static void bigglobe_updateCustomBlockEntityTagHook(Level world, Player player, BlockPos pos, ItemStack itemStack, CallbackInfoReturnable<Boolean> callback) {
		if (itemStack.getItem() instanceof SignColorizer colorizer) {
			SignBlockEntity sign = WorldUtil.getBlockEntity(world, pos, SignBlockEntity.class);
			if (sign != null) colorizer.setColors(sign);
		}
	}
}