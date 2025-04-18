package builderb0y.bigglobe.settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import org.jetbrains.annotations.NotNull;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseEncoder;
import builderb0y.autocodec.annotations.UseImprinter;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.*;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.util.DFUVersions;

@UseImprinter(name = "new", in = VariationsList.Imprinter.class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
@UseEncoder  (name = "new", in = VariationsList.Encoder  .class, usage = MemberUsage.METHOD_IS_FACTORY, strict = false)
public class VariationsList<T> {

	public static <T> T unwrap(DataResult<T> result) {
		T actualResult = DFUVersions.getResult(result);
		if (actualResult != null) return actualResult;
		else throw AutoCodecUtil.rethrow(new DecodeException(DFUVersions.getMessageLazy(result)));
	}

	public transient Data source;
	public transient List<T> elements;

	public static Data merge(Data oldObject, Data newObject, boolean deep) {
		MapData oldMap = oldObject.tryAsMap();
		MapData newMap = newObject.tryAsMap();
		if (oldMap != null && newMap != null) {
			MapData result = new MapData(oldMap.size() + newMap.size());
			result.value.putAll(oldMap.value);
			if (deep) {
				for (Map.Entry<Data, Data> entry : newMap.value.entrySet()) {
					result.value.merge(entry.getKey(), entry.getValue(), (first, second) -> merge(first, second, true));
				}
			}
			else {
				result.value.putAll(newMap.value);
			}
			return result;
		}
		return newObject;
	}

	public static Stream<Data> flatten(Stream<Data> oldLayer, Data[] newLayer, boolean deep) {
		return oldLayer.flatMap((Data element1) -> {
			return Arrays.stream(newLayer).map((Data element2) -> {
				return merge(element1, element2, deep);
			});
		});
	}

	public static Stream<Data> expand(Data root) {
		ListData variations = root.getMember("variations").tryAsList();
		if (variations != null) {
			boolean deep = root.getMember("deep").getAsBooleanOr(false);
			Data defaults = root.getMember("defaults");
			if (!defaults.isEmpty()) {
				Data[] layers = (
					variations.value.stream() /* [ {}, {} ] */ .flatMap(
						VariationsList::expand /* {} */
					)
					.toArray(Data.ARRAY_FACTORY)
				);
				return flatten(Stream.of(defaults), layers, deep);
			}
			else {
				Data[] layers = (
					variations.value.stream() /* [ [ {}, {} ], [ {}, {} ] ] */ .map(
						(Data list /* [ {}, {} ] */) -> {
							return ListData.collect(
								forceStream(list)
								.flatMap(VariationsList::expand /* {} */)
							);
						}
					)
					.toArray(Data.ARRAY_FACTORY)
				);
				Stream<Data> stream = forceStream(layers[0]);
				for (int index = 1, length = layers.length; index < length; index++) {
					stream = flatten(stream, forceStream(layers[index]).toArray(Data.ARRAY_FACTORY), deep);
				}
				return stream;
			}
		}
		else {
			ListData list = root.tryAsList();
			if (list != null) {
				return list.value.stream() /* [ {}, {} ] */.flatMap(VariationsList::expand /* {} */);
			}
			return Stream.of(root);
		}
	}

	public static Stream<Data> forceStream(Data data) {
		ListData list = data.tryAsList();
		if (list != null) return list.value.stream();
		else throw AutoCodecUtil.rethrow(new DecodeException(() -> "Not a list: " + data));
	}

	public static class Imprinter<T> extends NamedImprinter<VariationsList<T>> {

		public final AutoDecoder<List<T>> listEncoder;

		public Imprinter(ReifiedType<VariationsList<T>> type, AutoDecoder<List<T>> imprinter) {
			super(type);
			this.listEncoder = imprinter;
		}

		public Imprinter(FactoryContext<VariationsList<T>> context) {
			this(context.type, context.type(ReifiedType.<List<T>>parameterize(List.class, context.type.resolveParameter(VariationsList.class))).forceCreateDecoder());
		}

		@Override
		public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, VariationsList<T>> context) throws ImprintException {
			try {
				context.object.source = context.data;
				Data list = ListData.collect(expand(context.data));
				context.object.elements = context.input(list).decodeWith(this.listEncoder);
			}
			catch (ImprintException exception) {
				throw exception;
			}
			catch (DecodeException exception) {
				throw new ImprintException(exception);
			}
		}
	}

	public static class Encoder<T> extends NamedEncoder<VariationsList<T>> {

		public Encoder(ReifiedType<VariationsList<T>> type) {
			super(type);
		}

		public Encoder(FactoryContext<VariationsList<T>> context) {
			this(context.type);
		}

		@Override
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, VariationsList<T>> context) throws EncodeException {
			return context.object == null ? EmptyData.INSTANCE : context.object.source;
		}

		public static <T_From, T_To> T_To convert(Dynamic<T_From> dynamic, DynamicOps<T_To> ops) {
			return Dynamic.convert(dynamic.getOps(), ops, dynamic.getValue());
		}
	}
}