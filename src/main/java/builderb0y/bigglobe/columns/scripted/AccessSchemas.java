package builderb0y.bigglobe.columns.scripted;

import java.util.Map;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.TypeContext;
import builderb0y.bigglobe.columns.scripted.entries.Voronoi2DColumnEntry;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

public class AccessSchemas {

	public static abstract class AbstractAccessSchema implements AccessSchema {

		@Override
		public int hashCode() {
			return this.getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && this.getClass() == obj.getClass();
		}
	}

	public static abstract class _2DAccessSchema extends AbstractAccessSchema {

		@Override
		public boolean requiresYLevel() {
			return false;
		}
	}

	@RecordLike({})
	public static class Int2DAccessSchema extends _2DAccessSchema {

		public static final Int2DAccessSchema INSTANCE = new Int2DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.INT, null);
		}
	}

	@RecordLike({})
	public static class Long2DAccessSchema extends _2DAccessSchema {

		public static final Long2DAccessSchema INSTANCE = new Long2DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.LONG, null);
		}
	}

	@RecordLike({})
	public static class Float2DAccessSchema extends _2DAccessSchema {

		public static final Float2DAccessSchema INSTANCE = new Float2DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.FLOAT, null);
		}
	}

	@RecordLike({})
	public static class Double2DAccessSchema extends _2DAccessSchema {

		public static final Double2DAccessSchema INSTANCE = new Double2DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.DOUBLE, null);
		}
	}

	@RecordLike({})
	public static class Boolean2DAccessSchema extends _2DAccessSchema {

		public static final Boolean2DAccessSchema INSTANCE = new Boolean2DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.BOOLEAN, null);
		}
	}

	public static abstract class _3DAccessSchema extends AbstractAccessSchema {

		@Override
		public boolean requiresYLevel() {
			return false;
		}
	}

	@RecordLike({})
	public static class Int3DAccessSchema extends _3DAccessSchema {

		public static final Int3DAccessSchema INSTANCE = new Int3DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.INT, null);
		}
	}

	@RecordLike({})
	public static class Long3DAccessSchema extends _3DAccessSchema {

		public static final Long3DAccessSchema INSTANCE = new Long3DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.LONG, null);
		}
	}

	@RecordLike({})
	public static class Float3DAccessSchema extends _3DAccessSchema {

		public static final Float3DAccessSchema INSTANCE = new Float3DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.FLOAT, null);
		}
	}

	@RecordLike({})
	public static class Double3DAccessSchema extends _3DAccessSchema {

		public static final Double3DAccessSchema INSTANCE = new Double3DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.DOUBLE, null);
		}
	}

	@RecordLike({})
	public static class Boolean3DAccessSchema extends _3DAccessSchema {

		public static final Boolean3DAccessSchema INSTANCE = new Boolean3DAccessSchema();

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			return new TypeContext(TypeInfos.BOOLEAN, null);
		}
	}

	public static class Voronoi2DAccessSchema implements AccessSchema {

		public final @DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = Voronoi2DColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;

		public Voronoi2DAccessSchema(Map<String, AccessSchema> exports) {
			this.exports = exports;
		}

		@Override
		public boolean requiresYLevel() {
			return false;
		}

		@Override
		public TypeContext createType(ColumnCompileContext context) {
			VoronoiBaseCompileContext voronoiContext = new VoronoiBaseCompileContext(context);
			for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
				voronoiContext.mainClass.newMethod(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context.getSchemaType(entry.getValue()).type());
			}
			return new TypeContext(voronoiContext.mainClass.info, voronoiContext);
		}

		@Override
		public int hashCode() {
			return this.exports.hashCode() ^ Voronoi2DAccessSchema.class.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Voronoi2DAccessSchema that && this.exports.equals(that.exports);
		}
	}
}