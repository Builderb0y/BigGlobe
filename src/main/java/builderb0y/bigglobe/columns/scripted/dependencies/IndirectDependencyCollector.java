package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.HashSet;
import java.util.function.Consumer;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

public class IndirectDependencyCollector extends HashSet<RegistryEntry<? extends DependencyView>> implements Consumer<RegistryEntry<? extends DependencyView>> {

	public final BigGlobeScriptedChunkGenerator generator;

	public IndirectDependencyCollector(BigGlobeScriptedChunkGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void accept(RegistryEntry<? extends DependencyView> entry) {
		if (this.add(entry)) {
			entry.value().streamDirectDependencies(entry, this.generator.compiledWorldTraits).forEach(this);
		}
	}
}