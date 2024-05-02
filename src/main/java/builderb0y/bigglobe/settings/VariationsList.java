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

	public transient Dynamic<?> source;
	public transient List<T> elements;

	public static <T_Encoded> T_Encoded merge(T_Encoded oldObject, T_Encoded newObject, DynamicOps<T_Encoded> ops, boolean deep) {
		MapLike<T_Encoded> oldMap = ops.getMap(oldObject).result().orElse(null);
		MapLike<T_Encoded> newMap = ops.getMap(newObject).result().orElse(null);
		if (oldMap != null && newMap != null) {
			Map<T_Encoded, T_Encoded> result = new HashMap<>();
			oldMap.entries().forEach(pair -> result.put(pair.getFirst(), pair.getSecond()));
			if (deep) {
				newMap.entries().forEach(pair -> result.merge(
					pair.getFirst(),
					pair.getSecond(),
					(first, second) -> merge(first, second, ops, true)
				));
			}
			else {
				newMap.entries().forEach(pair -> result.put(pair.getFirst(), pair.getSecond()));
			}
			return ops.createMap(result);
		}
		return newObject;
	}

	public static <T_Encoded> Stream<T_Encoded> flatten(Stream<T_Encoded> oldLayer, T_Encoded[] newLayer, DynamicOps<T_Encoded> ops, boolean deep) {
		return oldLayer.flatMap((T_Encoded element1) -> {
			return Arrays.stream(newLayer).map((T_Encoded element2) -> {
				return merge(element1, element2, ops, deep);
			});
		});
	}

	@SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
	public static <T_Encoded> Stream<T_Encoded> expand(T_Encoded root, DynamicOps<T_Encoded> ops) {
		Stream<T_Encoded> variations = ops.get(root, "variations").flatMap(ops::getStream).result().orElse(null);
		if (variations != null) {
			boolean deep = ops.get(root, "deep").flatMap(ops::getBooleanValue).result().orElse(Boolean.FALSE);
			T_Encoded defaults = ops.get(root, "defaults").result().orElse(null);
			if (defaults != null) {
				T_Encoded[] layers = (T_Encoded[])(
					variations /* [ {}, {} ] */.flatMap(
						(T_Encoded element /* {} */) -> expand(element, ops)
					)
					.toArray()
				);
				return flatten(Stream.of(defaults), layers, ops, deep);
			}
			else {
				T_Encoded[] layers = (T_Encoded[])(
					variations /* [ [ {}, {} ], [ {}, {} ] ] */ .map(
						(T_Encoded list /* [ {}, {} ] */) -> ops.createList(
							unwrap(ops.getStream(list))
							.flatMap((T_Encoded element /* {} */) -> expand(element, ops))
						)
					)
					.toArray()
				);
				Stream<T_Encoded> stream = unwrap(ops.getStream(layers[0]));
				for (int index = 1, length = layers.length; index < length; index++) {
					stream = flatten(stream, (T_Encoded[])(unwrap(ops.getStream(layers[index])).toArray()), ops, deep);
				}
				return stream;
			}
		}
		else {
			Stream<T_Encoded> stream = ops.getStream(root).result().orElse(null);
			if (stream != null) {
				return stream /* [ {}, {} ] */.flatMap((T_Encoded element /* {} */) -> expand(element, ops));
			}
			return Stream.of(root);
		}
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
				context.object.source = new Dynamic<>(context.ops, context.input);
				T_Encoded list = context.ops.createList(expand(context.input, context.ops));
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
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, VariationsList<T>> context) throws EncodeException {
			return context.input == null ? context.empty() : convert(context.input.source, context.ops);
		}

		public static <T_From, T_To> T_To convert(Dynamic<T_From> dynamic, DynamicOps<T_To> ops) {
			return Dynamic.convert(dynamic.getOps(), ops, dynamic.getValue());
		}
	}
}