package builderb0y.bigglobe.blockdefs;

import java.util.Optional;

import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.CharredPressurePlateBlock;
import builderb0y.bigglobe.blocks.CharredSaplingBlock;

public class CharredBlocks {

	public static void init() {
		StrippableBlockRegistry.register(CHARRED_LOG, STRIPPED_CHARRED_LOG);
		StrippableBlockRegistry.register(CHARRED_WOOD, STRIPPED_CHARRED_WOOD);

		BlockEntityType.SIGN.addValidBlock(CHARRED_SIGN);
		BlockEntityType.SIGN.addValidBlock(CHARRED_WALL_SIGN);
		BlockEntityType.HANGING_SIGN.addValidBlock(CHARRED_HANGING_SIGN);
		BlockEntityType.HANGING_SIGN.addValidBlock(CHARRED_WALL_HANGING_SIGN);
		BlockEntityType.SHELF.addValidBlock(CHARRED_SHELF);
	}

	public static void initClient() {}

	public static final BlockSetType CHARRED_BLOCK_SET_TYPE = new BlockSetTypeBuilder().pressurePlateActivationRule(PressurePlateSensitivity.EVERYTHING).register(BigGlobeMod.modID("charred"));
	public static final WoodType CHARRED_WOOD_TYPE = new WoodTypeBuilder().register(BigGlobeMod.modID("charred"), CHARRED_BLOCK_SET_TYPE);
	public static final DoorBlock CHARRED_DOOR = BigGlobeBlocks.register(
		"charred_door",
		new DoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_door"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(3.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final ButtonBlock CHARRED_BUTTON = BigGlobeBlocks.register(
		"charred_button",
		new ButtonBlock(
			CHARRED_BLOCK_SET_TYPE,
			10,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_button"))
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.strength(0.5F)
			.sound(SoundType.WOOD)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final TrapDoorBlock CHARRED_TRAPDOOR = BigGlobeBlocks.register(
		"charred_trapdoor",
		new TrapDoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_trapdoor"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(3.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final PressurePlateBlock CHARRED_PRESSURE_PLATE = BigGlobeBlocks.register(
		"charred_pressure_plate",
		new CharredPressurePlateBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_pressure_plate"))
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.noOcclusion()
			.strength(0.5F)
			.sound(SoundType.WOOD)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final CeilingHangingSignBlock CHARRED_HANGING_SIGN = BigGlobeBlocks.register(
		"charred_hanging_sign",
		new CeilingHangingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_hanging_sign"))
			.mapColor(MapColor.COLOR_BLACK)
			.forceSolidOn()
			.instrument(NoteBlockInstrument.BASS)
			.noCollision()
			.strength(1.0F)
			.ignitedByLava()
		)
	);
	public static final WallHangingSignBlock CHARRED_WALL_HANGING_SIGN = BigGlobeBlocks.register(
		"charred_wall_hanging_sign",
		new WallHangingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_wall_hanging_sign"))
			.mapColor(MapColor.COLOR_BLACK)
			.forceSolidOn()
			.instrument(NoteBlockInstrument.BASS)
			.noCollision()
			.strength(1.0F)
			.ignitedByLava()
			.overrideLootTable(CHARRED_HANGING_SIGN.getLootTable())
		)
	);
	public static final StandingSignBlock CHARRED_SIGN = BigGlobeBlocks.register(
		"charred_sign",
		new StandingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_sign"))
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.noOcclusion()
			.strength(1.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final WallSignBlock CHARRED_WALL_SIGN = BigGlobeBlocks.register(
		"charred_wall_sign",
		new WallSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_wall_sign"))
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.noOcclusion()
			.strength(1.0F)
			.sound(SoundType.WOOD)
			.overrideLootTable(CHARRED_SIGN.getLootTable())
		)
	);
	public static final Block CHARRED_PLANKS = BigGlobeBlocks.register(
		"charred_planks",
		new Block(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_planks"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(2.0F, 3.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final FenceGateBlock CHARRED_FENCE_GATE = BigGlobeBlocks.register(
		"charred_fence_gate",
		new FenceGateBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
			.Properties
			.ofFullCopy(CHARRED_PLANKS)
			.setId(BigGlobeBlocks.key("charred_fence_gate"))
		)
	);
	public static final FenceBlock CHARRED_FENCE = BigGlobeBlocks.register(
		"charred_fence",
		new FenceBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(CHARRED_PLANKS)
			.setId(BigGlobeBlocks.key("charred_fence"))
		)
	);
	public static final SlabBlock CHARRED_SLAB = BigGlobeBlocks.register(
		"charred_slab",
		new SlabBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(CHARRED_PLANKS)
			.setId(BigGlobeBlocks.key("charred_slab"))
		)
	);
	public static final StairBlock CHARRED_STAIRS = BigGlobeBlocks.register(
		"charred_stairs",
		new StairBlock(
			CHARRED_PLANKS.defaultBlockState(),
			BlockBehaviour
			.Properties
			.ofFullCopy(CHARRED_PLANKS)
			.setId(BigGlobeBlocks.key("charred_stairs"))
		)
	);
	public static final SaplingBlock CHARRED_SAPLING = BigGlobeBlocks.register(
		"charred_sapling",
		new CharredSaplingBlock(
			new TreeGrower(
				"bigglobe:charred",
				Optional.empty(),
				Optional.of(
					ResourceKey.create(
						Registries.CONFIGURED_FEATURE,
						BigGlobeMod.modID("charred_tree_vanilla")
					)
				),
				Optional.empty()
			),
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_sapling"))
			.mapColor(MapColor.COLOR_BLACK)
			.noCollision()
			.noOcclusion()
			.randomTicks()
			.instabreak()
			.sound(SoundType.GRASS)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_CHARRED_SAPLING = BigGlobeBlocks.register(
		"potted_charred_sapling",
		BigGlobeBlocks.newPottedPlant(CHARRED_SAPLING, "potted_charred_sapling")
	);
	public static final Block CHARRED_LOG = BigGlobeBlocks.register(
		"charred_log",
		new RotatedPillarBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_log"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(2.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_LOG = BigGlobeBlocks.register(
		"stripped_charred_log",
		new RotatedPillarBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("stripped_charred_log"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(2.0F)
			.sound(SoundType.WOOD))
	);
	public static final Block CHARRED_WOOD = BigGlobeBlocks.register(
		"charred_wood",
		new RotatedPillarBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_wood"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(2.0F)
			.sound(SoundType.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_WOOD = BigGlobeBlocks.register(
		"stripped_charred_wood",
		new RotatedPillarBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("stripped_charred_wood"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(2.0F)
			.sound(SoundType.WOOD)
		)
	);
	//copy-paste of Blocks.createLeavesBlock(), but with MapColor.BLACK added.
	public static final LeavesBlock CHARRED_LEAVES = BigGlobeBlocks.register(
		"charred_leaves",
		new UntintedParticleLeavesBlock(
			0.02F,
			ParticleTypes.ASH,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_leaves"))
			.mapColor(MapColor.COLOR_BLACK)
			.strength(0.2F)
			.randomTicks()
			.sound(SoundType.GRASS)
			.noOcclusion()
			.isValidSpawn((BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
			.isSuffocating((BlockState state, BlockGetter world, BlockPos pos) -> false)
			.isViewBlocking((BlockState state, BlockGetter world, BlockPos pos) -> false)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final ShelfBlock CHARRED_SHELF = BigGlobeBlocks.register(
		"charred_shelf",
		new ShelfBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_shelf"))
			.mapColor(MapColor.COLOR_BLACK)
			.instrument(NoteBlockInstrument.BASS)
			.sound(SoundType.SHELF)
			.ignitedByLava()
			.strength(2.0F, 3.0F)
		)
	);
}