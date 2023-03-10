package builderb0y.bigglobe.structures;

import java.util.Optional;
import java.util.concurrent.RecursiveAction;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.math.pointSequences.HaltonIterator2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.Dvec3;

public class GeodeStructure extends BigGlobeStructure implements RawOverworldGenerationStructure {

	public static final Codec<GeodeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(GeodeStructure.class);

	public final Grid3D noise;
	public final RandomSource radius;
	public final BlocksConfig @VerifySizeRange(min = 1) @UseVerifier(name = "verifySorted", in = BlocksConfig.class, usage = MemberUsage.METHOD_IS_HANDLER) [] blocks;
	public final SpikesConfig spikes;
	public final @VerifyNullable TagKey<Block> growth_block_tag;

	public GeodeStructure(
		Config config,
		Grid3D noise,
		RandomSource radius,
		BlocksConfig[] blocks,
		SpikesConfig spikes,
		@VerifyNullable TagKey<Block> growth_block_tag
	) {
		super(config);
		this.noise = noise;
		this.radius = radius;
		this.blocks = blocks;
		this.spikes = spikes;
		this.growth_block_tag = growth_block_tag;
	}

	public static record BlocksConfig(
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double threshold,
		IRandomList<@UseName("state") BlockState> states
	) {

		public static <T_Encoded> void verifySorted(VerifyContext<T_Encoded, BlocksConfig[]> context) throws VerifyException {
			BlocksConfig[] array = context.object;
			if (array == null || array.length == 0) return;
			double threshold = array[0].threshold;
			for (int index = 1, length = array.length; index < length; index++) {
				double newThreshold = array[index].threshold;
				if (newThreshold > threshold) threshold = newThreshold;
				else throw new VerifyException(context.pathToStringBuilder().append(" must be sorted by threshold in ascending order.").toString());
			}
		}

		public boolean contains(BlockState state) {
			for (BlockState compare : this.states) {
				if (compare == state) return true;
			}
			return false;
		}
	}

	public static record SpikesConfig(
		RandomSource large_radius,
		RandomSource small_radius,
		RandomSource length,
		RandomSource commonness,
		RandomSource crookedness
	) {}

	public static void nextUnitVector(HaltonIterator2D iterator, Dvec3 vector) {
		iterator.next();
		double x = iterator.x;
		double y = iterator.y * BigGlobeMath.TAU;
		double r = Math.sqrt(1.0D - x * x);
		vector.set(Math.cos(y) * r, Math.sin(y) * r, x);
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		double radius = this.radius.get(context.random().nextLong());
		Dvec3 center = randomPosInChunk(context, radius, radius);
		if (center == null) return Optional.empty();

		long worldSeed = context.seed();
		long seed = chunkSeed(context, 0xD7F5815E2C4EAFCAL);
		return Optional.of(
			new StructurePosition(
				new BlockPos(center),
				(StructurePiecesCollector collector) -> {
					MainPiece mainPiece = new MainPiece(
						BigGlobeStructures.GEODE_PIECE_TYPE,
						center.x,
						center.y,
						center.z,
						radius,
						this.noise,
						this.blocks,
						this.growth_block_tag
					);
					collector.addPiece(mainPiece);
					//Grid3D grid = this.noise;
					//double rcpRadiusSquared = grid.max() / (radius * radius);
					Permuter permuter = new Permuter(seed);
					HaltonIterator2D iterator = new HaltonIterator2D(-1.0D, 0.0D, 1.0D, 1.0D, permuter.nextInt() & 0xFFFF);
					BlocksConfig lastConfig = this.blocks[this.blocks.length - 1];
					double secondLastThreshold = this.blocks.length > 1 ? this.blocks[this.blocks.length - 2].threshold : 0.0D;
					Dvec3
						unit   = new Dvec3(),
						point1 = new Dvec3(),
						point2 = new Dvec3();
					int spikeCount = (int)(radius * radius * this.spikes.commonness.get(permuter));
					spikeLoop:
					for (int spikeIndex = 0; spikeIndex < spikeCount; spikeIndex++) {
						nextUnitVector(iterator, unit);
						binarySearch: {
							double minRadius = 0.0D, maxRadius = radius;
							for (int refine = 0; refine < 8; refine++) {
								double midRadius = (minRadius + maxRadius) * 0.5D;
								point1.set(unit).multiply(midRadius).add(center);
								double noise = mainPiece.getNoise(
									BigGlobeMath.floorI(point1.x),
									BigGlobeMath.floorI(point1.y),
									BigGlobeMath.floorI(point1.z),
									worldSeed
								);
								if (noise > lastConfig.threshold) {
									minRadius = midRadius;
								}
								else if (noise < secondLastThreshold) {
									maxRadius = midRadius;
								}
								else {
									break binarySearch;
								}
							}
							continue spikeLoop;
						}
						point2
						.set(unit)
						.multiply(-this.spikes.length.get(permuter))
						.add(point1)
						.add(unit.setInSphere(permuter, this.spikes.crookedness.get(permuter)));
						collector.addPiece(
							new SpikePiece(
								BigGlobeStructures.GEODE_SPIKE_PIECE_TYPE,
								point1.x,
								point1.y,
								point1.z,
								this.spikes.large_radius.get(permuter),
								point2.x,
								point2.y,
								point2.z,
								this.spikes.small_radius.get(permuter),
								lastConfig.states
							)
						);
					}
				}
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.GEODE_TYPE;
	}

	public static class MainPiece extends DataStructurePiece<MainPiece.Data> implements RawOverworldGenerationStructurePiece {

		public static record Data(
			double x,
			double y,
			double z,
			@UseName("r") double radius,
			Grid3D noise,
			BlocksConfig[] blocks,
			@UseName("gbt") @VerifyNullable TagKey<Block> growth_block_tag
		) {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);
		}

		public MainPiece(StructurePieceType type, double x, double y, double z, double radius, Grid3D noise, BlocksConfig[] blocks, TagKey<Block> growth_block_tag) {
			super(
				type,
				0,
				new BlockBox(
					BigGlobeMath. ceilI(x - radius),
					BigGlobeMath. ceilI(y - radius),
					BigGlobeMath. ceilI(z - radius),
					BigGlobeMath.floorI(x + radius),
					BigGlobeMath.floorI(y + radius),
					BigGlobeMath.floorI(z + radius)
				),
				new Data(x, y, z, radius, noise, blocks, growth_block_tag)
			);
		}

		public MainPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		public double getNoise(int x, int y, int z, long seed) {
			return (
				this.data.noise.getValue(seed, x, y, z)
				- (
					BigGlobeMath.squareD(
						x - this.data.x,
						y - this.data.y,
						z - this.data.z
					)
					* this.data.noise.maxValue()
					/ BigGlobeMath.squareD(this.data.radius)
				)
			);
		}

		@Override
		public void generateRaw(RawOverworldGenerationStructurePiece.Context context) {
			ChunkPos chunkPos = context.chunk.getPos();
			int minX = chunkPos.getStartX();
			int minY = Math.max(this.boundingBox.getMinY(), context.chunk.getBottomY());
			int minZ = chunkPos.getStartZ();
			int maxX = chunkPos.getEndX();
			int maxY = Math.min(this.boundingBox.getMaxY(), context.chunk.getTopY() - 1);
			int maxZ = chunkPos.getEndZ();
			double[] samples = new double[maxY - minY + 1];
			double rcpRadius = 1.0D / this.data.radius;
			double noiseMax = this.data.noise.maxValue();
			Permuter permuter = new Permuter(Permuter.permute(context.seed ^ 0x963A72388F228396L, chunkPos));
			BlockPos.Mutable pos = new BlockPos.Mutable();
			for (int z = minZ; z <= maxZ; z++) {
				pos.setZ(z);
				double rz = BigGlobeMath.squareD((z - this.data.z) * rcpRadius);
				for (int x = minX; x <= maxX; x++) {
					pos.setX(x);
					double rxz = rz + BigGlobeMath.squareD((x - this.data.x) * rcpRadius);
					this.data.noise.getBulkY(context.seed, x, minY, z, samples, samples.length);
					for (int y = minY; y <= maxY; y++) {
						pos.setY(y);
						double rxyz = rxz + BigGlobeMath.squareD((y - this.data.y) * rcpRadius);
						double noise = samples[y - minY];
						noise -= rxyz * noiseMax;
						placed:
						if (noise > 0.0D) {
							for (BlocksConfig block : this.data.blocks) {
								if (noise < block.threshold) {
									context.chunk.setBlockState(pos, block.states.getRandomElement(permuter), false);
									break placed;
								}
							}
							context.chunk.setBlockState(pos, BlockStates.AIR, false);
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
			if (this.data.growth_block_tag == null) return;
			BlocksConfig[] blocks = this.data.blocks;
			if (blocks.length == 0) return;
			BlocksConfig blockConfig = blocks[blocks.length - 1];
			RegistryEntryList<Block> buds = Registry.BLOCK.getEntryList(this.data.growth_block_tag).orElse(null);
			if (buds == null || buds.size() == 0) return;
			int minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
			int minY = Math.max(this.boundingBox.getMinY(), chunkBox.getMinY());
			int minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
			int maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
			int maxY = Math.min(this.boundingBox.getMaxY(), chunkBox.getMaxY());
			int maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
			BlockPos.Mutable pos = new BlockPos.Mutable();
			Permuter permuter = new Permuter(Permuter.permute(world.getSeed() ^ 0x13AFC86BC0528060L, chunkPos));
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						if (world.isAir(pos.set(x, y, z))) {
							Direction direction = Permuter.choose(permuter, Directions.ALL);
							if (blockConfig.contains(world.getBlockState(pos.move(direction)))) {
								BlockState toPlace = (
									buds
									.getRandom(permuter.mojang())
									.orElseThrow()
									.value()
									.getDefaultState()
								);
								if (toPlace.contains(Properties.FACING)) {
									toPlace = toPlace.with(Properties.FACING, direction.getOpposite());
								}
								world.setBlockState(pos.set(x, y, z), toPlace, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							}
						}
					}
				}
			}
		}

		/**
		allows computing noise values for one column while
		a different column uses that noise to place blocks.
		this class is no longer used because of a rare race
		condition which hangs one of the worldgen threads.
		*/
		@Deprecated
		public static class NoiseSwapGenerateTask extends RecursiveAction {

			public final int minX, minY, minZ, maxX, maxY, maxZ;
			public final double centerX, centerY, centerZ;
			public final double rcpRadius;
			public final long seed;
			public final Grid3D grid;
			public double[] noise1, noise2;

			public int currentX, currentZ;

			public NoiseSwapGenerateTask(
				int minX,
				int minY,
				int minZ,
				int maxX,
				int maxY,
				int maxZ,
				double centerX,
				double centerY,
				double centerZ,
				double radius,
				long seed,
				Grid3D grid
			) {
				this.minX = minX;
				this.minY = minY;
				this.minZ = minZ;
				this.maxX = maxX;
				this.maxY = maxY;
				this.maxZ = maxZ;
				this.centerX = centerX;
				this.centerY = centerY;
				this.centerZ = centerZ;
				this.rcpRadius = 1.0D / radius;
				this.seed = seed;
				this.grid = grid;
				int verticalRange = maxY - minY + 1;
				this.noise1 = new double[verticalRange];
				this.noise2 = new double[verticalRange];
				this.currentX = minX;
				this.currentZ = minZ;
				this.fork();
			}

			public NoiseSwapGenerateTask(
				int minX,
				int minY,
				int minZ,
				int maxX,
				int maxY,
				int maxZ,
				Data data,
				long seed
			) {
				this(
					minX,
					minY,
					minZ,
					maxX,
					maxY,
					maxZ,
					data.x,
					data.y,
					data.z,
					data.radius,
					seed,
					data.noise
				);
			}

			public double[] swap() {
				this.quietlyJoin();
				if (++this.currentX > this.maxX) {
					this.currentX = this.minX;
					if (++this.currentZ > this.maxZ) {
						return this.noise1; //done!
					}
				}
				this.reinitialize();
				double[] toReturn = this.noise1;
				this.noise1 = this.noise2;
				this.noise2 = toReturn;
				this.fork();
				return toReturn;
			}

			@Override
			public void compute() {
				this.grid.getBulkY(this.seed, this.currentX, this.minY, this.currentZ, this.noise1, this.maxY - this.minY + 1);
				double rxz = BigGlobeMath.squareD(
					(this.currentX - this.centerX) * this.rcpRadius,
					(this.currentZ - this.centerZ) * this.rcpRadius
				);
				double noiseMax = this.grid.maxValue();
				for (int y = this.minY; y <= this.maxY; y++) {
					double rxyz = rxz + BigGlobeMath.squareD((y - this.centerY) * this.rcpRadius);
					this.noise1[y - this.minY] -= rxyz * noiseMax;
				}
			}
		}
	}

	public static class SpikePiece extends DataStructurePiece<SpikePiece.Data> implements RawOverworldGenerationStructurePiece {

		public static record Data(
			double x1,
			double y1,
			double z1,
			double r1,
			double x2,
			double y2,
			double z2,
			double r2,
			IRandomList<@UseName("state") BlockState> states
		) {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);
		}

		public SpikePiece(
			StructurePieceType type,
			double x1,
			double y1,
			double z1,
			double r1,
			double x2,
			double y2,
			double z2,
			double r2,
			IRandomList<BlockState> states
		) {
			super(
				type,
				0,
				new BlockBox(
					BigGlobeMath. ceilI(Math.min(x1 - r1, x2 - r2)),
					BigGlobeMath. ceilI(Math.min(y1 - r1, y2 - r2)),
					BigGlobeMath. ceilI(Math.min(z1 - r1, z2 - r2)),
					BigGlobeMath.floorI(Math.max(x1 + r1, x2 + r2)),
					BigGlobeMath.floorI(Math.max(y1 + r1, y2 + r2)),
					BigGlobeMath.floorI(Math.max(z1 + r1, z2 + r2))
				),
				new Data(x1, y1, z1, r1, x2, y2, z2, r2, states)
			);
		}

		public SpikePiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generateRaw(RawOverworldGenerationStructurePiece.Context context) {
			Data data = this.data;
			ChunkPos chunkPos = context.chunk.getPos();
			Permuter permuter = new Permuter(Permuter.permute(context.seed ^ 0xA895D1EC06824D06L, data.x1, data.y1, data.z1));
			int minX = chunkPos.getStartX();
			int minY = Math.max(this.boundingBox.getMinY(), context.chunk.getBottomY());
			int minZ = chunkPos.getStartZ();
			int maxX = chunkPos.getEndX();
			int maxY = Math.min(this.boundingBox.getMaxY(), context.chunk.getTopY() - 1);
			int maxZ = chunkPos.getEndZ();

			Dvec3 spikeOffset = new Dvec3(data.x2 - data.x1, data.y2 - data.y1, data.z2 - data.z1);
			Dvec3 relativePos = new Dvec3();
			Dvec3 nearest     = new Dvec3();
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int y = minY; y <= maxY; y++) {
						relativePos.set(x - data.x1, y - data.y1, z - data.z1);
						double dot = spikeOffset.dot(relativePos);
						double fraction = dot / spikeOffset.lengthSquared();
						fraction = MathHelper.clamp(fraction, 0.0D, 1.0D);
						nearest.set(spikeOffset).multiply(fraction);
						double distanceSquared = relativePos.distanceSquared(nearest);
						double thresholdSquared = BigGlobeMath.squareD(Interpolator.mixLinear(data.r1, data.r2, fraction));
						if (distanceSquared < thresholdSquared && context.chunk.getBlockState(mutablePos.set(x, y, z)).isAir()) {
							context.chunk.setBlockState(mutablePos, data.states.getRandomElement(permuter), false);
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
		) {}
	}
}