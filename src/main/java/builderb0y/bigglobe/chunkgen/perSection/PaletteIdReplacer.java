package builderb0y.bigglobe.chunkgen.perSection;

import java.util.BitSet;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;

import builderb0y.bigglobe.chunkgen.SectionGenerationContext;

public interface PaletteIdReplacer {

	public abstract int getReplacement(int id);

	public static class OneBlockReplacer implements PaletteIdReplacer {

		public final int from, to;

		public OneBlockReplacer(int from, int to) {
			this.from = from;
			this.to = to;
		}

		public OneBlockReplacer(SectionGenerationContext context, BlockState from, BlockState to) {
			PaletteStorage storage = context.storage();
			int fromID   = context.id(from);
			int toID     = context.id(to);
			if (storage != (storage = context.storage())) { //resize
				fromID   = context.id(from);
				toID     = context.id(to);
				assert storage == context.storage();
			}
			this.from = fromID;
			this.to   = toID;
		}

		@Override
		public int getReplacement(int id) {
			return id == this.from ? this.to : id;
		}
	}

	public static class TwoBlockReplacer implements PaletteIdReplacer {

		public final int from1, from2, to1, to2;

		public TwoBlockReplacer(int from1, int from2, int to1, int to2) {
			this.from1 = from1;
			this.from2 = from2;
			this.to1   = to1;
			this.to2   = to2;
		}

		public TwoBlockReplacer(SectionGenerationContext context, BlockState from1, BlockState to1, BlockState from2, BlockState to2) {
			PaletteStorage storage = context.storage();
			int from1ID = context.id(from1);
			int to1ID   = context.id(to1);
			int from2ID = context.id(from2);
			int to2ID   = context.id(to2);
			if (storage != (storage = context.storage())) { //resize
				from1ID = context.id(from1);
				to1ID   = context.id(to1);
				from2ID = context.id(from2);
				to2ID   = context.id(to2);
				assert storage     == context.storage();
			}
			this.from1 = from1ID;
			this.to1   = to1ID;
			this.from2 = from2ID;
			this.to2   = to2ID;
		}

		@Override
		public int getReplacement(int id) {
			if (id == this.from1) return this.to1;
			if (id == this.from2) return this.to2;
			return id;
		}
	}

	public static class ManyBlockReplacer implements PaletteIdReplacer {

		public final int[] lookup;

		public ManyBlockReplacer(int[] lookup) {
			this.lookup = lookup;
		}

		@Override
		public int getReplacement(int id) {
			return id < this.lookup.length ? this.lookup[id] : id;
		}
	}
}