package builderb0y.bigglobe.rendering;

import static org.lwjgl.opengl.GL31C.*;

public class TextureState {

	public int binder, bindQuery, index, texture;

	public TextureState(int binder, int bindQuery, int index) {
		this.binder = binder;
		this.bindQuery = bindQuery;
		this.index = index;
	}

	public static TextureState buffer(int index) {
		return new TextureState(GL_TEXTURE_BUFFER, GL_TEXTURE_BINDING_BUFFER, index);
	}

	public static FilteredTextureState _2D(int index) {
		return new FilteredTextureState(GL_TEXTURE_2D, GL_TEXTURE_BINDING_2D, index);
	}

	public void capture() {
		glActiveTexture(this.index);
		GLException.check();
		this.texture = glGetInteger(this.bindQuery);
		GLException.check();
	}

	public void restore() {
		glActiveTexture(this.index);
		GLException.check();
		glBindTexture(this.binder, this.texture);
		GLException.check();
	}
}