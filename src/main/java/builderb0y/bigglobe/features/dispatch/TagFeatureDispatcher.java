package builderb0y.bigglobe.features.dispatch;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class TagFeatureDispatcher implements FeatureDispatcher {

	public final RegistryEntryList<FeatureDispatcher> tag;
	public transient RegistryEntry<FeatureDispatcher>[] sortedEntries;

	public TagFeatureDispatcher(RegistryEntryList<FeatureDispatcher> tag) {
		this.tag = tag;
	}

	@SuppressWarnings("unchecked") //generic array.
	public RegistryEntry<FeatureDispatcher>[] getSortedEntries() {
		RegistryEntry<FeatureDispatcher>[] entries = this.sortedEntries;
		if (entries == null) {
			entries = (
				this
				.tag
				.stream()
				.sorted(
					Comparator.comparing(
						UnregisteredObjectException::getID
					)
				)
				.toArray(RegistryEntry[]::new)
			);

			if (entries.length != 0) {
				this.sortedEntries = entries;
			}
			else {
				BigGlobeMod.LOGGER.warn("TagFeatureDispatcher.getSortedEntries() was called before tags were populated OR the tag " + UnregisteredObjectException.getTagKey(this.tag).id() + " is empty.", new Throwable("Stack trace"));
			}
		}
		return entries;
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return Arrays.stream(this.getSortedEntries());
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, RegistryEntry<FeatureDispatcher> selfEntry) {
		for (RegistryEntry<FeatureDispatcher> entry : this.getSortedEntries()) {
			entry.value().generate(world, random, chunkSeed, entry);
		}
	}
}