package builderb0y.bigglobe.columns.scripted.entries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnGetterInsnTree2D;
import builderb0y.bigglobe.columns.scripted.ColumnGetterInsnTree3D;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class ColumnEntryAccessor {

	public final int slot;
	public boolean mustBeManuallyEnabledByVoronoi;

	public ColumnEntryAccessor(int slot) {
		this.slot = slot;
	}

	public boolean hasEntry() {
		return this.slot >= 0;
	}

	public boolean isMutable() {
		return this.hasEntry();
	}

	public abstract void setupAndCompile(ColumnEntryRegistry registry) throws ScriptParsingException;

	public abstract AccessType getAccessType();

	public InsnTree createGetter(
		ExpressionParser parser,
		@NotNull InsnTree accessor,
		@NotNull InsnTree column,
		@Nullable InsnTree y,
		boolean mutable
	)
		throws ScriptParsingException {
		AccessType type = this.getAccessType();
		if (type.is3D) {
			if (y == null) {
				throw new ScriptParsingException("This column value requires a Y level", parser.input);
			}
			y = y.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
			MethodInfo getter = MethodInfo.findMethod(this.getClass(), "get", type.returnType.typeInfo.toClass(), ScriptedColumn.class, int.class);
			if (mutable && this.isMutable()) {
				MethodInfo setter = MethodInfo.findMethod(this.getClass(), "set", void.class, ScriptedColumn.class, int.class, type.returnType.typeInfo.toClass());
				return new ColumnGetterInsnTree3D(accessor, column, y, getter, setter);
			}
			else {
				return invokeInstance(accessor, getter, column, y);
			}
		}
		else {
			if (y != null) {
				throw new ScriptParsingException("This column value does not allow a Y level", parser.input);
			}
			MethodInfo getter = MethodInfo.findMethod(this.getClass(), "get", type.returnType.typeInfo.toClass(), ScriptedColumn.class);
			if (mutable && this.isMutable()) {
				MethodInfo setter = MethodInfo.findMethod(this.getClass(), "set", void.class, ScriptedColumn.class, type.returnType.typeInfo.toClass());
				return new ColumnGetterInsnTree2D(accessor, column, getter, setter);
			}
			else {
				return invokeInstance(accessor, getter, column);
			}
		}
	}

	public final void populate(ScriptedColumn column, boolean enabledByVoronoi) {
		if (this.slot >= 0 && (enabledByVoronoi || !this.mustBeManuallyEnabledByVoronoi)) {
			this.doPopulate(column);
		}
	}

	public abstract void doPopulate(ScriptedColumn column);

	public abstract String valueToString(ScriptedColumn column, int y);

	public static abstract class Int2DColumnEntryAccessor extends ColumnEntryAccessor {

		public Int2DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract int get(ScriptedColumn column);

		@Override
		public AccessType getAccessType() {
			return AccessType.INT_2D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column));
		}
	}

	public static abstract class Int3DColumnEntryAccessor extends ColumnEntryAccessor {

		public Int3DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract int get(ScriptedColumn column, int y);

		@Override
		public AccessType getAccessType() {
			return AccessType.INT_3D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Integer.toString(this.get(column, y));
		}
	}

	public static abstract class Long2DColumnEntryAccessor extends ColumnEntryAccessor {

		public Long2DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract long get(ScriptedColumn column);

		@Override
		public AccessType getAccessType() {
			return AccessType.LONG_2D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Long.toString(this.get(column));
		}
	}

	public static abstract class Long3DColumnEntryAccessor extends ColumnEntryAccessor {

		public Long3DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract long get(ScriptedColumn column, int y);

		@Override
		public AccessType getAccessType() {
			return AccessType.LONG_3D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Long.toString(this.get(column, y));
		}
	}

	public static abstract class Float2DColumnEntryAccessor extends ColumnEntryAccessor {

		public Float2DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract float get(ScriptedColumn column);

		@Override
		public AccessType getAccessType() {
			return AccessType.FLOAT_2D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Float.toString(this.get(column));
		}
	}

	public static abstract class Float3DColumnEntryAccessor extends ColumnEntryAccessor {

		public Float3DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract float get(ScriptedColumn column, int y);

		@Override
		public AccessType getAccessType() {
			return AccessType.FLOAT_3D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Float.toString(this.get(column, y));
		}
	}

	public static abstract class Double2DColumnEntryAccessor extends ColumnEntryAccessor {

		public Double2DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract double get(ScriptedColumn column);

		@Override
		public AccessType getAccessType() {
			return AccessType.DOUBLE_2D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Double.toString(this.get(column));
		}
	}

	public static abstract class Double3DColumnEntryAccessor extends ColumnEntryAccessor {

		public Double3DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract double get(ScriptedColumn column, int y);

		@Override
		public AccessType getAccessType() {
			return AccessType.DOUBLE_3D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Double.toString(this.get(column, y));
		}
	}

	public static abstract class Boolean2DColumnEntryAccessor extends ColumnEntryAccessor {

		public Boolean2DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract boolean get(ScriptedColumn column);

		@Override
		public AccessType getAccessType() {
			return AccessType.BOOLEAN_2D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
			return Boolean.toString(this.get(column));
		}
	}

	public static abstract class Boolean3DColumnEntryAccessor extends ColumnEntryAccessor {

		public Boolean3DColumnEntryAccessor(int slot) {
			super(slot);
		}

		public abstract boolean get(ScriptedColumn column, int y);

		@Override
		public AccessType getAccessType() {
			return AccessType.BOOLEAN_3D;
		}

		@Override
		public String valueToString(ScriptedColumn column, int y) {
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