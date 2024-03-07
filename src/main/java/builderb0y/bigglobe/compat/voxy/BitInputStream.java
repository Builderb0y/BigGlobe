package builderb0y.bigglobe.compat.voxy;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;

public class BitInputStream implements Closeable {

	public final DataInputStream stream;
	public byte currentByte, currentBit;

	public BitInputStream(DataInputStream stream) {
		this.stream = stream;
	}

	public boolean readBit() throws IOException {
		if (this.currentBit == 0) {
			this.currentByte = this.stream.readByte();
			this.currentBit = 1;
		}
		boolean result = (this.currentByte & this.currentBit) != 0;
		this.currentBit <<= 1;
		return result;
	}

	@Override
	public void close() throws IOException {
		this.stream.close();
	}
}