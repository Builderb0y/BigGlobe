package builderb0y.bigglobe.chunkgen.scripted;

import java.util.List;
import java.util.function.IntFunction;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;

/** note: minY and maxY are INCLUSIVE. */
public class SegmentList<T> extends ObjectArrayList<Segment<T>> {

	public static final boolean ASSERTS = false;

	public int minY, maxY;

	public SegmentList(int minY, int maxY) {
		if (maxY < minY) throw new IllegalArgumentException("maxY (" + maxY + ") must be greater than or equal to minY (" + minY + ')');
		this.minY = minY;
		this.maxY = maxY;
	}

	public T[] flatten(IntFunction<T[]> arrayConstructor) {
		int arraySize = this.maxY - this.minY;
		//worst case scenario: Integer.MAX_VALUE - Integer.MIN_VALUE = -1.
		//adding 1 would make this 0 again, and the overflow would become undetectable.
		//that's why we have to split this into 2 conditions.
		if (arraySize < 0 || ++arraySize < 0) {
			throw new OutOfMemoryError("SegmentList covers too big of a Y range for flattening.");
		}
		T[] array = arrayConstructor.apply(arraySize);
		for (int segmentIndex = 0, size = this.size(); segmentIndex < size; segmentIndex++) {
			Segment<T> segment = this.get(segmentIndex);
			int minIndex = segment.minY - this.minY;
			int maxIndex = segment.maxY - this.minY;
			T object = segment.value;
			for (int objectIndex = minIndex; objectIndex <= maxIndex; objectIndex++) {
				array[objectIndex] = object;
			}
		}
		return array;
	}

	public void fillEmptySpace(T object) {
		if (this.isEmpty()) {
			this.add(this.newSegment(this.minY, this.maxY, object));
		}
		else {
			int size = this.size();
			Object[] oldArray = this.a;
			Object[] newArray = new Object[(size << 1) | 1];
			int readIndex = 0, writeIndex = 0;
			Segment<T> segment = segment(oldArray, 0);
			if (segment.minY > this.minY) {
				newArray[writeIndex++] = this.newSegment(this.minY, segment.minY - 1, object);
			}
			while (true) {
				Segment<T> lowSegment = segment(oldArray, readIndex++);
				newArray[writeIndex++] = lowSegment;
				if (readIndex < size) {
					Segment<T> highSegment = segment(oldArray, readIndex);
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
			((SegmentList)(this)).a = newArray;
			this.size = writeIndex;
		}
		if (ASSERTS) this.checkIntegrity();
	}

	public void retainFrom(SegmentList<?> that) {
		if (this.isEmpty()) {
			//nothing to do.
		}
		else if (that.isEmpty()) {
			this.clear();
		}
		else {
			Segment<?> segment = that.get(0);
			if (segment.minY > this.minY) {
				this.removeSegment(this.minY, segment.minY - 1);
			}
			for (int index = 1, size = that.size(); index < size; index++) {
				Segment<?> next = that.get(index);
				if (next.minY != segment.maxY + 1) {
					this.removeSegment(segment.maxY + 1, next.minY - 1);
				}
				segment = next;
			}
			if (segment.maxY < this.maxY) {
				this.removeSegment(segment.maxY + 1, this.maxY);
			}
		}
	}

	public void removeFrom(SegmentList<?> that) {
		if (this.isEmpty() || that.isEmpty()) {
			//nothing to do.
		}
		else {
			Segment<?> segment = that.get(0);
			int start = segment.minY;
			int end = segment.maxY;
			boolean mergedLast = false;
			for (int index = 1, size = that.size(); index < size; index++) {
				segment = that.get(index);
				//noinspection AssignmentUsedAsCondition
				if (mergedLast = (segment.minY == end + 1)) {
					end = segment.maxY;
				}
				else {
					this.removeSegment(start, end);
					start = segment.minY;
					end = segment.maxY;
				}
			}
			if (mergedLast) {
				this.removeSegment(start, end);
			}
		}
	}

	public void addAllSegments(SegmentList<T> that) {
		if (that.isEmpty()) {
			//nothing to do.
		}
		else if (this.isEmpty()) {
			for (int index = 0, size = that.size(); index < size; index++) {
				Segment<T> segment = that.get(index);
				int minY = Math.max(segment.minY, this.minY);
				int maxY = Math.min(segment.maxY, this.maxY);
				if (maxY >= minY) {
					this.add(this.newSegment(segment.minY, segment.maxY, segment.value));
				}
			}
		}
		else {
			for (int index = 0, size = that.size(); index < size; index++) {
				Segment<T> segment = that.get(index);
				int minY = Math.max(segment.minY, this.minY);
				int maxY = Math.min(segment.maxY, this.maxY);
				if (maxY >= minY) {
					this.addSegment(segment.minY, segment.maxY, segment.value);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Segment<T> segment(Object[] array, int index) {
		return (Segment<T>)(array[index]);
	}

	public void addSegment(int minY, int maxY, T object) {
		minY = Math.max(minY, this.minY);
		maxY = Math.min(maxY, this.maxY);
		if (maxY >= minY) {
			if (this.isEmpty()) {
				this.add(this.newSegment(minY, maxY, object));
			}
			else {
				Segment<T> highest = this.get(this.size() - 1);
				Segment<T> lowest  = this.get(0);
				if (minY > highest.maxY) {
					//new segment is above all other segments.
					this.add(this.newSegment(minY, maxY, object));
					this.mergeAt(this.size() - 1);
				}
				else if (minY > lowest.minY) {
					if (maxY >= highest.maxY) {
						//new segment contains highest.maxY only.
						int index = this.getSegmentIndex(minY, false);
						Segment<T> segment = this.get(index);
						if (segment.minY < minY) {
							segment.maxY = minY - 1;
							index++;
						}
						if (index < this.size()) this.size(index);
						this.add(this.newSegment(minY, maxY, object));
						this.mergeAt(this.size() - 1);
					}
					else {
						//new segment is in the middle of all other segments.
						int minIndex = this.getSegmentIndex(minY, false);
						int maxIndex = this.getSegmentIndex(maxY, true);
						if (maxIndex < minIndex) {
							assert maxIndex == minIndex - 1;
							//new segment is between 2 other segments.
							this.add(minIndex, this.newSegment(minY, maxY, object));
							this.mergeAt(minIndex);
						}
						else if (maxIndex == minIndex) {
							//new segment is inside another segment.
							Segment<T> segment = this.get(minIndex);

							if (minY <= segment.minY) {
								if (maxY >= segment.maxY) {
									//new segment contains existing segment.
									segment.minY = minY;
									segment.maxY = maxY;
									segment.value = object;
									this.mergeAt(minIndex);
								}
								else {
									//new segment covers the bottom of an existing segment.
									segment.minY = maxY + 1;
									this.add(minIndex, this.newSegment(minY, maxY, object));
									this.mergeAt(minIndex);
								}
							}
							else {
								if (maxY >= segment.maxY) {
									//new segment covers the top of an existing segment.
									segment.maxY = minY - 1;
									this.add(minIndex + 1, this.newSegment(minY, maxY, object));
									this.mergeAt(minIndex + 1);
								}
								else {
									//new segment is completely inside existing segment.
									int oldMaxY = segment.maxY;
									segment.maxY = minY - 1;
									this.addAll(minIndex + 1, List.of(
										this.newSegment(minY, maxY, object),
										this.newSegment(maxY + 1, oldMaxY, segment.value)
									));
									this.mergeAt(minIndex + 1);
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							Segment<T> lowSegment = this.get(minIndex);
							Segment<T> highSegment = this.get(maxIndex);
							if (lowSegment.minY < minY) {
								//partial intersection.
								lowSegment.maxY = minY - 1;
								minIndex++;
							}
							if (highSegment.maxY > maxY) {
								//partial intersection.
								highSegment.minY = maxY + 1;
								maxIndex--;
							}
							if (maxIndex >= minIndex) this.removeElements(minIndex, maxIndex + 1 /* convert to exclusive */);
							this.add(minIndex, this.newSegment(minY, maxY, object));
							this.mergeAt(minIndex);
						}
					}
				}
				else {
					if (maxY >= highest.maxY) {
						//segment contains all other segments.
						this.clear();
						this.add(this.newSegment(minY, maxY, object));
					}
					else if (maxY >= lowest.minY) {
						//segment contains lowest.minY only.
						int index = this.getSegmentIndex(maxY, true);
						Segment<T> segment = this.get(index);
						if (segment.maxY > maxY) {
							segment.minY = maxY + 1;
							index--;
						}
						if (index >= 0) this.removeElements(0, index + 1);
						this.add(0, this.newSegment(minY, maxY, object));
						this.mergeAt(0);
					}
					else {
						//segment is below all other segments.
						this.add(0, this.newSegment(minY, maxY, object));
						this.mergeAt(0);
					}
				}
			}
			if (ASSERTS) this.checkIntegrity();
		}
	}

	public void removeSegment(int minY, int maxY) {
		minY = Math.max(minY, this.minY);
		maxY = Math.min(maxY, this.maxY);
		if (maxY >= minY) {
			if (this.isEmpty()) {
				//no segments to remove.
			}
			else {
				Segment<T> highest = this.get(this.size() - 1);
				Segment<T> lowest  = this.get(0);
				if (minY > highest.maxY) {
					//new segment is above all other segments.
					//nothing to do in this case.
				}
				else if (minY > lowest.minY) {
					if (maxY >= highest.maxY) {
						//new segment contains highest.maxY only.
						int index = this.getSegmentIndex(minY, false);
						Segment<T> segment = this.get(index);
						if (segment.minY < minY) {
							segment.maxY = minY - 1;
							index++;
						}
						if (index < this.size()) this.size(index);
					}
					else {
						//new segment is in the middle of all other segments.
						int minIndex = this.getSegmentIndex(minY, false);
						int maxIndex = this.getSegmentIndex(maxY, true);
						if (maxIndex < minIndex) {
							assert maxIndex == minIndex - 1;
							//new segment is between 2 other segments.
						}
						else if (maxIndex == minIndex) {
							//new segment is inside another segment.
							Segment<T> segment = this.get(minIndex);

							if (minY <= segment.minY) {
								if (maxY >= segment.maxY) {
									//new segment contains existing segment.
									this.remove(minIndex);
								}
								else {
									//new segment covers the bottom of an existing segment.
									segment.minY = maxY + 1;
								}
							}
							else {
								if (maxY >= segment.maxY) {
									//new segment covers the top of an existing segment.
									segment.maxY = minY - 1;
								}
								else {
									//new segment is completely inside existing segment.
									int oldMaxY = segment.maxY;
									segment.maxY = minY - 1;
									this.add(minIndex + 1, this.newSegment(maxY + 1, oldMaxY, segment.value));
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							Segment<T> lowSegment = this.get(minIndex);
							Segment<T> highSegment = this.get(maxIndex);
							if (lowSegment.minY < minY) {
								//partial intersection.
								lowSegment.maxY = minY - 1;
								minIndex++;
							}
							if (highSegment.maxY > maxY) {
								//partial intersection.
								highSegment.minY = maxY + 1;
								maxIndex--;
							}
							if (maxIndex >= minIndex) this.removeElements(minIndex, maxIndex + 1 /* convert to exclusive */);
						}
					}
				}
				else {
					if (maxY >= highest.maxY) {
						//segment contains all other segments.
						this.clear();
					}
					else if (maxY >= lowest.minY) {
						//segment contains lowest.minY only.
						int index = this.getSegmentIndex(maxY, true);
						Segment<T> segment = this.get(index);
						if (segment.maxY > maxY) {
							segment.minY = maxY + 1;
							index--;
						}
						if (index >= 0) this.removeElements(0, index + 1);
					}
					else {
						//segment is below all other segments.
						//nothing to do in this case either.
					}
				}
			}
			if (ASSERTS) this.checkIntegrity();
		}
	}

	public void mergeAt(int index) {
		Segment<T> current = this.get(index);
		Segment<T> other;
		if (index + 1 < this.size() && (other = this.get(index + 1)).minY == current.maxY + 1 && other.value == current.value) {
			current.maxY = other.maxY;
			this.remove(index + 1);
		}
		if (index - 1 >= 0 && (other = this.get(index - 1)).maxY == current.minY - 1 && other.value == current.value) {
			current.minY = other.minY;
			this.remove(index - 1);
		}
	}

	@SuppressWarnings({ "AssertWithSideEffects", "ConstantValue" })
	public static void checkAssertsEnabled() {
		boolean asserts = false;
		assert asserts = true;
		if (!asserts) throw new AssertionError("asserts not enabled. run with -ea");
	}

	public void checkIntegrity() {
		checkAssertsEnabled();
		if (!this.isEmpty()) {
			assert this.get(0).minY >= this.minY;
			assert this.get(this.size() - 1).maxY <= this.maxY;
			for (int index = 0, size = this.size(); index < size; index++) {
				Segment<T> lowSegment = this.get(index);
				assert lowSegment.maxY >= lowSegment.minY;
				if (index + 1 < size) {
					Segment<T> highSegment = this.get(index + 1);
					assert highSegment.minY > lowSegment.maxY;
					assert highSegment.minY != lowSegment.maxY + 1 || highSegment.value != lowSegment.value;
				}
			}
		}
	}

	public int getSegmentIndex(int y, boolean low) {
		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			Segment<T> segment = this.get(midIndex);
			if (y < segment.minY) {
				maxIndex = midIndex - 1;
			}
			else if (y > segment.maxY) {
				minIndex = midIndex + 1;
			}
			else {
				return midIndex;
			}
		}
		return low ? maxIndex : minIndex;
	}

	public @Nullable Segment<T> getOverlappingSegment(int y) {
		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			Segment<T> segment = this.get(midIndex);
			if (y < segment.minY) {
				maxIndex = midIndex - 1;
			}
			else if (y > segment.maxY) {
				minIndex = midIndex + 1;
			}
			else {
				return segment;
			}
		}
		return null;
	}

	public int getTopOrBottomOfSegment(int y, boolean top, int default_) {
		if (this.isEmpty()) return default_;
		Segment<T> end;
		if (y < (end = this.get(0)).minY) return top ? end.minY - 1 : default_;
		if (y > (end = this.get(this.size() - 1)).maxY) return top ? default_ : end.maxY + 1;

		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			Segment<T> segment = this.get(midIndex);
			if (y < segment.minY) {
				maxIndex = midIndex - 1;
			}
			else if (y > segment.maxY) {
				minIndex = midIndex + 1;
			}
			else {
				return top ? segment.maxY : segment.minY;
			}
		}
		return top ? this.get(minIndex).minY - 1 : this.get(maxIndex).maxY + 1;
	}

	public @Nullable T getOverlappingObject(int y) {
		Segment<T> segment = this.getOverlappingSegment(y);
		return segment != null ? segment.value : null;
	}

	public Segment<T> newSegment(int minY, int maxY, T value) {
		return new Segment<>(minY, maxY, value);
	}

	public static class Segment<T> {

		public T value;
		public int minY, maxY;

		public Segment(int minY, int maxY, T value) {
			this.minY  = minY;
			this.maxY  = maxY;
			this.value = value;
		}

		@Override
		public String toString() {
			return "[" + this.minY + ", " + this.maxY + "]: " + this.value;
		}
	}
}