package builderb0y.bigglobe.rendering;

import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.opengl.GL30C.*;

public class EmptyVertexArray implements SafeCloseable {

	public int vao;

	public EmptyVertexArray() {
		this.vao = glGenVertexArrays();
	}

	@Override
	public void close() {
		if (this.vao != 0) {
			glDeleteVertexArrays(this.vao);
			this.vao = 0;
		}
	}
}