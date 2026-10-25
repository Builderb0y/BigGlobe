package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Event<T> {

	private final Class<T> type;
	private final Function<T[], T> invokerFactory;
	private final List<T> handlers = new ArrayList<>();
	private T invoker;

	public Event(Class<T> type, Function<T[], T> invokerFactory) {
		this.type = type;
		this.invokerFactory = invokerFactory;
		this.update();
	}

	@SuppressWarnings("unchecked")
	private void update() {
		T[] array = (T[])Array.newInstance(this.type, this.handlers.size());
		this.invoker = this.invokerFactory.apply(this.handlers.toArray(array));
	}

	public void register(T listener) {
		this.handlers.add(listener);
		this.update();
	}

	public T invoker() {
		return this.invoker;
	}
}
