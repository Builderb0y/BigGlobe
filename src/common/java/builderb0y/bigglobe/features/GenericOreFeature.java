package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class GenericOreFeature extends Feature<GenericOreFeature.Config> {

	public GenericOreFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public GenericOreFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		if (context.config().radius.requiresColumn() && !(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator)) return false;
		Permuter permuter = Permuter.from(context.random());

		double centerX = context.origin().getX() + permuter.nextDouble() - 0.5D;
		double centerY = context.origin().getY() + permuter.nextDouble() - 0.5D;
		double centerZ = context.origin().getZ() + permuter.nextDouble() - 0.5D;

		double radius = context.config().radius.get(
			context.config().radius.requiresColumn()
				? ((BigGlobeScriptedChunkGenerator)(context.chunkGenerator())).newColumn(
				context.level(),
				context.origin().getX(),
				context.origin().getZ(),
				ColumnUsage.FEATURES.maybeDhHints()
			)
				: null,
			context.origin().getY(),
			permuter
		);
		double radius2 = radius * radius;
		double reciprocalRadius2 = 1.0D / radius2;

		int minX = BigGlobeMath.ceilI(centerX - radius);
		int minY = BigGlobeMath.ceilI(centerY - radius);
		int minZ = BigGlobeMath.ceilI(centerZ - radius);
		int maxX = BigGlobeMath.floorI(centerX + radius);
		int maxY = BigGlobeMath.floorI(centerY + radius);
		int maxZ = BigGlobeMath.floorI(centerZ + radius);

		WorldGenLevel world = context.level();
		BlockState2ObjectMap<BlockState> states = context.config().states;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (pos.setZ(minZ); pos.getZ() <= maxZ; pos.setZ(pos.getZ() + 1)) {
			double offsetZ2 = BigGlobeMath.squareD(pos.getZ() - centerZ);
			for (pos.setX(minX); pos.getX() <= maxX; pos.setX(pos.getX() + 1)) {
				double offsetXZ2 = offsetZ2 + BigGlobeMath.squareD(pos.getX() - centerX);
				if (!(offsetXZ2 < radius2)) continue;
				for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
					double offsetXYZ2 = offsetXZ2 + BigGlobeMath.squareD(pos.getY() - centerY);
					if (!(offsetXYZ2 < radius2)) continue;
					double chance = BigGlobeMath.squareD(1.0D - offsetXYZ2 * reciprocalRadius2);
					if (Permuter.nextChancedBoolean(permuter, chance)) {
						BlockState replacement = states.runtimeStates.get(world.getBlockState(pos));
						if (replacement != null) world.setBlock(pos, replacement, Block.UPDATE_ALL);
					}
				}
			}
		}
		return true;
	}

	public static record Config(
		BlockState2ObjectMap<BlockState> states,
		@VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius
	)
		implements FeatureConfiguration {}
}