package builderb0y.bigglobe.blockdefs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.FlintBlock;
import builderb0y.bigglobe.blocks.StickBlock;
import builderb0y.bigglobe.blocks.SurfaceMaterialDecorationBlock;
import builderb0y.bigglobe.mixins.Items_PlaceableFlint;
import builderb0y.bigglobe.mixins.Items_PlaceableSticks;

/**
these blocks are referenced very early during *minecraft's* initialization,
before mods are loaded, via mixin.
see {@link Items_PlaceableSticks} and {@link Items_PlaceableFlint}.
bad things happen when BigGlobeBlocks registers its blocks too early.
so instead we have a separate class to hold these blocks
which doesn't register them on class initialization.
registering the blocks is done in {@link #init()}.
*/
public class VanillaBlocks {

	public static void init() {
		BigGlobeBlocks.register(STICK);
		BigGlobeBlocks.register(FLINT);
	}

	public static final SurfaceMaterialDecorationBlock
		STICK = new StickBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID("stick")))
			.mapColor(MapColor.COLOR_BROWN)
			.instabreak()
			.noCollision()
			.offsetType(OffsetType.XZ)
			.sound(SoundType.WOOD)
			.pushReaction(PushReaction.DESTROY)
		),
		FLINT = new FlintBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID("flint")))
			.mapColor(MapColor.METAL)
			.instabreak()
			.noCollision()
			.offsetType(OffsetType.XZ)
			.sound(SoundType.STONE)
			.pushReaction(PushReaction.DESTROY)
		);
}