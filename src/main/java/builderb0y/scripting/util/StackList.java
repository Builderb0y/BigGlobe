package builderb0y.scripting.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StackList<E> extends ObjectArrayList<E> {

	public IntArrayList sizes = new IntArrayList(16);

	public StackList(int capacity) {
		super(capacity);
	}

	public StackList() {}

	public StackList(StackList<E> that) {
		super(that);
		this.sizes = that.sizes.clone();
	}

	public void pushStack() {
		this.sizes.add(this.size());
	}

	public void popStack() {
		this.size(this.sizes.removeInt(this.sizes.size() - 1));
	}

	public boolean hasNewElements() {
		return this.size() > this.sizes.getInt(this.sizes.size() - 1);
	}

	@Override
	public void clear() {
		super.clear();
		this.sizes.clear();
	}
}