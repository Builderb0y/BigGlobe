package builderb0y.bigglobe.chunkgen;

import java.util.Optional;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;

import net.minecraft.structure.StructureSet;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.NetherColumn;
import builderb0y.bigglobe.settings.NetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.util.SemiThreadLocal;

@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeNetherChunkGenerator extends BigGlobeChunkGenerator {

	public static final AutoCoder<BigGlobeNetherChunkGenerator> NETHER_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(BigGlobeNetherChunkGenerator.class);
	public static final Codec<BigGlobeNetherChunkGenerator> NETHER_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(NETHER_CODER);

	@EncodeInline
	public final NetherSettings settings;

	public transient SemiThreadLocal<ChunkOfColumns<NetherColumn>> chunkColumnCache;

	public BigGlobeNetherChunkGenerator(
		NetherSettings settings,
		Registry<StructureSet> structureSetRegistry
	) {
		super(
			structureSetRegistry,
			Optional.empty(),
			new ColumnBiomeSource(
				settings.local_settings().elements.stream().map(LocalNetherSettings::biome)
			)
		);
		this.settings = settings;
	}

	public static AutoCoder<BigGlobeNetherChunkGenerator> createCoder(FactoryContext<BigGlobeNetherChunkGenerator> context) {
		return BigGlobeChunkGenerator.createCoder(context, "bigglobe", "nether");
	}

	@Override
	public void setSeed(long seed) {
		super.setSeed(seed);
		this.chunkColumnCache = SemiThreadLocal.weak(4, () -> {
			return new ChunkOfColumns<>(NetherColumn[]::new, this::column);
		});
	}

	@Override
	public NetherColumn column(int x, int z) {
		return new NetherColumn(this.settings, this.seed, x, z);
	}

	@Override
	public void generateRawTerrain(Executor executor, Chunk chunk, StructureAccessor structureAccessor, boolean distantHorizons) {

	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {

	}

	@Override
	public Codec<? extends ChunkGenerator> getCodec() {
		return NETHER_CODEC;
	}

	@Override
	public void populateEntities(ChunkRegion region) {

	}

	@Override
	public int getWorldHeight() {
		return this.settings.height();
	}

	@Override
	public int getSeaLevel() {
		return this.settings.min_y();
	}

	@Override
	public int getMinimumY() {
		return this.settings.min_y();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		return this.settings.max_y();
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		return null; //fixme: implement this.
	}
}