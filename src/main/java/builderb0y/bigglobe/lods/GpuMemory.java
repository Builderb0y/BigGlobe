package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.autocodec.util.AutoCodecUtil;

import static org.lwjgl.opengl.GL15C.*;

@Environment(EnvType.CLIENT)
public abstract class GpuMemory implements SafeCloseable {

	public Thread thread;
	public int binder, bindQuery;
	public long capacity;
	public int glID;

	public GpuMemory(long capacity, int binder, int bindQuery) {
		this.thread = Thread.currentThread();
		this.binder = binder;
		this.bindQuery = bindQuery;
		this.capacity = capacity;
		this.glID = this.nAllocate(true);
	}

	public int nAllocate(boolean restore) {
		int oldID = restore ? glGetInteger(this.bindQuery) : 0;
		int id = glGenBuffers();
		try {
			glBindBuffer(this.binder, id);
			this.populateInitialData();
		}
		catch (Throwable throwable) {
			glDeleteBuffers(id);
			AutoCodecUtil.rethrow(throwable);
		}
		finally {
			if (restore) glBindBuffer(this.binder, oldID);
		}
		return id;
	}

	public abstract void populateInitialData();

	public void checkThread() {
		if (Thread.currentThread() != this.thread) {
			throw new IllegalStateException("Calling on wrong thread! Expected " + this.thread + ", got " + Thread.currentThread());
		}
	}

	public int ensureOpen() {
		int id = this.glID;
		if (id != 0) return id;
		else throw new IllegalStateException("Already closed");
	}

	public SafeCloseable bind() {
		this.checkThread();
		int id = this.ensureOpen();
		int old = glGetInteger(this.bindQuery);
		if (old == id) return SafeCloseable.NOOP;
		glBindBuffer(this.binder, id);
		return () -> glBindBuffer(this.binder, old);
	}

	@Override
	public void close() {
		this.checkThread();
		int glID = this.glID;
		if (glID != 0) {
			this.glID = 0;
			this.capacity = 0L;
			glDeleteBuffers(glID);
		}
	}
}