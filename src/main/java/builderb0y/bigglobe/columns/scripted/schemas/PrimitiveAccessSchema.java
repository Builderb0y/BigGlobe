package builderb0y.bigglobe.columns.scripted.schemas;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class PrimitiveAccessSchema implements AccessSchema {

	public final PrimitiveType return_type;
	public final boolean is_3d;

	public PrimitiveAccessSchema(PrimitiveType return_type, boolean is_3d) {
		this.return_type = return_type;
		this.is_3d = is_3d;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		return new TypeContext(
			this.return_type.typeInfo,
			this.is_3d ? type(NumberArray.class) : this.return_type.typeInfo,
			null
		);
	}

	@Override
	public boolean requiresYLevel() {
		return this.is_3d;
	}

	@Override
	public int hashCode() {
		return this.return_type.hashCode() + Boolean.hashCode(this.is_3d);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof PrimitiveAccessSchema that &&
			this.return_type == that.return_type &&
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