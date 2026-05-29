package builderb0y.bigglobe.structures;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted2.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class DelegatingStructure extends Structure implements SizedStructure {

	public static final MapCodec<DelegatingStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(DelegatingStructure.class);

	public final Holder<Structure> delegate;
	public final @EncodeInline NullableConfig nullable_config;
	public final @VerifyNullable Integer max_radius_in_chunks;
	public boolean checkedCyclicReference;

	public DelegatingStructure(Holder<Structure> delegate, NullableConfig nullable_config, @VerifyNullable Integer max_radius_in_chunks) {
		super(nullable_config.toConfig());
		this.nullable_config = nullable_config;
		this.max_radius_in_chunks = max_radius_in_chunks;
		this.delegate = delegate;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return this.max_radius_in_chunks != null ? this.max_radius_in_chunks.intValue() : ((SizedStructure)(unwrap(this.delegate()).value())).bigglobe_getMaxRadiusInChunks();
	}

	public Holder<Structure> delegate() {
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

	public static Holder<Structure> unwrap(Holder<Structure> structure) {
		while (structure.value() instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate();
		}
		return structure;
	}

	@Override
	public HolderSet<Biome> biomes() {
		return this.nullable_config.biomes != null ? this.nullable_config.biomes.tag() : this.delegate().value().biomes();
	}

	@Override
	public Map<MobCategory, StructureSpawnOverride> spawnOverrides() {
		return this.nullable_config.spawn_overrides != null ? this.nullable_config.spawn_overrides : this.delegate().value().spawnOverrides();
	}

	@Override
	public GenerationStep.Decoration step() {
		return this.nullable_config.step != null ? this.nullable_config.step : this.delegate().value().step();
	}

	@Override
	public TerrainAdjustment terrainAdaptation() {
		return this.nullable_config.terrain_adaptation != null ? this.nullable_config.terrain_adaptation : this.delegate().value().terrainAdaptation();
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return this.delegate().value().findValidGenerationPoint(context);
	}

	public boolean canDelegateStart() {
		return this.nullable_config.spawn_overrides == null && this.nullable_config.step == null && this.nullable_config.terrain_adaptation == null;
	}

	@Override
	public StructureType<?> type() {
		return BigGlobeStructures.DELEGATING_TYPE;
	}

	public static record NullableConfig(
		@VerifyNullable DelayedEntryList<Biome> biomes,
		@VerifyNullable Map<
							@UseCoder(name = "SPAWN_GROUP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) MobCategory,
							@UseCoder(name = "STRUCTURE_SPAWNS_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) StructureSpawnOverride
							>
		spawn_overrides,
		GenerationStep.@VerifyNullable @UseCoder(name = "STEP_AUTO_CODER", in = NullableConfig.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER) Decoration step,
		@VerifyNullable TerrainAdjustment terrain_adaptation
	) {

		public static final AutoCoder<MobCategory> SPAWN_GROUP_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(MobCategory.CODEC);
		public static final AutoCoder<StructureSpawnOverride> STRUCTURE_SPAWNS_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(StructureSpawnOverride.CODEC);
		public static final AutoCoder<GenerationStep.Decoration> STEP_AUTO_CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(GenerationStep.Decoration.CODEC);

		public StructureSettings toConfig() {
			//fill in sensible defaults in case some other mod tries accessing this in a hacky way.
			return new StructureSettings(
				HolderSet.direct(Collections.emptyList()),
				Collections.emptyMap(),
				GenerationStep.Decoration.RAW_GENERATION,
				TerrainAdjustment.NONE
			);
		}
	}
}