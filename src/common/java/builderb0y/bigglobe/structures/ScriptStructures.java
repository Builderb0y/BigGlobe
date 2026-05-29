package builderb0y.bigglobe.structures;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.mixins.StructureAccessor_WorldAccess;
import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.StructureLocator;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresManyBoxes;
import builderb0y.bigglobe.util.Streamable;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.CollectionTransformer;

public class ScriptStructures extends ArrayWrapper<StructureStartWrapper> {

	public static final StructureStartWrapper[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final ScriptStructures EMPTY_SCRIPT_STRUCTURES = new ScriptStructures(EMPTY_STRUCTURE_START_ARRAY);

	public ScriptStructures(StructureStartWrapper[] starts) {
		super(starts);
	}

	public static ScriptStructures[] getStructures(
		BigGlobeScriptedChunkGenerator generator,
		ConfiguredColumnFactory columns,
		StructureManager accessor,
		ChunkPos chunkPos,
		ColumnValueOverridersWithRadiusCache overriders
	) {
		if (generator.structuresEnabled) {
			StructureAccessor_WorldAccess structureAccessorAccessor = (StructureAccessor_WorldAccess)(accessor);
			if (structureAccessorAccessor.bigglobe_getWorld() instanceof ServerLevelAccessor serverAccess) {
				Registry<Structure> structureRegistry = StructureLocator.structureRegistry(serverAccess);
				BoundingBox chunkBox = WorldUtil.chunkBox(chunkPos, serverAccess);
				class Search extends ManyStructuresManyBoxes {

					public Search(Streamable<Holder<Structure>> structures) {
						super(structures);
					}

					public Search() {
						super(generator.structureLocator().allStructures());
					}

					@Override
					public BoundingBox getAreaFor(Holder<Structure> structure) {
						int expand = ((SizedStructure)(structure.value())).bigglobe_getMaxRadiusInBlocks();
						return chunkBox.inflatedBy(expand, 0, expand);
					}

					@Override
					public WhatToSearchFor filter(Streamable<Holder<Structure>> structures) {
						return new Search(structures);
					}
				}
				return (ScriptStructures[])(
					generator
					.structureLocator()
					.getStructuresIntersecting(
						new StructureLocator.Params(
							generator,
							columns,
							serverAccess.getLevel(),
							new Search()
						)
					)
					.sorted(
						Comparator
						.comparing((StructureStartWrapper start) -> start.start().getStructure().step())
						.thenComparing((StructureStartWrapper start) -> StructureLocator.structureID(start.start().getStructure()))
					)
					.collect(
						Collector.<StructureStartWrapper, List<StructureStartWrapper>[], ScriptStructures[]>of(
							() -> new List[overriders.overriders().length],
							(List<StructureStartWrapper>[] lists, StructureStartWrapper wrapper) -> {
								for (int index : overriders.getIndices(wrapper.start().getStructure())) {
									List<StructureStartWrapper> list = lists[index];
									if (list == null) {
										list = lists[index] = new ArrayBuilder<>();
									}
									list.add(wrapper);
								}
							},
							(List<StructureStartWrapper>[] lists1, List<StructureStartWrapper>[] lists2) -> {
								for (int index = 0; index < lists2.length; index++) {
									if (lists2[index] != null) {
										if (lists1[index] != null) lists1[index].addAll(lists2[index]);
										else lists1[index] = lists2[index];
									}
								}
								return lists1;
							},
							(List<StructureStartWrapper>[] starts) -> CollectionTransformer.convertArray(
								starts,
								ScriptStructures[]::new,
								(List<StructureStartWrapper> wrappers) -> (
									wrappers == null
									? ScriptStructures.EMPTY_SCRIPT_STRUCTURES
									: new ScriptStructures(
										wrappers.toArray(
											StructureStartWrapper.ARRAY_FACTORY
										)
									)
								)
							)
						)
					)
				);
			}
		}
		ScriptStructures[] array = new ScriptStructures[overriders.overriders().length];
		Arrays.fill(array, EMPTY_SCRIPT_STRUCTURES);
		return array;
	}
}