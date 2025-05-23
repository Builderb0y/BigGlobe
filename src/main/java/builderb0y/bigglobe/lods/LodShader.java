package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.BigGlobeMod;

import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL20C.*;

@Environment(EnvType.CLIENT)
public class LodShader implements SafeCloseable {

	public int program, fragmentStage, vertexStage;

	public LodShader() {
		this.fragmentStage = glCreateShader(GL_FRAGMENT_SHADER);
		this.vertexStage = glCreateShader(GL_VERTEX_SHADER);
		this.program = glCreateProgram();
	}

	public void compileStage(int shader, String source) {
		glShaderSource(shader, source);
		glCompileShader(shader);
		String log = glGetShaderInfoLog(shader);
		if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new RuntimeException(log);
		}
		else if (!log.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Possible problems compiling " + this.getClass().getSimpleName() + ':');
			BigGlobeMod.LOGGER.warn(log);
		}
	}

	public void link() {
		glAttachShader(this.program, this.fragmentStage);
		glAttachShader(this.program, this.vertexStage);
		glLinkProgram(this.program);
		String log = glGetProgramInfoLog(this.program);
		if (glGetProgrami(this.program, GL_LINK_STATUS) != GL_TRUE) {
			throw new RuntimeException(log);
		}
		else if (!log.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Possible problems linking " + this.getClass().getSimpleName() + ':');
			BigGlobeMod.LOGGER.warn(log);
		}
	}

	@Override
	public void close() {
		if (this.fragmentStage != 0) { glDeleteShader(this.fragmentStage); this.fragmentStage = 0; }
		if (this.vertexStage != 0) { glDeleteShader(this.vertexStage); this.vertexStage = 0; }
		if (this.program != 0) { glDeleteProgram(this.program); this.program = 0; }
	}
}