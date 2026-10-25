package builderb0y.bigglobe.rendering;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;

import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.*;

public class GLException extends RuntimeException {

	public static final boolean ENABLED = Boolean.getBoolean("bigglobe.checkGLErrors");

	static {
		BigGlobeMod.LOGGER.info("GL error checking is " + (ENABLED ? "enabled" : "disabled") + ".");
	}

	public GLException(String message) {
		super(message);
	}

	public static void check() {
		if (ENABLED) forceCheck();
	}

	public static void forceCheck() {
		int error = glGetError();
		if (error != GL_NO_ERROR) {
			throw new GLException(message(error));
		}
	}

	public static @Nullable String checkMessage() {
		return ENABLED ? forceCheckMessage() : null;
	}

	public static @Nullable String forceCheckMessage() {
		int error = glGetError();
		return error != GL_NO_ERROR ? message(error) : null;
	}

	public static String message(int code) {
		return switch (code) {
			case GL_INVALID_ENUM -> "GL_INVALID_ENUM";
			case GL_INVALID_VALUE -> "GL_INVALID_VALUE";
			case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
			case GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
			case GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
			case GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
			case GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
			default -> "unknown error: 0x" + Integer.toHexString(code);
		};
	}

	public static void checkFramebuffer(int bindPoint) {
		int status = glCheckFramebufferStatus(bindPoint);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			if (status == 0) {
				check();
			}
			throw new GLException(framebufferMessage(status));
		}
	}

	public static @Nullable String checkFramebufferMessage(int bindPoint) {
		int code = glCheckFramebufferStatus(bindPoint);
		return code != GL_FRAMEBUFFER_COMPLETE ? framebufferMessage(code) : null;
	}

	public static String framebufferMessage(int code) {
		return switch (code) {
			case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
			case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
			case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
			case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
			case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
			case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS -> "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
			case GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
			case GL_FRAMEBUFFER_UNDEFINED -> "GL_FRAMEBUFFER_UNDEFINED";
			default -> "unknown framebuffer error: 0x" + Integer.toHexString(code);
		};
	}
}