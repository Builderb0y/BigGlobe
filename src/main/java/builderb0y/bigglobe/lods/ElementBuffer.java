package builderb0y.bigglobe.lods;

import static org.lwjgl.opengl.GL15C.*;

public class ElementBuffer extends ResizeableGpuMemory {

	public ElementBuffer() {
		super(
			(1L << LodQuadTree.MIN_LEVEL) * (1L << LodQuadTree.MIN_LEVEL) * //expected number of columns
			16L * //expected quads per column
			6L * //indices per quad
			Integer.BYTES, //bytes per index
			GL_ELEMENT_ARRAY_BUFFER,
			GL_ELEMENT_ARRAY_BUFFER_BINDING
		);
	}

	@Override
	public void populateInitialData() {
		try (NativeMemory memory = new NativeMemory(this.capacity)) {
			int vertexIndex = 0;
			while (memory.used < memory.capacity) {
				memory.appendInt(vertexIndex + 0);
				memory.appendInt(vertexIndex + 1);
				memory.appendInt(vertexIndex + 2);
				memory.appendInt(vertexIndex + 2);
				memory.appendInt(vertexIndex + 3);
				memory.appendInt(vertexIndex + 0);
				vertexIndex += 4;
			}
			assert memory.used == memory.capacity : "Created unexpected number of indices...";
			nglBufferData(this.binder, memory.capacity, memory.address, GL_STATIC_DRAW);
		}
	}

	public void ensureCapacity(int indexCount) {
		this.resize(((long)(indexCount)) * ((long)(Integer.BYTES)));
	}
}