package builderb0y.bigglobe.columns;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.*;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class ScriptValueAccessor {

	public int slot;

	public abstract InsnTree createGetter(ExpressionParser parser, @NotNull InsnTree accessor, @NotNull InsnTree column, @Nullable InsnTree y) throws ScriptParsingException;

	public abstract void populate(ScriptedColumn column);

	public static class CachedNoise3DValueAccessor extends ScriptValueAccessor {

		public static final MethodInfo GET = MethodInfo.getMethod(CachedNoise3DValueAccessor.class, "get");

		@Override
		public InsnTree createGetter(ExpressionParser parser, @NotNull InsnTree accessor, @NotNull InsnTree column, @Nullable InsnTree y) throws ScriptParsingException {
			if (y == null) throw new ScriptParsingException("Must provide y level for this column value", parser.input);
			TypeInfos.checkNumber(y.getTypeInfo());
			return invokeInstance(accessor, GET, column, y.cast(parser, TypeInfos.INT, CastMode.EXPLICIT_THROW));
		}

		public double get(ScriptedColumn column, int y) {
			return ((Noise3DEntry)(column.values[this.slot])).getValueCached(column, y);
		}

		@Override
		public void populate(ScriptedColumn column) {
			((Noise3DEntry)(column.values[this.slot])).populate(column);
		}
	}

	public static class UncachedNoise3DValueAccessor extends ScriptValueAccessor {

		public static final MethodInfo GET = MethodInfo.getMethod(UncachedNoise3DValueAccessor.class, "get");

		@Override
		public InsnTree createGetter(ExpressionParser parser, @NotNull InsnTree accessor, @NotNull InsnTree column, @Nullable InsnTree y) throws ScriptParsingException {
			if (y == null) throw new ScriptParsingException("Must provide y level for this column value", parser.input);
			TypeInfos.checkNumber(y.getTypeInfo());
			return invokeInstance(accessor, GET, column, y.cast(parser, TypeInfos.INT, CastMode.EXPLICIT_THROW));
		}

		public double get(ScriptedColumn column, int y) {
			return ((Noise3DEntry)(column.values[this.slot])).getValueUncached(column, y);
		}

		@Override
		public void populate(ScriptedColumn column) {
			//no-op for uncached noise.
		}
	}
}