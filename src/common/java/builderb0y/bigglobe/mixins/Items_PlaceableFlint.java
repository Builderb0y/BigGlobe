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
import net.minecraft.world.level.block.Block;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockdefs.VanillaBlocks;
import builderb0y.bigglobe.versions.IdentifierVersions;

#if MC_VERSION >= MC_26_2_0
	import net.minecraft.references.BlockItemId;
#endif

@Mixin(Items.class)
public abstract class Items_PlaceableFlint {

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
					target = "Lnet/minecraft/references/ItemIds;FLINT:Lnet/minecraft/resources/ResourceKey;",
					opcode = Opcodes.GETSTATIC
				),
				to = @At(
					value = "FIELD",
					target = "Lnet/minecraft/world/item/Items;FLINT:Lnet/minecraft/world/item/Item;",
					opcode = Opcodes.PUTSTATIC
				)
			)
		)
		private static Item bigglobe_makeFlintPlaceable(ResourceKey<Item> id) {
			return registerBlock(
				new BlockItemId(
					ResourceKey.create(
						Registries.BLOCK,
						BigGlobeMod.modID("flint")
					),
					id
				),
				VanillaBlocks.FLINT
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
				from = @At(value = "CONSTANT", args = "stringValue=flint"),
				to = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;FLINT:Lnet/minecraft/world/item/Item;", opcode = Opcodes.PUTSTATIC)
			)
		)
		private static Item bigglobe_makeFlintPlaceable(String name) {
			return registerItem(
				ResourceKey.create(Registries.ITEM, IdentifierVersions.vanilla(name)),
				(Item.Properties settings) -> new BlockItem(VanillaBlocks.FLINT, settings)
			);
		}

	#endif
}