package builderb0y.bigglobe.chunkgen;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBiomeScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class ScriptedColumnBiomeSource extends BiomeSource {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class);
	#else
		public static final Codec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class).codec();
	#endif

	public final ColumnYToBiomeScript.Holder script;
	public final TagKey<Biome> all_possible_biomes;
	public transient BigGlobeScriptedChunkGenerator generator;
	public transient ThreadLocal<@Nullable ScriptedColumn> columnThreadLocal;
	public final BetterRegistry<Biome> biomeRegistry;
	public transient Set<RegistryEntry<Biome>> allPossibleBiomes = Collections.emptySet();

	public ScriptedColumnBiomeSource(
		ColumnYToBiomeScript.Holder script,
		TagKey<Biome> all_possible_biomes,
		BetterRegistry<Biome> biomeRegistry
	) {
		this.script = script;
		this.all_possible_biomes = all_possible_biomes;
		this.biomeRegistry = biomeRegistry;
		this.columnThreadLocal = ThreadLocal.withInitial(() -> {
			if (this.generator != null) {
				return this.generator.columnEntryRegistry.columnFactory.create(
					new Params(
						this.generator.columnSeed,
						0,
						0,
						this.generator.height.min_y(),
						this.generator.height.max_y(),
						Purpose.generic(),
						this.generator.compiledWorldTraits
					)
				);
			}
			else {
				return null;
			}
		});
	}

	@Override
	public #if MC_VERSION >= MC_1_20_5 MapCodec #else Codec #endif <? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	public Set<RegistryEntry<Biome>> getBiomes() {
		if (this.allPossibleBiomes.isEmpty()) {
			RegistryEntryList<Biome> tag = this.biomeRegistry.getOrCreateTag(this.all_possible_biomes);
			if (tag.size() != 0) {
				this.allPossibleBiomes = tag.stream().collect(Collectors.toSet());
			}
			else {
				BigGlobeMod.LOGGER.warn("", new IllegalStateException("Something tried to query ScriptedColumnBiomeSource.getBiomes() before tags are loaded OR the biome tag " + this.all_possible_biomes.id() + " is empty."));
			}
		}
		return this.allPossibleBiomes;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		throw new UnsupportedOperationException("Call getBiomes().stream() instead of reflecting into this protected method.");
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
		ScriptedColumn column = this.columnThreadLocal.get();
		if (column != null) {
			column.setParams(column.params.at(x << 2, z << 2));
			return this.script.get(column, y << 2).entry();
		}
		else {
			return BigGlobeMod.getRegistry(RegistryKeyVersions.biome()).getOrCreateEntry(BiomeKeys.PLAINS);
		}
	}
}