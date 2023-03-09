package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.Column;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnPositionScriptEnvironment implements ScriptEnvironment {

	public InsnTree loadColumn;

	public ColumnPositionScriptEnvironment(InsnTree loadColumn) {
		this.loadColumn = loadColumn;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		ColumnValue<?> columnValue = ColumnValue.get(name);
		if (columnValue != null) {
			InsnTree loadColumnValue = ldc(ColumnYScriptEnvironment.GET_COLUMN_VALUE, constant(name));
			if (columnValue.dependsOnY()) {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, types("IDI"), CastMode.IMPLICIT_THROW, arguments);
				return invokeVirtual(
					loadColumnValue,
					ColumnYScriptEnvironment.COLUMN_GET_VALUE,
					new ColumnSetPosInsnTree(this.loadColumn, castArguments[0], castArguments[2]),
					castArguments[1]
				);
			}
			else {
				InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, types("II"), CastMode.IMPLICIT_THROW, arguments);
				return invokeVirtual(
					loadColumnValue,
					ColumnYScriptEnvironment.COLUMN_GET_VALUE_WITHOUT_Y,
					new ColumnSetPosInsnTree(this.loadColumn, castArguments[0], castArguments[1])
				);
			}
		}
		return null;
	}

	public static class ColumnSetPosInsnTree implements InsnTree {

		public InsnTree loadColumn, x, z;

		public ColumnSetPosInsnTree(InsnTree loadColumn, InsnTree x, InsnTree z) {
			this.loadColumn = loadColumn;
			this.x = x;
			this.z = z;
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.loadColumn.emitBytecode(method);
			method.node.visitInsn(DUP);
			this.x.emitBytecode(method);
			this.z.emitBytecode(method);
			method.node.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Column.class), "setPos", "(II)V", false);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return this.loadColumn.getTypeInfo();
		}
	}
}