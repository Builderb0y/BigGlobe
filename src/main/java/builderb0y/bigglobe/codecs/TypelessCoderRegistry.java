package builderb0y.bigglobe.codecs;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.AutoCodec;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.memberViews.FieldLikeMemberView;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class TypelessCoderRegistry<E> extends NamedCoder<E> {

	public final AutoCodec autoCodec;
	public final Set<String> commonFields;
	public final Map<String, AutoCoder<? extends E>> decodeLookup;
	public final Map<Class<? extends E>, AutoCoder<? extends E>> encodeLookup;

	public TypelessCoderRegistry(ReifiedType<E> type, AutoCodec autoCodec) {
		super(type);
		this.autoCodec = autoCodec;
		this.commonFields = Arrays.stream(autoCodec.reflect(type).getFields(true)).map(FieldLikeMemberView::getAliases).flatMap(Arrays::stream).collect(Collectors.toSet());
		this.decodeLookup = new HashMap<>();
		this.encodeLookup = new HashMap<>();
	}

	public <E2 extends E> void register(Class<E2> clazz) {
		AutoCoder<E2> coder = this.autoCodec.createCoder(clazz);
		if (this.encodeLookup.putIfAbsent(clazz, coder) != null) {
			throw new IllegalArgumentException("Duplicate class: " + clazz);
		}
		Stream<String> keys = coder.getKeys();
		if (keys == null) throw new IllegalArgumentException("Could not get keys from " + clazz);
		keys.filter((String key) -> !this.commonFields.contains(key)).forEach((String key) -> {
			if (this.decodeLookup.putIfAbsent(key, coder) != null) {
				throw new IllegalArgumentException("Duplicate key: " + key);
			}
		});
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable E decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String prevKey = null;
		AutoCoder<? extends E> coder = null;
		Stream<Pair<T_Encoded, T_Encoded>> stream = context.logger().unwrapLazy(context.ops.getMapValues(context.input), false, DecodeException::new);
		if (stream == null) throw context.notA("map");
		for (Iterator<Pair<T_Encoded, T_Encoded>> iterator = stream.iterator(); iterator.hasNext();) {
			String key = context.logger().unwrapLazy(context.ops.getStringValue(iterator.next().getFirst()), false, DecodeException::new);
			AutoCoder<? extends E> next = this.decodeLookup.get(key);
			if (next == null) {
				if (this.commonFields.contains(key)) continue;
				else throw new DecodeException(() -> "Unknown key: " + key);
			}
			if (coder == null) {
				coder = next;
				prevKey = key;
			}
			else if (coder != next) {
				final String prevKey_ = prevKey;
				throw new DecodeException(() -> "Cannot specify both " + prevKey_ + " and " + key + " at the same time");
			}
		}
		if (coder == null) {
			throw new DecodeException(() -> "Not enough information to determine type");
		}
		return context.decodeWith(coder);
	}

	@Override
	@OverrideOnly
	@SuppressWarnings("unchecked")
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, E> context) throws EncodeException {
		if (context.object == null) return context.empty();
		AutoCoder<? extends E> coder = this.encodeLookup.get(context.object.getClass());
		if (coder == null) throw new EncodeException(() -> "Unhandled type: " + context.object.getClass());
		return context.encodeWith((AutoCoder<E>)(coder));
	}
}