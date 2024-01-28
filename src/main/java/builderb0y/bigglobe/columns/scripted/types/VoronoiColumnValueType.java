package builderb0y.bigglobe.columns.scripted.types;

import java.util.Map;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class VoronoiColumnValueType implements ColumnValueType {

	public final @IdentifierName String name;
	public final @DefaultEmpty Map<@IdentifierName @UseVerifier(name = "checkNotReserved", in = VoronoiColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;

	public VoronoiColumnValueType(String name, Map<String, AccessSchema> exports) {
		this.name = name;
		this.exports = exports;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		VoronoiBaseCompileContext voronoiContext = new VoronoiBaseCompileContext(context, this.name);
		return new TypeContext(voronoiContext.mainClass.info, voronoiContext);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		throw new UnsupportedOperationException("Cannot create constant voronoi cell.");
	}

	@Override
	public void setupExternalEnvironment(TypeContext typeContext, ColumnCompileContext context, MutableScriptEnvironment environment, InsnTree loadColumn) {
		environment.addType(this.name, typeContext.type());
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.exports.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof VoronoiColumnValueType that &&
			this.name.equals(that.name) &&
			this.exports.equals(that.exports)
		);
	}

	@Override
	public String toString() {
		return "{ type: voronoi, name: " + this.name + ", exports: " + this.exports + " }";
	}
}