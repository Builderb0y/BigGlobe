package builderb0y.bigglobe.columns.scripted.schemas;

import java.util.Map;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class VoronoiAccessSchema implements AccessSchema {

	public final @DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = VoronoiColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;

	public VoronoiAccessSchema(Map<String, AccessSchema> exports) {
		this.exports = exports;
	}

	@Override
	public boolean is3D() {
		return false;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		VoronoiBaseCompileContext voronoiContext = new VoronoiBaseCompileContext(context);
		return new TypeContext(voronoiContext.mainClass.info, voronoiContext.mainClass.info, voronoiContext);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		throw new UnsupportedOperationException("Cannot create constant voronoi cell.");
	}

	@Override
	public int hashCode() {
		return this.exports.hashCode() ^ VoronoiAccessSchema.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof VoronoiAccessSchema that && this.exports.equals(that.exports);
	}
}