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
simple methods for reading and writing {@link Tag}'s.
*/
public class NbtIo2 {

	//////////////////////////////// writing ////////////////////////////////

	public static void write(OutputStream stream, Tag element) throws IOException {
		if (element == null) element = EndTag.INSTANCE;
		DataOutputStream data = new DataOutputStream(stream);
		data.writeByte(element.getId());
		element.write(data);
		data.flush(); //don't close data, because that would likely close stream too.
	}

	public static void writeCompressed(OutputStream stream, Tag element) throws IOException {
		GZIPOutputStream zip = new GZIPOutputStream(stream);
		write(zip, element);
		zip.finish();
	}

	public static void write(ByteBuf buffer, Tag element) {
		try {
			write(new ByteBufOutputStream(buffer), element);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static void writeCompressed(ByteBuf buffer, Tag element) {
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

	public static Tag read(InputStream stream, long limit) throws IOException {
		DataInputStream data = new DataInputStream(stream);
		TagType<?> type = TagTypes.getType(data.readUnsignedByte());

		return type.load(data, NbtAccounter.create(limit));
	}

	public static Tag read(InputStream stream) throws IOException {
		return read(stream, Long.MAX_VALUE);
	}

	public static Tag readCompressed(InputStream stream, long limit) throws IOException {
		return read(new GZIPInputStream(stream), limit);
	}

	public static Tag readCompressed(InputStream stream) throws IOException {
		return read(new GZIPInputStream(stream), Long.MAX_VALUE);
	}

	public static Tag read(ByteBuf buffer, long limit) {
		try {
			return read(new ByteBufInputStream(buffer), limit);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static Tag read(ByteBuf buffer) {
		try {
			return read(new ByteBufInputStream(buffer), Long.MAX_VALUE);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static Tag readCompressed(ByteBuf buffer, long limit) {
		try {
			return read(new GZIPInputStream(new ByteBufInputStream(buffer)), limit);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static Tag readCompressed(ByteBuf buffer) {
		try {
			return read(new GZIPInputStream(new ByteBufInputStream(buffer)), Long.MAX_VALUE);
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
}