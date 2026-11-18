package builderb0y.bigglobe.spawning;

import java.util.*;
import java.util.random.RandomGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.columns.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.parsing.ScriptParsingException;

@Wrapper
public class SpawnTweakers implements DelayedCompileable {

	public static final MobCategory[] SPAWNABLE_CATEGORIES;
	static {
		EnumSet<MobCategory> set = EnumSet.allOf(MobCategory.class);
		set.remove(MobCategory.MISC);
		SPAWNABLE_CATEGORIES = set.toArray(new MobCategory[set.size()]);
	}

	public final DelayedEntryList<SpawnTweaker> allTweakers;
	public final transient EnumMap<MobCategory, List<SpawnTweaker>> sortedTweakers;

	public SpawnTweakers(DelayedEntryList<SpawnTweaker> allTweakers) {
		this.allTweakers = allTweakers;
		this.sortedTweakers = new EnumMap<>(MobCategory.class);
		for (MobCategory category : MobCategory.values()) {
			this.sortedTweakers.put(category, new ArrayList<>());
		}
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
		MobCategory[] allCategories = MobCategory.values();
		for (SpawnTweaker tweaker : this.allTweakers.objectList()) {
			if (tweaker.getCategory() != null) {
				this.sortedTweakers.get(tweaker.getCategory()).add(tweaker);
			}
			else {
				for (MobCategory category : allCategories) {
					if (category != MobCategory.MISC) {
						this.sortedTweakers.get(category).add(tweaker);
					}
				}
			}
		}
		for (List<SpawnTweaker> tweakers : this.sortedTweakers.values()) {
			tweakers.sort(Comparator.naturalOrder());
		}
	}

	public List<SpawnTweaker> getTweakers(MobCategory category) {
		return this.sortedTweakers.get(category);
	}

	public WeightedList<SpawnerData> getSpawnEntries(
		BigGlobeScriptedChunkGenerator generator,
		BlockPos pos,
		MobCategory category,
		RandomGenerator random
	) {
		return this.getRawSpawnEntries(generator, pos, category, random).build(random);
	}

	public SpawnMap getRawSpawnEntries(
		BigGlobeScriptedChunkGenerator generator,
		BlockPos pos,
		MobCategory category,
		RandomGenerator random
	) {
		SpawnMap map = new SpawnMap(category);
		ScriptedColumn column = generator.newColumn(pos.getX(), pos.getZ(), ColumnUsage.GENERIC.normalHints());
		for (SpawnTweaker tweaker : this.getTweakers(category)) {
			tweaker.script().tweak(
				category,
				column,
				pos.getY(),
				random,
				map,
				tweaker.primary_entity() != null ? map._get(tweaker.primary_entity().value()) : null
			);
		}
		return map;
	}
}