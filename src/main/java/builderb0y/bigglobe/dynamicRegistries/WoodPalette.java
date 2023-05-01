package builderb0y.bigglobe.dynamicRegistries;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Range;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.ServerValue;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@SuppressWarnings("unused")
public class WoodPalette {

	public static final ServerValue<Map<RegistryKey<Biome>, List<RegistryEntry<WoodPalette>>>>
		BIOME_CACHE = new ServerValue<>(WoodPalette::computeBiomeCache);

	public final EnumMap<WoodPaletteType, Block> blocks;
	public final @VerifyNullable RegistryEntry<ConfiguredFeature<?, ?>> sapling_grow_feature;
	/** a tag containing biomes whose trees are made of this wood palette. */
	public final @VerifyNullable TagKey<Biome> biomes;
	public transient Set<RegistryKey<Biome>> biomeSet;

	public WoodPalette(
		EnumMap<WoodPaletteType, Block> blocks,
		@VerifyNullable RegistryEntry<ConfiguredFeature<?, ?>> sapling_grow_feature,
		@VerifyNullable TagKey<Biome> biomes
	) {
		this.blocks = blocks;
		this.sapling_grow_feature = sapling_grow_feature;
		this.biomes = biomes;
	}

	public Set<RegistryKey<Biome>> getBiomeSet() {
		if (this.biomeSet == null) {
			if (this.biomes != null) {
				Optional<RegistryEntryList.Named<Biome>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.BIOME).getEntryList(this.biomes);
				if (list.isPresent()) {
					this.biomeSet = list.get().stream().map(UnregisteredObjectException::getKey).collect(Collectors.toSet());
				}
				else {
					this.biomeSet = Collections.emptySet();
				}
			}
			else {
				this.biomeSet = Collections.emptySet();
			}
		}
		return this.biomeSet;
	}

	public static Map<RegistryKey<Biome>, List<RegistryEntry<WoodPalette>>> computeBiomeCache() {
		Map<RegistryKey<Biome>, List<RegistryEntry<WoodPalette>>> map = new HashMap<>();
		BigGlobeMod
		.getCurrentServer()
		.getRegistryManager()
		.get(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY)
		.streamEntries()
		.sequential()
		.forEach((RegistryEntry<WoodPalette> entry) -> {
			entry.value().getBiomeSet().forEach((RegistryKey<Biome> key) -> {
				map.computeIfAbsent(key, $ -> new ArrayList<>(8)).add(entry);
			});
		});
		return map;
	}

	//////////////////////////////// blocks ////////////////////////////////

	public Block getBlock(WoodPaletteType type) {
		Block block = this.blocks.get(type);
		if (block != null) return block;
		else throw new IllegalStateException("WoodPaletteType not present: " + type);
	}

	public Block logBlock          () { return this.getBlock(WoodPaletteType.LOG           ); }
	public Block woodBlock         () { return this.getBlock(WoodPaletteType.WOOD          ); }
	public Block strippedLogBlock  () { return this.getBlock(WoodPaletteType.STRIPPED_LOG  ); }
	public Block strippedWoodBlock () { return this.getBlock(WoodPaletteType.STRIPPED_WOOD ); }
	public Block planksBlock       () { return this.getBlock(WoodPaletteType.PLANKS        ); }
	public Block stairsBlock       () { return this.getBlock(WoodPaletteType.STAIRS        ); }
	public Block slabBlock         () { return this.getBlock(WoodPaletteType.SLAB          ); }
	public Block fenceBlock        () { return this.getBlock(WoodPaletteType.FENCE         ); }
	public Block fenceGateBlock    () { return this.getBlock(WoodPaletteType.FENCE_GATE    ); }
	public Block doorBlock         () { return this.getBlock(WoodPaletteType.DOOR          ); }
	public Block trapdoorBlock     () { return this.getBlock(WoodPaletteType.TRAPDOOR      ); }
	public Block pressurePlateBlock() { return this.getBlock(WoodPaletteType.PRESSURE_PLATE); }
	public Block buttonBlock       () { return this.getBlock(WoodPaletteType.BUTTON        ); }
	public Block leavesBlock       () { return this.getBlock(WoodPaletteType.LEAVES        ); }
	public Block saplingBlock      () { return this.getBlock(WoodPaletteType.SAPLING       ); }
	public Block pottedSaplingBlock() { return this.getBlock(WoodPaletteType.POTTED_SAPLING); }
	public Block standingSignBlock () { return this.getBlock(WoodPaletteType.STANDING_SIGN ); }
	public Block wallSignBlock     () { return this.getBlock(WoodPaletteType.WALL_SIGN     ); }

	//////////////////////////////// states ////////////////////////////////

	public BlockState getState(WoodPaletteType type) {
		return this.getBlock(type).getDefaultState();
	}

	public BlockState logState(Axis axis) {
		return (
			this.getState(WoodPaletteType.LOG)
			.withIfExists(Properties.AXIS, axis)
		);
	}

	public BlockState woodState(Axis axis) {
		return (
			this.getState(WoodPaletteType.WOOD)
			.withIfExists(Properties.AXIS, axis)
		);
	}

	public BlockState strippedLogState(Axis axis) {
		return (
			this.getState(WoodPaletteType.STRIPPED_LOG)
			.withIfExists(Properties.AXIS, axis)
		);
	}

	public BlockState strippedWoodState(Axis axis) {
		return (
			this.getState(WoodPaletteType.STRIPPED_WOOD)
			.withIfExists(Properties.AXIS, axis)
		);
	}

	public BlockState planksState() {
		return (
			this.getState(WoodPaletteType.PLANKS)
		);
	}

	public BlockState stairsState(Direction facing, BlockHalf half, StairShape shape, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.STAIRS)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.BLOCK_HALF, half)
			.withIfExists(Properties.STAIR_SHAPE, shape)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState slabState(BlockHalf half, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.SLAB)
			.withIfExists(Properties.BLOCK_HALF, half)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState fenceState(boolean north, boolean east, boolean south, boolean west, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.FENCE)
			.withIfExists(Properties.NORTH, north)
			.withIfExists(Properties.EAST, east)
			.withIfExists(Properties.SOUTH, south)
			.withIfExists(Properties.WEST, west)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState fenceGateState(Direction facing, boolean open, boolean in_wall, boolean powered) {
		return (
			this.getState(WoodPaletteType.FENCE_GATE)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.OPEN, open)
			.withIfExists(Properties.IN_WALL, in_wall)
			.withIfExists(Properties.POWERED, powered)
		);
	}

	public BlockState doorState(Direction facing, DoubleBlockHalf half, DoorHinge hinge, boolean open, boolean powered) {
		return (
			this.getState(WoodPaletteType.DOOR)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.DOUBLE_BLOCK_HALF, half)
			.withIfExists(Properties.DOOR_HINGE, hinge)
			.withIfExists(Properties.OPEN, open)
			.withIfExists(Properties.POWERED, powered)
		);
	}

	public BlockState trapdoorState(Direction facing, BlockHalf half, boolean open, boolean powered, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.TRAPDOOR)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.BLOCK_HALF, half)
			.withIfExists(Properties.OPEN, open)
			.withIfExists(Properties.POWERED, powered)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState pressurePlateState(boolean powered) {
		return (
			this.getState(WoodPaletteType.PRESSURE_PLATE)
			.withIfExists(Properties.POWERED, powered)
		);
	}

	public BlockState buttonState(WallMountLocation face, Direction facing, boolean powered) {
		return (
			this.getState(WoodPaletteType.BUTTON)
			.withIfExists(Properties.WALL_MOUNT_LOCATION, face)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.POWERED, powered)
		);
	}

	public BlockState leavesState(@Range(from = 1, to = 7) int distance, boolean persistent, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.LEAVES)
			.withIfExists(Properties.DISTANCE_1_7, distance)
			.withIfExists(Properties.PERSISTENT, persistent)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState saplingState(@Range(from = 0, to = 1) int stage) {
		return (
			this.getState(WoodPaletteType.SAPLING)
			.withIfExists(Properties.STAGE, stage)
		);
	}

	public BlockState pottedSaplingState() {
		return (
			this.getState(WoodPaletteType.POTTED_SAPLING)
		);
	}

	public BlockState standingSignState(int rotation, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.STANDING_SIGN)
			.withIfExists(Properties.ROTATION, rotation)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	public BlockState wallSignState(Direction facing, boolean waterlogged) {
		return (
			this.getState(WoodPaletteType.WALL_SIGN)
			.withIfExists(Properties.HORIZONTAL_FACING, facing)
			.withIfExists(Properties.WATERLOGGED, waterlogged)
		);
	}

	//////////////////////////////// types ////////////////////////////////

	public static enum WoodPaletteType implements StringIdentifiable {
		LOG,
		WOOD,
		STRIPPED_LOG,
		STRIPPED_WOOD,
		PLANKS,
		STAIRS,
		SLAB,
		FENCE,
		FENCE_GATE,
		DOOR,
		TRAPDOOR,
		PRESSURE_PLATE,
		BUTTON,
		LEAVES,
		SAPLING,
		POTTED_SAPLING,
		STANDING_SIGN,
		WALL_SIGN;

		public static final WoodPaletteType[] VALUES = values();
		public static final Map<String, WoodPaletteType> LOWER_CASE_LOOKUP = (
			Arrays
			.stream(VALUES)
			.collect(
				Collectors.toMap(
					(WoodPaletteType type) -> type.lowerCaseName,
					Function.identity()
				)
			)
		);

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String asString() {
			return this.lowerCaseName;
		}
	}
}