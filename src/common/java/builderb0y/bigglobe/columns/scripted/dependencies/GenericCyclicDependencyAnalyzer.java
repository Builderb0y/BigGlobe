package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public abstract class GenericCyclicDependencyAnalyzer<T> implements Consumer<T> {

	public final Set<T> stack = new ObjectOpenHashSet<>(16);
	public final Map<T, Set<T>> reachable = new Object2ObjectOpenHashMap<>(256);

	public abstract Stream<? extends T> getDependencies(T object);

	public boolean areCyclesFatal(T node) {
		return true;
	}

	@Override
	public void accept(T node) {
		boolean added = this.stack.add(node);
		try {
			Set<T> dependencies = this.reachable.computeIfAbsent(node, (T _) -> new ObjectOpenHashSet<>());
			if (dependencies.addAll(this.stack)) {
				this.getDependencies(node).forEach(this);
			}
			if (!added && this.areCyclesFatal(node)) {
				throw this.fail(node);
			}
		}
		finally {
			if (added) this.stack.remove(node);
		}
	}

	public CyclicDependencyException fail(T node) {
		@SuppressWarnings("unchecked")
		T[] cycle = (T[])(this.stack.toArray());
		for (int startingIndex = 0; startingIndex < cycle.length; startingIndex++) {
			if (cycle[startingIndex].equals(node)) {
				StringBuilder message = new StringBuilder();
				message.append(this.format(node));
				for (int index = startingIndex; ++index < cycle.length;) {
					message.append(" -> ").append(this.format(cycle[index]));
				}
				message.append(" -> ").append(this.format(node));
				return new CyclicDependencyException(message.toString());
			}
		}
		throw new AssertionError("Could not find object in stack after failing to add it to the stack???");
	}

	public String format(T object) {
		return String.valueOf(object);
	}
}