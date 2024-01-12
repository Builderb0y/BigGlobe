package builderb0y.bigglobe.columns.scripted;

import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;

public record VoronoiSettings(
	double weight,
	RegistryEntry<ColumnEntry> @DefaultEmpty [] enables,
	@DefaultEmpty Map<String, RegistryEntry<ColumnEntry>> exports
) {

}