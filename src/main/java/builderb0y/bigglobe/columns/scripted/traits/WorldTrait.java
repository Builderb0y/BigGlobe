package builderb0y.bigglobe.columns.scripted.traits;

import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.parsing.input.ScriptUsage;

public record WorldTrait(
	@EncodeInline AccessSchema schema,
	@UseName("default") @VerifyNullable ScriptUsage fallback
)
implements DependencyView {

	public static final AutoCoder<WorldTrait> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(WorldTrait.class);

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies(RegistryEntry<? extends DependencyView> self, WorldTraits traits) {
		return traits.dependenciesPerTrait.get(self).streamDirectDependencies();
	}
}