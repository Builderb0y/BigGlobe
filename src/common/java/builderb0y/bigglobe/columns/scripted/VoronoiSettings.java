package builderb0y.bigglobe.columns.scripted;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted2.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@UseVerifier(name = "verify", in = VoronoiSettings.class, usage = MemberUsage.METHOD_IS_HANDLER)
public record VoronoiSettings(
	Holder<ColumnEntry> owner,
	@VerifyFloatRange(min = 0.0D) double weight,
	@DefaultEmpty Set<Holder<ColumnEntry>> enables,
	@DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = VoronoiColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, Holder<ColumnEntry>> exports
)
	implements SimpleDependencyView {

	public static final AutoCoder<VoronoiSettings> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(VoronoiSettings.class);

	public static <T_Enabled> void verify(VerifyContext<T_Enabled, VoronoiSettings> context) throws VerifyException {
		VoronoiSettings settings = context.object;
		if (settings == null) return;
		for (Holder<ColumnEntry> value : settings.exports().values()) {
			if (!settings.enables().contains(value)) {
				throw new VerifyException(() -> "voronoi_settings exports " + UnregisteredObjectException.getID(value) + " without enabling it");
			}
		}
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.of(Stream.of(this.owner), this.enables.stream(), this.exports.values().stream()).flatMap(Function.identity());
	}
}