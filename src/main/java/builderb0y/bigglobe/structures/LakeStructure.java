package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.joml.Vector3d;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.settings.OverworldBiomeLayout.PrimarySurface;
import builderb0y.bigglobe.settings.OverworldBiomeLayout.SecondarySurface;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.randomSources.RandomSource;

public class LakeStructure extends BigGlobeStructure implements RawGenerationStructure {

	public static final int MUD_BLEND_HEIGHT = 4;
	public static final int OUTER_CIRCLE_SAMPLES = 16;
	static {
		assert (OUTER_CIRCLE_SAMPLES & (OUTER_CIRCLE_SAMPLES - 1)) == 0;
		assert OUTER_CIRCLE_SAMPLES >> 2 << 2 == OUTER_CIRCLE_SAMPLES;
	}
	public static final double[] SIN16_CACHE = new double[OUTER_CIRCLE_SAMPLES];
	static {
		for (int angleIndex = 0; angleIndex < 16; angleIndex++) {
			SIN16_CACHE[angleIndex] = Math.sin(angleIndex * (BigGlobeMath.TAU / OUTER_CIRCLE_SAMPLES));
		}
	}
	public static final Codec<LakeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LakeStructure.class);

	public final RandomSource horizontal_radius, vertical_depth;
	public final BlockState fluid;
	public final @VerifyNullable PrimarySurface primary_surface;
	public final SecondarySurface @VerifyNullable @SingletonArray [] secondary_surfaces;
	public final @VerifyNullable SortedFeatureTag fluid_surface_feature;

	public LakeStructure(
		Config config,
		RandomSource horizontal_radius,
		RandomSource vertical_depth,
		BlockState fluid,
		@VerifyNullable PrimarySurface primary_surface,
		SecondarySurface @VerifyNullable [] secondary_surfaces,
		@VerifyNullable SortedFeatureTag fluid_surface_feature
	) {
		super(config);
		this.horizontal_radius     = horizontal_radius;
		this.vertical_depth        = vertical_depth;
		this.fluid                 = fluid;
		this.primary_surface       = primary_surface;
		this.secondary_surfaces    = secondary_surfaces;
		this.fluid_surface_feature = fluid_surface_feature;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		Vector3d pos = randomPosAtSurface(context, 0.0D);
		if (pos == null) return Optional.empty();

		Permuter permuter = Permuter.from(context.random());
		double horizontalRadius = this.horizontal_radius.get(permuter);
		double centerHeight = pos.y;
		long seed = permuter.nextLong();
		WorldColumn column = WorldColumn.forGenerator(context.seed(), context.chunkGenerator(), context.noiseConfig(), 0, 0);
		for (int angleIndex = 0; angleIndex < OUTER_CIRCLE_SAMPLES; angleIndex++) {
			double x = pos.x + horizontalRadius * SIN16_CACHE[(angleIndex + (OUTER_CIRCLE_SAMPLES >> 2)) & (OUTER_CIRCLE_SAMPLES - 1)];
			double z = pos.z + horizontalRadius * SIN16_CACHE[angleIndex];
			column.setPosUnchecked(BigGlobeMath.floorI(x), BigGlobeMath.floorI(z));
			double height = column.getFinalTopHeightD();
			if (height < pos.y) pos.y = height;
		}
		double verticalDepth = (centerHeight - pos.y - MUD_BLEND_HEIGHT) + horizontalRadius * this.vertical_depth.get(permuter);

		Piece piece = new Piece(
			BigGlobeStructures.LAKE_PIECE_TYPE,
			pos.x,
			pos.y,
			pos.z,
			horizontalRadius,
			verticalDepth,
			this.fluid,
			this.primary_surface,
			this.secondary_surfaces,
			seed,
			this.fluid_surface_feature
		);
		StructurePiecesCollector collector = new StructurePiecesCollector();
		collector.addPiece(piece);
		return Optional.of(
			new StructurePosition(
				new BlockPos(pos.x, pos.y, pos.z),
				Either.right(collector)
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.LAKE_TYPE;
	}

	public static class Piece extends DataStructurePiece<Piece.Data> implements RawGenerationStructurePiece {

		public static record Data(
			double x,
			double y,
			double z,
			@UseName("hr") double horizontal_radius,
			@UseName("vd") double vertical_depth,
			@UseName("fl") BlockState fluid,
			@UseName("ps") @VerifyNullable PrimarySurface primary_surface,
			SecondarySurface @UseName("ss") @SingletonArray @VerifyNullable [] secondary_surfaces,
			long seed,
			@UseName("fsf") @VerifyNullable SortedFeatureTag fluid_surface_feature
		) {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public int waterSurface() {
				return BigGlobeMath.ceilI(this.y) - MUD_BLEND_HEIGHT;
			}
		}

		public Piece(
			StructurePieceType type,
			double x,
			double y,
			double z,
			double horizontal_radius,
			double vertical_depth,
			BlockState fluid,
			@VerifyNullable PrimarySurface primary_surface,
			SecondarySurface @VerifyNullable [] secondary_surfaces,
			long seed,
			SortedFeatureTag fluid_surface_feature
		) {
			super(
				type,
				0,
				new BlockBox(
					BigGlobeMath.floorI(x - horizontal_radius),
					BigGlobeMath.floorI(y - vertical_depth),
					BigGlobeMath.floorI(z - horizontal_radius),
					BigGlobeMath.ceilI(x + horizontal_radius),
					BigGlobeMath.ceilI(y),
					BigGlobeMath.ceilI(z + horizontal_radius)
				),
				new Data(x, y, z, horizontal_radius, vertical_depth, fluid, primary_surface, secondary_surfaces, seed, fluid_surface_feature)
			);
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		public boolean isInsideCircle(int x, int z) {
			return BigGlobeMath.squareD(
				x - this.data.x,
				z - this.data.z
			)
			< BigGlobeMath.squareD(this.data.horizontal_radius);
		}

		@Override
		public void generateRaw(Context context) {
			//if (true) return;
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			Data data = this.data;
			BlockState fluid = data.fluid();
			int maxY = data.waterSurface();
			for (int index = 0; index < 256; index++) {
				WorldColumn column = context.columns.getColumn(index);
				if (this.isInsideCircle(column.x, column.z)) {
					mutablePos.setX(column.x).setZ(column.z);
					int minY = column.getFinalTopHeightI();
					for (int y = minY; y < maxY; y++) {
						context.chunk.setBlockState(mutablePos.setY(y), fluid, false);
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
			SortedFeatureTag featureEntry = this.data.fluid_surface_feature;
			if (featureEntry == null) return;

			ConfiguredFeature<?, ?>[] features = featureEntry.getSortedFeatures(world);
			int featureCount = features.length;
			if (featureCount == 0) return;

			int minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
			int minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
			int maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
			int maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
			int waterSurface = this.data.waterSurface();
			BlockPos.Mutable pos = new BlockPos.Mutable();
			Permuter permuter = new Permuter(0L);
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int index = 0; index < featureCount; index++) {
						permuter.setSeed(Permuter.permute(world.getSeed() ^ 0xA8E28B8E72CA658DL, x, z, index));
						features[index].generate(
							world,
							chunkGenerator,
							permuter.mojang(),
							pos.set(x, waterSurface, z)
						);
					}
				}
			}
		}

		/** used by {@link BigGlobeOverworldChunkGenerator#runHeightOverrides(OverworldColumn, ScriptStructures, boolean)}. */
		public double getDip(int x, int z, double distance) {
			double distanceFraction = distance / this.data.horizontal_radius;
			double angle = Math.atan2(z - this.data.z, x - this.data.x) * (1.0D / BigGlobeMath.TAU) + 0.5D;
			double noiseAmplitude = 16.0D * BigGlobeMath.squareD(distanceFraction - distanceFraction * distanceFraction);
			double dip = Interpolator.smooth(distanceFraction) - 1.0D;
			dip += radialMix(this.data.seed, distance, angle) * noiseAmplitude;
			return dip * this.data.vertical_depth;
		}

		public static double radialMix(long seed, double radius, double angle) {
			radius *= 0.0625D;
			int     floorRadius = (int)(radius);
			int      ceilRadius = floorRadius + 1;
			double  fractRadius = radius - floorRadius;
			double smoothRadius = Interpolator.smooth(fractRadius);
			double sum = 0.0D;
			for (int index = 1; index <= floorRadius; index++) {
				sum += radialNoise(Permuter.permute(seed, index), index << 3, angle) / (index + 1);
			}
			sum += radialNoise(Permuter.permute(seed, ceilRadius), ceilRadius << 3, angle) * smoothRadius / (ceilRadius + 1);
			return sum;
		}

		public static double radialNoise(long seed, int scale, double angle) {
			angle *= scale;
			int floorCoord = BigGlobeMath.floorI(angle);
			if (floorCoord >= scale) floorCoord -= scale;
			int ceilCoord = floorCoord + 1;
			if (ceilCoord >= scale) ceilCoord -= scale;
			double fractCoord = angle - floorCoord;
			double smoothCoord = Interpolator.smooth(fractCoord);
			return Interpolator.mixLinear(
				hash(seed, floorCoord),
				hash(seed, ceilCoord),
				smoothCoord
			);
		}

		public static double hash(long seed, int coord) {
			return Permuter.toUniformDouble(Permuter.permute(seed, coord));
		}
	}
}