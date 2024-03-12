package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Chunk;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
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
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OreFeature extends DummyFeature<OreFeature.Config> implements RockReplacerFeature<OreFeature.Config> {

	public OreFeature(Codec<Config> codec) {
		super(codec);
	}

	public OreFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		Chunk chunk,
		int minSection,
		int maxSection,
		Config config
	) {
		Async.loop(minSection, maxSection, 1, (int sectionCoord) -> {
			SectionGenerationContext context = SectionGenerationContext.forSectionCoord(
				chunk,
				chunk.getSection(chunk.sectionCoordToIndex(sectionCoord)),
				sectionCoord,
				generator.worldSeed
			);
			ScriptedColumn offsetColumn = generator.columnEntryRegistry.columnFactory.create(worldWrapper.params);
			generateAllIntersecting(
				context.storage(),
				/**
				the implementation used for columns is WorldWrapper,
				which is not thread-safe. I expect many threads to be
				querying columns at the same time, so I need to lock it.
				*/
				worldWrapper,
				offsetColumn,
				context.startX(),
				context.startY(),
				context.startZ(),
				config,
				config.getReplacer(context),
				config.seed.xor(context.worldSeed())
			);
		});
	}

	public static void generateAllIntersecting(
		PaletteStorage storage,
		WorldWrapper columns,
		ScriptedColumn offsetColumn,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		Config ore,
		PaletteIdReplacer replacer,
		long oreSeed
	) {
		for (int offsetX = -16; offsetX <= 16; offsetX += 16) {
			long seedX = Permuter.permute(oreSeed, startXAbsolute + offsetX);
			for (int offsetZ = -16; offsetZ <= 16; offsetZ += 16) {
				long seedXZ = Permuter.permute(seedX, startZAbsolute + offsetZ);
				if (offsetX == 0 && offsetZ == 0) {
					generateCenterChunk(
						storage,
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
						storage,
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
		PaletteStorage storage,
		WorldWrapper columns,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		Config ore,
		PaletteIdReplacer replacer,
		long oreSeedXZ
	) {
		for (int offsetY = -16; offsetY <= 16; offsetY += 16) {
			long  seedXYZ = Permuter.permute(oreSeedXZ, startYAbsolute + offsetY);
			double centerXRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x545D7EBDE4B67550L) * 16.0D;
			double centerYRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x7EDC505CF32DE484L) * 16.0D + offsetY;
			double centerZRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0xA8C2A92FDD33298DL) * 16.0D;
			ScriptedColumn column = columns.lookupColumn(
				startXAbsolute + BigGlobeMath.floorI(centerXRelative),
				startZAbsolute + BigGlobeMath.floorI(centerZRelative)
			);
			generate(
				storage,
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
		PaletteStorage storage,
		ScriptedColumn column,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		int offsetX,
		int offsetZ,
		Config ore,
		PaletteIdReplacer replacer,
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
				storage,
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
		PaletteStorage storage,
		double centerXRelative,
		double centerYRelative,
		double centerZRelative,
		int startYAbsolute,
		Config ore,
		PaletteIdReplacer replacer,
		long veinSeed,
		ScriptedColumn column
	) {
		double radius = ore.radius.get(Permuter.stafford(veinSeed ^ 0x643CE1A830E16C28L));
		int minX = Math.max(0,  BigGlobeMath. ceilI(centerXRelative - radius));
		int minY = Math.max(0,  BigGlobeMath. ceilI(centerYRelative - radius));
		int minZ = Math.max(0,  BigGlobeMath. ceilI(centerZRelative - radius));
		int maxX = Math.min(15, BigGlobeMath.floorI(centerXRelative + radius));
		int maxY = Math.min(15, BigGlobeMath.floorI(centerYRelative + radius));
		int maxZ = Math.min(15, BigGlobeMath.floorI(centerZRelative + radius));
		if (maxX >= minX && maxY >= minY && maxZ >= minZ) {
			/**
			columns are not inherently thread-safe, and we expect to
			be using many columns at once to compute the ore chance.
			this presents the possibility of a race condition if 2
			ore veins try to use the same column at the same time,
			and both of them try to compute a new value.
			*/
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
							double blockRNG = Permuter.toPositiveDouble(Permuter.permute(veinSeed ^ 0xC2BB0ACBE4E52811L, index));
							double blockChance = BigGlobeMath.squareD(1.0D - offsetXYZ2 * reciprocalRadius2);
							if (blockRNG < blockChance) {
								int oldID = storage.get(index);
								int newID = replacer.getReplacement(oldID);
								if (newID != oldID) storage.set(index, newID);
							}
						}
					}
				}
			}
		}
	}

	public static class Config extends DummyConfig {

		public final @SeedModes(Seed.NUMBER | Seed.STRING) Seed seed;
		public final ColumnYToDoubleScript.Holder chance;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius;
		public final BlockState2ObjectMap<@VerifyNormal BlockState> blocks;

		public Config(
			Seed seed,
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius,
			BlockState2ObjectMap<BlockState> blocks
		) {
			this.seed = seed;
			this.chance = chance;
			this.radius = radius;
			this.blocks = blocks;
		}

		public PaletteIdReplacer getReplacer(SectionGenerationContext context) {
			return PaletteIdReplacer.of(context, this.blocks);
		}
	}
}