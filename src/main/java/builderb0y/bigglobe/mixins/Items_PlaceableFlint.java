package builderb0y.bigglobe.mixins;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import builderb0y.bigglobe.blocks.BigGlobeBlocks.VanillaBlocks;

@Mixin(Items.class)
public class Items_PlaceableFlint {

	@Redirect(
		method = "<clinit>",
		at     = @At(
			value  = "NEW",
			target = "net/minecraft/item/Item"
		),
		slice = @Slice(
			from = @At(value = "CONSTANT", args   = "stringValue=flint"),
			to   = @At(value = "FIELD",    target = "Lnet/minecraft/item/Items;FLINT:Lnet/minecraft/item/Item;", opcode = Opcodes.PUTSTATIC)
		)
	)
	private static Item bigglobe_makeSticksPlaceable(Item.Settings settings) {
		return new BlockItem(VanillaBlocks.FLINT, settings);
	}
}