package builderb0y.bigglobe.structures;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DyeColor;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class CampfireStructure extends BigGlobeStructure {

	public static final Codec<CampfireStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(CampfireStructure.class);

	public final RegistryEntry<WoodPalette> palette;
	public final boolean soul;
	public final Placement placement;
	public final double tent_chance;

	public CampfireStructure(
		Config config,
		RegistryEntry<WoodPalette> palette,
		boolean soul,
		Placement placement,
		double tent_chance
	) {
		super(config);
		this.palette = palette;
		this.soul = soul;
		this.placement = placement;
		this.tent_chance = tent_chance;
	}

	public static enum Placement implements StringIdentifiable {

		SURFACE {

			@Override
			public @Nullable BlockPos getRandomPos(Context context) {
				return randomBlockInSurface(context, 1);
			}
		},

		UNDERGROUND {

			@Override
			public @Nullable BlockPos getRandomPos(Context context) {
				Permuter permuter = Permuter.from(context.random());
				int rng = permuter.nextInt() & 255;
				int x = context.chunkPos().getStartX() | (rng & 15);
				int z = context.chunkPos().getStartZ() | (rng >>> 4);
				int minY = context.chunkGenerator().getMinimumY();
				int maxY = context.chunkGenerator().getHeightOnGround(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
				if (maxY > minY) {
					VerticalBlockSample sample = context.chunkGenerator().getColumnSample(x, z, context.world(), context.noiseConfig());
					for (int attempt = 0; attempt < 16; attempt++) {
						int y = permuter.nextInt(minY, maxY);
						if (sample.getState(y).isAir()) {
							do if (--y < minY) return null;
							while (sample.getState(y).isAir());
							if (sample.getState(y).isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
								return new BlockPos(x, y + 1, z);
							}
						}
					}
				}
				return null;
			}
		};

		public final String name = this.name().toLowerCase(Locale.ROOT);

		public abstract @Nullable BlockPos getRandomPos(Context context);

		@Override
		public String asString() {
			return this.name;
		}
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		BlockPos campfirePos = this.placement.getRandomPos(context);
		if (campfirePos == null) return Optional.empty();
		Permuter permuter = Permuter.from(context.random());
		return Optional.of(
			new StructurePosition(
				campfirePos,
				collector -> {
					collector.addPiece(new CampfirePiece(
						BigGlobeStructures.CAMPFIRE_PIECE_TYPE,
						campfirePos.getX(),
						campfirePos.getY(),
						campfirePos.getZ(),
						new CampfirePiece.Data(this.palette, this.soul)
					));
					if (Permuter.nextChancedBoolean(permuter, this.tent_chance)) {
						double angle = permuter.nextDouble(BigGlobeMath.TAU);
						double radius = permuter.nextDouble(8.0D, 16.0D);
						int tentX = BigGlobeMath.roundI(campfirePos.getX() + Math.cos(angle) * radius);
						int tentZ = BigGlobeMath.roundI(campfirePos.getZ() + Math.sin(angle) * radius);
						collector.addPiece(new TentPiece(
							BigGlobeStructures.CAMPFIRE_TENT_PIECE_TYPE,
							tentX,
							campfirePos.getY(),
							tentZ,
							new TentPiece.Data(this.palette)
						));
					}
				}
			)
		);
	}

	public static BlockState getWool(DyeColor color) {
		return (
			switch (color) {
				case WHITE      -> Blocks.WHITE_WOOL;
				case ORANGE     -> Blocks.ORANGE_WOOL;
				case MAGENTA    -> Blocks.MAGENTA_WOOL;
				case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
				case YELLOW     -> Blocks.YELLOW_WOOL;
				case LIME       -> Blocks.LIME_WOOL;
				case PINK       -> Blocks.PINK_WOOL;
				case GRAY       -> Blocks.GRAY_WOOL;
				case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
				case CYAN       -> Blocks.CYAN_WOOL;
				case PURPLE     -> Blocks.PURPLE_WOOL;
				case BLUE       -> Blocks.BLUE_WOOL;
				case BROWN      -> Blocks.BROWN_WOOL;
				case GREEN      -> Blocks.GREEN_WOOL;
				case RED        -> Blocks.RED_WOOL;
				case BLACK      -> Blocks.BLACK_WOOL;
			}
		)
		.getDefaultState();
	}

	public static BlockState getBed(DyeColor color, Direction facing, BedPart part) {
		return (
			switch (color) {
				case WHITE      -> Blocks.WHITE_BED;
				case ORANGE     -> Blocks.ORANGE_BED;
				case MAGENTA    -> Blocks.MAGENTA_BED;
				case LIGHT_BLUE -> Blocks.LIGHT_BLUE_BED;
				case YELLOW     -> Blocks.YELLOW_BED;
				case LIME       -> Blocks.LIME_BED;
				case PINK       -> Blocks.PINK_BED;
				case GRAY       -> Blocks.GRAY_BED;
				case LIGHT_GRAY -> Blocks.LIGHT_GRAY_BED;
				case CYAN       -> Blocks.CYAN_BED;
				case PURPLE     -> Blocks.PURPLE_BED;
				case BLUE       -> Blocks.BLUE_BED;
				case BROWN      -> Blocks.BROWN_BED;
				case GREEN      -> Blocks.GREEN_BED;
				case RED        -> Blocks.RED_BED;
				case BLACK      -> Blocks.BLACK_BED;
			}
		)
		.getDefaultState()
		.with(HorizontalFacingBlock.FACING, facing)
		.with(BedBlock.PART, part);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.CAMPFIRE_TYPE;
	}

	public static abstract class Piece<D> extends DataStructurePiece<D> {

		public Piece(StructurePieceType type, int length, BlockBox boundingBox, D data) {
			super(type, length, boundingBox, data);
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public int x() {
			return (this.boundingBox.getMinX() + this.boundingBox.getMaxX() + 1) >> 1;
		}

		public abstract int y();

		public int z() {
			return (this.boundingBox.getMinZ() + this.boundingBox.getMaxZ() + 1) >> 1;
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox chunkBox) {
			return (
				Coordinator
				.forWorld(world, Block.NOTIFY_LISTENERS | Block.FORCE_STATE)
				.inBox(chunkBox, false)
				.translate(this.x(), this.y(), this.z())
			);
		}
	}

	public static class CampfirePiece extends Piece<CampfirePiece.Data> {

		public static record Data(RegistryEntry<WoodPalette> palette, boolean soul) {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public BlockState getCampfireState(BlockPos pos) {
				long seed = Permuter.permute(0xF880F3296B23861BL, pos);
				int bits = Permuter.toUniformInt(seed);
				return (
					(this.soul ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE)
					.getDefaultState()
					.with(CampfireBlock.LIT, (bits & 1) != 0)
					.with(CampfireBlock.FACING, Directions.HORIZONTAL[(bits >>> 1) & 3])
				);
			}

			public BlockState getCobbleState(BlockPos pos) {
				if (this.soul) {
					return Blocks.BLACKSTONE.getDefaultState();
				}
				else {
					return Permuter.toBoolean(Permuter.permute(0xB4E17805BA6FFFDFL, pos)) ? BlockStates.MOSSY_COBBLESTONE : BlockStates.COBBLESTONE;
				}
			}
		}

		public CampfirePiece(StructurePieceType type, int x, int y, int z, Data data) {
			super(type, 0, new BlockBox(x - 4, y - 1, z - 4, x + 4, y, z + 4), data);
		}

		public CampfirePiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			WoodPalette palette = this.data.palette.value();
			Coordinator coordinator = this.coordinator(world, chunkBox);
			coordinator.setBlockState(0, -1, 0, this.data::getCobbleState);
			coordinator.rotate4x90().setBlockState(1, -1, 0, this.data::getCobbleState);
			coordinator.setBlockState(0, 0, 0, this.data::getCampfireState);
			long positionSeed = Permuter.permute(0xD932A55C7CE20F40L, this.x(), this.y(), this.z());
			for (BlockRotation rotation : Directions.ROTATIONS) {
				long rotationSeed = Permuter.permute(positionSeed, rotation.ordinal());
				int bits = Permuter.nextBoundedInt(rotationSeed, 6);
				if (bits > 1) coordinator.rotate1x(rotation).setBlockStateLine((bits & 1) + 3, 0, -1, 0, 0, 1, 3, palette.logState(Axis.Z));
			}
		}

		@Override
		public int y() {
			return this.boundingBox.getMaxY();
		}
	}

	public static class TentPiece extends Piece<TentPiece.Data> {

		public static record Data(RegistryEntry<WoodPalette> palette) {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);
		}

		public TentPiece(StructurePieceType type, int x, int y, int z, Data data) {
			super(type, 0, new BlockBox(x - 2, y - 1, z - 2, x + 2, y + 3, z + 2), data);
		}

		public TentPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			WoodPalette palette = this.data.palette.value();
			Coordinator root = this.coordinator(world, chunkBox);
			int bits = random.nextInt();
			Coordinator outerRotation = root.rotate1x(Directions.ROTATIONS[bits & 3]);
			Coordinator innerRotation = root.rotate1x(Directions.ROTATIONS[(bits >>> 2) & 3]);
			//fences
			root.rotate4x90().setBlockState(-2, 0, -2, palette.fenceState(false, false, false, false, false));
			outerRotation.flip2Z().setBlockStateLine(0, 0, -2, 0, 1, 0, 3, palette.fenceState(false, false, false, false, false));
			//wool
			outerRotation.multiTranslate(
				-2, 1, 0,
				-1, 2, 0,
				0, 3, 0,
				1, 2, 0,
				2, 1, 0
			)
			.setBlockStateLine(0, 0, -2, 0, 0, 1, 5, CampfireStructure.getWool(SheepEntity.generateDefaultColor(random)));

			DyeColor bedColor = SheepEntity.generateDefaultColor(random);
			innerRotation.flip2X().setBlockStateLine(-1, 0, 0, 0, 0, 1, getBed(bedColor, Direction.SOUTH, BedPart.FOOT), getBed(bedColor, Direction.SOUTH, BedPart.HEAD));
			innerRotation.setBlockState(0, 0, 1, Blocks.CRAFTING_TABLE.getDefaultState());
			innerRotation.setBlockStateAndBlockEntity(0, 0, 0, Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH), BlockEntityType.CHEST, (pos, chest) -> {
				chest.setLootTable(LootTables.SPAWN_BONUS_CHEST, Permuter.permute(0x20B7FFB56D249782L, pos));
			});
		}

		@Override
		public int y() {
			return this.boundingBox.getMinY() + 1;
		}
	}
}