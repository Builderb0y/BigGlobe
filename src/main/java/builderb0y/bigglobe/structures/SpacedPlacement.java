package builderb0y.bigglobe.structures;

import net.minecraft.util.math.ChunkPos;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public class SpacedPlacement {

	public final @VerifyIntRange(min = 0, minInclusive = false) int spacing;
	public final @VerifySorted(lessThanOrEqual = "spacing") int variation;
	public final Seed salt;
	public final transient int offsetX, offsetZ;

	public SpacedPlacement(int spacing, int variation, Seed salt) {
		this.spacing   = spacing;
		this.variation = variation;
		this.salt      = salt;
		this.offsetX   = Permuter.nextBoundedInt(salt.xor(0x44709082ACB19293L), spacing);
		this.offsetZ   = Permuter.nextBoundedInt(salt.xor(0x616174FD0F0A273FL), spacing);
	}

	public boolean isStartChunk(int chunkX, int chunkZ, long seed) {
		chunkX += this.offsetX;
		chunkZ += this.offsetZ;
		int regionX = Math.floorDiv(chunkX, this.spacing);
		int regionZ = Math.floorDiv(chunkZ, this.spacing);
		long regionSeed = Permuter.permute(seed ^ this.salt.xor(0xD7984F4760B04172L), regionX, regionZ);
		int correctX = Permuter.nextBoundedInt(regionSeed ^ 0xD5926784417BE983L, this.variation) + this.spacing * regionX;
		if (chunkX != correctX) return false;
		int correctZ = Permuter.nextBoundedInt(regionSeed ^ 0x08C87A3F03C6971DL, this.variation) + this.spacing * regionZ;
		return chunkZ == correctZ;
	}

	public boolean isStartChunk(ChunkPos chunkPos, long seed) {
		return this.isStartChunk(chunkPos.x, chunkPos.z, seed);
	}
}