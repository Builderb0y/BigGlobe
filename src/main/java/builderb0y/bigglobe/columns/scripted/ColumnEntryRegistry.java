package builderb0y.bigglobe.columns.scripted;

import java.util.*;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntryAccessor;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryRegistrable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public final BetterRegistry<ColumnEntryRegistrable> registry;
	public final BetterRegistry<VoronoiSettings> voronoiRegistry;
	public final ColumnEntryRegistrable[] cachedRegistrables;
	public final Map<Identifier, ColumnEntryAccessor> namedAccessors;

	public ColumnEntryRegistry(BetterRegistry<ColumnEntryRegistrable> columnEntries, BetterRegistry<VoronoiSettings> voronoiRegistry) {
		this.registry = columnEntries;
		this.voronoiRegistry = voronoiRegistry;
		RegistryEntry<ColumnEntryRegistrable>[] registrables = columnEntries.streamEntries().toArray(RegistryEntry[]::new);
		this.cachedRegistrables = Arrays.stream(registrables).map(RegistryEntry::value).filter(ColumnEntryRegistrable::hasEntry).toArray(ColumnEntryRegistrable[]::new);
		int length = registrables.length;
		Map<Identifier, ColumnEntryAccessor> namedAccessors = new HashMap<>(length + 16);
		IdentityHashMap<ColumnEntryAccessor, RegistryEntry<ColumnEntryRegistrable>> fromCache = new IdentityHashMap<>(length + 16);
		IdentityHashMap<RegistryEntry<ColumnEntryRegistrable>, List<ColumnEntryAccessor>> toCache = new IdentityHashMap<>(length + 16);
		int slot = 0;
		for (RegistryEntry<ColumnEntryRegistrable> entry : registrables) {
			ColumnEntryRegistrable registrable = entry.value();
			List<ColumnEntryAccessor> current = new ArrayList<>(1);
			registrable.createAccessors(entry.getKey().orElseThrow().getValue().toString(), registrable.hasEntry() ? slot++ : -1, (String name, ColumnEntryAccessor accessor) -> {
				ColumnEntryAccessor old = namedAccessors.putIfAbsent(new Identifier(name), accessor);
				if (old != null) throw new IllegalArgumentException("Duplicate accessor named " + name + ": " + old + " -> " + accessor);
				fromCache.put(accessor, entry);
				current.add(accessor);
			});
			toCache.put(entry, current);
		}
		this.namedAccessors = namedAccessors;

		voronoiRegistry
		.streamEntries()
		.map(RegistryEntry::value)
		.map(VoronoiSettings::enables)
		.flatMap(Arrays::stream)
		.map(toCache::get)
		.flatMap(List::stream)
		.forEach((ColumnEntryAccessor accessor) -> accessor.mustBeManuallyEnabledByVoronoi = true);

		for (ColumnEntryAccessor accessor : this.namedAccessors.values()) try {
			accessor.setupAndCompile(this);
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException("Exception preparing column value " + fromCache.get(accessor), exception);
		}
	}

	public <A extends ColumnEntryAccessor> A getAccessor(String name, Class<A> type) {
		Identifier identifier = BigGlobeAutoCodec.toID(name, BigGlobeMod.MODID);
		ColumnEntryAccessor accessor = this.namedAccessors.get(identifier);
		if (accessor == null) throw new NullPointerException("No such accessor with name " + identifier);
		return type.cast(accessor);
	}

	public ColumnEntry[] createEntries() {
		ColumnEntryRegistrable[] cachedRegistrables = this.cachedRegistrables;
		int length = cachedRegistrables.length;
		ColumnEntry[] entries = new ColumnEntry[length];
		for (int index = 0; index < length; index++) {
			entries[index] = cachedRegistrables[index].createEntry();
		}
		return entries;
	}
}