package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.chunk.Chunk;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.settings.Seed.SeedModes;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public abstract class AbstractOreFeature<T_Config extends AbstractOreFeature.Config> extends DummyFeature<T_Config> implements RockReplacerFeature<T_Config> {

	public AbstractOreFeature(Codec<T_Config> codec) {
		super(codec);
	}

	@Override
	public void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		Chunk chunk,
		int minSection,
		int maxSection,
		T_Config config
	) {
		Async.loop(BigGlobeThreadPool.autoExecutor(), minSection, maxSection, 1, (int sectionCoord) -> {
			SectionGenerationContext context = SectionGenerationContext.forSectionCoord(
				chunk,
				chunk.getSection(chunk.sectionCoordToIndex(sectionCoord)),
				sectionCoord
			);
			OreBlockReplacer replacer = this.getReplacer(context, config);
			if (replacer != null) {
				ScriptedColumn offsetColumn = generator.columnEntryRegistry.columnFactory.create(worldWrapper.params);
				generateAllIntersecting(
					context,
					worldWrapper,
					offsetColumn,
					config,
					replacer,
					config.seed.xor(generator.columnSeed)
				);
			}
		});
	}

	public static void generateAllIntersecting(
		SectionGenerationContext context,
		WorldWrapper columns,
		ScriptedColumn offsetColumn,
		Config ore,
		OreBlockReplacer replacer,
		long oreSeed
	) {
		int startXAbsolute = context.startX();
		int startYAbsolute = context.startY();
		int startZAbsolute = context.startZ();
		for (int offsetX = -16; offsetX <= 16; offsetX += 16) {
			long seedX = Permuter.permute(oreSeed, startXAbsolute + offsetX);
			for (int offsetZ = -16; offsetZ <= 16; offsetZ += 16) {
				long seedXZ = Permuter.permute(seedX, startZAbsolute + offsetZ);
				if (offsetX == 0 && offsetZ == 0) {
					generateCenterChunk(
						context,
						columns,
						startXAbsolute,
						startYAbsolute,
						startZAbsolute,
						ore,
						replacer,
						seedXZ
					);
				}
				else {
					generateOffsetChunk(
						context,
						offsetColumn,
						startXAbsolute,
						startYAbsolute,
						startZAbsolute,
						offsetX,
						offsetZ,
						ore,
						replacer,
						seedXZ
					);
				}
			}
		}
	}

	public static void generateCenterChunk(
		SectionGenerationContext context,
		WorldWrapper columns,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		Config ore,
		OreBlockReplacer replacer,
		long oreSeedXZ
	) {
		for (int offsetY = -16; offsetY <= 16; offsetY += 16) {
			long   seedXYZ         = Permuter.permute(oreSeedXZ, startYAbsolute + offsetY);
			double centerXRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x545D7EBDE4B67550L) * 16.0D;
			double centerYRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x7EDC505CF32DE484L) * 16.0D + offsetY;
			double centerZRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0xA8C2A92FDD33298DL) * 16.0D;
			ScriptedColumn  column = columns.lookupColumn(
				startXAbsolute + BigGlobeMath.floorI(centerXRelative),
				startZAbsolute + BigGlobeMath.floorI(centerZRelative)
			);
			generate(
				context,
				centerXRelative,
				centerYRelative,
				centerZRelative,
				startYAbsolute,
				ore,
				replacer,
				seedXYZ,
				column
			);
		}
	}

	public static void generateOffsetChunk(
		SectionGenerationContext context,
		ScriptedColumn column,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		int offsetX,
		int offsetZ,
		Config ore,
		OreBlockReplacer replacer,
		long oreSeedXZ
	) {
		for (int offsetY = -16; offsetY <= 16; offsetY += 16) {
			long  seedXYZ = Permuter.permute(oreSeedXZ, startYAbsolute + offsetY);
			double centerXRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x545D7EBDE4B67550L) * 16.0D + offsetX;
			double centerYRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x7EDC505CF32DE484L) * 16.0D + offsetY;
			double centerZRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0xA8C2A92FDD33298DL) * 16.0D + offsetZ;
			column.setParams(column.params.at(
				startXAbsolute + BigGlobeMath.floorI(centerXRelative),
				startZAbsolute + BigGlobeMath.floorI(centerZRelative)
			));
			generate(
				context,
				centerXRelative,
				centerYRelative,
				centerZRelative,
				startYAbsolute,
				ore,
				replacer,
				seedXYZ,
				column
			);
		}
	}

	public static void generate(
		SectionGenerationContext context,
		double centerXRelative,
		double centerYRelative,
		double centerZRelative,
		int startYAbsolute,
		Config ore,
		OreBlockReplacer replacer,
		long veinSeed,
		ScriptedColumn column
	) {
		/**
		columns are not inherently thread-safe, and we expect to
		be using many columns at once to compute the ore chance.
		this presents the possibility of a race condition if 2
		ore veins try to use the same column at the same time,
		and both of them try to compute a new value.
		*/
		double radius;
		synchronized (column) {
			radius = ore.radius.get(column, startYAbsolute, Permuter.stafford(veinSeed ^ 0x643CE1A830E16C28L));
		}
		int minX = Math.max(0,  BigGlobeMath. ceilI(centerXRelative - radius));
		int minY = Math.max(0,  BigGlobeMath. ceilI(centerYRelative - radius));
		int minZ = Math.max(0,  BigGlobeMath. ceilI(centerZRelative - radius));
		int maxX = Math.min(15, BigGlobeMath.floorI(centerXRelative + radius));
		int maxY = Math.min(15, BigGlobeMath.floorI(centerYRelative + radius));
		int maxZ = Math.min(15, BigGlobeMath.floorI(centerZRelative + radius));
		if (maxX >= minX && maxY >= minY && maxZ >= minZ) {
			double chance;
			synchronized (column) {
				chance = ore.chance.get(column, BigGlobeMath.floorI(startYAbsolute + centerYRelative));
			}
			if (Permuter.nextChancedBoolean(veinSeed ^ 0xEC59F958C7921509L, chance)) {
				double radius2 = BigGlobeMath.squareD(radius);
				double reciprocalRadius2 = 1.0D / radius2;
				for (int y = minY; y <= maxY; y++) {
					double offsetY2 = BigGlobeMath.squareD(y - centerYRelative);
					for (int z = minZ; z <= maxZ; z++) {
						double offsetYZ2 = BigGlobeMath.squareD(z - centerZRelative) + offsetY2;
						if (!(offsetYZ2 < radius2)) continue;
						for (int x = minX; x <= maxX; x++) {
							double offsetXYZ2 = BigGlobeMath.squareD(x - centerXRelative) + offsetYZ2;
							if (!(offsetXYZ2 < radius2)) continue;
							int index = (y << 8) | (z << 4) | x;
							replacer.replace(
								column,
								context,
								index,
								x | context.startX(),
								y | context.startY(),
								z | context.startZ(),
								Permuter.permute(veinSeed ^ 0xCC40A392B2FBC5A5L, index),
								centerXRelative + context.startX(),
								centerYRelative + context.startY(),
								centerZRelative + context.startZ(),
								radius,
								offsetXYZ2 * reciprocalRadius2
							);
						}
					}
				}
			}
		}
	}

	public abstract OreBlockReplacer getReplacer(SectionGenerationContext context, T_Config config);

	public static abstract class OreBlockReplacer {

		public abstract void replace(
			ScriptedColumn column,
			SectionGenerationContext context,
			int index,
			int blockX,
			int blockY,
			int blockZ,
			long blockSeed,
			double veinCenterX,
			double veinCenterY,
			double veinCenterZ,
			double veinRadius,
			double blockVeinFractionSquared
		);
	}

	public static class Config extends DummyConfig {

		public final @SeedModes(Seed.NUMBER | Seed.STRING) Seed seed;
		public final ColumnYToDoubleScript.Holder chance;
		public final ColumnYToDoubleScript.@VerifyNullable Holder core_chance;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius;

		public Config(
			Seed seed,
			ColumnYToDoubleScript.Holder chance,
			ColumnYToDoubleScript.Holder core_chance,
			RandomSource radius
		) {
			this.seed        = seed;
			this.chance      = chance;
			this.core_chance = core_chance;
			this.radius      = radius;
		}

		public ColumnYToDoubleScript.Holder getCoreChance() {
			return this.core_chance != null ? this.core_chance : this.chance;
		}
	}
}