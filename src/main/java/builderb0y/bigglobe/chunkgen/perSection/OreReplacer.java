package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.ores.OreFeature;
import builderb0y.bigglobe.features.ores.OreFeature.PaletteIdReplacer;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class OreReplacer {

	public static void generate(
		SectionGenerationContext context,
		ChunkOfColumns<? extends WorldColumn> columns,
		OreFeature.Config[] ores
	) {
		if (ores.length == 0) return;

		WorldColumn offsetColumn = columns.getColumn(0).blankCopy();
		long globalSeed = context.worldSeed() ^ 0x8AB8301CD904A370L;
		int startX = context.startX();
		int startY = context.startY();
		int startZ = context.startZ();

		for (int index = 0, length = ores.length; index < length; index++) {
			long oreSeed = Permuter.permute(globalSeed, index);
			OreFeature.Config ore = ores[index];
			PaletteIdReplacer replacer = ore.getReplacer(context);
			PaletteStorage storage = context.storage();
			generate(
				storage,
				columns,
				offsetColumn,
				startX,
				startY,
				startZ,
				ore,
				replacer,
				oreSeed
			);
		}
	}

	public static void generate(
		PaletteStorage storage,
		ChunkOfColumns<? extends WorldColumn> columns,
		WorldColumn offsetColumn,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		OreFeature.Config ore,
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
						startXAbsolute,
						startYAbsolute,
						startZAbsolute,
						offsetX,
						offsetZ,
						ore,
						replacer,
						seedXZ,
						offsetColumn
					);
				}
			}
		}
	}

	public static void generateCenterChunk(
		PaletteStorage storage,
		ChunkOfColumns<? extends WorldColumn> columns,
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		OreFeature.Config ore,
		PaletteIdReplacer replacer,
		long oreSeedXZ
	) {
		for (int offsetY = -16; offsetY <= 16; offsetY += 16) {
			long  seedXYZ = Permuter.permute(oreSeedXZ, startYAbsolute + offsetY);
			double centerXRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x545D7EBDE4B67550L) * 16.0D;
			double centerYRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x7EDC505CF32DE484L) * 16.0D + offsetY;
			double centerZRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0xA8C2A92FDD33298DL) * 16.0D;
			WorldColumn column = columns.getColumn(
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
		int startXAbsolute,
		int startYAbsolute,
		int startZAbsolute,
		int offsetX,
		int offsetZ,
		OreFeature.Config ore,
		PaletteIdReplacer replacer,
		long oreSeedXZ,
		WorldColumn column
	) {
		for (int offsetY = -16; offsetY <= 16; offsetY += 16) {
			long seedXYZ = Permuter.permute(oreSeedXZ, startYAbsolute + offsetY);
			double centerXRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x545D7EBDE4B67550L) * 16.0D + offsetX;
			double centerYRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0x7EDC505CF32DE484L) * 16.0D + offsetY;
			double centerZRelative = Permuter.nextPositiveDouble(seedXYZ ^ 0xA8C2A92FDD33298DL) * 16.0D + offsetZ;
			column.setPos(
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

	public static void generate(
		PaletteStorage storage,
		double centerXRelative,
		double centerYRelative,
		double centerZRelative,
		int startYAbsolute,
		OreFeature.Config ore,
		PaletteIdReplacer replacer,
		long veinSeed,
		WorldColumn column
	) {
		double radius = ore.radius.get(Permuter.stafford(veinSeed ^ 0x643CE1A830E16C28L));
		int minX = Math.max(0,  BigGlobeMath. ceilI(centerXRelative - radius));
		int minY = Math.max(0,  BigGlobeMath. ceilI(centerYRelative - radius));
		int minZ = Math.max(0,  BigGlobeMath. ceilI(centerZRelative - radius));
		int maxX = Math.min(15, BigGlobeMath.floorI(centerXRelative + radius));
		int maxY = Math.min(15, BigGlobeMath.floorI(centerYRelative + radius));
		int maxZ = Math.min(15, BigGlobeMath.floorI(centerZRelative + radius));
		if (
			maxX >= minX && maxY >= minY && maxZ >= minZ &&
			ore.canSpawnAt(column, BigGlobeMath.floorI(centerYRelative)) &&
			Permuter.nextChancedBoolean(veinSeed ^ 0xEC59F958C7921509L, ore.getChance(column, startYAbsolute + centerYRelative))
		) {
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