package builderb0y.bigglobe.compat.voxy;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;

public class BitInputStream implements Closeable {

	public final DataInputStream source;
	public byte currentByte, currentBit;

	public BitInputStream(DataInputStream source) {
		this.source = source;
	}

	public boolean readBit() throws IOException {
		if (this.currentBit == 0) {
			this.currentByte = this.source.readByte();
			this.currentBit = 1;
		}
		boolean result = (this.currentByte & this.currentBit) != 0;
		this.currentBit <<= 1;
		return result;
	}

	@Override
	public void close() throws IOException {
		this.source.close();
	}
}