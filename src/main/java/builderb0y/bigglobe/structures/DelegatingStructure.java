package builderb0y.bigglobe.structures;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

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
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class DelegatingStructure extends Structure implements SizedStructure {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<DelegatingStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(DelegatingStructure.class);
	#else
		public static final Codec<DelegatingStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(DelegatingStructure.class).codec();
	#endif

	public final RegistryEntry<Structure> delegate;
	public final @EncodeInline NullableConfig nullable_config;
	public final @VerifyNullable Integer max_radius_in_chunks;
	public boolean checkedCyclicReference;

	public DelegatingStructure(RegistryEntry<Structure> delegate, NullableConfig nullable_config, @VerifyNullable Integer max_radius_in_chunks) {
		super(nullable_config.toConfig());
		this.nullable_config = nullable_config;
		this.max_radius_in_chunks = max_radius_in_chunks;
		this.delegate = delegate;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return this.max_radius_in_chunks != null ? this.max_radius_in_chunks.intValue() : ((SizedStructure)(unwrap(this.delegate()).value())).bigglobe_getMaxRadiusInChunks();
	}

	public RegistryEntry<Structure> delegate() {
		if (!this.checkedCyclicReference) {
			Set<Structure> stack = new ReferenceOpenHashSet<>();
			for (Structure structure = this; structure instanceof DelegatingStructure delegating; structure = delegating.delegate.value()) {
				if (!stack.add(structure)) {
					throw new CyclicDependencyException("Cyclic delegating structure: first structure delegates to " + UnregisteredObjectException.getID(this.delegate));
				}
			}
			this.checkedCyclicReference = true;
		}
		return this.delegate;
	}

	public static RegistryEntry<Structure> unwrap(RegistryEntry<Structure> structure) {
		while (structure.value() instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate();
		}
		return structure;
	}

	@Override
	public RegistryEntryList<Biome> getValidBiomes() {
		return this.nullable_config.biomes != null ? this.nullable_config.biomes.tag() : this.delegate().value().getValidBiomes();
	}

	@Override
	public Map<SpawnGroup, StructureSpawns> getStructureSpawns() {
		return this.nullable_config.spawn_overrides != null ? this.nullable_config.spawn_overrides : this.delegate().value().getStructureSpawns();
	}

	@Override
	public GenerationStep.Feature getFeatureGenerationStep() {
		return this.nullable_config.step != null ? this.nullable_config.step : this.delegate().value().getFeatureGenerationStep();
	}

	@Override
	public StructureTerrainAdaptation getTerrainAdaptation() {
		return this.nullable_config.terrain_adaptation != null ? this.nullable_config.terrain_adaptation : this.delegate().value().getTerrainAdaptation();
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		return this.delegate().value().getValidStructurePosition(context);
	}

	public boolean canDelegateStart() {
		return this.nullable_config.spawn_overrides == null && this.nullable_config.step == null && this.nullable_config.terrain_adaptation == null;
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.DELEGATING_TYPE;
	}

	public static record NullableConfig(
		@VerifyNullable DelayedEntryList<Biome> biomes,
		@VerifyNullable Map<
			@UseCoder(name = "SPAWN_GROUP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) SpawnGroup,
			@UseCoder(name = "STRUCTURE_SPAWNS_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) StructureSpawns
		>
		spawn_overrides,
		GenerationStep.@VerifyNullable @UseCoder(name = "STEP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) Feature step,
		@VerifyNullable StructureTerrainAdaptation terrain_adaptation
	) {

		public static final AutoCoder<SpawnGroup> SPAWN_GROUP_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(SpawnGroup.CODEC);
		public static final AutoCoder<StructureSpawns> STRUCTURE_SPAWNS_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(StructureSpawns.CODEC);
		public static final AutoCoder<GenerationStep.Feature> STEP_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(GenerationStep.Feature.CODEC);

		public Config toConfig() {
			//fill in sensible defaults in case some other mod tries accessing this in a hacky way.
			return new Config(
				RegistryEntryList.of(Collections.emptyList()),
				Collections.emptyMap(),
				GenerationStep.Feature.RAW_GENERATION,
				StructureTerrainAdaptation.NONE
			);
		}
	}
}