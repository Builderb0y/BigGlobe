package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.Column;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.GetFieldInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnYScriptEnvironment implements ScriptEnvironment {

	public static final TypeInfo
		BASE_COLUMN_TYPE  = type(Column.class),
		WORLD_COLUMN_TYPE = type(WorldColumn.class);
	public static final MethodInfo
		GET_COLUMN_VALUE = method(
			ACC_PUBLIC | ACC_STATIC,
			ColumnYScriptEnvironment.class,
			"getColumnValue",
			ColumnValue.class,
			MethodHandles.Lookup.class,
			String.class,
			Class.class,
			String.class
		),
		COLUMN_GET_VALUE = method(
			ACC_PUBLIC,
			type(ColumnValue.class),
			"getValue",
			TypeInfos.DOUBLE,
			WORLD_COLUMN_TYPE,
			TypeInfos.DOUBLE
		);
	public static final FieldInfo
		COLUMN_X_INFO = field(ACC_PUBLIC, BASE_COLUMN_TYPE, "x", TypeInfos.INT),
		COLUMN_Z_INFO = field(ACC_PUBLIC, BASE_COLUMN_TYPE, "z", TypeInfos.INT);

	public InsnTree loadColumn;
	public @Nullable InsnTree loadY;
	public boolean exposePosition;
	public Set<ColumnValue<?>> usedValues = new HashSet<>(4);

	public ColumnYScriptEnvironment(VarInfo columnInfo, VarInfo yInfo) {
		this.loadColumn = new LoadInsnTree(columnInfo);
		this.loadY = new LoadInsnTree(yInfo);
		this.exposePosition = true;
	}

	public ColumnYScriptEnvironment(InsnTree loadColumn, @Nullable InsnTree loadY, boolean exposePosition) {
		this.loadColumn = loadColumn;
		this.loadY = loadY;
		this.exposePosition = exposePosition;
	}

	public InsnTree loadX() {
		return new GetFieldInsnTree(this.loadColumn, COLUMN_X_INFO);
	}

	public InsnTree loadZ() {
		return new GetFieldInsnTree(this.loadColumn, COLUMN_Z_INFO);
	}

	public @Nullable ColumnValue<?> getValue(String name) {
		ColumnValue<?> value = ColumnValue.get(name);
		if (value != null) {
			if (value.dependsOnY() && this.loadY == null) {
				value = null;
			}
			else {
				this.usedValues.add(value);
			}
		}
		return value;
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		if (this.exposePosition) {
			return switch (name) {
				case "x" -> {
					yield this.loadX();
				}
				case "y" -> {
					if (this.loadY != null) this.usedValues.add(ColumnValue.Y);
					yield this.loadY;
				}
				case "z" -> {
					yield this.loadZ();
				}
				default -> {
					yield this.getValue(name) != null ? this.getColumnValue(name, this.loadY) : null;
				}
			};
		}
		else {
			ColumnValue<?> value = this.getValue(name);
			return value != null && value != ColumnValue.Y ? this.getColumnValue(name, this.loadY) : null;
		}
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		if (this.getValue(name) != null) {
			InsnTree castArgument = ScriptEnvironment.castArgument(parser, name, TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW, arguments);
			return this.getColumnValue(name, castArgument);
		}
		return null;
	}

	public InsnTree getColumnValue(String id, InsnTree y) {
		return invokeVirtual(
			ldc(
				GET_COLUMN_VALUE,
				constant(id)
			),
			COLUMN_GET_VALUE,
			this.loadColumn,
			y
		);
	}

	@SuppressWarnings("unused")
	public static ColumnValue<?> getColumnValue(MethodHandles.Lookup lookup, String name, Class<?> type, String id) {
		ColumnValue<?> value = ColumnValue.get(id);
		if (value == null) throw new IllegalStateException("ColumnValue not present in registry: " + id);
		return value;
	}
}