package builderb0y.bigglobe.structures.management;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.structure.Structure;

import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.util.Streamable;

public class EmptyStructureLocator extends StructureLocator {

	public static final EmptyStructureLocator INSTANCE = new EmptyStructureLocator();

	@Override
	public Streamable<Holder<Structure>> allStructures() {
		return Streamable.empty();
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresIntersecting(Params params) {
		return Stream.empty();
	}

	@Override
	public @Nullable WeightedList<SpawnerData> getMobSpawns(Context context, BlockPos blockPos, MobCategory group) {
		return null;
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresInside(Params params) {
		return Stream.empty();
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresNearby(Params params, BlockPos center) {
		return Stream.empty();
	}

	@Override
	public boolean maybeHasBiomes(BiomeParams params) {
		return false;
	}
}