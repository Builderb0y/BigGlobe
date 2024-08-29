package builderb0y.bigglobe.columns.scripted.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import builderb0y.bigglobe.columns.scripted.ColumnValueHolder.ColumnValueInfo.Mutability;
import builderb0y.bigglobe.columns.scripted.ColumnValueHolder.UnresolvedColumnValueInfo;
import builderb0y.bigglobe.columns.scripted.VoronoiDataBase;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareZeroConditionTree;
import builderb0y.scripting.bytecode.tree.flow.compare.IntCompareZeroInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ConditionToBooleanInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseAndInsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class AbstractVoronoiDataCompileContext extends DataCompileContext {

	public AbstractVoronoiDataCompileContext(DataCompileContext parent) {
		super(parent);
	}

	@Override
	public Stream<UnresolvedColumnValueInfo> preprocessColumnValueInfos(Stream<UnresolvedColumnValueInfo> infos) {
		return Stream.concat(
			Stream.of(
				new UnresolvedColumnValueInfo("id",                         TypeInfos.STRING, false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("cell_x",                     TypeInfos.INT,    false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("cell_z",                     TypeInfos.INT,    false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("center_x",                   TypeInfos.INT,    false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("center_z",                   TypeInfos.INT,    false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("soft_distance_squared",      TypeInfos.DOUBLE, false, Mutability.VORONOI),
				new UnresolvedColumnValueInfo("soft_distance",              TypeInfos.DOUBLE, false, Mutability.VORONOI),
				new UnresolvedColumnValueInfo("hard_distance_squared",      TypeInfos.DOUBLE, false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("hard_distance",              TypeInfos.DOUBLE, false, Mutability.VORONOI),
				new UnresolvedColumnValueInfo("euclidean_distance_squared", TypeInfos.DOUBLE, false, Mutability.COMPUTED),
				new UnresolvedColumnValueInfo("euclidean_distance",         TypeInfos.DOUBLE, false, Mutability.VORONOI)
			),
			infos
		);
	}

	@Override
	public void preprocessColumnValueCases(Int2ObjectSortedMap<InsnTree> cases, PreprocessMethod method) {
		InsnTree self = this.loadSelf();
		switch (method) {
			case IS_COLUMN_VALUE_PRESENT -> {
				addCase(cases, "soft_distance_squared",      this.testFlag(self, VoronoiDataBase.SOFT_DISTANCE_SQUARED_FLAG), false);
				addCase(cases, "soft_distance",              this.testFlag(self, VoronoiDataBase.SOFT_DISTANCE_FLAG        ), false);
				addCase(cases, "hard_distance",              this.testFlag(self, VoronoiDataBase.HARD_DISTANCE_FLAG        ), false);
				addCase(cases, "euclidean_distance",         this.testFlag(self, VoronoiDataBase.EUCLIDEAN_DISTANCE_FLAG   ), false);
			}
			case GET_COLUMN_VALUE -> {
				addCase(cases, "id",                         VoronoiDataBase.INFO.id                               (self), true);
				addCase(cases, "cell_x",                     VoronoiDataBase.INFO.get_cell_x                       (self), true);
				addCase(cases, "cell_z",                     VoronoiDataBase.INFO.get_cell_z                       (self), true);
				addCase(cases, "center_x",                   VoronoiDataBase.INFO.get_center_x                     (self), true);
				addCase(cases, "center_z",                   VoronoiDataBase.INFO.get_center_z                     (self), true);
				addCase(cases, "soft_distance_squared",      VoronoiDataBase.INFO.get_soft_distance_squared        (self), true);
				addCase(cases, "soft_distance",              VoronoiDataBase.INFO.get_soft_distance                (self), true);
				addCase(cases, "hard_distance_squared",      VoronoiDataBase.INFO.get_hard_distance_squared        (self), true);
				addCase(cases, "hard_distance",              VoronoiDataBase.INFO.get_hard_distance                (self), true);
				addCase(cases, "euclidean_distance_squared", VoronoiDataBase.INFO.get_euclidean_distance_squared   (self), true);
				addCase(cases, "euclidean_distance",         VoronoiDataBase.INFO.get_euclidean_distance           (self), true);
			}
			case SET_COLUMN_VALUE -> {
				//no-op.
			}
			case PRE_COMPUTE_COLUMN_VALUE -> {
				addCase(cases, "soft_distance_squared",      VoronoiDataBase.INFO.pre_compute_soft_distance_squared(self), false);
				addCase(cases, "soft_distance",              VoronoiDataBase.INFO.pre_compute_soft_distance        (self), false);
				addCase(cases, "hard_distance",              VoronoiDataBase.INFO.pre_compute_hard_distance        (self), false);
				addCase(cases, "euclidean_distance",         VoronoiDataBase.INFO.pre_compute_euclidean_distance   (self), false);
			}
		}
	}

	public InsnTree testFlag(InsnTree self, int index) {
		return new ConditionToBooleanInsnTree(
			new IntCompareZeroConditionTree(
				new BitwiseAndInsnTree(
					getField(
						self,
						new FieldInfo(
							ACC_PUBLIC,
							type(VoronoiDataBase.class),
							"flags_0",
							TypeInfos.INT
						)
					),
					ldc(1 << index),
					IAND
				),
				IFNE
			)
		);
	}

	public static void addCase(Int2ObjectSortedMap<InsnTree> cases, String name, InsnTree getter, boolean convertToObject) {
		if (convertToObject) getter = toObject(getter, name);
		getter = guard(getter, name);
		cases.merge(name.hashCode(), getter, InsnTrees::seq);
	}

	@Override
	public Stream<String> preprocessValidColumnValues(Stream<String> valid) {
		return Stream.concat(
			Stream.of(
				"id",
				"cell_x",
				"cell_z",
				"center_x",
				"center_z",
				"soft_distance_squared",
				"soft_distance",
				"hard_distance_squared",
				"hard_distance",
				"euclidean_distance_squared",
				"euclidean_distance"
			),
			valid
		);
	}
}