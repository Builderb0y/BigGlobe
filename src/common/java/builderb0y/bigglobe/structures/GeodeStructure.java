package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.math.pointSequences.PointIterator3D;
import builderb0y.bigglobe.math.pointSequences.SphericalPointIterator;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.versions.ChunkVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public class GeodeStructure extends BigGlobeStructure implements RawGenerationStructure {

	public static final MapCodec<GeodeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(GeodeStructure.class);

	public final Grid3D noise;
	public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 112.0D) RandomSource radius;
	public final BlocksConfig @VerifyNotEmpty @UseVerifier(name = "verifySorted", in = BlocksConfig.class, usage = MemberUsage.METHOD_IS_HANDLER) [] blocks;
	public final SpikesConfig spikes;
	public final GrowthConfig @VerifyNullable @SingletonArray [] growth;

	public GeodeStructure(
		StructureSettings config,
		ColumnToIntScript.@VerifyNullable Catcher min_y,
		ColumnToIntScript.@VerifyNullable Catcher surface_y,
		Grid3D noise,
		RandomSource radius,
		BlocksConfig[] blocks,
		SpikesConfig spikes,
		GrowthConfig @VerifyNullable [] growth
	) {
		super(config, min_y, surface_y);
		this.noise = noise;
		this.radius = radius;
		this.blocks = blocks;
		this.spikes = spikes;
		this.growth = growth;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return (int)(this.radius.maxValue() * 0.0625D);
	}

	public static record GrowthConfig(
		DelayedEntryList<Block> place,
		DelayedEntryList<Block> against
	) {}

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
				else throw new VerifyException(() -> context.pathToStringBuilder().append(" must be sorted by threshold in ascending order.").toString());
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

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		BlockPos centerPos = this.randomBlockInChunk(context, this.radius.maxValue(), BigGlobeMath.ceilI(this.radius.maxValue()));
		if (centerPos == null) return Optional.empty();
		ScriptedColumn column;
		if (context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			column = generator.newColumn(context.heightAccessor(), centerPos.getX(), centerPos.getZ(), ColumnUsage.GENERIC.maybeDhHints());
		}
		else {
			return Optional.empty();
		}
		Permuter permuter = new Permuter(context.random().nextLong());
		double radius = this.radius.get(column, centerPos.getY(), permuter);
		long worldSeed = generator.columnSeed;
		return Optional.of(
			new GenerationStub(
				centerPos,
				(StructurePiecesBuilder collector) -> {
					double centerX = centerPos.getX() + permuter.nextDouble();
					double centerY = centerPos.getY() + permuter.nextDouble();
					double centerZ = centerPos.getZ() + permuter.nextDouble();
					MainPiece mainPiece = new MainPiece(
						BigGlobeStructures.GEODE_PIECE_TYPE,
						centerX,
						centerY,
						centerZ,
						radius,
						this.noise,
						this.blocks,
						this.growth
					);
					collector.addPiece(mainPiece);
					PointIterator3D iterator = SphericalPointIterator.halton(permuter.nextInt() & 0xFFFF, 1.0D);
					BlocksConfig lastConfig = this.blocks[this.blocks.length - 1];
					double secondLastThreshold = this.blocks.length > 1 ? this.blocks[this.blocks.length - 2].threshold : 0.0D;
					Vector3d
						unit = new Vector3d(),
						point1 = new Vector3d(),
						point2 = new Vector3d();
					int spikeCount = (int)(radius * radius * this.spikes.commonness.get(column, centerPos.getY(), permuter));
					spikeLoop:
					for (int spikeIndex = 0; spikeIndex < spikeCount; spikeIndex++) {
						iterator.next();
						unit.set(iterator.x(), iterator.y(), iterator.z());
						binarySearch:
						{
							double minRadius = 0.0D, maxRadius = radius;
							for (int refine = 0; refine < 8; refine++) {
								double midRadius = (minRadius + maxRadius) * 0.5D;
								point1.set(unit).mul(midRadius).add(centerX, centerY, centerZ);
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
							.mul(-this.spikes.length.get(column, centerPos.getY(), permuter))
							.add(point1)
							.add(Vectors.setInSphere(unit, permuter, this.spikes.crookedness.get(column, centerPos.getY(), permuter)));
						collector.addPiece(
							new SpikePiece(
								BigGlobeStructures.GEODE_SPIKE_PIECE_TYPE,
								point1.x,
								point1.y,
								point1.z,
								this.spikes.large_radius.get(column, centerPos.getY(), permuter),
								point2.x,
								point2.y,
								point2.z,
								this.spikes.small_radius.get(column, centerPos.getY(), permuter),
								lastConfig.states
							)
						);
					}
				}
			)
		);
	}

	@Override
	public StructureType<?> type() {
		return BigGlobeStructures.GEODE_TYPE;
	}

	public static class MainPiece extends DataStructurePiece<MainPiece.Data> implements RawGenerationStructurePiece {

		public static class Data {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public double x, y, z;
			/**
			spikes may have already been positioned when {@link #move(int, int, int)}
			is called. since noise uses absolute coordinates, this is a way of un-translating
			the noise only, so that the spikes line up with the noise again.
			*/
			public @DefaultInt(0) int offsetX, offsetY, offsetZ;
			public @UseName("r") double radius;
			public Grid3D noise;
			public BlocksConfig[] blocks;
			public @UseName("gbt") GrowthConfig @VerifyNullable @SingletonArray [] growth;

			public Data(
				double x,
				double y,
				double z,
				int offsetX,
				int offsetY,
				int offsetZ,
				double radius,
				Grid3D noise,
				BlocksConfig[] blocks,
				GrowthConfig @VerifyNullable [] growth
			) {
				this.x = x;
				this.y = y;
				this.z = z;
				this.offsetX = offsetX;
				this.offsetY = offsetY;
				this.offsetZ = offsetZ;
				this.noise = noise;
				this.radius = radius;
				this.blocks = blocks;
				this.growth = growth;
			}
		}

		public MainPiece(
			StructurePieceType type,
			double x,
			double y,
			double z,
			double radius,
			Grid3D noise,
			BlocksConfig[] blocks,
			GrowthConfig[] growth
		) {
			super(
				type,
				0,
				new BoundingBox(
					BigGlobeMath.ceilI(x - radius),
					BigGlobeMath.ceilI(y - radius),
					BigGlobeMath.ceilI(z - radius),
					BigGlobeMath.floorI(x + radius),
					BigGlobeMath.floorI(y + radius),
					BigGlobeMath.floorI(z + radius)
				),
				new Data(x, y, z, 0, 0, 0, radius, noise, blocks, growth)
			);
		}

		public MainPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		public double getNoise(int x, int y, int z, long seed) {
			return (
				this.data.noise.getValue(
					seed,
					x - this.data.offsetX,
					y - this.data.offsetY,
					z - this.data.offsetZ
				)
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
		public void generateRaw(RawGenerationStructurePiece.Context context) {
			ChunkPos chunkPos = context.chunk.getPos();
			int minX = chunkPos.getMinBlockX();
			int minY = Math.max(this.boundingBox.minY(), HeightLimitViewVersions.getMinY(context.chunk));
			int minZ = chunkPos.getMinBlockZ();
			int maxX = chunkPos.getMaxBlockX();
			int maxY = Math.min(this.boundingBox.maxY(), HeightLimitViewVersions.getMaxY(context.chunk) - 1);
			int maxZ = chunkPos.getMaxBlockZ();
			try (NumberArray samples = NumberArray.allocateDoublesDirect(maxY - minY + 1)) {
				double rcpRadius = 1.0D / this.data.radius;
				double noiseMax = this.data.noise.maxValue();
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
				for (int z = minZ; z <= maxZ; z++) {
					pos.setZ(z);
					double rz = BigGlobeMath.squareD((z - this.data.z) * rcpRadius);
					for (int x = minX; x <= maxX; x++) {
						pos.setX(x);
						double rxz = rz + BigGlobeMath.squareD((x - this.data.x) * rcpRadius);
						this.data.noise.getBulkY(
							context.columnSeed,
							x - this.data.offsetX,
							minY - this.data.offsetY,
							z - this.data.offsetZ,
							samples
						);
						for (int y = minY; y <= maxY; y++) {
							pos.setY(y);
							double rxyz = rxz + BigGlobeMath.squareD((y - this.data.y) * rcpRadius);
							double noise = samples.implGetD(y - minY);
							noise -= rxyz * noiseMax;
							placed:
							if (noise > 0.0D) {
								for (BlocksConfig block : this.data.blocks) {
									if (noise < block.threshold) {
										ChunkVersions.setBlockState(
											context.chunk,
											pos,
											block.states.getRandomElement(
												Permuter.permute(context.columnSeed ^ 0x84DA20CB58CD2DFBL /* make sure this matches SpikePiece */, x, y, z
												)
											),
											Block.UPDATE_CLIENTS
										);
										break placed;
									}
								}
								ChunkVersions.setBlockState(context.chunk, pos, BlockStates.AIR, Block.UPDATE_CLIENTS);
							}
						}
					}
				}
			}
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			net.minecraft.util.RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			GrowthConfig[] growth = this.data.growth;
			if (growth == null || growth.length == 0) return;
			int minX = Math.max(this.boundingBox.minX(), chunkBox.minX());
			int minY = Math.max(this.boundingBox.minY(), chunkBox.minY());
			int minZ = Math.max(this.boundingBox.minZ(), chunkBox.minZ());
			int maxX = Math.min(this.boundingBox.maxX(), chunkBox.maxX());
			int maxY = Math.min(this.boundingBox.maxY(), chunkBox.maxY());
			int maxZ = Math.min(this.boundingBox.maxZ(), chunkBox.maxZ());
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			Permuter permuter = new Permuter(0L);
			long seed = world.getSeed() ^ 0x13AFC86BC0528060L;
			for (int y = minY; y <= maxY; y++) {
				long seedY = Permuter.permute(seed, y);
				for (int z = minZ; z <= maxZ; z++) {
					long seedZ = Permuter.permute(seedY, z);
					for (int x = minX; x <= maxX; x++) {
						if (world.isEmptyBlock(pos.set(x, y, z))) {
							long seedX = Permuter.permute(seedZ, x);
							permuter.setSeed(seedX);
							Direction direction = Permuter.choose(permuter, Directions.ALL);
							Block against = world.getBlockState(pos.move(direction)).getBlock();
							for (GrowthConfig growthConfig : growth) {
								if (growthConfig.against.contains(against) && !growthConfig.place.isEmpty()) {
									BlockState toPlace = growthConfig.place.randomObject(permuter).defaultBlockState();
									if (toPlace.hasProperty(BlockStateProperties.FACING)) {
										toPlace = toPlace.setValue(BlockStateProperties.FACING, direction.getOpposite());
									}
									world.setBlock(pos.set(x, y, z), toPlace, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
									break;
								} //if growth matches
							} //for growth
						} //if air
					} //for x
				} //for z
			} //for y
		} //method

		@Override
		public void move(int x, int y, int z) {
			super.move(x, y, z);
			this.data.x += x;
			this.data.y += y;
			this.data.z += z;
			this.data.offsetX += x;
			this.data.offsetY += y;
			this.data.offsetZ += z;
		}
	}

	public static class SpikePiece extends DataStructurePiece<SpikePiece.Data> implements RawGenerationStructurePiece {

		public static class Data {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public double x1, y1, z1, r1;
			public double x2, y2, z2, r2;
			public IRandomList<@UseName("state") BlockState> states;

			public Data(
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
				this.x1 = x1;
				this.y1 = y1;
				this.z1 = z1;
				this.r1 = r1;
				this.x2 = x2;
				this.y2 = y2;
				this.z2 = z2;
				this.r2 = r2;
				this.states = states;
			}
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
				new BoundingBox(
					BigGlobeMath.ceilI(Math.min(x1 - r1, x2 - r2)),
					BigGlobeMath.ceilI(Math.min(y1 - r1, y2 - r2)),
					BigGlobeMath.ceilI(Math.min(z1 - r1, z2 - r2)),
					BigGlobeMath.floorI(Math.max(x1 + r1, x2 + r2)),
					BigGlobeMath.floorI(Math.max(y1 + r1, y2 + r2)),
					BigGlobeMath.floorI(Math.max(z1 + r1, z2 + r2))
				),
				new Data(x1, y1, z1, r1, x2, y2, z2, r2, states)
			);
		}

		public SpikePiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generateRaw(RawGenerationStructurePiece.Context context) {
			Data data = this.data;
			ChunkPos chunkPos = context.chunk.getPos();
			int minX = chunkPos.getMinBlockX();
			int minY = Math.max(this.boundingBox.minY(), HeightLimitViewVersions.getMinY(context.chunk));
			int minZ = chunkPos.getMinBlockZ();
			int maxX = chunkPos.getMaxBlockX();
			int maxY = Math.min(this.boundingBox.maxY(), HeightLimitViewVersions.getMaxY(context.chunk) - 1);
			int maxZ = chunkPos.getMaxBlockZ();

			Vector3d spikeOffset = new Vector3d(data.x2 - data.x1, data.y2 - data.y1, data.z2 - data.z1);
			Vector3d relativePos = new Vector3d();
			Vector3d nearest = new Vector3d();
			BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int y = minY; y <= maxY; y++) {
						relativePos.set(x - data.x1, y - data.y1, z - data.z1);
						double dot = spikeOffset.dot(relativePos);
						double fraction = dot / spikeOffset.lengthSquared();
						fraction = Mth.clamp(fraction, 0.0D, 1.0D);
						nearest.set(spikeOffset).mul(fraction);
						double distanceSquared = relativePos.distanceSquared(nearest);
						double thresholdSquared = BigGlobeMath.squareD(Interpolator.mixLinear(data.r1, data.r2, fraction));
						if (distanceSquared < thresholdSquared && context.chunk.getBlockState(mutablePos.set(x, y, z)).isAir()) {
							ChunkVersions.setBlockState(
								context.chunk,
								mutablePos,
								data.states.getRandomElement(
									Permuter.permute(context.columnSeed ^ 0x84DA20CB58CD2DFBL /* make sure this matches MainPiece */, x, y, z)
								),
								Block.UPDATE_CLIENTS
							);
						}
					}
				}
			}
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			net.minecraft.util.RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {}

		@Override
		public void move(int x, int y, int z) {
			super.move(x, y, z);
			this.data.x1 += x;
			this.data.y1 += y;
			this.data.z1 += z;
			this.data.x2 += x;
			this.data.y2 += y;
			this.data.z2 += z;
		}
	}
}