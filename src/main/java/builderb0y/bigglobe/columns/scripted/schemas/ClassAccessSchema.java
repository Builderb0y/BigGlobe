package builderb0y.bigglobe.columns.scripted.schemas;

import java.util.LinkedHashMap;
import java.util.Map;

import builderb0y.autocodec.annotations.UseImplementation;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.CustomClassCompileContext;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class ClassAccessSchema implements AccessSchema {

	public final @IdentifierName String class_name;
	public final @UseImplementation(LinkedHashMap.class) Map<@IdentifierName String, AccessSchema> exports;
	public final boolean is_3d;

	public ClassAccessSchema(String class_name, Map<String, AccessSchema> exports, boolean is_3d) {
		this.class_name = class_name;
		this.exports = exports;
		this.is_3d = is_3d;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		CustomClassCompileContext newContext = new CustomClassCompileContext(context, this);
		//todo: need object array field type when 3D and cached.
		return new TypeContext(newContext.mainClass.info, newContext.mainClass.info, newContext);
	}

	@Override
	public boolean requiresYLevel() {
		return this.is_3d;
	}

	@Override
	public int hashCode() {
		int hash = this.class_name.hashCode();
		hash = hash * 31 + this.exports.hashCode();
		hash = hash * 31 + Boolean.hashCode(this.is_3d);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ClassAccessSchema that &&
			this.class_name.equals(that.class_name) &&
			this.exports.equals(that.exports) &&
			this.is_3d == that.is_3d
		);
	}
}