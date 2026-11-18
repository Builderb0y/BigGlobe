package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.impl.biome.modification.BuiltInResourceKeys;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public abstract class FeatureAdapter<C>
#if MC_VERSION >= MC_26_3_0
	implements Feature
#else
	extends Feature
#endif
{

	#if MC_VERSION >= MC_26_3_0

		public final C config;

		public FeatureAdapter(C config) {
			this.config = config;
		}

		public C config() {
			return this.config;
		}

	#else

		public FeatureAdapter(Codec<C> codec) {
			super(codec);
		}

	#endif

	@Override
	public MapCodec<? extends Feature> codec() {
		return BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(this.getClass());
	}

	@Override
	public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		return this.place(new Context<>(
			world
			.registryAccess()
			.lookupOrThrow(Registries.FEATURE_TYPE)
			.getResourceKey(this.codec())
			.orElse(null),
			world,
			chunkGenerator,
			random,
			origin,
			this.config
		));
	}

	public abstract boolean place(Context<C> context);

	public static record Context<C>(
		@Nullable ResourceKey<?> topFeature,
		WorldGenLevel level,
		ChunkGenerator chunkGenerator,
		RandomSource random,
		BlockPos origin,
		C config
	) {}

	public static interface Config
	#if MC_VERSION < MC_26_3_0
		extends FeatureConfiguration
	#endif
	{}
}