package builderb0y.bigglobe.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class TextCoding {

	public static NbtElement toNbt(@Nullable Text text) {
		return text != null ? TextCodecs.CODEC.encodeStart(NbtOps.INSTANCE, text).result().orElse(null) : null;
	}

	public static void write(PacketByteBuf buffer, @Nullable Text text) {
		buffer.writeNbt(toNbt(text));
	}

	public static @Nullable Text fromNbt(NbtElement element) {
		if (element == null || element == NbtEnd.INSTANCE) return null;
		return TextCodecs.CODEC.parse(NbtOps.INSTANCE, element).result().orElse(null);
	}

	public static @Nullable Text read(PacketByteBuf buffer) {
		return fromNbt(buffer.readNbt(NbtTagSizeTracker.of(16384L)));
	}
}