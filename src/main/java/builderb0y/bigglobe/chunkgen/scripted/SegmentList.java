package builderb0y.bigglobe.chunkgen.scripted;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

/** note: minY and maxY are INCLUSIVE. */
public abstract class SegmentList<T_Segment extends SegmentList.Segment> extends ObjectArrayList<T_Segment> {

	public static final boolean ASSERTS = false;

	public int minY, maxY;

	public SegmentList(int minY, int maxY) {
		if (maxY < minY) throw new IllegalArgumentException("maxY (" + maxY + ") must be greater than or equal to minY (" + minY + ')');
		this.minY = minY;
		this.maxY = maxY;
	}

	public void retainFrom(SegmentList<?> that) {
		if (this.isEmpty()) {
			//nothing to do.
		}
		else if (that.isEmpty()) {
			this.clear();
		}
		else {
			Segment segment = that.get(0);
			if (segment.minY > this.minY) {
				this.removeSegment(this.minY, segment.minY - 1);
			}
			for (int index = 1, size = that.size(); index < size; index++) {
				Segment next = that.get(index);
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
			Segment segment = that.get(0);
			int start = segment.minY;
			int end = segment.maxY;
			for (int index = 0, size = that.size(); index < size; index++) {
				segment = that.get(index);
				if (segment.minY == end + 1) {
					end = segment.maxY;
				}
				else {
					this.removeSegment(start, end);
					start = segment.minY;
					end = segment.maxY;
				}
			}
			this.removeSegment(start, end);
		}
	}

	public void addAllSegments(SegmentList<T_Segment> that) {
		if (that.isEmpty()) {
			//nothing to do.
		}
		else if (this.isEmpty()) {
			for (int index = 0, size = that.size(); index < size; index++) {
				T_Segment segment = that.get(index);
				int minY = Math.max(segment.minY, this.minY);
				int maxY = Math.min(segment.maxY, this.maxY);
				if (maxY >= minY) {
					this.add((T_Segment)(segment.clone()));
				}
			}
		}
		else {
			for (int index = 0, size = that.size(); index < size; index++) {
				this.addSegment(that.get(index));
			}
		}
	}

	public T_Segment addSegment(T_Segment segment) {
		return this.addSegment(segment.minY, segment.maxY);
	}

	public @Nullable T_Segment addSegment(int minY, int maxY) {
		minY = Math.max(minY, this.minY);
		maxY = Math.min(maxY, this.maxY);
		T_Segment result;
		if (maxY >= minY) {
			if (this.isEmpty()) {
				this.add(result = this.newSegment(minY, maxY));
			}
			else {
				T_Segment highest = this.get(this.size() - 1);
				T_Segment lowest  = this.get(0);
				if (minY > highest.maxY) {
					//new segment is above all other segments.
					this.add(result = this.newSegment(minY, maxY));
					this.mergeAt(this.size() - 1);
				}
				else if (minY > lowest.minY) {
					if (maxY >= highest.maxY) {
						//new segment contains highest.maxY only.
						int index = this.getSegmentIndex(minY, false);
						T_Segment segment = this.get(index);
						if (segment.minY < minY) {
							segment.maxY = minY - 1;
							index++;
						}
						if (index < this.size()) this.size(index);
						this.add(result = this.newSegment(minY, maxY));
						this.mergeAt(this.size() - 1);
					}
					else {
						//new segment is in the middle of all other segments.
						int minIndex = this.getSegmentIndex(minY, false);
						int maxIndex = this.getSegmentIndex(maxY, true);
						if (maxIndex < minIndex) {
							assert maxIndex == minIndex - 1;
							//new segment is between 2 other segments.
							this.add(minIndex, result = this.newSegment(minY, maxY));
							this.mergeAt(minIndex);
						}
						else if (maxIndex == minIndex) {
							//new segment is inside another segment.
							T_Segment segment = this.get(minIndex);

							if (minY <= segment.minY) {
								if (maxY >= segment.maxY) {
									//new segment contains existing segment.
									this.set(minIndex, result = this.newSegment(minY, maxY));
									this.mergeAt(minIndex);
								}
								else {
									//new segment covers the bottom of an existing segment.
									segment.minY = maxY + 1;
									this.add(minIndex, result = this.newSegment(minY, maxY));
									this.mergeAt(minIndex);
								}
							}
							else {
								if (maxY >= segment.maxY) {
									//new segment covers the top of an existing segment.
									segment.maxY = minY - 1;
									this.add(minIndex + 1, result = this.newSegment(minY, maxY));
									this.mergeAt(minIndex + 1);
								}
								else {
									//new segment is completely inside existing segment.
									@SuppressWarnings("unchecked")
									T_Segment clone = (T_Segment)(segment.clone());
									segment.maxY = minY - 1;
									clone.minY = maxY + 1;
									this.addAll(minIndex + 1, List.of(
										result = this.newSegment(minY, maxY),
										clone
									));
									this.mergeAt(minIndex + 1);
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							T_Segment lowSegment = this.get(minIndex);
							T_Segment highSegment = this.get(maxIndex);
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
							this.add(minIndex, result = this.newSegment(minY, maxY));
							this.mergeAt(minIndex);
						}
					}
				}
				else {
					if (maxY >= highest.maxY) {
						//segment contains all other segments.
						this.clear();
						this.add(result = this.newSegment(minY, maxY));
					}
					else if (maxY >= lowest.minY) {
						//segment contains bottom of list (and possibly the middle segments).
						int index = this.getSegmentIndex(maxY, true);
						T_Segment segment = this.get(index);
						if (segment.maxY > maxY) {
							segment.minY = maxY + 1;
							index--;
						}
						if (index >= 0) this.removeElements(0, index + 1);
						this.add(0, result = this.newSegment(minY, maxY));
						this.mergeAt(0);
					}
					else {
						//segment is below all other segments.
						this.add(0, result = this.newSegment(minY, maxY));
						this.mergeAt(0);
					}
				}
			}
			if (ASSERTS) this.checkIntegrity();
		}
		else {
			result = null;
		}
		return result;
	}

	public void removeSegment(int minY, int maxY) {
		minY = Math.max(minY, this.minY);
		maxY = Math.min(maxY, this.maxY);
		if (maxY >= minY) {
			if (this.isEmpty()) {
				//no segments to remove.
			}
			else {
				T_Segment highest = this.get(this.size() - 1);
				T_Segment lowest  = this.get(0);
				if (minY > highest.maxY) {
					//new segment is above all other segments.
					//nothing to do in this case.
				}
				else if (minY > lowest.minY) {
					if (maxY >= highest.maxY) {
						//new segment contains highest.maxY only.
						int index = this.getSegmentIndex(minY, false);
						T_Segment segment = this.get(index);
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
							T_Segment segment = this.get(minIndex);

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
									@SuppressWarnings("unchecked")
									T_Segment clone = (T_Segment)(segment.clone());
									segment.maxY = minY - 1;
									clone.minY = maxY + 1;
									this.add(minIndex + 1, clone);
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							T_Segment lowSegment = this.get(minIndex);
							T_Segment highSegment = this.get(maxIndex);
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
						T_Segment segment = this.get(index);
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
		T_Segment current = this.get(index);
		T_Segment other;
		if (index + 1 < this.size() && (other = this.get(index + 1)).minY == current.maxY + 1 && other.canMergeWith(current)) {
			current.maxY = other.maxY;
			this.remove(index + 1);
		}
		if (index - 1 >= 0 && (other = this.get(index - 1)).maxY == current.minY - 1 && other.canMergeWith(current)) {
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
				T_Segment lowSegment = this.get(index);
				assert lowSegment.maxY >= lowSegment.minY;
				if (index + 1 < size) {
					T_Segment highSegment = this.get(index + 1);
					assert highSegment.minY > lowSegment.maxY;
					assert highSegment.minY != lowSegment.maxY + 1 || !highSegment.canMergeWith(lowSegment);
				}
			}
		}
	}

	public int getSegmentIndex(int y, boolean low) {
		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			T_Segment segment = this.get(midIndex);
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

	public @Nullable T_Segment getOverlappingSegment(int y) {
		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			T_Segment segment = this.get(midIndex);
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
		T_Segment end;
		if (y < (end = this.get(0)).minY) return top ? end.minY - 1 : default_;
		if (y > (end = this.get(this.size() - 1)).maxY) return top ? default_ : end.maxY + 1;

		int minIndex = 0, maxIndex = this.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			T_Segment segment = this.get(midIndex);
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

	public abstract T_Segment newSegment(int minY, int maxY);

	public static abstract class Segment implements Cloneable {

		public int minY, maxY;

		public Segment(int minY, int maxY) {
			this.minY = minY;
			this.maxY = maxY;
		}

		public abstract boolean canMergeWith(Segment that);

		@Override
		public Segment clone() {
			try {
				return (Segment)(super.clone());
			}
			catch (CloneNotSupportedException exception) {
				throw new AssertionError(exception);
			}
		}

		@Override
		public String toString() {
			return "[" + this.minY + ", " + this.maxY + "]";
		}
	}
}