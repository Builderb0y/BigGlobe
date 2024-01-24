package builderb0y.bigglobe.columns.scripted.schemas;

import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.MappedRangeObjectArray;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class AbstractAccessSchema implements AccessSchema {

	public final boolean is_3d;
	public final transient TypeInfo exposedType;

	public AbstractAccessSchema(boolean is_3d, TypeInfo exposedType) {
		this.is_3d = is_3d;
		this.exposedType = exposedType;
	}

	@Override
	public boolean is3D() {
		return this.is_3d;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		return new TypeContext(
			this.exposedType,
			this.is_3d
			? type(this.exposedType.isPrimitive() ? MappedRangeNumberArray.class : MappedRangeObjectArray.class)
			: this.exposedType,
			null
		);
	}
}