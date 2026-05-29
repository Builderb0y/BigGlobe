package builderb0y.bigglobe.columns.scripted2.dependencies;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public abstract class GenericCyclicDependencyAnalyzer<T> implements Consumer<T> {

	public final Set<T>
		seen = new ObjectOpenHashSet<>(256),
		stack = new ObjectLinkedOpenHashSet<>(16);

	public abstract Stream<? extends T> getDependencies(T object);

	public boolean areCyclesFatal(T object) {
		return true;
	}

	@Override
	public void accept(T object) {
		if (this.stack.add(object)) {
			try {
				if (this.seen.add(object)) {
					this.getDependencies(object).forEach(this);
				}
			}
			finally {
				this.stack.remove(object);
			}
		}
		else {
			//cycle detected.
			//to test whether or not it's fatal,
			//we first chop the nodes that led to the cycle,
			//and then test if any of the remaining nodes are fatal.
			//consider the following graph:
			//	A -> B <-> C
			//where only node A is fatal.
			//the cycle only concerns nodes B and C,
			//which are both non-fatal,
			//so this cycle is not fatal.
			//if we had started with node A,
			//then it will be on our stack anyway.
			//that's why we drop it before checking fatality.
			@SuppressWarnings("unchecked")
			T[] cycle = (T[])(this.stack.toArray());
			int length = cycle.length;
			for (int drop = 0; drop < length; drop++) {
				if (cycle[drop] == object) {
					//found first node that's part of the cycle.
					//now check if any remaining nodes are fatal.
					for (int startingIndex = drop; startingIndex < cycle.length; startingIndex++) {
						if (this.areCyclesFatal(cycle[startingIndex])) {
							//found a fatal node in the cycle.
							//now we want to print the nodes out such
							//that the first node printed is fatal,
							//and the last node is the same as the first node.
							StringBuilder message = new StringBuilder();
							message.append(this.format(cycle[startingIndex]));
							for (int index = startingIndex; ++index < cycle.length;) {
								message.append(" -> ").append(this.format(cycle[index]));
							}
							for (int index = drop; index < startingIndex; index++) {
								message.append(" -> ").append(this.format(cycle[index]));
							}
							message.append(" -> ").append(this.format(cycle[startingIndex]));
							throw new CyclicDependencyException(message.toString());
						}
					}
				}
			}
		}
	}

	public String format(T object) {
		return String.valueOf(object);
	}
}