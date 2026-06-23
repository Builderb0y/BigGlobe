package builderb0y.bigglobe.spawning;

import java.util.Map;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.entries.EntityTypeEntry;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class SpawnMap {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo get, put, remove, clear;
	}

	public final MobCategory category;
	public final Map<EntityType<?>, SpawnParams> backingMap = new Object2ObjectOpenHashMap<>();

	public SpawnMap(MobCategory category) {
		this.category = category;
	}

	public void checkType(EntityTypeEntry entry) {
		MobCategory category = entry.entry.value().getCategory();
		if (category != this.category) {
			throw new IllegalArgumentException("A script in the " + this.category.getSerializedName() + " category is trying to spawn " + entry.id() + ", but that mob is in the " + category.getSerializedName() + " category");
		}
	}

	public SpawnParams _get(EntityType<?> entity) {
		return this.backingMap.computeIfAbsent(entity, (EntityType<?> _) -> new SpawnParams());
	}

	public SpawnParams get(EntityTypeEntry entity) {
		this.checkType(entity);
		return this._get(entity.object());
	}

	public SpawnParams put(EntityTypeEntry entity, SpawnParams params) {
		this.checkType(entity);
		return this.backingMap.put(entity.object(), params);
	}

	public void remove(EntityTypeEntry entity) {
		this.checkType(entity);
		this.backingMap.remove(entity.object());
	}

	public void clear() {
		this.backingMap.clear();
	}

	public WeightedList<SpawnerData> build(RandomGenerator random) {
		return WeightedList.of(
			this
			.backingMap
			.entrySet()
			.stream()
			.filter((Map.Entry<EntityType<?>, SpawnParams> entry) -> (
				entry.getValue().weight > 0.0D &&
				entry.getValue().count > 0
			))
			.map((Map.Entry<EntityType<?>, SpawnParams> entry) -> new Weighted<>(
				new SpawnerData(
					entry.getKey(),
					entry.getValue().count,
					entry.getValue().count
				),
				Permuter.roundRandomlyI(random, entry.getValue().weight * 256.0D)
			))
			.toList()
		);
	}

	public static class SpawnParams {

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public FieldInfo weight, count;
		}
		public static final MethodInfo CONSTRUCTUR = MethodInfo.getConstructor(SpawnParams.class);

		public double weight;
		public int count;

		@Override
		public String toString() {
			return "SpawnParams: { weight: " + this.weight + ", count: " + this.count + " }";
		}
	}
}