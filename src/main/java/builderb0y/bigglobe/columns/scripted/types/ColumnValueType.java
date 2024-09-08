package builderb0y.bigglobe.columns.scripted.types;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;

@UseCoder(name = "REGISTRY", in = ColumnValueType.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ColumnValueType extends CoderRegistryTyped<ColumnValueType> {

	/**
	allows "double" to be decoded as { "type": "double" }, and vise versa.
	*/
	public static final CoderRegistry<ColumnValueType> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_value_type")) {

		@Override
		public <T_Encoded> @Nullable ColumnValueType decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isMap()) {
				return super.decode(context);
			}
			else {
				T_Encoded map = context.createStringMap(Collections.singletonMap(this.keyName, context.input));
				return super.decode(context.input(map));
			}
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ColumnValueType> context) throws EncodeException {
			T_Encoded encoded = super.encode(context);
			Stream<Pair<T_Encoded, T_Encoded>> stream = context.ops.getMapValues(encoded).result().orElse(null);
			if (stream != null) {
				Map<T_Encoded, T_Encoded> map = stream.collect(Pair.toMap());
				if (map.size() == 1) {
					Map.Entry<T_Encoded, T_Encoded> entry = map.entrySet().iterator().next();
					String stringKey = context.ops.getStringValue(entry.getKey()).result().orElse(null);
					if (this.keyName.equals(stringKey)) {
						return entry.getValue();
					}
				}
			}
			return encoded;
		}
	};
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("byte"              ),              ByteColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("short"             ),             ShortColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("int"               ),               IntColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("long"              ),              LongColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("float"             ),             FloatColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("double"            ),            DoubleColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("boolean"           ),           BooleanColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("voronoi"           ),           VoronoiColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("block"             ),             BlockColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("block_state"       ),        BlockStateColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("biome"             ),             BiomeColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("configured_feature"), ConfiguredFeatureColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("wood_palette"      ),       WoodPaletteColumnValueType.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("class"             ),             ClassColumnValueType.class);
	}};

	public abstract TypeContext createType(ColumnCompileContext context);

	public abstract InsnTree createConstant(Object object, ColumnCompileContext context);

	public default void setupInternalEnvironment(
		MutableScriptEnvironment environment,
		TypeContext typeContext,
		DataCompileContext context,
		MutableDependencyView dependencies
	) {}

	public default void setupExternalEnvironment(
		MutableScriptEnvironment environment,
		TypeContext typeContext,
		ColumnCompileContext context,
		ExternalEnvironmentParams params
	) {}

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	public static record TypeContext(
		/** the type returned by the getter method. */
		@NotNull TypeInfo type,
		/**
		if this schema requires a new class to represent,
		then this component represents a DataCompileContext
		which is responsible for compiling that class.
		otherwise, this component holds null.
		*/
		@Nullable DataCompileContext context
	) {}
}