package builderb0y.bigglobe.features;

import java.util.Optional;

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

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
	import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
	import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
#endif

public abstract class FeatureAdapter<C extends FeatureAdapter.Config>
#if MC_VERSION >= MC_26_3_0
	implements Feature
#else
	extends Feature<C>
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

		@Override
		public MapCodec<? extends Feature> codec() {
			return BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(this.getClass());
		}

		@Override
		public boolean place(
			WorldGenLevel world,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BlockPos origin
		) {
			return this.place(new Context<>(
				world
				.registryAccess()
				.lookupOrThrow(Registries.FEATURE)
				.getResourceKey(this)
				.orElse(null),
				world,
				chunkGenerator,
				random,
				origin,
				this.config
			));
		}

	#else

		public FeatureAdapter(Codec<C> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<C> context) {
			ConfiguredFeature<?, ?> feature = context.topFeature().orElse(null);
			ResourceKey<?> key = null;
			if (feature != null) {
				key = (
					context
					.level()
					.registryAccess()
					.lookupOrThrow(Registries.CONFIGURED_FEATURE)
					.getResourceKey(feature)
					.orElse(null)
				);
			}
			return this.place(new Context<>(
				key,
				context.level(),
				context.chunkGenerator(),
				context.random(),
				context.origin(),
				context.config()
			));
		}

	#endif

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