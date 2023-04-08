package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.serialization.Codec;
import org.joml.Vector3d;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.nether.NoiseOverrider;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.Vectors;

public class NetherPillarStructure extends BigGlobeStructure {

	public static final Codec<NetherPillarStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(NetherPillarStructure.class);

	public final double per_chunk;
	public final RandomSource height;
	public final int spawn_attempts;
	public final double broken_chance;

	public NetherPillarStructure(
		Config config,
		double per_chunk,
		RandomSource height,
		int spawn_attempts,
		double broken_chance
	) {
		super(config);
		this.per_chunk = per_chunk;
		this.height = height;
		this.spawn_attempts = spawn_attempts;
		this.broken_chance = broken_chance;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		if (!(context.chunkGenerator() instanceof BigGlobeChunkGenerator generator)) return Optional.empty();
		WorldColumn column = generator.column(0, 0);
		Permuter permuter = Permuter.from(context.random());
		return Optional.of(
			new StructurePosition(
				context.chunkPos().getStartPos(),
				collector -> {
					for (int chunkAttempt = Permuter.roundRandomlyI(permuter, this.per_chunk); --chunkAttempt >= 0;) {
						double centerX = context.chunkPos().getStartX() + permuter.nextDouble(16.0D);
						double centerY = this.height.get(permuter);
						double centerZ = context.chunkPos().getStartZ() + permuter.nextDouble(16.0D);
						column.setPosUnchecked(BigGlobeMath.floorI(centerX), BigGlobeMath.floorI(centerZ));
						if (!column.isTerrainAt(BigGlobeMath.floorI(centerY), false)) {
							extendLoop:
							for (int spawnAttempt = this.spawn_attempts; --spawnAttempt >= 0;) {
								Vector3d pos1 = new Vector3d(centerX, centerY, centerZ);
								Vector3d pos2 = new Vector3d(centerX, centerY, centerZ);
								final int stepSize = 8;
								Vector3d direction = Vectors.setOnSphere(new Vector3d(), permuter, stepSize);
								direction.y *= 2.0D;
								end1: {
									for (int i = 0; i < 96 / stepSize; i++) {
										pos2.add(direction);
										column.setPosUnchecked(BigGlobeMath.floorI(pos2.x), BigGlobeMath.floorI(pos2.z));
										if (column.isTerrainAt(BigGlobeMath.floorI(pos2.y), false)) {
											break end1;
										}
									}
									continue extendLoop;
								}
								end2: {
									for (int i = 0; i < 96 / stepSize; i++) {
										pos1.sub(direction);
										column.setPosUnchecked(BigGlobeMath.floorI(pos1.x), BigGlobeMath.floorI(pos1.z));
										if (column.isTerrainAt(BigGlobeMath.floorI(pos1.y), false)) {
											break end2;
										}
									}
									continue extendLoop;
								}
								direction.normalize();
								double length = pos1.distance(pos2);
								double endRadius = permuter.nextDouble(0.5D, 1.0D) * Math.sqrt(length);
								double centerRadius = permuter.nextDouble(0.25D, 0.5D) * endRadius;
								if (Permuter.nextChancedBoolean(permuter, this.broken_chance)) centerRadius *= -0.5D;
								Box box = new Box(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z);
								BlockBox blockBox = new BlockBox(
									BigGlobeMath.floorI(box.minX - endRadius * 2.0D),
									BigGlobeMath.floorI(box.minY - endRadius * 2.0D),
									BigGlobeMath.floorI(box.minZ - endRadius * 2.0D),
									BigGlobeMath. ceilI(box.maxX + endRadius * 2.0D),
									BigGlobeMath. ceilI(box.maxY + endRadius * 2.0D),
									BigGlobeMath. ceilI(box.maxZ + endRadius * 2.0D)
								);
								collector.addPiece(
									new Piece(
										BigGlobeStructures.NETHER_PILLAR_PIECE,
										blockBox,
										new Piece.Data(
											pos1.x, pos1.y, pos1.z,
											pos2.x, pos2.y, pos2.z,
											direction.x, direction.y, direction.z,
											length,
											centerRadius,
											endRadius
										)
									)
								);
								break extendLoop;
							}
						}
					}
				}
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.NETHER_PILLAR;
	}

	public static class Piece extends DataStructurePiece<Piece.Data> {

		@RecordLike({ "x1", "y1", "z1", "x2", "y2", "z2", "centerRadius", "endRadius" })
		public static class Data {

			public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

			public final double x1, y1, z1;
			public final double x2, y2, z2;
			public final transient double vx, vy, vz;
			public final transient double vLength;
			public final @UseName("cr") double centerRadius;
			public final @UseName("er") double endRadius;

			public Data(
				double x1, double y1, double z1,
				double x2, double y2, double z2,
				double vx, double vy, double vz,
				double vLength,
				double centerRadius,
				double endRadius
			) {
				this.x1 = x1; this.y1 = y1; this.z1 = z1;
				this.x2 = x2; this.y2 = y2; this.z2 = z2;
				this.vx = vx; this.vy = vy; this.vz = vz;
				this.vLength      = vLength;
				this.centerRadius = centerRadius;
				this.endRadius    = endRadius;
			}

			public Data(
				double x1, double y1, double z1,
				double x2, double y2, double z2,
				double centerRadius,
				double endRadius
			) {
				double dx = x2 - x1;
				double dy = y2 - y1;
				double dz = z2 - z1;
				double length = Math.sqrt(BigGlobeMath.squareD(dx, dy, dz));
				this.x1 = x1; this.y1 = y1; this.z1 = z1;
				this.x2 = x2; this.y2 = y2; this.z2 = z2;
				this.vx = dx / length;
				this.vy = dy / length;
				this.vz = dz / length;
				this.vLength = length;
				this.centerRadius = centerRadius;
				this.endRadius = endRadius;
			}

			@Override
			public String toString() {
				return (
					"NetherPillarStructure.Piece.Data: { " +
					"pos1: " + this.x1 + ", " + this.y1 + ", " + this.z1 + ", " +
					"pos2: " + this.x2 + ", " + this.y2 + ", " + this.z2 + ", " +
					"v: " + this.vx + ", " + this.vy + ", " + this.vz + ", " +
					"vLength: " + this.vLength + ", " +
					"centerRadius: " + this.centerRadius + ", " +
					"endRadius: " + this.endRadius +
					" }"
				);
			}
		}

		public Piece(StructurePieceType type, BlockBox boundingBox, Data data) {
			super(type, 0, boundingBox, data);
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public AutoCoder<Data> dataCoder() {
			return Data.CODER;
		}

		@Override
		public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {

		}

		public class Projector {

			public double relativeX, relativeY, relativeZ;
			public double dotX, dotY, dotZ, dot;
			public double projectionX, projectionY, projectionZ;
			public double projectionDistanceSquared;

			public Projector setX(double x) {
				this.dotX = (this.relativeX = x - Piece.this.data.x1) * Piece.this.data.vx;
				return this;
			}

			public Projector setY(double y) {
				this.dotY = (this.relativeY = y - Piece.this.data.y1) * Piece.this.data.vy;
				return this;
			}

			public Projector setZ(double z) {
				this.dotZ = (this.relativeZ = z - Piece.this.data.z1) * Piece.this.data.vz;
				return this;
			}

			public Projector project(boolean clamp) {
				double dot = this.dotX + this.dotY + this.dotZ;
				if (clamp) dot = Interpolator.clamp(0.0D, Piece.this.data.vLength, dot);
				this.dot = dot;
				this.projectionDistanceSquared = BigGlobeMath.squareD(
					this.relativeX - (this.projectionX = dot * Piece.this.data.vx),
					this.relativeY - (this.projectionY = dot * Piece.this.data.vy),
					this.relativeZ - (this.projectionZ = dot * Piece.this.data.vz)
				);
				return this;
			}

			public double getProjectionX() {
				return this.projectionX + Piece.this.data.x1;
			}

			public double getProjectionY() {
				return this.projectionY + Piece.this.data.y1;
			}

			public double getProjectionZ() {
				return this.projectionZ + Piece.this.data.z1;
			}

			public double getProjectionDistance() {
				return Math.sqrt(this.projectionDistanceSquared);
			}

			public double getExpectedRadius() {
				return Interpolator.mixLinear(Piece.this.data.centerRadius, Piece.this.data.endRadius, BigGlobeMath.squareD(this.dot / Piece.this.data.vLength * 2.0D - 1.0D));
			}

			public double getRadiusFractionSquared() {
				double expectedRadius = this.getExpectedRadius();
				return expectedRadius > 0.0D ? this.projectionDistanceSquared / BigGlobeMath.squareD(expectedRadius) : -1.0D;
			}
		}

		public void runCaveExclusions(NoiseOverrider.Context context) {
			Projector projector = this.new Projector().setX(context.column.x).setZ(context.column.z);
			/**
			my claim is that the function
			y -> projector.setY(y).project(false).projectionDistance()
			is a hyperbolic function. to prove this, we will look at a
			different specialization of a more broad set of problems:
			the closest point between 2 arbitrary lines.

			say you have 2 lines, line1 and line2,
			and a point on each of those lines,
			point1 on line1, and point2 on line2.
			which pair of points has the smallest distance between them?

			for our first simplified case, assume line1
			intersects the origin, and is aligned with the x axis.
			also assume line2 intersects (0, 1, 0) and is aligned with the z axis.
			for any given point2, the closest point to it on line1 will be the origin,
			so the distance from point1 to point2 is:
				sqrt((x2 - x1) ^ 2 + (y2 - y1) ^ 2 + (z2 - z1) ^ 2)
				= sqrt((0 - 0) ^ 2 + (1 - 0) ^ 2 + (z2 - 0) ^ 2)
				= sqrt(0^2 + 1^2 + z2^2)
				= sqrt(z2 ^ 2 + 1)
			this is a hyperbola, as desired.

			what if line2 intersects (0, 1, 0), but is no longer aligned with the z axis?
			in this case, the closest point1 to it is (x2, 0, z2).
			the distance then, is:
				sqrt((x2 - x1) ^ 2 + (y2 - y1) ^ 2 + (z2 - z1) ^ 2)
				= sqrt((x2 - x2) ^ 2 + (1 - 0) ^ 2) + (z2 - 0) ^ 2)
				= sqrt(0^2 + 1^2 + z2^2)
				= sqrt(z2^2 + 1)
			the x coordinates cancel each other out, and we still have a hyperbola.

			final case: what if line2 intersects a different point besides (0, 1, 0)?
			say, for example, (0, 4, 0)
			I'll spare you the math, but the end result is:
				sqrt(z2^2 + 4^2)
				= sqrt(z2^2 + 16)
			this is STILL a hyperbola.

			to complete this proof, the last assumption we need is that changing
			the orientation of space does not change the distance between 2 points.
			so, any function which linearly maps a number to a point on line1, and then
			point1 to its closest counterpart on line2, must be a hyperbolic function.

			so... how does this proof help us figure out which blocks need to be placed?
			well, a pillar is a line, and a column is also a line.
			so by definition, y -> projector.setY(y).project(false).projectionDistance()
			is a hyperbolic function.
			additionally, y -> projector.setY(y).project(false).projectionDistanceSquared will
			be a parabolic function, because a hyperbola is just the square root of a parabola.
			a parabolic function of y can be defined as ay^2 + by + c for some constants a, b, and c.
			if we can find these constants, then we can directly deduce some properties of the parabola.
			in particular, we can deduce its midpoint, and the interval of its domain
			which is less than some threshold, by using the quadratic formula.
			knowing this interval means we know which Y
			levels are within some distance of the pillar.
			the distance to the pillar that we need in order to place
			blocks changes based on where along the pillar we are,
			but it does have an upper bound: this.endRadius.
			at the very least, this vastly reduces the number of
			y levels that we need to check in the first place.

			sounds good in theory, but how do we actually find those constants from earlier?
			well, we can directly query the parabola function at arbitrary Y levels,
			and we only need 3 points to find a parabola which passes through all of them.
			we can technically use any 3 points, but -1, 0, and 1 have the simplest math.
			the system of equations that arise from these points is as follows:
				a * (-1) ^ 2 + b * (-1) + c = (the distance squared at y = -1)
				a * 0^2 + b * 0 + c = (the distance squared at y = 0)
				a * 1^2 + b * 1 + c = (the distance squared at y = 1)
			we have 3 equations, and 3 unknowns,
			so this is a system which can be solved.
			the solution is given in the code below.
			*/
			double distanceSquaredAtNegativeOne = projector.setY(-1).project(false).projectionDistanceSquared;
			double distanceSquaredAtZero        = projector.setY( 0).project(false).projectionDistanceSquared;
			double distanceSquaredAtOne         = projector.setY( 1).project(false).projectionDistanceSquared;

			double a = (distanceSquaredAtOne + distanceSquaredAtNegativeOne) * 0.5D - distanceSquaredAtZero;
			double b = (distanceSquaredAtOne - distanceSquaredAtNegativeOne) * 0.5D;
			double c = distanceSquaredAtZero;
			assert a >= 0.0D : "Nether pillar is inverted: " + Piece.this.data + "; column at " + context.column.x + ", " + context.column.z;

			/**
			the interval where the hyperbola is less than this.endRadius is the
			same as the interval where the parabola is less than this.endRadius ^ 2.
			the quadratic formula, (-b +/- sqrt(b^2 - 4ac)) / 2a,
			can give the interval where the parabola is less than 0.
			to get the interval where the parabola is less than
			this.endRadius ^ 2, we simply need to subtract it from c.
			*/
			c -= BigGlobeMath.squareD(Piece.this.data.endRadius) * 4.0D;
			double sqrtTerm = b * b - 4.0D * a * c;
			if (sqrtTerm <= 0.0D) return; //interval is non-existent.
			sqrtTerm = Math.sqrt(sqrtTerm);
			double div = 0.5D / a;
			int minY = BigGlobeMath.ceilI((-b - sqrtTerm) * div);
			if (minY >= context.topI) return; //interval does not intersect the Y range we care about.
			int maxY = BigGlobeMath.floorI((-b + sqrtTerm) * div);
			if (maxY < context.bottomI) return; //interval does not intersect the Y range we care about.

			minY = Math.max(Math.max(minY, Piece.this.boundingBox.getMinY()), context.bottomI);
			maxY = Math.min(Math.min(maxY, Piece.this.boundingBox.getMaxY()), context.topI - 1);
			for (int y = minY; y <= maxY; y++) {
				double fraction = projector.setY(y).project(true).getRadiusFractionSquared();
				if (fraction > 0.0D && fraction < 4.0D) {
					context.exclude(y, BigGlobeMath.squareD(BigGlobeMath.squareD(fraction * 0.25D - 1.0D)));
				}
			}
		}
	}
}