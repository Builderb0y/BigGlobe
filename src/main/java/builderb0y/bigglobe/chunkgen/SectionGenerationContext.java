package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
import builderb0y.bigglobe.util.Tripwire;

public class SectionGenerationContext {

	public final ChunkAccess chunk;
	public final LevelChunkSection section;
	public final int sectionStartY;

	public SectionGenerationContext(ChunkAccess chunk, LevelChunkSection section, int sectionStartY) {
		if ((sectionStartY & 15) != 0) {
			throw new IllegalArgumentException("sectionStartY should be divisible by 16");
		}
		this.chunk = chunk;
		this.section = section;
		this.sectionStartY = sectionStartY;
	}

	public static SectionGenerationContext forSectionIndex(ChunkAccess chunk, LevelChunkSection section, int index) {
		return new SectionGenerationContext(chunk, section, chunk.getSectionYFromSectionIndex(index) << 4);
	}

	public static SectionGenerationContext forSectionCoord(ChunkAccess chunk, LevelChunkSection section, int sectionCoord) {
		return new SectionGenerationContext(chunk, section, sectionCoord << 4);
	}

	public static SectionGenerationContext forBlockCoord(ChunkAccess chunk, LevelChunkSection section, int blockCoord) {
		return new SectionGenerationContext(chunk, section, blockCoord);
	}

	public ChunkAccess chunk() {
		return this.chunk;
	}

	public ChunkPos chunkPos() {
		return this.chunk.getPos();
	}

	public LevelChunkSection section() {
		return this.section;
	}

	public PalettedContainer<BlockState> container() {
		return this.section.getStates();
	}

	public Palette<BlockState> palette() {
		return SectionUtil.palette(this.container());
	}

	public BitStorage storage() {
		return SectionUtil.storage(this.container());
	}

	public int id(BlockState state) {
		return SectionUtil.id(this.container(), state);
	}

	public int sectionX() {
		return this.chunkPos().x;
	}

	public int sectionY() {
		return this.sectionStartY >> 4;
	}

	public int sectionZ() {
		return this.chunkPos().z;
	}

	public int startX() {
		return this.chunkPos().getMinBlockX();
	}

	public int startY() {
		return this.sectionStartY;
	}

	public int startZ() {
		return this.chunkPos().getMinBlockZ();
	}

	public int endX() {
		return this.startX() | 15;
	}

	public int endY() {
		return this.startY() | 15;
	}

	public int endZ() {
		return this.startZ() | 15;
	}

	public void setAllStates(BlockState state, boolean distantHorizons) {
		if (this.palette() instanceof SingularPalette_EntryAccess singular) {
			//how to set 4096 blocks in one operation.
			singular.bigglobe_setEntry(state);
		}
		else {
			//ideally, this method should only be called when the chunk section is empty.
			//if for any reason the call happens at the wrong time,
			//or another mod changes how palettes work,
			//we should still handle those cases sanely.
			//
			//note: distant horizons changes the order which chunk statuses are generated in,
			//causing feature and empty chunks to generate adjacent to each other.
			//the features spill over into empty chunks, causing them to no longer be empty.
			if (Tripwire.isEnabled() && !distantHorizons) {
				Tripwire.logWithStackTrace(this + " does not have a SingularPalette.");
			}
			long payload = this.id(state);
			BitStorage storage = this.storage();
			for (int bits = storage.getBits(); bits < 64; bits <<= 1) {
				payload |= payload << bits;
			}
			Arrays.fill(storage.getRaw(), payload);
		}
	}

	public void recalculateCounts() {
		this.section.recalcBlockCounts();
	}

	@Override
	public String toString() {
		return "SectionGenerationContext: { at: " + this.sectionX() + ", " + this.sectionY() + ", " + this.sectionZ() + " (world position: " + this.startX() + ", " + this.startY() + ", " + this.startZ() + " -> " + this.endX() + ", " + this.endY() + ", " + this.endZ() + "), chunk: " + this.chunk() + ", section: " + this.section() + ", palette: " + this.palette() + ", storage: " + this.storage() + " }";
	}
}