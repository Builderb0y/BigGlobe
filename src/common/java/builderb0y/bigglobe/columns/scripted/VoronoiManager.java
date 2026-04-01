package builderb0y.bigglobe.columns.scripted;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiImplCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.types.VoronoiColumnValueType;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class VoronoiManager {

	public final ColumnEntryRegistry registry;
	public final Map<ColumnEntry, Holder<ColumnEntry>> columnEntryReverseLookup = new IdentityHashMap<>(256);
	public final Map<VoronoiSettings, Holder<VoronoiSettings>> voronoiSettingsReverseLookup = new IdentityHashMap<>(64);
	public final Map<VoronoiColumnValueType, Entry> voronoi2EntryMap = new IdentityHashMap<>(16);
	public final Map<VoronoiSettings, VoronoiImplCompileContext> settings2ImplContext = new IdentityHashMap<>(64);
	public final Map<ColumnEntry, List<Holder<VoronoiSettings>>> enablingSettingsMap = new IdentityHashMap<>(64);

	public VoronoiManager(ColumnEntryRegistry registry) {
		this.registry = registry;
		BetterRegistry<ColumnEntry> columnEntryRegistry = registry.registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY);
		BetterRegistry<VoronoiSettings> voronoiRegistry = registry.registries.getRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY);

		voronoiRegistry.streamEntries().forEach((Holder<VoronoiSettings> voronoiEntry) -> {
			Holder<ColumnEntry> owner = voronoiEntry.value().owner();
			if (!(owner.value() instanceof VoronoiColumnEntry)) {
				throw new IllegalArgumentException("voronoi_settings " + UnregisteredObjectException.getID(voronoiEntry) + " is owned by column_value " + UnregisteredObjectException.getID(owner) + " but this column value is not of type voronoi.");
			}
		});

		Map<String, Identifier> voronoiCellNames = new HashMap<>(8);
		columnEntryRegistry.streamEntries().forEach((Holder<ColumnEntry> entry) -> {
			if (entry.value() instanceof VoronoiColumnEntry voronoi) {
				String name = ((VoronoiColumnValueType)(voronoi.params.type())).name;
				Identifier id = UnregisteredObjectException.getID(entry);
				Identifier old = voronoiCellNames.putIfAbsent(name, id);
				if (old != null) {
					throw new IllegalStateException("Voronoi-typed column values " + id + " and " + old + " share the same cell name '" + name + "' (they shouldn't).");
				}
			}
		});

		columnEntryRegistry.streamEntries().forEach((Holder<ColumnEntry> registryEntry) -> this.columnEntryReverseLookup.put(registryEntry.value(), registryEntry));
		voronoiRegistry.streamEntries().forEach((Holder<VoronoiSettings> registryEntry) -> this.voronoiSettingsReverseLookup.put(registryEntry.value(), registryEntry));

		columnEntryRegistry.streamEntries().forEach((Holder<ColumnEntry> columnRegistryEntry) -> {
			if (columnRegistryEntry.value() instanceof VoronoiColumnEntry voronoiColumnEntry) {
				List<Holder<VoronoiSettings>> settings = (
					voronoiRegistry
						.streamEntries()
						.filter((Holder<VoronoiSettings> voronoiRegistryEntry) -> (
							voronoiRegistryEntry.value().owner() == columnRegistryEntry
						))
						.peek((Holder<VoronoiSettings> voronoiRegistryEntry) -> {
							Map<String, AccessSchema> expected = voronoiRegistryEntry.value().exports().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, Holder<ColumnEntry>> entry) -> entry.getValue().value().getAccessSchema()));
							if (!voronoiColumnEntry.exports().equals(expected)) {
								throw new IllegalStateException("Export mismatch between column value " + UnregisteredObjectException.getID(columnRegistryEntry) + ' ' + voronoiColumnEntry.exports() + " and voronoi settings " + UnregisteredObjectException.getID(voronoiRegistryEntry) + ' ' + expected);
							}
						})
						.sorted(Comparator.comparing(UnregisteredObjectException::getID))
						.toList()
				);
				VoronoiBaseCompileContext baseContext = new VoronoiBaseCompileContext(
					registry.columnContext,
					((VoronoiColumnValueType)(voronoiColumnEntry.getAccessSchema().type())).name,
					!voronoiColumnEntry.exports().isEmpty()
				);
				List<VoronoiImplCompileContext> implContexts = (
					settings
						.stream()
						.map((Holder<VoronoiSettings> voronoiSettingsRegistryEntry) -> (
							new VoronoiImplCompileContext(baseContext, voronoiSettingsRegistryEntry)
						))
						.toList()
				);
				this.voronoi2EntryMap.put(voronoiColumnEntry.voronoiType(), new Entry(settings, baseContext, implContexts));
				for (int index = 0, size = settings.size(); index < size; index++) {
					this.settings2ImplContext.put(settings.get(index).value(), implContexts.get(index));
				}
			}
			this.enablingSettingsMap.put(
				columnRegistryEntry.value(),
				voronoiRegistry
					.streamEntries()
					.filter((Holder<VoronoiSettings> voronoiEntry) -> (
						voronoiEntry.value().enables().contains(columnRegistryEntry)
					))
					.sorted(Comparator.comparing(UnregisteredObjectException::getID))
					.toList()
			);
		});
	}

	public Holder<ColumnEntry> entryOf(ColumnEntry entry) {
		return this.columnEntryReverseLookup.get(entry);
	}

	public Holder<VoronoiSettings> entryOf(VoronoiSettings settings) {
		return this.voronoiSettingsReverseLookup.get(settings);
	}

	public List<Holder<VoronoiSettings>> getOptionsFor(VoronoiColumnEntry entry) {
		return this.voronoi2EntryMap.get(entry.voronoiType()).settings;
	}

	public VoronoiBaseCompileContext getBaseContextFor(VoronoiColumnEntry entry) {
		return this.voronoi2EntryMap.get(entry.voronoiType()).baseContext;
	}

	public List<VoronoiImplCompileContext> getImplContextsFor(VoronoiColumnEntry entry) {
		return this.voronoi2EntryMap.get(entry.voronoiType()).implContexts;
	}

	public List<Holder<VoronoiSettings>> getOptionsFor(VoronoiColumnValueType type) {
		return this.voronoi2EntryMap.get(type).settings;
	}

	public VoronoiBaseCompileContext getBaseContextFor(VoronoiColumnValueType type) {
		return this.voronoi2EntryMap.get(type).baseContext;
	}

	public List<VoronoiImplCompileContext> getImplContextsFor(VoronoiColumnValueType type) {
		return this.voronoi2EntryMap.get(type).implContexts;
	}

	public VoronoiImplCompileContext getImplContextFor(VoronoiSettings settings) {
		return this.settings2ImplContext.get(settings);
	}

	public List<Holder<VoronoiSettings>> getEnablingSettings(ColumnEntry entry) {
		return this.enablingSettingsMap.get(entry);
	}

	public Stream<DataCompileContext> getValidOn(ColumnEntry entry) {
		List<Holder<VoronoiSettings>> enablers = this.getEnablingSettings(entry);
		return enablers.isEmpty() ? Stream.of(this.registry.columnContext) : enablers.stream().map(Holder::value).map(this::getImplContextFor);
	}

	public static record Entry(
		List<Holder<VoronoiSettings>> settings,
		VoronoiBaseCompileContext baseContext,
		List<VoronoiImplCompileContext> implContexts
	) {}
}