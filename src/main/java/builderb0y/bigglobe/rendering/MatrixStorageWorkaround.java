package builderb0y.bigglobe.rendering;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.*;

import builderb0y.bigglobe.util.SafeCloseable;

/**
one person has encountered an error in LWJGL caused by JVM implementation details.
as such, I cannot use {@link Matrix4f#getToAddress(long)}.
minecraft does it this way instead, so that's what I'll do too.
*/
public class MatrixStorageWorkaround implements SafeCloseable {

	public FloatBuffer buffer = MemoryUtil.memAllocFloat(16);

	public void set(Matrix4f matrix) {
		matrix.get(this.buffer);
	}

	public long address() {
		return MemoryUtil.memAddress(this.buffer);
	}

	@Override
	public void close() {
		FloatBuffer buffer = this.buffer;
		if (buffer != null) {
			this.buffer = null;
			MemoryUtil.memFree(buffer);
		}
	}
}