package builderb0y.bigglobe.util;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface SafeCloseable extends AutoCloseable {

	public static final SafeCloseable NOOP = () -> {};

	public static SafeCloseable of(RenderTarget target) {
		return target != null ? target::destroyBuffers : null;
	}

	@Override
	public abstract void close();
}