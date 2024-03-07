package builderb0y.bigglobe.compat.voxy;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;

public class BitOutputStream implements Closeable {

	public final DataOutputStream stream;
	public byte currentByte, currentBit;

	public BitOutputStream(DataOutputStream stream) {
		this.stream = stream;
		this.currentBit = 1;
	}

	public void write(boolean value) throws IOException {
		if (value) this.currentByte |= this.currentBit;
		if ((this.currentBit <<= 1) == 0) {
			this.stream.writeByte(this.currentByte);
			this.currentByte = 0;
			this.currentBit = 1;
		}
	}

	public BitOutputStream append(boolean value) throws IOException {
		this.write(value);
		return this;
	}

	@Override
	public void close() throws IOException {
		if (this.currentBit != 1) this.stream.writeByte(this.currentByte);
		this.stream.close();
	}
}