package builderb0y.bigglobe.util;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.profiler.Profiler;

import builderb0y.bigglobe.commands.WorldgenTimingsCommand;

/**
simplified version of {@link Profiler} designed to
track timings of worldgen specifically across threads.
the backing map, {@link #timings} is
manipulated by {@link WorldgenTimingsCommand}.
*/
public class WorldgenProfiler {

	public @Nullable ConcurrentHashMap<Object, Long> timings;

	public <X extends Throwable> void run(Object marker, ThrowingRunnable<X> task) throws X {
		ConcurrentHashMap<Object, Long> map = this.timings;
		if (map != null) {
			long startTime = System.nanoTime();
			task.run();
			long endTime = System.nanoTime();
			map.merge(marker, endTime - startTime, Long::sum);
		}
		else {
			task.run();
		}
	}

	public <T, X extends Throwable> T get(Object marker, ThrowingSupplier<T, X> task) throws X {
		ConcurrentHashMap<Object, Long> map = this.timings;
		if (map != null) {
			long startTime = System.nanoTime();
			T result = task.get();
			long endTime = System.nanoTime();
			map.merge(marker, endTime - startTime, Long::sum);
			return result;
		}
		else {
			return task.get();
		}
	}
}