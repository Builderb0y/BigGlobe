package builderb0y.bigglobe.mixins;

import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.core.registries.Registries;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockdefs.VanillaBlocks;
import builderb0y.bigglobe.versions.IdentifierVersions;

@Mixin(Items.class)
public class Items_PlaceableSticks {

	#if MC_VERSION >= MC_26_2_0

		@Shadow private static Item registerBlock(BlockItemId id, Block block) {
			throw new UnsupportedOperationException("Implemented via mixin");
		}

		@Redirect(
			method = "<clinit>",
			at = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/item/Items;registerItem(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/item/Item;"
			),
			slice = @Slice(
				from = @At(
					value = "FIELD",
					target = "Lnet/minecraft/references/ItemIds;STICK:Lnet/minecraft/resources/ResourceKey;",
					opcode = Opcodes.GETSTATIC
				),
				to = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/item/Items;STICK:Lnet/minecraft/world/item/Item;",
					opcode = Opcodes.PUTSTATIC
				)
			)
		)
		private static Item bigglobe_makeSticksPlaceable(ResourceKey<Item> id) {
			return registerBlock(
				new BlockItemId(
					ResourceKey.create(
						Registries.BLOCK,
						BigGlobeMod.modID("stick")
					),
					id
				),
				VanillaBlocks.STICK
			);
		}

	#else

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
				from = @At(value = "CONSTANT", args = "stringValue=stick"),
				to = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;STICK:Lnet/minecraft/world/item/Item;", opcode = Opcodes.PUTSTATIC)
			)
		)
		private static Item bigglobe_makeSticksPlaceable(String name) {
			return registerItem(
				ResourceKey.create(Registries.ITEM, IdentifierVersions.vanilla(name)),
				(Item.Properties settings) -> new BlockItem(VanillaBlocks.STICK, settings)
			);
		}

	#endif
}