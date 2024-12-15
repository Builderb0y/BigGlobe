package builderb0y.bigglobe.features;

import java.util.Comparator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

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

	public static class QueuedPlacement extends BlockPos.Mutable {

		public final FlowerFeature.Entry entry;

		public QueuedPlacement(int i, int j, int k, FlowerFeature.Entry entry) {
			super(i, j, k);
			this.entry = entry;
		}
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		ScriptedColumnLookup columns = ScriptedColumnLookup.GLOBAL.getCurrent();
		if (columns == null) {
			if (context.getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
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
			int startX = context.getOrigin().getX() & ~15;
			int startZ = context.getOrigin().getZ() & ~15;
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

	public QueuedPlacement getQueuedPlacement(FeatureContext<Config> context, ScriptedColumn column) {
		int y = context.getConfig().y_level.get(column);
		if (context.getWorld().isOutOfHeightLimit(y)) return null;
		long overlapSeed = Permuter.permute(context.getWorld().getSeed() ^ 0x3C8F9545BAE6971FL, column.x(), column.z());
		int overlapChance = 0;
		FlowerFeature.Entry chosen = null;
		RestrictedList<FlowerFeature.Entry> validEntries = new RestrictedList<>(null, column, y);
		for (FlowerFeature.Config link : context.getConfig().getFlattenedFlowers()) {
			validEntries.elements = link.entries.elements;
			long groupSeed = link.seed.xor(context.getWorld().getSeed());
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

	public void placeQueued(FeatureContext<Config> context, QueuedPlacement placement) {
		StructureWorldAccess world = context.getWorld();
		BlockPos.Mutable pos = WorldUtil.findNonReplaceableGroundMutable(world, placement);
		if (pos == null) return;
		long seed = Permuter.permute(world.getSeed() ^ 0x9A99AA4557D5FE0FL, placement);

		int groundY = pos.getY();
		int flowerY = groundY + 1;
		if (placement.entry.under() != null) {
			BlockState oldState = world.getBlockState(pos);
			if (SingleBlockFeature.place(world, pos.setY(groundY), seed ^ 0xFF2635C589727C53L, placement.entry.under())) {
				if (!SingleBlockFeature.place(world, pos.setY(flowerY), seed ^ 0x4E991C867DC4A20AL, placement.entry.state())) {
					world.setBlockState(pos.setY(groundY), oldState, Block.NOTIFY_ALL);
				}
			}
		}
		else {
			SingleBlockFeature.place(world, pos.setY(flowerY), seed ^ 0x61425E46B2DFE013L, placement.entry.state());
		}
	}

	public static class Config implements FeatureConfig {

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
					.filter((RegistryEntry<ConfiguredFeature<?, ?>> entry) -> {
						if (entry.value().feature() == BigGlobeFeatures.FLOWER) {
							return true;
						}
						else {
							BigGlobeMod.LOGGER.warn("A flower controller references " + UnregisteredObjectException.getID(entry) + ", but this feature is not of type \"bigglobe:flower\". It will be ignored.");
							return false;
						}
					})
					.sorted(Comparator.comparing(UnregisteredObjectException::getID))
					.<FeatureConfig>map((RegistryEntry<ConfiguredFeature<?, ?>> entry) -> entry.value().config())
					.toArray(FlowerFeature.Config[]::new)
				);
			}
			return this.flattenedFlowers;
		}
	}
}