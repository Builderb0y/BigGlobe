package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.GpuMemory;
import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.opengl.GL31C.*;

@Environment(EnvType.CLIENT)
public class TextureBuffer extends GpuMemory {

	public static final long MAX_SIZE = (
		glGetInteger(GL_MAX_TEXTURE_BUFFER_SIZE) //number of texels.
		* 4L //number of channels per texel.
		* Float.BYTES //number of bytes per channel.
	);

	public NativeMemory cpuBuffer;

	public TextureBuffer() {
		super(
			4096L * //expected number of vec4's.
			4L * //number of floats in a vec4.
			Float.BYTES, //number of bytes in a float.
			GL_TEXTURE_BUFFER,
			GL_TEXTURE_BINDING_BUFFER
		);
		this.cpuBuffer = new NativeMemory(this.capacity);
	}

	@Override
	public void populateInitialData() {
		nglBufferData(this.binder, 0L, 0L, GL_DYNAMIC_DRAW);
	}

	@Override
	public SafeCloseable bind() {
		this.checkThread();
		int id = this.ensureOpen();
		glBindBuffer(this.binder, id);
		//you can bind textures and buffers to GL_TEXTURE_BUFFER,
		//but GL_TEXTURE_BINDING_BUFFER will only return the texture.
		//as far as I can tell, there is no way to query what *buffer*
		//is bound to GL_TEXTURE_BUFFER, so there is no way to restore this state.
		//hopefully no other mods depend on this bind point being bound to a specific thing.
		return SafeCloseable.NOOP;
	}

	public void uploadAndClear() {
		if (this.cpuBuffer.used > MAX_SIZE) {
			throw new GLException("GL_MAX_TEXTURE_BUFFER_SIZE exceeded");
		}
		nglBufferData(this.binder, this.cpuBuffer.used, this.cpuBuffer.address, GL_DYNAMIC_DRAW);
		this.cpuBuffer.clear();
	}

	@Override
	public void close() {
		super.close();
		this.cpuBuffer.close();
	}
}