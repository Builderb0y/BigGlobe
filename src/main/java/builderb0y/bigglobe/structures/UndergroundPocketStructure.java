package builderb0y.bigglobe.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.*;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.settings.Seed.NumberSeed;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.TagOrObjectKey;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.BlockPosVersions;

public class UndergroundPocketStructure extends BigGlobeStructure implements RawGenerationStructure {

	public static final Codec<UndergroundPocketStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(UndergroundPocketStructure.class);

	public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 112.0D) RandomSource radius;
	public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 1.0D, maxInclusive = false) RandomSource noise_decay;
	public final @VerifyNullable FluidConfig fluid;
	public final @VerifyNullable FloorCeilingConfig floor, ceiling;

	public static record FluidConfig(
		RandomSource level,
		BlockState state,
		@VerifyNullable TagOrObjectKey<ConfiguredFeature<?, ?>> decorator
	) {

		public BakedFluidConfig bake(double y, double sqrtMaxThickness, RandomGenerator random) {
			return new BakedFluidConfig(
				BigGlobeMath.floorI(
					Interpolator.mixLinear(
						y - sqrtMaxThickness,
						y + sqrtMaxThickness,
						this.level.get(random)
					)
				),
				this.state,
				this.decorator
			);
		}
	}

	public static record FloorCeilingConfig(
		@VerifyNullable SurfaceConfig surface,
		@VerifyNullable TagOrObjectKey<ConfiguredFeature<?, ?>> decorator
	) {}

	public static record SurfaceConfig(
		ColumnYRandomToDoubleScript.Holder depth,
		BlockState top,
		BlockState under
	) {}

	public static class BakedFluidConfig {

		public int level;
		public BlockState state;
		public @VerifyNullable TagOrObjectKey<ConfiguredFeature<?, ?>> decorator;

		public BakedFluidConfig(int level, BlockState state, @VerifyNullable TagOrObjectKey<ConfiguredFeature<?, ?>> decorator) {
			this.level = level;
			this.state = state;
			this.decorator = decorator;
		}
	}

	public UndergroundPocketStructure(
		Config config,
		RandomSource radius,
		RandomSource noise_decay,
		@VerifyNullable FluidConfig fluid,
		@VerifyNullable FloorCeilingConfig floor,
		@VerifyNullable FloorCeilingConfig ceiling
	) {
		super(config);
		this.radius      = radius;
		this.noise_decay = noise_decay;
		this.fluid       = fluid;
		this.floor       = floor;
		this.ceiling     = ceiling;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		Permuter permuter = Permuter.from(context.random());
		double radius = this.radius.get(permuter);
		double noiseDecay = this.noise_decay.get(permuter);
		List<Grid2D> layers = new ArrayList<>(8);
		double amplitude = radius;
		int scale = ((int)(radius)) >> 1;
		for (; scale >= 2; scale >>= 1, amplitude *= noiseDecay) {
			layers.add(new SmoothGrid2D(new NumberSeed(permuter.nextLong()), amplitude, scale, scale));
		}
		Grid2D noise = new SummingGrid2D(layers.toArray(new Grid2D[layers.size()]));
		double sqrtMaxThickness = Math.sqrt(noise.maxValue());
		Vector3d center = randomPosInChunk(context, radius, sqrtMaxThickness);
		if (center == null) return Optional.empty();
		return Optional.of(
			new StructurePosition(
				BlockPosVersions.floor(center.x, center.y, center.z),
				collector -> {
					collector.addPiece(
						new Piece(
							BigGlobeStructures.UNDERGROUND_POCKET_PIECE,
							new Piece.Data(
								center.x,
								center.y,
								center.z,
								radius,
								noise,
								this.floor,
								this.ceiling,
								this.fluid != null ? this.fluid.bake(center.y, sqrtMaxThickness, permuter) : null
							)
						)
					);
				}
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.UNDERGROUND_POCKET;
	}

	public static class Piece extends DataStructurePiece<Piece.Data> implements RawGenerationStructurePiece {

		public static class Data {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public double x, y, z, radius;
			public Grid2D noise;
			public transient double sqrtMaxThickness;
			public @VerifyNullable FloorCeilingConfig floor, ceiling;
			public @VerifyNullable BakedFluidConfig fluid;
			public transient long nextWarnTime = Long.MIN_VALUE;

			public Data(
				double x,
				double y,
				double z,
				double radius,
				Grid2D noise,
				@VerifyNullable FloorCeilingConfig floor,
				@VerifyNullable FloorCeilingConfig ceiling,
				@VerifyNullable BakedFluidConfig fluid
			) {
				this.x                 = x;
				this.y                 = y;
				this.z                 = z;
				this.radius            = radius;
				this.noise             = noise;
				this.sqrtMaxThickness  = Math.sqrt(noise.maxValue());
				this.floor             = floor;
				this.ceiling           = ceiling;
				this.fluid             = fluid;
			}
		}

		public Piece(StructurePieceType type, @NotNull Data data) {
			super(
				type,
				0,
				WorldUtil.createBlockBox(
					BigGlobeMath. ceilI(data.x - data.radius),
					BigGlobeMath. ceilI(data.y - data.sqrtMaxThickness),
					BigGlobeMath. ceilI(data.z - data.radius),
					BigGlobeMath.floorI(data.x + data.radius),
					BigGlobeMath.floorI(data.y + data.sqrtMaxThickness),
					BigGlobeMath.floorI(data.z + data.radius)
				),
				data
			);
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generateRaw(Context context) {
			ChunkPos chunkPos = context.chunk.getPos();
			int minX = chunkPos.getStartX();
			int minZ = chunkPos.getStartZ();
			int maxX = minX | 15;
			int maxZ = minZ | 15;
			BlockPos.Mutable pos = new BlockPos.Mutable();
			try (NumberArray thicknessSamples = NumberArray.allocateDoublesDirect(16)) {
				for (int z = minZ; z <= maxZ; z++) {
					pos.setZ(z);
					double offsetZ2 = BigGlobeMath.squareD(z - this.data.z);
					this.data.noise.getBulkX(context.pieceSeed, minX, z, thicknessSamples);
					for (int x = minX; x <= maxX; x++) {
						pos.setX(x);
						double offsetXZ2 = offsetZ2 + BigGlobeMath.squareD(x - this.data.x);
						if (!(offsetXZ2 < BigGlobeMath.squareD(this.data.radius))) continue;
						double thickness = thicknessSamples.getD(x & 15) - BigGlobeMath.squareD(offsetXZ2 / BigGlobeMath.squareD(this.data.radius)) * this.data.noise.maxValue();
						for (int y = BigGlobeMath.floorI(this.data.y); BigGlobeMath.squareD(y - this.data.y) <= thickness; y--) {
							pos.setY(y);
							context.chunk.setBlockState(pos, this.data.fluid != null && y < this.data.fluid.level ? this.data.fluid.state : BlockStates.AIR, false);
						}
						for (int y = BigGlobeMath.floorI(this.data.y) + 1; BigGlobeMath.squareD(y - this.data.y) <= thickness; y++) {
							pos.setY(y);
							context.chunk.setBlockState(pos, this.data.fluid != null && y < this.data.fluid.level ? this.data.fluid.state : BlockStates.AIR, false);
						}
					}
				}
			}
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
			int minX = chunkPos.getStartX();
			int minZ = chunkPos.getStartZ();
			int maxX = minX | 15;
			int maxZ = minZ | 15;
			BlockPos.Mutable pos = new BlockPos.Mutable();
			WorldColumn column = WorldColumn.forWorld(world, 0, 0);
			Permuter permuter = new Permuter(0L);
			try (NumberArray thicknessSamples = NumberArray.allocateDoublesDirect(16)) {
				for (int z = minZ; z <= maxZ; z++) {
					pos.setZ(z);
					double offsetZ2 = BigGlobeMath.squareD(z - this.data.z);
					this.data.noise.getBulkX(world.getSeed(), minX, z, thicknessSamples);
					for (int x = minX; x <= maxX; x++) {
						pos.setX(x);
						double offsetXZ2 = offsetZ2 + BigGlobeMath.squareD(x - this.data.x);
						if (!(offsetXZ2 < BigGlobeMath.squareD(this.data.radius))) continue;
						column.setPos(x, z);
						double thickness = Math.sqrt(thicknessSamples.getD(x & 15) - BigGlobeMath.squareD(offsetXZ2 / BigGlobeMath.squareD(this.data.radius)) * this.data.noise.maxValue());
						if (thickness > 0.0D) {
							int minY = BigGlobeMath. ceilI(this.data.y - thickness);
							int maxY = BigGlobeMath.floorI(this.data.y + thickness);
							if (this.data.floor != null && this.data.floor.surface != null) {
								this.generateSurface(world, pos.setY(minY - 1), this.data.floor.surface, column, -1, permuter);
							}
							if (this.data.ceiling != null && this.data.ceiling.surface != null) {
								this.generateSurface(world, pos.setY(maxY + 1), this.data.ceiling.surface, column, 1, permuter);
							}
							boolean warn = System.currentTimeMillis() >= this.data.nextWarnTime;
							//noinspection NonShortCircuitBooleanExpression
							if (
								(
									this.data.floor != null &&
									this.data.floor.decorator != null &&
									this.generateFeature(world, pos.setY(minY), chunkGenerator, random, this.data.floor.decorator, "floor.decorator", warn)
								)
								| (
									this.data.ceiling != null &&
									this.data.ceiling.decorator != null &&
									this.generateFeature(world, pos.setY(maxY), chunkGenerator, random, this.data.ceiling.decorator, "ceiling.decorator", warn)
								)
								| (
									this.data.fluid != null &&
									this.data.fluid.decorator != null &&
									this.data.fluid.level > minY &&
									this.data.fluid.level < maxY &&
									this.generateFeature(world, pos.setY(this.data.fluid.level), chunkGenerator, random, this.data.fluid.decorator, "fluid.decorator", warn)
								)
							) {
								this.data.nextWarnTime = System.currentTimeMillis() + 5000L;
							}
						}
					}
				}
			}
		}

		public void generateSurface(
			StructureWorldAccess world,
			BlockPos.Mutable pos,
			SurfaceConfig surfaceConfig,
			WorldColumn column,
			int dy,
			Permuter permuter
		) {
			permuter.setSeed(Permuter.permute(world.getSeed() ^ 0xBC348ECC95D7CB9CL, pos));
			column.setPos(pos.getX(), pos.getZ());
			int depth = BigGlobeMath.floorI(surfaceConfig.depth.evaluate(column, pos.getY(), permuter));
			if (depth > 0) {
				world.setBlockState(pos, surfaceConfig.top, Block.NOTIFY_LISTENERS);
				for (int loop = 1; loop < depth; loop++) {
					world.setBlockState(pos.setY(pos.getY() + dy), surfaceConfig.under, Block.NOTIFY_LISTENERS);
				}
			}
		}

		/**
		generates the given feature at the given position,
		and returns true if an error was logged.
		*/
		public boolean generateFeature(
			StructureWorldAccess world,
			BlockPos pos,
			ChunkGenerator generator,
			Random random,
			TagOrObjectKey<ConfiguredFeature<?, ?>> key,
			String type,
			boolean warn
		) {
			TagOrObject<ConfiguredFeature<?, ?>> tagOrObject = key.resolve(world.getRegistryManager(), message -> {
				if (warn) BigGlobeMod.LOGGER.warn("Error with " + type + " in underground_pocket structure: " + message.get());
				return null;
			});
			if (tagOrObject == null) return warn;

			int index = 0;
			long seed = Permuter.permute(Permuter.permute(world.getSeed() ^ 0x90551635CB49F3F7L, type.hashCode() /* should always be interned, so fast. */), pos);
			for (RegistryEntry<ConfiguredFeature<?, ?>> entry : tagOrObject) {
				random.setSeed(Permuter.permute(seed, index++));
				entry.value().generate(world, generator, random, pos);
			}
			return false;
		}

		@Override
		public void translate(int x, int y, int z) {
			super.translate(x, y, z);
			Data data = this.data;
			data.x += x;
			data.y += y;
			data.z += z;
			if (data.fluid != null) data.fluid.level += y;
		}
	}
}