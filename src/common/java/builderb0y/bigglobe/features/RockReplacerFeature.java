package builderb0y.bigglobe.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Async;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public interface RockReplacerFeature<T_Config extends FeatureConfiguration> {

	public abstract void replaceRocks(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		T_Config config
	);

	/**
	splits the work into a part which is done once per chunk,
	and a part which is done for every section, and only modifies that section.
	this lets all rock replacers be applied to one section before moving on to the next one.
	returns null if the feature can't work one section at a time.
	*/
	public default @Nullable SectionReplacer prepare(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		T_Config config
	) {
		return null;
	}

	public static interface SectionReplacer {

		/**
		may be called for sections outside the range this replacer works on,
		in which case it should do nothing.
		may be called from several threads at once, but only once per section.
		*/
		public abstract void replaceSection(int sectionCoord);

		/**
		adds every state which might be present in the section after this replacer runs,
		assuming present contains every state which might be present before it runs.
		only used to grow the palette ahead of time, so this is allowed to be wrong.
		*/
		public default void addPossibleOutputs(int sectionCoord, Set<BlockState> present) {}
	}

	/**
	applies all the replacers in the same order as calling replaceRocks() on each one would,
	but one section at a time wherever possible.
	if every replacer supports {@link #prepare}, afterAll is run on every section
	after all the replacers are done with it, and this method returns true.
	otherwise, this method returns false and the caller should run it itself.
	*/
	public static boolean replaceAll(
		BigGlobeScriptedChunkGenerator generator,
		WorldWrapper worldWrapper,
		ChunkAccess chunk,
		int minSection,
		int maxSection,
		ConfiguredRockReplacerFeature<?>[] replacers,
		@Nullable IntConsumer afterAll
	) {
		int chunkMinSection = HeightLimitViewVersions.getSectionMinY(chunk);
		int chunkMaxSection = HeightLimitViewVersions.getSectionMaxY(chunk);
		boolean ranAfterAll = false;
		int index = 0;
		while (index < replacers.length) {
			List<SectionReplacer> group = new ArrayList<>(replacers.length - index);
			SectionReplacer prepared;
			while (index < replacers.length && (prepared = replacers[index].prepare(generator, worldWrapper, chunk, minSection, maxSection)) != null) {
				group.add(prepared);
				index++;
			}
			boolean last = index == replacers.length;
			if (!group.isEmpty()) {
				SectionReplacer[] groupArray = group.toArray(new SectionReplacer[group.size()]);
				IntConsumer after = last && group.size() == replacers.length ? afterAll : null;
				Async.loop(BigGlobeThreadPool.autoExecutor(), chunkMinSection, chunkMaxSection, 1, (int sectionCoord) -> {
					growPalette(chunk, sectionCoord, groupArray);
					for (SectionReplacer replacer : groupArray) {
						replacer.replaceSection(sectionCoord);
					}
					if (after != null) after.accept(sectionCoord);
				});
				if (after != null) ranAfterAll = true;
			}
			if (!last) {
				replacers[index++].replaceRocks(generator, worldWrapper, chunk, minSection, maxSection);
			}
		}
		if (replacers.length == 0 && afterAll != null) {
			Async.loop(BigGlobeThreadPool.autoExecutor(), chunkMinSection, chunkMaxSection, 1, afterAll);
			ranAfterAll = true;
		}
		return ranAfterAll;
	}

	/**
	rock replacers add a lot of new states to sections, one at a time,
	and every time the palette runs out of room, all 4096 blocks get re-packed.
	so, grow the palette once, to a size which fits everything that could get added.
	the palette gets re-packed anyway when the chunk is saved,
	so this has no effect on what ends up on disk.
	*/
	public static void growPalette(ChunkAccess chunk, int sectionCoord, SectionReplacer[] replacers) {
		SectionGenerationContext context = SectionGenerationContext.forSectionCoord(
			chunk,
			chunk.getSection(chunk.getSectionIndexFromSectionY(sectionCoord)),
			sectionCoord
		);
		if (context.storage().getBits() >= 8) return;
		Palette<BlockState> palette = context.palette();
		int paletteSize = palette.getSize();
		Set<BlockState> present = new ReferenceOpenHashSet<>(paletteSize + 32);
		for (int id = 0; id < paletteSize; id++) {
			present.add(palette.valueFor(id));
		}
		int before = present.size();
		for (SectionReplacer replacer : replacers) {
			replacer.addPossibleOutputs(sectionCoord, present);
		}
		if (present.size() > before) {
			context.ensurePaletteCapacity(present.size());
		}
	}

	public static record ConfiguredRockReplacerFeature<T_Config extends FeatureConfiguration>(RockReplacerFeature<T_Config> feature, T_Config config) {

		@SuppressWarnings("unchecked")
		public ConfiguredRockReplacerFeature(ConfiguredFeature<?, ?> configuredFeature) {
			this(
				(RockReplacerFeature<T_Config>)(configuredFeature.feature()),
				(T_Config)(configuredFeature.config())
			);
		}

		public void replaceRocks(
			BigGlobeScriptedChunkGenerator generator,
			WorldWrapper worldWrapper,
			ChunkAccess chunk,
			int minSection,
			int maxSection
		) {
			this.feature.replaceRocks(
				generator,
				worldWrapper,
				chunk,
				minSection,
				maxSection,
				this.config
			);
		}

		public @Nullable SectionReplacer prepare(
			BigGlobeScriptedChunkGenerator generator,
			WorldWrapper worldWrapper,
			ChunkAccess chunk,
			int minSection,
			int maxSection
		) {
			return this.feature.prepare(
				generator,
				worldWrapper,
				chunk,
				minSection,
				maxSection,
				this.config
			);
		}
	}
}
