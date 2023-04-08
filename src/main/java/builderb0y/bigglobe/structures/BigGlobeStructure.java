package builderb0y.bigglobe.structures;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

//config needs to be encoded inline, but I can't annotate the field on the super class.
//and also my ReflectionManager can't see the backing field anyway.
@AddPseudoField(name = "config", getter = "getConfig")
public abstract class BigGlobeStructure extends Structure {

	public BigGlobeStructure(Config config) {
		super(config);
	}

	public @EncodeInline Config getConfig() {
		return this.config;
	}

	public static long chunkSeed(Context context, long salt) {
		return Permuter.permute(context.seed() ^ salt, context.chunkPos());
	}

	public static @Nullable BlockPos randomBlockInSurface(Context context, int offset) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getStartX() | (bits & 15);
		int z = context.chunkPos().getStartZ() | ((bits >>> 4) & 15);
		int y = context.chunkGenerator().getHeightInGround(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
		if (y >= context.world().getBottomY()) {
			return new BlockPos(x, y + offset, z);
		}
		else {
			return null;
		}
	}

	public static @Nullable Vector3d randomPosAtSurface(Context context, double offset) {
		double x = context.chunkPos().getStartX() + 16.0D * context.random().nextDouble();
		double z = context.chunkPos().getStartZ() + 16.0D * context.random().nextDouble();
		WorldColumn column = WorldColumn.forGenerator(
			context.seed(),
			context.chunkGenerator(),
			context.noiseConfig(),
			BigGlobeMath.floorI(x),
			BigGlobeMath.floorI(z)
		);
		double y = column.getFinalTopHeightD();
		if (!Double.isNaN(y)) {
			return new Vector3d(x, y + offset, z);
		}
		else {
			return null;
		}
	}

	public static @Nullable BlockPos randomBlockInChunk(Context context, int horizontalRadius, int verticalRadius) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getStartX() | (bits & 15);
		int z = context.chunkPos().getStartZ() | ((bits >>> 4) & 15);
		WorldColumn column = WorldColumn.forGenerator(
			context.seed(),
			context.chunkGenerator(),
			context.noiseConfig(),
			x,
			z
		);
		int minY = context.chunkGenerator().getMinimumY() + verticalRadius;
		int maxY = column.getFinalTopHeightI();
		for (int angleIndex = 0; angleIndex < 8; angleIndex++) {
			double angle = angleIndex * (BigGlobeMath.TAU / 8.0D);
			int x2 = BigGlobeMath.floorI(x + Math.cos(angle) * horizontalRadius);
			int z2 = BigGlobeMath.floorI(z + Math.sin(angle) * horizontalRadius);
			column.setPosUnchecked(x2, z2);
			maxY = Math.min(maxY, column.getFinalTopHeightI());
		}
		maxY -= verticalRadius;
		if (maxY >= minY) {
			int y = context.random().nextBetween(minY, maxY);
			return new BlockPos(x, y, z);
		}
		else {
			return null;
		}
	}

	public static @Nullable Vector3d randomPosInChunk(Context context, double horizontalRadius, double verticalRadius) {
		double x = context.chunkPos().getStartX() + 16.0D * context.random().nextDouble();
		double z = context.chunkPos().getStartZ() + 16.0D * context.random().nextDouble();
		WorldColumn column = WorldColumn.forGenerator(
			context.seed(),
			context.chunkGenerator(),
			context.noiseConfig(),
			BigGlobeMath.floorI(x),
			BigGlobeMath.floorI(z)
		);
		double minY = column.getFinalBottomHeightD() + verticalRadius;
		double maxY = column.getFinalTopHeightD();
		for (int angleIndex = 0; angleIndex < 8; angleIndex++) {
			double angle = angleIndex * (BigGlobeMath.TAU / 8.0D);
			int x2 = BigGlobeMath.floorI(x + Math.cos(angle) * horizontalRadius);
			int z2 = BigGlobeMath.floorI(z + Math.sin(angle) * horizontalRadius);
			column.setPosUnchecked(x2, z2);
			maxY = Math.min(maxY, column.getFinalTopHeightD());
		}
		maxY -= verticalRadius;
		if (maxY >= minY) {
			double y = context.random().nextDouble() * (maxY - minY) + minY;
			return new Vector3d(x, y, z);
		}
		else {
			return null;
		}
	}
}