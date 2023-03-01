package builderb0y.bigglobe.chunkgen.perSection;

import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.noise.Permuter;

public class CobblestoneReplacer {

	public static void generate(SectionGenerationContext context, int perSection) {
		long sectionSeed = context.sectionSeed(0xA8ADF1CDD2F8A81CL);
		PaletteStorage storage = context.storage();
		int stoneID      = context.id(BlockStates.STONE);
		int deepslateID  = context.id(BlockStates.DEEPSLATE);
		int cobbleID     = context.id(BlockStates.COBBLESTONE);
		int deepCobbleID = context.id(BlockStates.COBBLED_DEEPSLATE);
		if (storage != (storage = context.storage())) { //resize.
			stoneID      = context.id(BlockStates.STONE);
			deepslateID  = context.id(BlockStates.DEEPSLATE);
			cobbleID     = context.id(BlockStates.COBBLESTONE);
			deepCobbleID = context.id(BlockStates.COBBLED_DEEPSLATE);
			assert storage == context.storage();
		}
		for (int attempt = 0; attempt < perSection; attempt++) {
			int index = ((int)(Permuter.permute(sectionSeed, attempt))) & 4095;
			int old = storage.get(index);
			if (old == stoneID) {
				storage.set(index, cobbleID);
			}
			else if (old == deepslateID) {
				storage.set(index, deepCobbleID);
			}
		}
	}
}