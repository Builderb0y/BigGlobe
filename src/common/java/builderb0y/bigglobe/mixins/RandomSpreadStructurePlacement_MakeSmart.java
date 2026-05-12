package builderb0y.bigglobe.mixins;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.SmartStructurePlacement;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacement_MakeSmart extends StructurePlacement implements SmartStructurePlacement {

	@Shadow
	@Final
	private int spacing;

	public RandomSpreadStructurePlacement_MakeSmart() {
		super(null, null, 0.0F, 0, null);
	}

	@Shadow
	public abstract ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ);

	@Override
	public Stream<StructureStartWrapper> bigglobe_generateStructuresInArea(Context context) {
		int chunkSpacing = this.spacing, blockSpacing = chunkSpacing << 4;
		long seed = context.structureState().getLevelSeed();
		int regionMinX = Math.floorDiv(context.area().minX(), blockSpacing);
		int regionMinZ = Math.floorDiv(context.area().minZ(), blockSpacing);
		int regionMaxX = Math.floorDiv(context.area().maxX(), blockSpacing);
		int regionMaxZ = Math.floorDiv(context.area().maxZ(), blockSpacing);
		return IntStream.rangeClosed(regionMinZ, regionMaxZ).map((int regionZ) -> regionZ * chunkSpacing).mapToObj((int chunkZ) -> {
			return IntStream.rangeClosed(regionMinX, regionMaxX).map((int regionX) -> regionX * chunkSpacing).mapToObj((int chunkX) -> {
				return this.getPotentialStructureChunk(seed, chunkX, chunkZ);
			});
		})
		.flatMap(Function.identity())
		.filter((ChunkPos chunkPos) -> this.isStructureChunk(context.structureState(), chunkPos.x(), chunkPos.z()))
		.map(context::createRandomFromSetAt)
		.filter(Objects::nonNull);
	}
}