package builderb0y.bigglobe.columns.scripted.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.*;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

@UseCoder(name = "REGISTRY", in = AccessSchema.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface AccessSchema extends CoderRegistryTyped<AccessSchema> {

	public static final CoderRegistry<AccessSchema> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_entry_access_schema"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("primitive"), PrimitiveAccessSchema.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("voronoi"), Voronoi2DAccessSchema.class);
	}};

	public abstract TypeContext createType(ColumnCompileContext context);

	public abstract boolean requiresYLevel();

	public default LazyVarInfo[] getterParameters() {
		return (
			this.requiresYLevel()
			? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) }
			: LazyVarInfo.ARRAY_FACTORY.empty()
		);
	}

	public default LazyVarInfo[] setterParameters(DataCompileContext context) {
		return (
			this.requiresYLevel()
			? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT), new LazyVarInfo("value", context.root().getSchemaType(this).exposedType()) }
			: new LazyVarInfo[] { new LazyVarInfo("value", context.root().getSchemaType(this).exposedType()) }
		);
	}

	public default MethodInfo getterDescriptor(int flags, String name, DataCompileContext context) {
		return new MethodInfo(
			flags,
			context.selfType(),
			name,
			context.root().getSchemaType(this).exposedType(),
			this.requiresYLevel()
			? new TypeInfo[] { TypeInfos.INT }
			: TypeInfo.ARRAY_FACTORY.empty()
		);
	}

	public default MethodInfo setterDescriptor(int flags, String name, DataCompileContext context) {
		return new MethodInfo(
			flags,
			context.selfType(),
			name,
			TypeInfos.VOID,
			this.requiresYLevel()
			? new TypeInfo[] { TypeInfos.INT, context.root().getSchemaType(this).exposedType() }
			: new TypeInfo[] { context.root().getSchemaType(this).exposedType() }
		);
	}

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	public static record TypeContext(
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