package builderb0y.bigglobe.spawning;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;

@Wrapper
public class SpawnTweakers {

	public static final MobCategory[] SPAWNABLE_CATEGORIES;
	static {
		EnumSet<MobCategory> set = EnumSet.allOf(MobCategory.class);
		set.remove(MobCategory.MISC);
		SPAWNABLE_CATEGORIES = set.toArray(new MobCategory[set.size()]);
	}

	public final BetterRegistry<SpawnTweaker> registry;
	public static record Key(MobCategory category, Holder<Biome> biome) {

		public boolean matches(SpawnTweaker tweaker) {
			MobCategory category = tweaker.getCategory();
			return (category == null || category == this.category) && tweaker.biomes().contains(this.biome);
		}
	}
	public final transient Map<Key, List<SpawnTweaker>> tweakers;

	public SpawnTweakers(BetterRegistry<SpawnTweaker> registry) {
		this.registry = registry;
		this.tweakers = new Object2ObjectOpenHashMap<>();
	}

	public List<SpawnTweaker> getTweakers(MobCategory category, Holder<Biome> biome) {
		synchronized (this.tweakers) {
			return this.tweakers.computeIfAbsent(new Key(category, biome), (Key key) -> {
				return this.registry.streamEntries().map(Holder<SpawnTweaker>::value).filter(key::matches).sorted().toList();
			});
		}
	}

	public WeightedList<SpawnerData> getSpawnEntries(
		BigGlobeScriptedChunkGenerator generator,
		BlockPos pos,
		MobCategory category,
		Holder<Biome> biome,
		RandomGenerator random
	) {
		return this.getRawSpawnEntries(generator, pos, category, biome, random).build(random);
	}

	public SpawnMap getRawSpawnEntries(
		BigGlobeScriptedChunkGenerator generator,
		BlockPos pos,
		MobCategory category,
		Holder<Biome> biome,
		RandomGenerator random
	) {
		SpawnMap map = new SpawnMap(category);
		BiomeEntry biomeEntry = new BiomeEntry(biome);
		ScriptedColumn column = generator.newColumn(pos.getX(), pos.getZ(), ColumnUsage.GENERIC.normalHints());
		for (SpawnTweaker tweaker : this.getTweakers(category, biome)) {
			tweaker.script().tweak(
				column,
				pos.getY(),
				biomeEntry,
				random,
				map,
				tweaker.primary_entity() != null ? map._get(tweaker.primary_entity().value()) : null
			);
		}
		return map;
	}
}