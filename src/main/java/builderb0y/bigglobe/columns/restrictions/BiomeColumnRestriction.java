package builderb0y.bigglobe.columns.restrictions;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;

@UseCoder(name = "INSTANCE", in = BiomeColumnRestriction.Coder.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public class BiomeColumnRestriction extends Object2DoubleOpenHashMap<RegistryEntry<Biome>> implements ColumnRestriction {

	@Override
	public double getRestriction(WorldColumn column, double y) {
		return this.getDouble(column.getBiome(BigGlobeMath.floorI(y)));
	}

	/*
	@Override
	public boolean dependsOnY(WorldColumn column) {
		return column.biomeDependsOnY();
	}
	*/

	@Override
	public void forEachValue(Consumer<? super ColumnValue<?>> action) {}

	@Override
	public Stream<ColumnValue<?>> getValues() {
		return Stream.empty();
	}

	public static class Coder extends NamedCoder<BiomeColumnRestriction> {

		public static final AutoCoder<RegistryEntry<Biome>> BIOME_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(
			new ReifiedType<>() {}
		);
		public static final AutoCoder<Double> CHANCE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(
			new ReifiedType<@VerifyFloatRange(min = 0.0D, max = 1.0D) Double>() {}
		);
		public static final AutoEncoder<Map<RegistryEntry<Biome>, Double>> MAP_ENCODER = BigGlobeAutoCodec.AUTO_CODEC.createEncoder(
			new ReifiedType<>() {}
		);

		public static final Coder INSTANCE = new Coder(ReifiedType.from(BiomeColumnRestriction.class));

		public Coder(ReifiedType<BiomeColumnRestriction> handledType) {
			super(handledType);
		}

		@Override
		public <T_Encoded> @Nullable BiomeColumnRestriction decode(DecodeContext<T_Encoded> context) throws DecodeException {
			Map<DecodeContext<T_Encoded>, DecodeContext<T_Encoded>> map = context.forceAsContextMap();
			BiomeColumnRestriction result = new BiomeColumnRestriction();
			for (Map.Entry<DecodeContext<T_Encoded>, DecodeContext<T_Encoded>> entry : map.entrySet()) {
				if (entry.getKey().forceAsString().equals("default")) {
					result.defaultReturnValue(entry.getValue().decodeWith(CHANCE_CODER));
				}
				else {
					RegistryEntry<Biome> biome = entry.getKey().decodeWith(BIOME_CODER);
					double chance = entry.getValue().decodeWith(CHANCE_CODER);
					result.put(biome, chance);
				}
			}
			return result;
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(EncodeContext<T_Encoded, BiomeColumnRestriction> context) throws EncodeException {
			if (context.input == null) return context.emptyMap();
			@SuppressWarnings({ "unchecked", "rawtypes" })
			T_Encoded result = (T_Encoded)(context.encodeWith((AutoEncoder)(MAP_ENCODER)));
			if (context.input.defaultReturnValue() != 0.0D) {
				result = context.addToStringMap(result, "default", context.createDouble(context.input.defaultReturnValue()));
			}
			return result;
		}
	}
}