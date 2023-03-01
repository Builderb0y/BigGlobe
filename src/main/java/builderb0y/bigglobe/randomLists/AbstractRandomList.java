package builderb0y.bigglobe.randomLists;

import java.util.*;

public abstract class AbstractRandomList<E> extends AbstractList<E> implements IRandomList<E> {

	//all methods here are organized into sections.
	//sub-classes should follow the same ordering.
	//the below comments are for copy-pasting
	//purposes whenever I make a new implementation.

	//////////////////////////////// get ////////////////////////////////

	//////////////////////////////// set ////////////////////////////////

	//////////////////////////////// contains ////////////////////////////////

	//////////////////////////////// size/empty ////////////////////////////////

	//////////////////////////////// add ////////////////////////////////

	//////////////////////////////// remove ////////////////////////////////

	//////////////////////////////// iterators ////////////////////////////////

	//////////////////////////////// view ////////////////////////////////

	//////////////////////////////// other ////////////////////////////////

	//and now for the sections for *this* class: (some of them are empty though)

	//////////////////////////////// get ////////////////////////////////

	//////////////////////////////// set ////////////////////////////////

	@Override
	public E set(int index, E element, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double setWeight(int index, double weight) {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// contains ////////////////////////////////

	//////////////////////////////// size/empty ////////////////////////////////

	//////////////////////////////// add ////////////////////////////////

	@Override
	public boolean add(E element, double weight) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element, double weight) {
		throw new UnsupportedOperationException();
	}

	//////////////////////////////// remove ////////////////////////////////

	//////////////////////////////// iterators ////////////////////////////////

	@Override
	public WeightedIterator<E> iterator() {
		return this.new Itr();
	}

	@Override
	public WeightedListIterator<E> listIterator() {
		return this.new ListItr(0);
	}

	@Override
	public WeightedListIterator<E> listIterator(int index) {
		this.checkIndexForAdd(index);
		return this.new ListItr(index);
	}

	//////////////////////////////// view ////////////////////////////////

	@Override
	public IRandomList<E> subList(int fromIndex, int toIndex) {
		this.checkBoundsForSubList(fromIndex, toIndex);
		return (
			this instanceof RandomAccess
			? this.new RandomAccessSubList(fromIndex, toIndex)
			: this.new SubList(fromIndex, toIndex)
		);
	}

	//////////////////////////////// other ////////////////////////////////

	@Override
	public String toString() {
		return this.defaultToString();
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass") //checked by defaultEquals()
	public boolean equals(Object o) {
		return this.defaultEquals(o);
	}

	@Override
	public int hashCode() {
		return this.defaultHashCode();
	}

	public void checkIndex(int index) {
		if (index < 0 || index >= this.size()) {
			throw new IndexOutOfBoundsException(this.outOfBoundsMessage(index));
		}
	}

	public void checkIndexForAdd(int index) {
		if (index < 0 || index > this.size()) {
			throw new IndexOutOfBoundsException(this.outOfBoundsMessage(index));
		}
	}

	public String outOfBoundsMessage(int index) {
		return "Index: " + index + ", Size: " + this.size();
	}

	public void checkBoundsForSubList(int fromIndex, int toIndex) {
		if (fromIndex < 0) {
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		}
		if (toIndex > this.size()) {
			throw new IndexOutOfBoundsException("toIndex = " + toIndex + ", size: " + this.size());
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex (" + fromIndex + ") > toIndex (" + toIndex + ')');
		}
	}

	//AbstractList's implementations of these classes are either private or package-private.
	//as such, I can't extend them. so... copy-paste time.

	public class Itr implements WeightedIterator<E> {

		//Index of element to be returned by subsequent call to next.
		public int cursor = 0;

		//Index of element returned by most recent call to next or previous.
		//Reset to -1 if this element is deleted by a call to remove.
		public int lastRet = -1;

		//The modCount value that the iterator believes that the backing List should have.
		//If this expectation is violated, the iterator has detected concurrent modification.
		public int expectedModCount = AbstractRandomList.this.modCount;

		@Override
		public boolean hasNext() {
			return this.cursor != AbstractRandomList.this.size();
		}

		@Override
		public E next() {
			this.checkForComodification();
			try {
				int i = this.cursor;
				E next = AbstractRandomList.this.get(i);
				this.lastRet = i;
				this.cursor = i + 1;
				return next;
			}
			catch (IndexOutOfBoundsException e) {
				this.checkForComodification();
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			if (this.lastRet < 0) throw new IllegalStateException();
			this.checkForComodification();

			try {
				AbstractRandomList.this.remove(this.lastRet);
				if (this.lastRet < this.cursor) this.cursor--;
				this.lastRet = -1;
				this.expectedModCount = AbstractRandomList.this.modCount;
			}
			catch (IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public double getWeight() {
			int lastRet = this.lastRet;
			if (lastRet < 0) throw new IllegalStateException();
			this.checkForComodification();
			return AbstractRandomList.this.getWeight(lastRet);
		}

		public void checkForComodification() {
			if (AbstractRandomList.this.modCount != this.expectedModCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public class ListItr extends Itr implements WeightedListIterator<E> {

		public ListItr(int index) {
			this.cursor = index;
		}

		@Override
		public boolean hasPrevious() {
			return this.cursor != 0;
		}

		@Override
		public E previous() {
			this.checkForComodification();
			try {
				int i = this.cursor - 1;
				E previous = AbstractRandomList.this.get(i);
				this.lastRet = this.cursor = i;
				return previous;
			}
			catch (IndexOutOfBoundsException e) {
				this.checkForComodification();
				throw new NoSuchElementException();
			}
		}

		@Override
		public int nextIndex() {
			return this.cursor;
		}

		@Override
		public int previousIndex() {
			return this.cursor - 1;
		}

		@Override
		public void set(E e) {
			if (this.lastRet < 0) throw new IllegalStateException();
			this.checkForComodification();

			try {
				AbstractRandomList.this.set(this.lastRet, e);
				this.expectedModCount = AbstractRandomList.this.modCount;
			}
			catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public void setWeight(double weight) {
			if (this.lastRet < 0) throw new IllegalStateException();
			this.checkForComodification();

			try {
				AbstractRandomList.this.setWeight(this.lastRet, weight);
				this.expectedModCount = AbstractRandomList.this.modCount;
			}
			catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}

		@Override
		public void add(E e) {
			this.checkForComodification();

			try {
				int i = this.cursor;
				AbstractRandomList.this.add(i, e);
				this.lastRet = -1;
				this.cursor = i + 1;
				this.expectedModCount = AbstractRandomList.this.modCount;
			}
			catch (IndexOutOfBoundsException ex) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public class SubList extends AbstractRandomList<E> {

		public final int offset;
		public int size;

		public SubList(int fromIndex, int toIndex) {
			this.offset = fromIndex;
			this.size = toIndex - fromIndex;
			this.modCount = AbstractRandomList.this.modCount;
		}

		//////////////////////////////// get ////////////////////////////////

		@Override
		public E get(int index) {
			this.checkIndex(index);
			this.checkForComodification();
			return AbstractRandomList.this.get(index + this.offset);
		}

		@Override
		public double getWeight(int index) {
			this.checkIndex(index);
			this.checkForComodification();
			return AbstractRandomList.this.getWeight(index + this.offset);
		}

		//////////////////////////////// set ////////////////////////////////

		@Override
		public E set(int index, E element) {
			this.checkIndex(index);
			this.checkForComodification();
			return AbstractRandomList.this.set(index + this.offset, element);
		}

		@Override
		public E set(int index, E element, double weight) {
			this.checkIndex(index);
			this.checkForComodification();
			return AbstractRandomList.this.set(index + this.offset, element, weight);
		}

		@Override
		public double setWeight(int index, double weight) {
			this.checkIndex(index);
			this.checkForComodification();
			return AbstractRandomList.this.setWeight(index + this.offset, weight);
		}

		//////////////////////////////// contains ////////////////////////////////

		//////////////////////////////// size/empty ////////////////////////////////

		@Override
		public int size() {
			this.checkForComodification();
			return this.size;
		}

		//////////////////////////////// add ////////////////////////////////

		@Override
		public void add(int index, E element) {
			this.checkIndexForAdd(index);
			this.checkForComodification();
			AbstractRandomList.this.add(index + this.offset, element);
			this.modCount = AbstractRandomList.this.modCount;
			this.size++;
		}

		@Override
		public boolean add(E element, double weight) {
			this.checkForComodification();
			AbstractRandomList.this.add(this.size + this.offset, element, weight);
			return true;
		}

		@Override
		public void add(int index, E element, double weight) {
			this.checkIndex(index);
			this.checkForComodification();
			AbstractRandomList.this.add(index + this.offset, element, weight);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			return this.addAll(this.size, c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			this.checkIndexForAdd(index);
			int cSize = c.size();
			if (cSize == 0) return false;

			this.checkForComodification();
			AbstractRandomList.this.addAll(this.offset + index, c);
			this.modCount = AbstractRandomList.this.modCount;
			this.size += cSize;
			return true;
		}

		//////////////////////////////// remove ////////////////////////////////

		@Override
		public E remove(int index) {
			this.checkIndex(index);
			this.checkForComodification();
			E result = AbstractRandomList.this.remove(index + this.offset);
			this.modCount = AbstractRandomList.this.modCount;
			this.size--;
			return result;
		}

		@Override
		public void removeRange(int fromIndex, int toIndex) {
			this.checkForComodification();
			AbstractRandomList.this.removeRange(fromIndex + this.offset, toIndex + this.offset);
			this.modCount = AbstractRandomList.this.modCount;
			this.size -= (toIndex - fromIndex);
		}

		//////////////////////////////// iterators ////////////////////////////////

		@Override
		public WeightedIterator<E> iterator() {
			return this.listIterator();
		}

		@Override
		public WeightedListIterator<E> listIterator(final int index) {
			this.checkForComodification();
			this.checkIndexForAdd(index);
			WeightedListIterator<E> delegate = AbstractRandomList.this.listIterator(index + SubList.this.offset);
			return new WeightedListIterator<E>() {

				@Override
				public boolean hasNext() {
					return this.nextIndex() < SubList.this.size;
				}

				@Override
				public E next() {
					if (this.hasNext()) return delegate.next();
					else throw new NoSuchElementException();
				}

				@Override
				public double getWeight() {
					return delegate.getWeight();
				}

				@Override
				public boolean hasPrevious() {
					return this.previousIndex() >= 0;
				}

				@Override
				public E previous() {
					if (this.hasPrevious()) return delegate.previous();
					else throw new NoSuchElementException();
				}

				@Override
				public int nextIndex() {
					return delegate.nextIndex() - SubList.this.offset;
				}

				@Override
				public int previousIndex() {
					return delegate.previousIndex() - SubList.this.offset;
				}

				@Override
				public void remove() {
					delegate.remove();
					SubList.this.modCount = AbstractRandomList.this.modCount;
					SubList.this.size--;
				}

				@Override
				public void set(E e) {
					delegate.set(e);
				}

				@Override
				public void setWeight(double weight) {
					delegate.setWeight(weight);
				}

				@Override
				public void add(E e) {
					delegate.add(e);
					SubList.this.modCount = AbstractRandomList.this.modCount;
					SubList.this.size++;
				}
			};
		}

		//////////////////////////////// view ////////////////////////////////

		@Override
		public IRandomList<E> subList(int fromIndex, int toIndex) {
			this.checkBoundsForSubList(fromIndex, toIndex);
			return AbstractRandomList.this.subList(this.offset + fromIndex, this.offset + toIndex);
		}

		//////////////////////////////// other ////////////////////////////////

		public void checkForComodification() {
			if (this.modCount != AbstractRandomList.this.modCount) {
				throw new ConcurrentModificationException();
			}
		}
	}

	public class RandomAccessSubList extends SubList implements RandomAccessRandomList<E> {

		public RandomAccessSubList(int fromIndex, int toIndex) {
			super(fromIndex, toIndex);
		}
	}
}