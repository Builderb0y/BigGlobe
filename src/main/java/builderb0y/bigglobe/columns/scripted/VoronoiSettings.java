package builderb0y.bigglobe.columns.scripted;

import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryRegistrable;

public record VoronoiSettings(
	double weight,
	RegistryEntry<ColumnEntryRegistrable> @DefaultEmpty [] enables,
	@DefaultEmpty Map<Identifier, RegistryEntry<ColumnEntryRegistrable>> provides
) {

}