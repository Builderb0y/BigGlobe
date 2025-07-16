package builderb0y.bigglobe.rendering;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;

public class FilteredTextureState extends TextureState {

	public int minFilter, magFilter, wrapX, wrapY;

	public FilteredTextureState(int binder, int bindQuery, int index) {
		super(binder, bindQuery, index);
	}

	public void set(int texture, int minFilter, int magFilter, int wrapX, int wrapY) {
		glActiveTexture(this.index);
		GLException.check();
		glBindTexture(this.binder, texture);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_MIN_FILTER, minFilter);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_MAG_FILTER, magFilter);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_WRAP_S, wrapX);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_WRAP_T, wrapY);
		GLException.check();
	}

	@Override
	public void capture() {
		super.capture();
		this.minFilter = glGetTexParameteri(this.binder, GL_TEXTURE_MIN_FILTER);
		GLException.check();
		this.magFilter = glGetTexParameteri(this.binder, GL_TEXTURE_MAG_FILTER);
		GLException.check();
		this.wrapX = glGetTexParameteri(this.binder, GL_TEXTURE_WRAP_S);
		GLException.check();
		this.wrapY = glGetTexParameteri(this.binder, GL_TEXTURE_WRAP_T);
		GLException.check();
	}

	@Override
	public void restore() {
		super.restore();
		glTexParameteri(this.binder, GL_TEXTURE_MIN_FILTER, this.minFilter);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_MAG_FILTER, this.magFilter);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_WRAP_S, this.wrapX);
		GLException.check();
		glTexParameteri(this.binder, GL_TEXTURE_WRAP_T, this.wrapY);
		GLException.check();
	}
}