package builderb0y.bigglobe.mixins;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import builderb0y.bigglobe.blocks.BigGlobeBlocks.VanillaBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@Mixin(Items.class)
public class Items_PlaceableSticks {

	@Redirect(
		method = "<clinit>",
		at = @At(
			value = "NEW",
			target = "(Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;"
		),
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=stick"),
			to = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;STICK:Lnet/minecraft/world/item/Item;", opcode = Opcodes.PUTSTATIC)
		)
	)
	private static Item bigglobe_makeSticksPlaceable(Item.Properties settings) {
		return new BlockItem(VanillaBlocks.STICK, settings);
	}
}
