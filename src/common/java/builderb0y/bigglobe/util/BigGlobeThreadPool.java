package builderb0y.bigglobe.util;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;

public class BigGlobeThreadPool {

	public static final AtomicBoolean BUSY = new AtomicBoolean(false);
	public static final LinkedBlockingDeque<Runnable> TASKS = new LinkedBlockingDeque<>();
	public static final ThreadPoolExecutor POOL;

	static {
		int threads = Math.max(Runtime.getRuntime().availableProcessors() - 4, 1); //reserve space for client and server thread.
		POOL = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS, TASKS, WorkerThread::new);
		POOL.prestartAllCoreThreads();
	}

	public static final Executor
		MAIN_EXECUTOR = TASKS::addFirst,
		LOD_EXECUTOR  = TASKS::addLast;

	public static void checkNotAlreadyInPool() {
		if (Thread.currentThread() instanceof WorkerThread) {
			throw new IllegalThreadStateException("Do not use BigGlobeThreadPool on a thread started by BigGlobeThreadPool.");
		}
	}

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

	public static void onMainTaskStarted() {
		BUSY.set(true);
	}

	public static Executor mainExecutor() {
		checkNotAlreadyInPool();
		checkThreads();
		onMainTaskStarted();
		return MAIN_EXECUTOR;
	}

	public static Executor lodExecutor() {
		checkNotAlreadyInPool();
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

	public static boolean isBusy() {
		return BUSY.getAndSet(false);
	}

	public static class WorkerThread extends Thread {

		public WorkerThread(Runnable task) {
			super(task, "Big Globe Worker Thread");
			this.setDaemon(true);
			this.setUncaughtExceptionHandler((Thread thread, Throwable exception) -> {
				BigGlobeMod.LOGGER.error("An unexpected exception occurred in " + thread + ": ", exception);
				BigGlobeThreadPool.POOL.prestartAllCoreThreads();
			});
		}
	}
}