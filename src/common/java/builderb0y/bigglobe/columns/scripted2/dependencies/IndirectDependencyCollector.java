package builderb0y.bigglobe.columns.scripted2.dependencies;

import java.util.HashSet;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

public class IndirectDependencyCollector extends HashSet<Holder<? extends DependencyView>> implements Consumer<Holder<? extends DependencyView>> {

	public final BigGlobeScriptedChunkGenerator generator;

	public IndirectDependencyCollector(BigGlobeScriptedChunkGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void accept(Holder<? extends DependencyView> entry) {
		if (this.add(entry)) {
			entry.value().streamDirectDependencies(entry, this.generator.compiledWorldTraits).forEach(this);
		}
	}
}