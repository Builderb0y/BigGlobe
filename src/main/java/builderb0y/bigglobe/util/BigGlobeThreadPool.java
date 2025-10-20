package builderb0y.bigglobe.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;

public class BigGlobeThreadPool {

	public static final LinkedBlockingDeque<Runnable> TASKS = new LinkedBlockingDeque<>();
	public static final ThreadPoolExecutor POOL;
	static {
		int threads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1); //reserve space for client and server thread.
		POOL = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS, TASKS, (Runnable task) -> {
			Thread thread = new Thread(task, "Big Globe Worker Thread");
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler((Thread thread_, Throwable exception) -> {
				BigGlobeMod.LOGGER.error("An unexpected exception occurred in " + thread + ": ", exception);
				BigGlobeThreadPool.POOL.prestartAllCoreThreads();
			});
			return thread;
		});
		POOL.prestartAllCoreThreads();
	}
	public static final Executor
		MAIN_EXECUTOR = TASKS::addFirst,
		LOD_EXECUTOR  = TASKS::addLast;

	public static void checkThreads() {
		int threads = BigGlobeConfig.INSTANCE.get().threads();
		synchronized (POOL) {
			if (threads < POOL.getMaximumPoolSize()) {
				POOL.setCorePoolSize(threads);
				POOL.setMaximumPoolSize(threads);
			}
			else if (threads > POOL.getMaximumPoolSize()) {
				POOL.setMaximumPoolSize(threads);
				POOL.setCorePoolSize(threads);
				POOL.prestartAllCoreThreads();
			}
		}
	}

	public static Executor mainExecutor() {
		checkThreads();
		return MAIN_EXECUTOR;
	}

	public static Executor lodExecutor() {
		checkThreads();
		return LOD_EXECUTOR;
	}

	public static AsyncRunner mainRunner() {
		return new AsyncRunner(mainExecutor());
	}

	public static AsyncRunner lodRunner() {
		return new AsyncRunner(lodExecutor());
	}

	public static AsyncRunner runner(boolean distantHorizons) {
		return distantHorizons ? lodRunner() : mainRunner();
	}

	public static AsyncRunner autoRunner() {
		return runner(DistantHorizonsCompat.isOnDistantHorizonThread());
	}

	public static Executor executor(boolean distantHorizons) {
		return distantHorizons ? lodExecutor() : mainExecutor();
	}

	public static Executor autoExecutor() {
		return executor(DistantHorizonsCompat.isOnDistantHorizonThread());
	}
}