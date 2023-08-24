package builderb0y.bigglobe.structures;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.util.TagOrObject;

public class DelegatingStructure extends Structure {

	public static final Codec<DelegatingStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(DelegatingStructure.class);

	public final RegistryEntry<Structure> delegate;
	public final @EncodeInline NullableConfig nullable_config;

	public DelegatingStructure(NullableConfig nullable_config, RegistryEntry<Structure> delegate) {
		super(nullable_config.toConfig());
		this.nullable_config = nullable_config;
		this.delegate = delegate;
	}

	@Override
	public RegistryEntryList<Biome> getValidBiomes() {
		return this.nullable_config.biomes != null ? this.nullable_config.biomes.toRegistryEntryList() : this.delegate.value().getValidBiomes();
	}

	@Override
	public Map<SpawnGroup, StructureSpawns> getStructureSpawns() {
		return this.nullable_config.spawn_overrides != null ? this.nullable_config.spawn_overrides : this.delegate.value().getStructureSpawns();
	}

	@Override
	public GenerationStep.Feature getFeatureGenerationStep() {
		return this.nullable_config.step != null ? this.nullable_config.step : this.delegate.value().getFeatureGenerationStep();
	}

	@Override
	public StructureTerrainAdaptation getTerrainAdaptation() {
		return this.nullable_config.terrain_adaptation != null ? this.nullable_config.terrain_adaptation : this.delegate.value().getTerrainAdaptation();
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		#if MC_VERSION <= MC_1_19_2
			return this.delegate.value().getStructurePosition(context);
		#else
			return this.delegate.value().getValidStructurePosition(context);
		#endif
	}

	public boolean canDelegateStart() {
		return this.nullable_config.spawn_overrides == null && this.nullable_config.step == null && this.nullable_config.terrain_adaptation == null;
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.DELEGATING_TYPE;
	}

	public static record NullableConfig(
		@VerifyNullable TagOrObject<Biome> biomes,
		@VerifyNullable Map<
			@UseCoder(name = "SPAWN_GROUP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) SpawnGroup,
			@UseCoder(name = "STRUCTURE_SPAWNS_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) StructureSpawns
		>
		spawn_overrides,
		GenerationStep.@VerifyNullable @UseCoder(name = "STEP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) Feature step,
		@VerifyNullable StructureTerrainAdaptation terrain_adaptation
	) {

		public static final AutoCoder<SpawnGroup> SPAWN_GROUP_AUTO_CODER = BigGlobeAutoCodec.forceNullable(BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(SpawnGroup.CODEC, false));
		public static final AutoCoder<StructureSpawns> STRUCTURE_SPAWNS_AUTO_CODER = BigGlobeAutoCodec.forceNullable(BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(StructureSpawns.CODEC, false));
		public static final AutoCoder<GenerationStep.Feature> STEP_AUTO_CODER = BigGlobeAutoCodec.forceNullable(BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(GenerationStep.Feature.CODEC, false));

		public Config toConfig() {
			//fill in sensible defaults in case some other mod tries accessing this in a hacky way.
			return new Config(
				this.biomes             != null ? this.biomes.toRegistryEntryList() : RegistryEntryList.of(Collections.emptyList()),
				this.spawn_overrides    != null ? this.spawn_overrides              : Collections.emptyMap(),
				this.step               != null ? this.step                         : GenerationStep.Feature.RAW_GENERATION,
				this.terrain_adaptation != null ? this.terrain_adaptation           : StructureTerrainAdaptation.NONE
			);
		}
	}
}