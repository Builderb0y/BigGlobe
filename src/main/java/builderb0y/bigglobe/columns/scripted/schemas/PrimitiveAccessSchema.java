package builderb0y.bigglobe.columns.scripted.schemas;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class PrimitiveAccessSchema implements AccessSchema {

	public final boolean is_3d;

	public PrimitiveAccessSchema(boolean is_3d) {
		this.is_3d = is_3d;
	}

	public abstract PrimitiveType primitiveType();

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		return new TypeContext(
			this.primitiveType().typeInfo,
			this.is_3d ? type(NumberArray.class) : this.primitiveType().typeInfo,
			null
		);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		return switch (this.primitiveType()) {
			case BOOLEAN -> ldc(((Boolean)(object)).booleanValue());
			case BYTE    -> ldc(((Number )(object)).   byteValue());
			case SHORT   -> ldc(((Number )(object)).  shortValue());
			case INT     -> ldc(((Number )(object)).    intValue());
			case LONG    -> ldc(((Number )(object)).   longValue());
			case FLOAT   -> ldc(((Number )(object)).  floatValue());
			case DOUBLE  -> ldc(((Number )(object)). doubleValue());
		};
	}

	@Override
	public boolean is3D() {
		return this.is_3d;
	}

	@Override
	public int hashCode() {
		return this.primitiveType().hashCode() + Boolean.hashCode(this.is_3d);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof PrimitiveAccessSchema that &&
			this.primitiveType() == that.primitiveType() &&
			this.is_3d == that.is_3d
		);
	}

	public static enum PrimitiveType {
		BOOLEAN(TypeInfos.BOOLEAN),
		BYTE   (TypeInfos.BYTE   ),
		SHORT  (TypeInfos.SHORT  ),
		INT    (TypeInfos.INT    ),
		LONG   (TypeInfos.LONG   ),
		FLOAT  (TypeInfos.FLOAT  ),
		DOUBLE (TypeInfos.DOUBLE );

		public final TypeInfo typeInfo;

		PrimitiveType(TypeInfo info) {
			this.typeInfo = info;
		}
	}
}