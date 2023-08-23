package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;

import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Tripwire;

public class SectionGenerationContext {

	public final Chunk chunk;
	public final ChunkSection section;
	public final int sectionStartY;
	public final long worldSeed;
	public final ChunkOfColumns<? extends WorldColumn> columns;
	#if MC_VERSION < MC_1_20_0
	public final @Nullable LightPositionCollector lights;
	#endif

	public SectionGenerationContext(Chunk chunk, ChunkSection section, int sectionStartY, long worldSeed, ChunkOfColumns<? extends WorldColumn> columns) {
		this.chunk         = chunk;
		this.section       = section;
		this.sectionStartY = sectionStartY;
		this.worldSeed     = worldSeed;
		this.columns       = columns;
		#if MC_VERSION < MC_1_20_0
			this.lights = chunk instanceof ProtoChunk ? new LightPositionCollector(this.startX(), this.startY(), this.startZ()) : null;
		#endif
	}

	public static SectionGenerationContext forIndex(Chunk chunk, ChunkSection section, int index, long worldSeed, ChunkOfColumns<? extends WorldColumn> columns) {
		return new SectionGenerationContext(chunk, section, chunk.sectionIndexToCoord(index) << 4, worldSeed, columns);
	}

	public static SectionGenerationContext forSectionCoord(Chunk chunk, ChunkSection section, int sectionCoord, long worldSeed, ChunkOfColumns<? extends WorldColumn> columns) {
		return new SectionGenerationContext(chunk, section, sectionCoord << 4, worldSeed, columns);
	}

	public void addLight(int index) {
		#if MC_VERSION < MC_1_20_0
			if (this.lights != null) {
				this.lights.add(index);
			}
		#endif
	}

	public boolean hasLights() {
		#if MC_VERSION < MC_1_20_0
			return this.lights != null && !this.lights.isEmpty();
		#else
			return false;
		#endif
	}

	#if MC_VERSION < MC_1_20_0
		public @Nullable LightPositionCollector lights() { return this.lights; }
	#else
		public @Nullable LightPositionCollector lights() { return null; }
	#endif
	public Chunk chunk() { return this.chunk; }
	public ChunkPos chunkPos() { return this.chunk.getPos(); }
	public ChunkSection section() { return this.section; }
	public PalettedContainer<BlockState> container() { return this.section.getBlockStateContainer(); }
	public Palette<BlockState> palette() { return SectionUtil.palette(this.container()); }
	public PaletteStorage storage() { return SectionUtil.storage(this.container()); }
	public int id(BlockState state) { return SectionUtil.id(this.container(), state); }
	public int sectionX() { return this.chunkPos().x; }
	public int sectionY() { return this.sectionStartY >> 4; }
	public int sectionZ() { return this.chunkPos().z; }
	public int startX() { return this.chunkPos().getStartX(); }
	public int startY() { return this.sectionStartY; }
	public int startZ() { return this.chunkPos().getStartZ(); }
	public int endX() { return this.startX() | 15; }
	public int endY() { return this.startY() | 15; }
	public int endZ() { return this.startZ() | 15; }
	public long worldSeed() { return this.worldSeed; }

	public long chunkSeed(long salt) {
		return Permuter.permute(this.worldSeed ^ salt, this.sectionX(), this.sectionZ());
	}

	public long sectionSeed(long salt) {
		return Permuter.permute(this.worldSeed ^ salt, this.sectionX(), this.sectionY(), this.sectionZ());
	}

	public void setNonEmpty(int nonEmptyBlocks) {
		SectionUtil.setNonEmptyBlocks(this.section(), nonEmptyBlocks);
	}

	public void setRandomTickingBlocks(int randomTicking) {
		SectionUtil.setRandomTickingBlocks(this.section(), randomTicking);
	}

	public void setRandomTickingFluids(int nonEmptyFluids) {
		SectionUtil.setRandomTickingFluids(this.section(), nonEmptyFluids);
	}

	public void setAllStates(BlockState state) {
		if (this.palette() instanceof SingularPalette_EntryAccess singular) {
			//how to set 4096 blocks in one operation.
			singular.bigglobe_setEntry(state);
		}
		else {
			//ideally, this method should only be called when the chunk section is empty.
			//if for any reason the call happens at the wrong time,
			//or another mod changes how palettes work,
			//we should still handle those cases sanely.
			if (Tripwire.isEnabled()) {
				Tripwire.logWithStackTrace(this + " does not have a SingularPalette.");
			}
			long payload = this.id(state);
			PaletteStorage storage = this.storage();
			for (int bits = storage.getElementBits(); bits < 64; bits <<= 1) {
				payload |= payload << bits;
			}
			Arrays.fill(storage.getData(), payload);
		}
	}

	/**
	the vanilla implementation uses an Int2IntOpenHashMap,
	when a simple short[] is sufficient.
	*/
	public void recalculateCounts() {
		Palette<BlockState> palette = this.palette();
		PaletteStorage storage = this.storage();
		int paletteSize = palette.getSize();
		short[] counts = new short[paletteSize];
		storage.forEach((int id) -> counts[id]++);
		int nonEmpty      = 0;
		int tickingBlocks = 0;
		int tickingFluids = 0;
		for (int id = 0; id < paletteSize; id++) {
			BlockState blockState = palette.get(id);
			FluidState fluidState = blockState.getFluidState();
			//note: ChunkSection.setBlockState() and ChunkSection.calculateCounts()
			//have different logic for accumulating counts, and in particular,
			//these 2 implementations produce different results.
			//it also doesn't help that ChunkSection.hasRandomFluidTicks()
			//simply returns nonEmptyFluidCount > 0.
			//I believe that the backing field, nonEmptyFluidCount, is misnamed,
			//and should be named randomTickingFluidCount instead.
			//the code that follows here is my "best guess" at what the correct code should be,
			//even though it produces different results than *both* vanilla implementations.
			if (!blockState.isAir()) {
				int count = counts[id];
				nonEmpty += count;
				if (blockState.hasRandomTicks()) tickingBlocks += count;
				if (fluidState.hasRandomTicks()) tickingFluids += count;
			}
		}
		this.setNonEmpty(nonEmpty);
		this.setRandomTickingBlocks(tickingBlocks);
		this.setRandomTickingFluids(tickingFluids);
	}

	@Override
	public String toString() {
		return "SectionGenerationContext: { at: " + this.sectionX() + ", " + this.sectionY() + ", " + this.sectionZ() + " (world position: " + this.startX() + ", " + this.startY() + ", " + this.startZ() + " -> " + this.endX() + ", " + this.endY() + ", " + this.endZ() + "), chunk: " + this.chunk() + ", section: " + this.section() + ", palette: " + this.palette() + ", storage: " + this.storage() + " }";
	}
}