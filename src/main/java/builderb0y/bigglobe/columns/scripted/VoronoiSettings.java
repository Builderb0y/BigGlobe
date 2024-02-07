package builderb0y.bigglobe.columns.scripted;

import java.util.Map;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@UseVerifier(name = "verify", in = VoronoiSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
public record VoronoiSettings(
	RegistryEntry<ColumnEntry> owner,
	double weight,
	@DefaultEmpty Set<RegistryEntry<ColumnEntry>> enables,
	@DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = VoronoiColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, RegistryEntry<ColumnEntry>> exports
) {

	public static <T_Enabled> void verify(VerifyContext<T_Enabled, VoronoiSettings> context) throws VerifyException {
		VoronoiSettings settings = context.object;
		if (settings == null) return;
		for (RegistryEntry<ColumnEntry> value : settings.exports().values()) {
			if (!settings.enables().contains(value)) {
				throw new VerifyException(() -> "voronoi_settings exports " + UnregisteredObjectException.getID(value) + " without enabling it");
			}
		}
	}
}