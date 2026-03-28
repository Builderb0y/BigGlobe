package builderb0y.bigglobe.chunkgen.scripted;

import java.util.function.IntFunction;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractObjectSegmentList<T_Value, T_Segment extends AbstractObjectSegmentList.ObjectSegment<T_Value>> extends SegmentList<T_Segment> {

	public AbstractObjectSegmentList(int minY, int maxY) {
		super(minY, maxY);
	}

	public T_Value[] flatten(IntFunction<T_Value[]> arrayConstructor) {
		int arraySize = this.maxY - this.minY;
		//worst case scenario: Integer.MAX_VALUE - Integer.MIN_VALUE = -1.
		//adding 1 would make this 0 again, and the overflow would become undetectable.
		//that's why we have to split this into 2 conditions.
		if (arraySize < 0 || ++arraySize < 0) {
			throw new OutOfMemoryError("SegmentList covers too big of a Y range for flattening.");
		}
		T_Value[] array = arrayConstructor.apply(arraySize);
		for (int segmentIndex = 0, size = this.size(); segmentIndex < size; segmentIndex++) {
			T_Segment segment = this.get(segmentIndex);
			int minIndex = segment.minY - this.minY;
			int maxIndex = segment.maxY - this.minY;
			T_Value object = segment.value;
			for (int objectIndex = minIndex; objectIndex <= maxIndex; objectIndex++) {
				array[objectIndex] = object;
			}
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	public static <T_Segment> T_Segment segment(Object[] array, int index) {
		return (T_Segment)(array[index]);
	}

	public void fillEmptySpace(T_Value object) {
		if (this.isEmpty()) {
			this.add(this.newSegment(this.minY, this.maxY, object));
		}
		else {
			int size = this.size();
			Object[] oldArray = this.a;
			Object[] newArray = new Object[(size << 1) | 1];
			int readIndex = 0, writeIndex = 0;
			T_Segment segment = segment(oldArray, 0);
			if (segment.minY > this.minY) {
				newArray[writeIndex++] = this.newSegment(this.minY, segment.minY - 1, object);
			}
			while (true) {
				T_Segment lowSegment = segment(oldArray, readIndex++);
				newArray[writeIndex++] = lowSegment;
				if (readIndex < size) {
					T_Segment highSegment = segment(oldArray, readIndex);
					if (highSegment.minY != lowSegment.maxY + 1) {
						newArray[writeIndex++] = this.newSegment(lowSegment.maxY + 1, highSegment.minY - 1, object);
					}
				}
				else {
					if (this.maxY != lowSegment.maxY + 1) {
						newArray[writeIndex++] = this.newSegment(lowSegment.maxY + 1, this.maxY, object);
					}
					break;
				}
			}
			((ObjectSegmentList)(this)).a = newArray;
			this.size = writeIndex;
		}
		if (ASSERTS) this.checkIntegrity();
	}

	public @Nullable T_Segment addSegment(int minY, int maxY, T_Value object) {
		T_Segment segment = this.addSegment(minY, maxY);
		if (segment != null) segment.value = object;
		return segment;
	}

	@Override
	public T_Segment addSegment(T_Segment segment) {
		T_Segment result = super.addSegment(segment);
		if (result != null) result.value = segment.value;
		return result;
	}

	public @Nullable T_Value getOverlappingObject(int y) {
		T_Segment segment = this.getOverlappingSegment(y);
		return segment != null ? segment.value : null;
	}

	public T_Segment newSegment(int minY, int maxY, T_Value value) {
		T_Segment segment = this.newSegment(minY, maxY);
		segment.value = value;
		return segment;
	}

	public static class ObjectSegment<T> extends Segment {

		public T value;

		public ObjectSegment(int minY, int maxY) {
			super(minY, maxY);
		}

		@Override
		public boolean canMergeWith(Segment that) {
			return this.value == ((ObjectSegment<?>)(that)).value;
		}

		@Override
		public String toString() {
			return super.toString() + ": " + this.value;
		}
	}
}