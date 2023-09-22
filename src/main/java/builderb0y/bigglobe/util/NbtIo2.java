package builderb0y.bigglobe.util;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import net.minecraft.nbt.*;

/**
it genuinely surprises me that {@link NbtIo} doesn't have
simple methods for reading and writing {@link NbtElement}'s.
*/
public class NbtIo2 {

	//////////////////////////////// writing ////////////////////////////////

	public static void write(OutputStream stream, NbtElement element) throws IOException {
		DataOutputStream data = new DataOutputStream(stream);
		data.writeByte(element.getType());
		element.write(data);
		data.flush(); //don't close data, because that would likely close stream too.
	}

	public static void writeCompressed(OutputStream stream, NbtElement element) throws IOException {
		GZIPOutputStream zip = new GZIPOutputStream(stream);
		write(zip, element);
		zip.finish();
	}

	public static void write(ByteBuf buffer, NbtElement element) {
		try {
			write(new ByteBufOutputStream(buffer), element);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static void writeCompressed(ByteBuf buffer, NbtElement element) {
		try {
			GZIPOutputStream zip = new GZIPOutputStream(new ByteBufOutputStream(buffer));
			write(zip, element);
			zip.finish();
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	//////////////////////////////// reading ////////////////////////////////

	public static NbtElement read(InputStream stream, long limit) throws IOException {
		DataInputStream data = new DataInputStream(stream);
		NbtType<?> type = NbtTypes.byId(data.readUnsignedByte());
		#if MC_VERSION >= MC_1_20_2
			return type.read(data, NbtTagSizeTracker.of(limit));
		#else
			return type.read(data, 0, new NbtTagSizeTracker(limit));
		#endif
	}

	public static NbtElement read(InputStream stream) throws IOException {
		return read(stream, Long.MAX_VALUE);
	}

	public static NbtElement readCompressed(InputStream stream, long limit) throws IOException {
		return read(new GZIPInputStream(stream), limit);
	}

	public static NbtElement readCompressed(InputStream stream) throws IOException {
		return read(new GZIPInputStream(stream), Long.MAX_VALUE);
	}

	public static NbtElement read(ByteBuf buffer, long limit) {
		try {
			return read(new ByteBufInputStream(buffer), limit);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static NbtElement read(ByteBuf buffer) {
		try {
			return read(new ByteBufInputStream(buffer), Long.MAX_VALUE);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static NbtElement readCompressed(ByteBuf buffer, long limit) {
		try {
			return read(new GZIPInputStream(new ByteBufInputStream(buffer)), limit);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static NbtElement readCompressed(ByteBuf buffer) {
		try {
			return read(new GZIPInputStream(new ByteBufInputStream(buffer)), Long.MAX_VALUE);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}