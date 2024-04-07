package builderb0y.bigglobe.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import builderb0y.bigglobe.compat.DistantHorizonsCompat;

public class BigGlobeThreadPool {

	public static final ForkJoinPool POOL = new ForkJoinPool(Math.max(Runtime.getRuntime().availableProcessors() - 2, 1));

	public static AsyncRunner mainRunner() {
		return new AsyncRunner(POOL);
	}

	public static AsyncRunner lodRunner() {
		return new AsyncRunner(POOL);
	}

	public static AsyncRunner runner(boolean distantHorizons) {
		return distantHorizons ? lodRunner() : mainRunner();
	}

	public static AsyncRunner autoRunner() {
		return runner(DistantHorizonsCompat.isOnDistantHorizonThread());
	}

	public static Executor executor(boolean distantHorizons) {
		return POOL;
	}

	public static Executor autoExecutor() {
		return executor(DistantHorizonsCompat.isOnDistantHorizonThread());
	}

	public static boolean isBusy() {
		return POOL.hasQueuedSubmissions();
	}
}