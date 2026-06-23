package builderb0y.bigglobe.mixins;

import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Items;

import builderb0y.bigglobe.blockdefs.VanillaBlocks;
import builderb0y.bigglobe.versions.IdentifierVersions;

@Mixin(Items.class)
public abstract class Items_PlaceableFlint {

	@Shadow
	private static Item registerItem(ResourceKey<Item> key, Function<Properties, Item> itemFactory) {
		return null;
	}

	@Redirect(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/Items;registerItem(Ljava/lang/String;)Lnet/minecraft/world/item/Item;"
		),
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=flint"),
			to = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;FLINT:Lnet/minecraft/world/item/Item;", opcode = Opcodes.PUTSTATIC)
		)
	)
	private static Item bigglobe_makeSticksPlaceable(String name) {
		return registerItem(
			ResourceKey.create(Registries.ITEM, IdentifierVersions.vanilla(name)),
			(Item.Properties settings) -> new BlockItem(VanillaBlocks.FLINT, settings)
		);
	}
}