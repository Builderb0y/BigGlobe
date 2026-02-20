package builderb0y.bigglobe.chunkgen;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKeys;
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
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.util.DelayedEntryList;

public class ScriptedColumnBiomeSource extends BiomeSource {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class);
	#else
		public static final Codec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class).codec();
	#endif

	public final ColumnYToBiomeScript.Holder script;
	public final DelayedEntryList<Biome> all_possible_biomes;
	public transient BigGlobeScriptedChunkGenerator generator;
	public transient ThreadLocal<@Nullable ScriptedColumn> columnThreadLocal;
	public final BetterRegistry<Biome> biomeRegistry;

	public ScriptedColumnBiomeSource(
		ColumnYToBiomeScript.Holder script,
		DelayedEntryList<Biome> all_possible_biomes,
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
						ColumnUsage.GENERIC.maybeDhHints(),
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
		return this.all_possible_biomes.entrySet();
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return this.all_possible_biomes.entryStream();
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
		ScriptedColumn column = this.columnThreadLocal.get();
		if (column != null) {
			column.setParams(column.params.at(x << 2, z << 2).hints(ColumnUsage.GENERIC.maybeDhHints()));
			return this.script.get(column, y << 2).entry;
		}
		else {
			return BigGlobeMod.getRegistry(RegistryKeys.BIOME).requireEntry(BiomeKeys.PLAINS);
		}
	}

	@Override
	public String toString() {
		//some other mod injects toString() into BiomeSource,
		//which explicitly queries our biome tag.
		//toString() is called by AutoCodec immediately after deserialization,
		//which logs a stack trace that we don't want.
		return "ScriptedColumnBiomeSource";
	}
}