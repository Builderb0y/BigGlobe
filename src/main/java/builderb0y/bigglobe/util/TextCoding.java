package builderb0y.bigglobe.util;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.DataOps;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class TextCoding {

	public static final AutoCoder<Component> CODER = new NamedCoder<>("TextCoding.CODER") {

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, Component> context) throws EncodeException {
			Component text = context.object;
			if (text == null) return EmptyData.INSTANCE;

			return context.logger().unwrapLazy(
				ComponentSerialization.CODEC.encodeStart(DataOps.UNCOMPRESSED, text),
				true,
				EncodeException::new
			);
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable Component decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;

			return context.logger().unwrapLazy(
				ComponentSerialization.CODEC.parse(DataOps.UNCOMPRESSED, context.data),
				true,
				DecodeException::new
			);
		}
	};

	public static Tag toNbt(@Nullable Component text) {
		if (text == null) return null;

		return ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, text).result().orElse(null);
	}

	public static @Nullable Component fromNbt(Tag element) {
		if (element == null || element == EndTag.INSTANCE) return null;

		return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, element).result().orElse(null);
	}

	public static @Nullable Component read(FriendlyByteBuf buffer) {

		return fromNbt(buffer.readNbt(NbtAccounter.create(16384L)));
	}

	public static void write(FriendlyByteBuf buffer, @Nullable Component text) {

		buffer.writeNbt(toNbt(text));
	}
}