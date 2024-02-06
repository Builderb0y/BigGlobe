package builderb0y.bigglobe.columns.scripted;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public record AccessSchema(ColumnValueType type, boolean is_3d) {

	public AccessContext createType(ColumnCompileContext context) {
		TypeContext typeContext = context.getTypeContext(this.type);
		if (this.is_3d) {
			if (typeContext.type().isPrimitive()) {
				return new AccessContext(typeContext.type(), InsnTrees.type(MappedRangeNumberArray.class), typeContext.context());
			}
			else {
				return new AccessContext(typeContext.type(), InsnTrees.type(MappedRangeObjectArray.class), typeContext.context());
			}
		}
		else {
			return new AccessContext(typeContext.type(), typeContext.type(), typeContext.context());
		}
	}

	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		return this.type.createConstant(object, context);
	}

	public LazyVarInfo[] getterParameters() {
		return (
			this.is_3d
			? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) }
			: LazyVarInfo.ARRAY_FACTORY.empty()
		);
	}

	public LazyVarInfo[] setterParameters(DataCompileContext context) {
		return (
			this.is_3d
			? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT), new LazyVarInfo("value", context.root().getAccessContext(this).exposedType()) }
			: new LazyVarInfo[] { new LazyVarInfo("value", context.root().getAccessContext(this).exposedType()) }
		);
	}

	public MethodInfo getterDescriptor(int flags, String name, DataCompileContext context) {
		return new MethodInfo(
			flags,
			context.selfType(),
			name,
			context.root().getAccessContext(this).exposedType(),
			this.is_3d
			? new TypeInfo[] { TypeInfos.INT }
			: TypeInfo.ARRAY_FACTORY.empty()
		);
	}

	public MethodInfo setterDescriptor(int flags, String name, DataCompileContext context) {
		return new MethodInfo(
			flags,
			context.selfType(),
			name,
			TypeInfos.VOID,
			this.is_3d
			? new TypeInfo[] { TypeInfos.INT, context.root().getAccessContext(this).exposedType() }
			: new TypeInfo[] { context.root().getAccessContext(this).exposedType() }
		);
	}

	public static record AccessContext(
		/** the type returned by the getter method. */
		@NotNull TypeInfo exposedType,
		/** the type of the backing field. */
		@NotNull TypeInfo fieldType,
		/**
		if this schema requires a new class to represent,
		then this component represents a DataCompileContext
		which is responsible for compiling that class.
		otherwise, this component holds null.
		*/
		@Nullable DataCompileContext context
	) {

		public TypeInfo commonType() {
			if (this.exposedType.equals(this.fieldType)) {
				return this.exposedType;
			}
			else {
				throw new IllegalStateException("Type mismatch between field type " + this.fieldType + " and exposed type " + this.exposedType);
			}
		}
	}
}