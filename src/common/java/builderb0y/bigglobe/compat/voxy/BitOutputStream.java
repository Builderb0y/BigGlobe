package builderb0y.bigglobe.compat.voxy;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;

public class BitOutputStream implements Closeable {

	public final DataOutputStream source;
	public byte currentByte, currentBit;

	public BitOutputStream(DataOutputStream source) {
		this.source = source;
		this.currentBit = 1;
	}

	public void write(boolean value) throws IOException {
		if (value) this.currentByte |= this.currentBit;
		if ((this.currentBit <<= 1) == 0) {
			this.source.writeByte(this.currentByte);
			this.currentByte = 0;
			this.currentBit = 1;
		}
	}

	@Override
	public void close() throws IOException {
		if (this.currentBit != 1) this.source.writeByte(this.currentByte);
		this.source.close();
	}
}