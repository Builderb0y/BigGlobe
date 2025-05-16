package builderb0y.bigglobe.lods;

import static org.lwjgl.opengl.GL11C.*;

public class GLException extends RuntimeException {

	public final int code;

	public GLException(int code) {
		super(switch (code) {
			case GL_INVALID_ENUM      -> "GL_INVALID_ENUM";
			case GL_INVALID_VALUE     -> "GL_INVALID_VALUE";
			case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
			case GL_STACK_OVERFLOW    -> "GL_STACK_OVERFLOW";
			case GL_STACK_UNDERFLOW   -> "GL_STACK_UNDERFLOW";
			case GL_OUT_OF_MEMORY     -> "GL_OUT_OF_MEMORY";
			default                   -> "unknown error: 0x" + Integer.toHexString(code);
		});
		this.code = code;
	}

	public static void check() {
		int error = glGetError();
		if (error != GL_NO_ERROR) throw new GLException(error);
	}
}