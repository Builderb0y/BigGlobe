package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class BedrockReplacer {

	public static void generate(SectionGenerationContext context) {
		long sectionSeed = context.sectionSeed(0x6C781D03A0FDC114L);
		int bedrockID = context.id(BlockStates.BEDROCK);
		PaletteStorage storage = context.storage();
		for (int index = 0; index < 4096; index++) {
			int y = index >>> 8;
			int chance = BigGlobeMath.squareI(16 - y);
			int rng = ((int)(Permuter.permute(sectionSeed, index))) & 255;
			if (rng < chance) {
				storage.set(index, bedrockID);
			}
		}
	}
}