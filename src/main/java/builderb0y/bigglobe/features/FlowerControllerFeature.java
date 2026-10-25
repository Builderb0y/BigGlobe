package builderb0y.bigglobe.features;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RestrictedList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.*;

public class FlowerControllerFeature extends Feature<FlowerControllerFeature.Config> {

	public FlowerControllerFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public FlowerControllerFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class QueuedPlacement extends BlockPos.MutableBlockPos {

		public final FlowerFeature.Entry entry;

		public QueuedPlacement(int i, int j, int k, FlowerFeature.Entry entry) {
			super(i, j, k);
			this.entry = entry;
		}
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		ScriptedColumnLookup columns = ScriptedColumnLookup.GLOBAL.getCurrent();
		if (columns == null) {
			if (context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
				columns = new ScriptedColumnLookup.Impl(
					generator.columnEntryRegistry.columnFactory,
					new ScriptedColumn.Params(generator, 0, 0, ColumnUsage.FEATURES.maybeDhHints())
				);
			}
			else {
				BigGlobeMod.LOGGER.warn("Attempt to place flower controller feature outside of a Big Globe world.");
				return false;
			}
		}

		try (
			AsyncConsumer<QueuedPlacement> async = new AsyncConsumer<>(
				BigGlobeThreadPool.autoExecutor(),
				(QueuedPlacement placement) -> {
					if (placement != null) this.placeQueued(context, placement);
				}
			)
		) {
			int startX = context.origin().getX() & ~15;
			int startZ = context.origin().getZ() & ~15;
			for (int index = 0; index < 256; index++) {
				ScriptedColumn column = columns.lookupColumn(
					startX | (index & 15),
					startZ | (index >>> 4)
				);
				async.submit(() -> this.getQueuedPlacement(context, column));
			}
		}

		return true;
	}

	public QueuedPlacement getQueuedPlacement(FeaturePlaceContext<Config> context, ScriptedColumn column) {
		int y = context.config().y_level.get(column);
		if (context.level().isOutsideBuildHeight(y)) return null;
		long overlapSeed = Permuter.permute(context.level().getSeed() ^ 0x3C8F9545BAE6971FL, column.x(), column.z());
		int overlapChance = 0;
		FlowerFeature.Entry chosen = null;
		RestrictedList<FlowerFeature.Entry> validEntries = new RestrictedList<>(null, column, y);
		for (FlowerFeature.Config link : context.config().getFlattenedFlowers()) {
			validEntries.elements = link.entries.elements;
			long groupSeed = link.seed.xor(context.level().getSeed());
			int scale = link.distance;
			int variation = link.variation;
			int inGridX = BigGlobeMath.modulus_BP(column.x(), scale);
			int inGridZ = BigGlobeMath.modulus_BP(column.z(), scale);
			int gridStartX = column.x() - inGridX;
			int gridStartZ = column.z() - inGridZ;
			Grid2D flowerNoise = link.noise;
			double noise = flowerNoise.getValue(groupSeed, column.x(), column.z());
			for (int offsetX = -scale; offsetX <= scale; offsetX += scale) {
				for (int offsetZ = -scale; offsetZ <= scale; offsetZ += scale) {
					int otherGridStartX = gridStartX + offsetX;
					int otherGridStartZ = gridStartZ + offsetZ;
					long otherGridSeed = Permuter.permute(groupSeed ^ 0xA2BBF085229FA361L, otherGridStartX, otherGridStartZ);
					if (!Permuter.nextChancedBoolean(otherGridSeed += Permuter.PHI64, link.spawn_chance)) continue;
					FlowerFeature.Entry entry;
					RandomSource radiusSource;
					if (Permuter.nextChancedBoolean(otherGridSeed += Permuter.PHI64, link.randomize_chance)) {
						entry = validEntries.getRandomElement(Permuter.permute(otherGridSeed += Permuter.PHI64, column.x(), column.z()));
						if (entry == null) continue;
						radiusSource = link.randomize_radius;
					}
					else {
						entry = validEntries.getRandomElement(Permuter.stafford(otherGridSeed += Permuter.PHI64));
						if (entry == null) continue;
						radiusSource = entry.radius();
					}
					double radius = radiusSource.get(column, y, Permuter.stafford(otherGridSeed += Permuter.PHI64));
					double otherGridCenterX = Permuter.nextPositiveDouble(otherGridSeed += Permuter.PHI64) * variation + offsetX;
					double otherGridCenterZ = Permuter.nextPositiveDouble(otherGridSeed += Permuter.PHI64) * variation + offsetZ;
					double distanceSquaredToCenter = BigGlobeMath.squareD(inGridX - otherGridCenterX, inGridZ - otherGridCenterZ);
					distanceSquaredToCenter /= BigGlobeMath.squareD(radius);
					double groupNoise = noise - distanceSquaredToCenter * flowerNoise.maxValue();
					if (Permuter.nextChancedBoolean(overlapSeed += Permuter.PHI64, groupNoise)) {
						if (overlapChance++ == 0 || Permuter.nextBoundedInt(overlapSeed += Permuter.PHI64, overlapChance) == 0) {
							chosen = entry;
						}
					}
				}
			}
		}
		return chosen != null ? new QueuedPlacement(column.x(), y, column.z(), chosen) : null;
	}

	public void placeQueued(FeaturePlaceContext<Config> context, QueuedPlacement placement) {
		WorldGenLevel world = context.level();
		BlockPos.MutableBlockPos pos = WorldUtil.findNonReplaceableGroundMutable(world, placement);
		if (pos == null) return;
		long seed = Permuter.permute(world.getSeed() ^ 0x9A99AA4557D5FE0FL, placement);

		int groundY = pos.getY();
		int flowerY = groundY + 1;
		if (placement.entry.under() != null) {
			BlockState oldState = world.getBlockState(pos);
			if (SingleBlockFeature.place(world, pos.setY(groundY), seed ^ 0xFF2635C589727C53L, placement.entry.under())) {
				if (!SingleBlockFeature.place(world, pos.setY(flowerY), seed ^ 0x4E991C867DC4A20AL, placement.entry.state())) {
					world.setBlock(pos.setY(groundY), oldState, Block.UPDATE_ALL);
				}
			}
		}
		else {
			SingleBlockFeature.place(world, pos.setY(flowerY), seed ^ 0x61425E46B2DFE013L, placement.entry.state());
		}
	}

	public static class Config implements FeatureConfiguration {

		public final ColumnToIntScript.Holder y_level;
		public final DelayedEntryList<ConfiguredFeature<?, ?>> flowers;
		public transient FlowerFeature.Config @Nullable [] flattenedFlowers;

		public Config(
			ColumnToIntScript.Holder y_level,
			DelayedEntryList<ConfiguredFeature<?, ?>> flowers
		) {
			this.y_level = y_level;
			this.flowers = flowers;
		}

		public FlowerFeature.Config @NotNull [] getFlattenedFlowers() {
			if (this.flattenedFlowers == null) {
				this.flattenedFlowers = (
					this
						.flowers
						.entryStream()
						.filter((Holder<ConfiguredFeature<?, ?>> entry) -> {
							if (entry.value().feature() == BigGlobeFeatures.FLOWER) {
								return true;
							}
							else {
								BigGlobeMod.LOGGER.warn("A flower controller references " + UnregisteredObjectException.getID(entry) + ", but this feature is not of type \"bigglobe:flower\". It will be ignored.");
								return false;
							}
						})
						.sorted(Comparator.comparing(UnregisteredObjectException::getID))
						.<FeatureConfiguration>map((Holder<ConfiguredFeature<?, ?>> entry) -> entry.value().config())
						.toArray(FlowerFeature.Config[]::new)
				);
			}
			return this.flattenedFlowers;
		}
	}
}