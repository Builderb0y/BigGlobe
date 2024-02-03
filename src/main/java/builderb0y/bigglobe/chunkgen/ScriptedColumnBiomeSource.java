package builderb0y.bigglobe.chunkgen;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToBiomeScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class ScriptedColumnBiomeSource extends BiomeSource {

	public static final Codec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptedColumnBiomeSource.class);

	public final ColumnYToBiomeScript.Holder script;
	public final TagKey<Biome> all_possible_biomes;
	public transient BigGlobeScriptedChunkGenerator generator;
	public transient ThreadLocal<ScriptedColumn> columnThreadLocal;

	public ScriptedColumnBiomeSource(ColumnYToBiomeScript.Holder script, TagKey<Biome> all_possible_biomes) {
		this.script = script;
		this.all_possible_biomes = all_possible_biomes;
		this.columnThreadLocal = ThreadLocal.withInitial(() -> {
			if (this.generator != null) {
				return this.generator.columnEntryRegistry.columnFactory.create(
					this.generator.seed,
					0,
					0,
					this.generator.height.min_y(),
					this.generator.height.max_y(),
					false
				);
			}
			else {
				return null;
			}
		});
	}

	@Override
	public Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	public Stream<RegistryEntry<Biome>> biomeStream() {
		return BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeyVersions.biome()).getEntryList(this.all_possible_biomes).map(RegistryEntryList::stream).orElseGet(Stream::empty);
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
		ScriptedColumn column = this.columnThreadLocal.get();
		if (column != null) {
			column.setPosDH(x << 2, z << 2, DistantHorizonsCompat.isOnDistantHorizonThread());
			return this.script.get(column, y << 2).entry();
		}
		else {
			return BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeyVersions.biome()).entryOf(BiomeKeys.PLAINS);
		}
	}
}