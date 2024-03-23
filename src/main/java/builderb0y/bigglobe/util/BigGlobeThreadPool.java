package builderb0y.bigglobe.util;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;

public class BigGlobeThreadPool {

	public static final BigGlobeThreadPool INSTANCE = new BigGlobeThreadPool();

	public final Thread[] threads;
	public final ConcurrentLinkedDeque<Runnable> queue;
	public final AtomicInteger nextThreadToWake;

	public final Executor mainTerrain, lodTerrain;

	public BigGlobeThreadPool(int maxThreads) {
		this.threads          = new Thread[maxThreads];
		this.queue            = new ConcurrentLinkedDeque<>();
		this.nextThreadToWake = new AtomicInteger();
		this.mainTerrain      = this::submitMainTerrain;
		this.lodTerrain       = this::submitLodTerrain;
	}

	public BigGlobeThreadPool() {
		//save some processing power for the client and server threads.
		this(Math.max(Runtime.getRuntime().availableProcessors() - 2, 1));
	}

	public AsyncRunner mainRunner() {
		return new AsyncRunner(this.mainTerrain);
	}

	public AsyncRunner lodRunner() {
		return new AsyncRunner(this.lodTerrain);
	}

	public AsyncRunner runner(boolean distantHorizons) {
		return distantHorizons ? this.lodRunner() : this.mainRunner();
	}

	public AsyncRunner autoRunner() {
		return this.runner(DistantHorizonsCompat.isOnDistantHorizonThread());
	}

	public Executor executor(boolean distantHorizons) {
		return distantHorizons ? this.lodTerrain : this.mainTerrain;
	}

	public Executor autoExecutor() {
		return this.executor(DistantHorizonsCompat.isOnDistantHorizonThread());
	}

	public void submitMainTerrain(Runnable task) {
		this.queue.addFirst(task);
		this.wakeup();
	}

	public void submitLodTerrain(Runnable task) {
		this.queue.addLast(task);
		this.wakeup();
	}

	public void wakeup() {
		int oldIndex = this.nextThreadToWake.get();
		int newIndex = oldIndex + 1;
		if (newIndex >= this.threads.length) newIndex = 0;
		this.nextThreadToWake.compareAndSet(oldIndex, newIndex);

		Thread thread = this.threads[oldIndex];
		if (thread != null) {
			LockSupport.unpark(thread);
		}
		else {
			(this.threads[oldIndex] = new WorkerThread(this, oldIndex)).start();
		}
	}

	public static class WorkerThread extends Thread {

		public final BigGlobeThreadPool pool;

		public WorkerThread(BigGlobeThreadPool pool, int index) {
			this.pool = pool;
			this.setName("Big Globe Worker Thread " + index);
			this.setDaemon(true);
		}

		@Override
		public void run() {
			while (true) {
				Runnable task = this.pool.queue.pollFirst();
				if (task != null) {
					try {
						task.run();
					}
					catch (Throwable throwable) {
						BigGlobeMod.LOGGER.error("Exception in " + this.getName() + ": ", throwable);
					}
				}
				else {
					LockSupport.park(this.pool);
				}
			}
		}
	}
}