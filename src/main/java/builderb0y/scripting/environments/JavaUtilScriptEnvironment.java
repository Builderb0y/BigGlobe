package builderb0y.scripting.environments;

import java.util.*;

public class JavaUtilScriptEnvironment extends MultiScriptEnvironment {

	public static final JavaUtilScriptEnvironment INSTANCE = new JavaUtilScriptEnvironment();

	public JavaUtilScriptEnvironment() {
							this.environments.add(new NoFunctionalStuffClassScriptEnvironment(ArrayDeque.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Deque.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(PriorityQueue.class));
					this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Queue.class));

							this.environments.add(new NoFunctionalStuffClassScriptEnvironment(ArrayList.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(LinkedList.class));
					this.environments.add(new ListScriptEnvironment());

							this.environments.add(new NoFunctionalStuffClassScriptEnvironment(LinkedHashSet.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(HashSet.class));
								this.environments.add(new NoFunctionalStuffClassScriptEnvironment(TreeSet.class));
							this.environments.add(new NoFunctionalStuffClassScriptEnvironment(SortedSet.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(NavigableSet.class));
					this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Set.class));
				this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Collection.class));
			this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Iterable.class));

					this.environments.add(new NoFunctionalStuffClassScriptEnvironment(LinkedHashMap.class));
				this.environments.add(new NoFunctionalStuffClassScriptEnvironment(HashMap.class));
						this.environments.add(new NoFunctionalStuffClassScriptEnvironment(TreeMap.class));
					this.environments.add(new NoFunctionalStuffClassScriptEnvironment(SortedMap.class));
				this.environments.add(new NoFunctionalStuffClassScriptEnvironment(NavigableMap.class));
			this.environments.add(new MapScriptEnvironment());

				this.environments.add(new NoFunctionalStuffClassScriptEnvironment(ListIterator.class));
			this.environments.add(new NoFunctionalStuffClassScriptEnvironment(Iterator.class));
		this.environments.add(new ObjectScriptEnvironment());
	}
}