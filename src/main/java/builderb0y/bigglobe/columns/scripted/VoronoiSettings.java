package builderb0y.bigglobe.columns.scripted;

import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.Voronoi2DColumnEntry;

public record VoronoiSettings(
	double weight,
	RegistryEntry<ColumnEntry> @DefaultEmpty [] enables,
	@DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = Voronoi2DColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, RegistryEntry<ColumnEntry>> exports
) {

}