package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.bigglobe.scripting.wrappers.tags.StructureTag;
import builderb0y.bigglobe.structures.NoopStructure;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class StructureEntry extends EntryWrapper<Structure, StructureTag> {

	public static final TypeInfo TYPE = TypeInfo.of(StructureEntry.class);
	public static final NoopStructure CLIENT_NOOP_STRUCTURE = new NoopStructure(
		new Structure.StructureSettings(

			HolderSet.empty(),

			Collections.emptyMap(),
			GenerationStep.Decoration.RAW_GENERATION,
			TerrainAdjustment.NONE
		)
	);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public final BiomeTag validBiomes;
	public final GenerationStep.Decoration step;
	public final TerrainAdjustment terrainAdaptation;
	public final StructureTypeEntry type;

	public StructureEntry(Holder<Structure> entry) {
		super(entry);
		this.validBiomes = new BiomeTag(new DelayedEntryList<>(BigGlobeMod.getRegistry(Registries.BIOME), entry.value().biomes()));
		this.step = entry.value().step();
		this.terrainAdaptation = entry.value().terrainAdaptation();
		this.type = new StructureTypeEntry(
			RegistryVersions.getEntry(
				BuiltInRegistries.STRUCTURE_TYPE,
				entry.value().type()
			)
		);
	}

	public StructureEntry(
		Holder<Structure> entry,
		BiomeTag validBiomes,
		GenerationStep.Decoration step,
		TerrainAdjustment terrainAdaptation
	) {
		super(entry);
		this.step = step;
		this.validBiomes = validBiomes;
		this.terrainAdaptation = terrainAdaptation;
		this.type = new StructureTypeEntry(
			RegistryVersions.getEntry(
				BuiltInRegistries.STRUCTURE_TYPE,
				entry.value().type()
			)
		);
	}

	public static StructureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static StructureEntry of(String id, int flags) {
		Holder<Structure> entry = ConstantFactory.getEntryServerOnly(Registries.STRUCTURE, id, flags, CLIENT_NOOP_STRUCTURE);
		return entry != null ? new StructureEntry(entry) : null;
	}

	public StructureTypeEntry type() {
		return this.type;
	}

	public String generationStep() {
		return this.step.getSerializedName();
	}

	public BiomeTag validBiomes() {
		return this.validBiomes;
	}

	public String terrainAdaptation() {
		return this.terrainAdaptation.getSerializedName();
	}

	@Override
	public boolean isIn(StructureTag entries) {
		return super.isIn(entries);
	}
}