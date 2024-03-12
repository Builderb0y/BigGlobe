package builderb0y.bigglobe.chunkgen;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;

import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.mixins.SingularPalette_EntryAccess;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Tripwire;

public class SectionGenerationContext {

	public final Chunk chunk;
	public final ChunkSection section;
	public final int sectionStartY;
	public final long worldSeed;
	#if MC_VERSION < MC_1_20_0
	public final @Nullable LightPositionCollector lights;
	#endif

	public SectionGenerationContext(Chunk chunk, ChunkSection section, int sectionStartY, long worldSeed) {
		if ((sectionStartY & 15) != 0) {
			throw new IllegalArgumentException("sectionStartY should be divisible by 16");
		}
		this.chunk         = chunk;
		this.section       = section;
		this.sectionStartY = sectionStartY;
		this.worldSeed     = worldSeed;
		#if MC_VERSION < MC_1_20_0
			this.lights = chunk instanceof ProtoChunk ? new LightPositionCollector(this.startX(), this.startY(), this.startZ()) : null;
		#endif
	}

	public static SectionGenerationContext forSectionIndex(Chunk chunk, ChunkSection section, int index, long worldSeed) {
		return new SectionGenerationContext(chunk, section, chunk.sectionIndexToCoord(index) << 4, worldSeed);
	}

	public static SectionGenerationContext forSectionCoord(Chunk chunk, ChunkSection section, int sectionCoord, long worldSeed) {
		return new SectionGenerationContext(chunk, section, sectionCoord << 4, worldSeed);
	}

	public static SectionGenerationContext forBlockCoord(Chunk chunk, ChunkSection section, int blockCoord, long worldSeed) {
		return new SectionGenerationContext(chunk, section, blockCoord, worldSeed);
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

	public void recalculateCounts() {
		this.section.calculateCounts();
	}

	@Override
	public String toString() {
		return "SectionGenerationContext: { at: " + this.sectionX() + ", " + this.sectionY() + ", " + this.sectionZ() + " (world position: " + this.startX() + ", " + this.startY() + ", " + this.startZ() + " -> " + this.endX() + ", " + this.endY() + ", " + this.endZ() + "), chunk: " + this.chunk() + ", section: " + this.section() + ", palette: " + this.palette() + ", storage: " + this.storage() + " }";
	}
}