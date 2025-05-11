package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.autocodec.util.AutoCodecUtil;

import static org.lwjgl.opengl.GL15C.*;

@Environment(EnvType.CLIENT)
public abstract class GpuMemory implements SafeCloseable {

	public final Thread thread;
	public final int binder, bindQuery;
	public final long capacity;
	public int glID;

	public GpuMemory(long capacity, int binder, int bindQuery, Object arg) {
		this.thread = Thread.currentThread();
		this.binder = binder;
		this.bindQuery = bindQuery;
		this.capacity = capacity;
		this.glID = this.nAllocate(arg);
	}

	public GpuMemory(long capacity, int binder, int bindQuery) {
		this(capacity, binder, bindQuery, null);
	}

	public int nAllocate(Object arg) {
		int oldID = glGetInteger(this.bindQuery);
		int id = glGenBuffers();
		try {
			glBindBuffer(this.binder, id);
			this.populateInitialData(arg);
		}
		catch (Throwable throwable) {
			glDeleteBuffers(id);
			AutoCodecUtil.rethrow(throwable);
		}
		finally {
			glBindBuffer(this.binder, oldID);
		}
		return id;
	}

	public abstract void populateInitialData(Object arg);

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
			glDeleteBuffers(glID);
		}
	}
}