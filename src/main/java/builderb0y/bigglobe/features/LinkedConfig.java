package builderb0y.bigglobe.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator.SortedFeatures;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.DummyFeature.DummyConfig;
import builderb0y.bigglobe.features.LinkedConfig.EntryConfig;
import builderb0y.bigglobe.features.LinkedConfig.GroupConfig;
import builderb0y.bigglobe.randomLists.IRestrictedListElement;
import builderb0y.bigglobe.settings.VariationsList;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class LinkedConfig<
	T_GroupConfig extends GroupConfig,
	T_EntryConfig extends EntryConfig<T_Entry>,
	T_Entry extends LinkedConfig.Entry
> {

	public static final ObjectArrayFactory<LinkedConfig<?, ?, ?>> ARRAY_FACTORY = new ObjectArrayFactory<>(LinkedConfig.class).generic();

	public final Identifier name;
	public final T_GroupConfig group;
	public final List<T_Entry> entries;

	public LinkedConfig(Identifier name, T_GroupConfig group, List<T_Entry> entries) {
		this.name = name;
		this.group = group;
		this.entries = entries;
	}

	public static class GroupConfig extends DummyConfig {

	}

	public static class EntryConfig<T_Entry extends LinkedConfig.Entry> extends DummyConfig {

		public final Identifier group;
		public final VariationsList<T_Entry> entries;

		public EntryConfig(Identifier group, VariationsList<T_Entry> entries) {
			this.group = group;
			this.entries = entries;
		}
	}

	public static class Entry implements IRestrictedListElement {

		public final double weight;
		public final ColumnRestriction restrictions;

		public Entry(double weight, ColumnRestriction restrictions) {
			this.weight = weight;
			this.restrictions = restrictions;
		}

		@Override
		public double getWeight() {
			return this.weight;
		}

		@Override
		public ColumnRestriction getRestrictions() {
			return this.restrictions;
		}
	}

	public static abstract class Factory<
		T_LinkedConfig extends LinkedConfig<T_GroupConfig, T_EntryConfig, T_Entry>,
		T_GroupConfig  extends GroupConfig,
		T_EntryConfig  extends EntryConfig<T_Entry>,
		T_Entry        extends LinkedConfig.Entry
	> {

		public final ObjectArrayFactory<T_LinkedConfig> linkedConfigArrayFactory;
		public final Feature<T_GroupConfig> groupFeature;
		public final Feature<T_EntryConfig> entryFeature;

		public Factory(ObjectArrayFactory<T_LinkedConfig> linkedConfigArrayFactory, Feature<T_GroupConfig> groupFeature, Feature<T_EntryConfig> entryFeature) {
			this.linkedConfigArrayFactory = linkedConfigArrayFactory;
			this.groupFeature = groupFeature;
			this.entryFeature = entryFeature;
		}

		public abstract T_LinkedConfig newConfig(Identifier name, T_GroupConfig groupConfig, List<T_Entry> entries);

		public T_LinkedConfig[] newArray(int length) {
			return this.linkedConfigArrayFactory.apply(length);
		}

		public T_LinkedConfig[] link(SortedFeatures sortedFeatures) {
			Map<Identifier, Mutable> map = new HashMap<>(8);
			sortedFeatures.streamRegistryEntries(this.groupFeature).forEach(registryEntry -> {
				T_GroupConfig groupConfig = registryEntry.value().config();
				Identifier group = UnregisteredObjectException.getID(registryEntry);
				Mutable mutable = map.computeIfAbsent(group, $ -> new Mutable());
				if (mutable.group == null) mutable.group = groupConfig;
				else throw new IllegalStateException("Multiple flower groups with the same ID: " + group);

			});
			sortedFeatures.streamConfigs(this.entryFeature).forEach(entryConfig -> {
				Mutable mutable = map.computeIfAbsent(entryConfig.group, $ -> new Mutable());
				mutable.entries.addAll(entryConfig.entries.elements);
			});

			return (
				map
				.entrySet()
				.stream()
				.map(entry -> entry.getValue().toImmutable(entry.getKey()))
				.toArray(this::newArray)
			);
		}

		public class Mutable {

			public T_GroupConfig group;
			public List<T_Entry> entries = new ArrayList<>(32);

			public T_LinkedConfig toImmutable(Identifier groupName) {
				if (this.group == null) throw new IllegalStateException("Missing group definition json file: " + groupName);
				if (this.entries.isEmpty()) throw new IllegalStateException("No entries for group: " + groupName);
				return Factory.this.newConfig(groupName, this.group, this.entries);
			}
		}
	}
}