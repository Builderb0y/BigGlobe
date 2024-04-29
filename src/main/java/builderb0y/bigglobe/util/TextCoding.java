package builderb0y.bigglobe.util;

import java.io.IOException;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBufOutputStream;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

#if MC_VERSION >= MC_1_20_4
import net.minecraft.text.TextCodecs;
#endif

public class TextCoding {

	public static NbtElement toNbt(@Nullable Text text) {
		if (text == null) return null;
		#if MC_VERSION >= MC_1_20_4
			return TextCodecs.CODEC.encodeStart(NbtOps.INSTANCE, text).result().orElse(null);
		#else
			JsonElement json = Text.Serializer.toJsonTree(text);
			return JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
		#endif
	}

	public static void write(PacketByteBuf buffer, @Nullable Text text) {
		#if MC_VERSION >= MC_1_20_2
			buffer.writeNbt(toNbt(text));
		#else
			NbtIo2.write(buffer, toNbt(text));
		#endif
	}

	public static @Nullable Text fromNbt(NbtElement element) {
		if (element == null || element == NbtEnd.INSTANCE) return null;
		#if MC_VERSION >= MC_1_20_4
			return TextCodecs.CODEC.parse(NbtOps.INSTANCE, element).result().orElse(null);
		#else
			JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, element);
			return Text.Serializer.fromJson(json);
		#endif
	}

	public static @Nullable Text read(PacketByteBuf buffer) {
		#if MC_VERSION >= MC_1_20_2
			return fromNbt(buffer.readNbt(NbtTagSizeTracker.of(16384L)));
		#else
			return fromNbt(NbtIo2.read(buffer, 16384L));
		#endif
	}
}