package builderb0y.bigglobe.chunkgen;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;

import builderb0y.autocodec.util.Compatibility;
import builderb0y.bigglobe.columns.WorldColumn;

public class ColumnBiomeSource extends BiomeSource {

	public static final Codec<ColumnBiomeSource> CODEC = new Codec<>() {

		@Override
		public <T> DataResult<Pair<ColumnBiomeSource, T>> decode(DynamicOps<T> ops, T input) {
			return Compatibility.createErrorDataResult(() -> "Should not decode ColumnBiomeSource directly.");
		}

		@Override
		public <T> DataResult<T> encode(ColumnBiomeSource input, DynamicOps<T> ops, T prefix) {
			return Compatibility.createErrorDataResult(() -> "Should not encode ColumnBiomeSource directly.");
		}

		@Override
		public String toString() {
			return "ColumnBiomeSource.CODEC";
		}
	};

	public BigGlobeChunkGenerator generator;
	public ThreadLocal<WorldColumn> threadLocalColumn;

	public ColumnBiomeSource(Stream<RegistryEntry<Biome>> biomeStream) {
		super(biomeStream);
	}

	public void setGenerator(BigGlobeChunkGenerator generator) {
		this.generator = generator;
		this.threadLocalColumn = ThreadLocal.withInitial(() -> generator.column(0, 0));
	}

	@Override
	public Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseSampler noise) {
		WorldColumn column = this.threadLocalColumn.get();
		column.setPos(x << 2, z << 2);
		return column.getBiome(y << 2);
	}
}