package builderb0y.bigglobe.rendering2.lods;

public record DrawRange(int firstVertex, int vertexCount) {

	public static DrawRange fromBytes(int firstByte, int byteCount) {
		if (firstByte % LodVertexFormat.VERTEX_BYTES != 0 || byteCount % LodVertexFormat.VERTEX_BYTES != 0) {
			throw new IllegalArgumentException("Not aligned");
		}
		return new DrawRange(firstByte / LodVertexFormat.VERTEX_BYTES, byteCount / LodVertexFormat.VERTEX_BYTES);
	}

	public boolean canDraw() {
		return this.vertexCount != 0;
	}

	public int lastVertex() {
		return this.firstVertex + this.vertexCount;
	}

	public int firstIndex() {
		return this.firstVertex() * 6 / 4;
	}

	public int indexCount() {
		return this.vertexCount() * 6 / 4;
	}

	public int lastIndex() {
		return this.lastVertex() * 6 / 4;
	}
}