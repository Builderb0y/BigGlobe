package builderb0y.bigglobe.chunkgen;

import java.util.Set;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBiomeScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.util.DelayedEntryList;

public class ScriptedColumnBiomeSource extends BiomeSource {

	public static final MapCodec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class);

	public final ColumnYToBiomeScript.Catcher script;
	public final DelayedEntryList<Biome> all_possible_biomes;
	public transient BigGlobeScriptedChunkGenerator generator;
	public transient ThreadLocal<@Nullable ScriptedColumn> columnThreadLocal;
	public final BetterRegistry<Biome> biomeRegistry;

	public ScriptedColumnBiomeSource(
		ColumnYToBiomeScript.Catcher script,
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
	public MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	public Set<Holder<Biome>> possibleBiomes() {
		return this.all_possible_biomes.entrySet();
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return this.all_possible_biomes.entryStream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler noise) {
		ScriptedColumn column = this.columnThreadLocal.get();
		if (column != null) {
			column.setParams(column.params.at(x << 2, z << 2).hints(ColumnUsage.GENERIC.maybeDhHints()));
			return this.script.get(column, y << 2).entry;
		}
		else {
			return BigGlobeMod.getRegistry(Registries.BIOME).requireEntry(Biomes.PLAINS);
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