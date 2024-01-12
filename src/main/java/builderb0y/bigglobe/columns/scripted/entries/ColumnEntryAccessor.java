package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

public interface ColumnEntryAccessor {

	public abstract AccessType getAccessType();

	public abstract String valueToString(ScriptedColumn column, int y);

	public static interface Int2DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract int get(ScriptedColumn column);

		@Override
		public default AccessType getAccessType() {
			return AccessType.INT_2D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column));
		}
	}

	public static interface Int3DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract int get(ScriptedColumn column, int y);

		@Override
		public default AccessType getAccessType() {
			return AccessType.INT_3D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column, y));
		}
	}

	public static interface Long2DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract long get(ScriptedColumn column);

		@Override
		public default AccessType getAccessType() {
			return AccessType.LONG_2D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Long.toString(this.get(column));
		}
	}

	public static interface Long3DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract long get(ScriptedColumn column, int y);

		@Override
		public default AccessType getAccessType() {
			return AccessType.LONG_3D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Long.toString(this.get(column, y));
		}
	}

	public static interface Float2DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract float get(ScriptedColumn column);

		@Override
		public default AccessType getAccessType() {
			return AccessType.FLOAT_2D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Float.toString(this.get(column));
		}
	}

	public static interface Float3DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract float get(ScriptedColumn column, int y);

		@Override
		public default AccessType getAccessType() {
			return AccessType.FLOAT_3D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Float.toString(this.get(column, y));
		}
	}

	public static interface Double2DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract double get(ScriptedColumn column);

		@Override
		public default AccessType getAccessType() {
			return AccessType.DOUBLE_2D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Double.toString(this.get(column));
		}
	}

	public static interface Double3DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract double get(ScriptedColumn column, int y);

		@Override
		public default AccessType getAccessType() {
			return AccessType.DOUBLE_3D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Double.toString(this.get(column, y));
		}
	}

	public static interface Boolean2DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract boolean get(ScriptedColumn column);

		@Override
		public default AccessType getAccessType() {
			return AccessType.BOOLEAN_2D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Boolean.toString(this.get(column));
		}
	}

	public static interface Boolean3DColumnEntryAccessor extends ColumnEntryAccessor {

		public abstract boolean get(ScriptedColumn column, int y);

		@Override
		public default AccessType getAccessType() {
			return AccessType.BOOLEAN_3D;
		}

		@Override
		public default String valueToString(ScriptedColumn column, int y) {
			return Boolean.toString(this.get(column, y));
		}
	}

	public static enum AccessType {
		INT_2D(ReturnType.INT, false),
		INT_3D(ReturnType.INT, true),
		LONG_2D(ReturnType.LONG, false),
		LONG_3D(ReturnType.LONG, true),
		FLOAT_2D(ReturnType.FLOAT, false),
		FLOAT_3D(ReturnType.FLOAT, true),
		DOUBLE_2D(ReturnType.DOUBLE, false),
		DOUBLE_3D(ReturnType.DOUBLE, true),
		BOOLEAN_2D(ReturnType.BOOLEAN, false),
		BOOLEAN_3D(ReturnType.BOOLEAN, true);

		public final ReturnType returnType;
		public final boolean is3D;

		AccessType(ReturnType returnType, boolean is3D) {
			this.returnType = returnType;
			this.is3D = is3D;
		}
	}

	public static enum ReturnType {
		INT(TypeInfos.INT),
		LONG(TypeInfos.LONG),
		FLOAT(TypeInfos.FLOAT),
		DOUBLE(TypeInfos.DOUBLE),
		BOOLEAN(TypeInfos.BOOLEAN);

		public final TypeInfo typeInfo;

		ReturnType(TypeInfo typeInfo) {
			this.typeInfo = typeInfo;
		}
	}
}