package builderb0y.bigglobe.blockdefs;

import java.util.EnumMap;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import builderb0y.bigglobe.blocks.*;

public class EndBlocks {

	public static void init() {}

	public static void initClient() {}

	public static final Block CHORUS_NYLIUM = BigGlobeBlocks.register(
		"chorus_nylium",
		new ChorusNyliumBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("chorus_nylium"))
			.mapColor(MapColor.COLOR_PURPLE)
			.sound(SoundType.STONE)
			.strength(3.0F, 9.0F)
			.requiresCorrectToolForDrops()
		)
	);

	public static final Block OVERGROWN_END_STONE = BigGlobeBlocks.register(
		"overgrown_end_stone",
		new OvergrownEndStoneBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("overgrown_end_stone"))
			.mapColor(MapColor.ICE)
			.sound(SoundType.STONE)
			.strength(3.0F, 9.0F)
			.requiresCorrectToolForDrops()
		)
	);

	public static final DoublePlantBlock TALL_CHORUS_SPORES = BigGlobeBlocks.register(
		"tall_chorus_spores",
		new TallChorusSporeBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("tall_chorus_spores"))
			.mapColor(MapColor.COLOR_PURPLE)
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.noCollision()
			.noOcclusion()
			.instabreak()
			.pushReaction(PushReaction.DESTROY)
		)
	);

	public static final ChorusSporeBlock MEDIUM_CHORUS_SPORES = BigGlobeBlocks.register(
		"medium_chorus_spores",
		new MediumChorusSporeBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("medium_chorus_spores"))
			.mapColor(MapColor.COLOR_PURPLE)
			.replaceable()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.noCollision()
			.noOcclusion()
			.instabreak()
			.pushReaction(PushReaction.DESTROY),
			TALL_CHORUS_SPORES.builtInRegistryHolder()
		)
	);

	public static final ChorusSporeBlock SHORT_CHORUS_SPORES = BigGlobeBlocks.register(
		"short_chorus_spores",
		new ShortChorusSporeBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("short_chorus_spores"))
			.mapColor(MapColor.COLOR_PURPLE)
			.replaceable()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.noCollision()
			.noOcclusion()
			.instabreak()
			.pushReaction(PushReaction.DESTROY),
			MEDIUM_CHORUS_SPORES.builtInRegistryHolder()
		)
	);

	public static final EnumMap<CloudColor, CloudBlock> VOID_CLOUDS = new EnumMap<>(CloudColor.class);

	static {
		for (CloudColor color : CloudColor.VALUES) {
			EndBlocks.VOID_CLOUDS.put(
				color,
				BigGlobeBlocks.register(
					color.voidName,
					new CloudBlock(
						BlockBehaviour
						.Properties
						.of()
						.setId(BigGlobeBlocks.key(color.voidName))
						.mapColor(MapColor.COLOR_BLACK)
						.strength(0.2F)
						.sound(SoundType.WOOL)
						.lightLevel(
							color == CloudColor.BLANK
							? (BlockState state) -> 0
							: (BlockState state) -> 5
						)
						.isValidSpawn(Blocks::never),
						color,
						true
					)
				)
			);
		}
	}

	public static final Block VOIDMETAL_BLOCK = BigGlobeBlocks.register(
		"voidmetal_block",
		new Block(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("voidmetal_block"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(5.0F, 6.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.METAL)
		)
	);
}