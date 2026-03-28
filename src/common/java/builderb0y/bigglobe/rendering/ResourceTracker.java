package builderb0y.bigglobe.rendering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class ResourceTracker implements SafeCloseable {

	public final List<SafeCloseable> resources = new ArrayList<>();

	public <R extends SafeCloseable> R track(R resource) {
		if (resource != null) {
			this.resources.add(resource);
		}
		return resource;
	}

	public void untrackAll() {
		this.resources.clear();
	}

	@Override
	public void close() {
		closeAll(this.resources);
	}

	public static void closeAll(AutoCloseable... closeables) {
		closeAll(Arrays.asList(closeables));
	}

	public static void closeAll(Iterable<? extends AutoCloseable> closeables) {
		RuntimeException rootException = null;
		for (AutoCloseable resource : closeables) {
			if (resource != null) try {
				resource.close();
			}
			catch (Throwable throwable) {
				if (rootException == null) rootException = new RuntimeException("Some resources failed to close; see below.");
				rootException.addSuppressed(throwable);
			}
		}
		if (rootException != null) throw rootException;
	}
}