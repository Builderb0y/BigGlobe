package builderb0y.bigglobe.settings;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.LinkedArrayList;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

public class VoronoiDiagram2D {

	public static final int DIMENSIONS = 2;
	public static final int CACHE_SIZE = 4;

	/**
	when {@link #variation} is less than or equal to half of {@link #distance},
	it is guaranteed that every desired point will be in one of the 8 nearest cells.
	*/
	public static final byte[] ADJACENT_8 = {
		-1, -1,
		-1, +0,
		-1, +1,

		+0, -1,
		+0, +1,

		+1, -1,
		+1, +0,
		+1, +1,
	};

	/**
	when {@link #variation} is greater than half of {@link #distance},
	it is guaranteed that every desired point will be in one of the 20 nearest cells.
	*/
	public static final byte[] ADJACENT_20 = {
		-2, -1,
		-2, +0,
		-2, +1,

		-1, -2,
		-1, -1,
		-1, +0,
		-1, +1,
		-1, +2,

		+0, -2,
		+0, -1,
		+0, +1,
		+0, +2,

		+1, -2,
		+1, -1,
		+1, +0,
		+1, +1,
		+1, +2,

		+2, -1,
		+2, +0,
		+2, +1,
	};

	public final Seed seed;
	public final @VerifyIntRange(min = 0, minInclusive = false) int distance;
	public final @VerifyIntRange(min = 0, minInclusive = false) @VerifySorted(lessThanOrEqual = "distance") int variation;

	public final transient Cell[] cellCache = new Cell[CACHE_SIZE];

	public VoronoiDiagram2D(Seed seed, int distance, int variation) {
		this.seed = seed;
		this.distance = distance;
		this.variation = variation;
	}

	public int getCenterX(int cellX, int cellZ) {
		return cellX * this.distance + Permuter.nextBoundedInt(Permuter.permute(this.seed.xor(0xCB38A1093F60FE7BL), cellX, cellZ), this.variation);
	}

	public int getCenterZ(int cellX, int cellZ) {
		return cellZ * this.distance + Permuter.nextBoundedInt(Permuter.permute(this.seed.xor(0x3ED5E5CB4436A416L), cellX, cellZ), this.variation);
	}

	public SeedPoint getSeedPoint(int cellX, int cellZ) {
		int centerX = this.getCenterX(cellX, cellZ);
		int centerZ = this.getCenterZ(cellX, cellZ);
		return new SeedPoint(cellX, cellZ, centerX, centerZ, this.seed.value);
	}

	public AdjacentSeedPoint getAdjacentSeedPoint(SeedPoint center, int cellX, int cellZ) {
		int centerX = this.getCenterX(cellX, cellZ);
		int centerZ = this.getCenterZ(cellX, cellZ);
		return new AdjacentSeedPoint(cellX, cellZ, centerX, centerZ, this.seed.value, center.angleTo(centerX, centerZ));
	}

	public SeedPoint getNearestSeedPoint(int blockX, int blockZ, @Nullable SeedPoint guess) {
		int centerCellX = Math.floorDiv(blockX, this.distance);
		int centerCellZ = Math.floorDiv(blockZ, this.distance);

		int closestCellX = centerCellX;
		int closestCellZ = centerCellZ;
		int closestCenterX = this.getCenterX(centerCellX, centerCellZ);
		int closestCenterZ = this.getCenterZ(centerCellX, centerCellZ);
		long closestCenterD = BigGlobeMath.squareL(closestCenterX - blockX, closestCenterZ - blockZ);
		if (guess != null) {
			long newCenterD = guess.squareDistanceTo(blockX, blockZ);
			if (newCenterD < closestCenterD) {
				closestCellX = guess.cellX;
				closestCellZ = guess.cellZ;
				closestCenterX = guess.centerX;
				closestCenterZ = guess.centerZ;
				closestCenterD = newCenterD;
			}
		}

		//search nearby seed points which are at least as close as our first seed point
		//no sense checking seed points which are guaranteed to be farther away
		double radius = Math.sqrt(closestCenterD);
		int minCellX = Math.floorDiv(floorI(blockX - radius), this.distance);
		int maxCellX = Math.floorDiv(floorI(blockX + radius), this.distance);
		int minCellZ = Math.floorDiv(floorI(blockZ - radius), this.distance);
		int maxCellZ = Math.floorDiv(floorI(blockZ + radius), this.distance);

		for (int newCellX = minCellX; newCellX <= maxCellX; newCellX++) {
			for (int newCellZ = minCellZ; newCellZ <= maxCellZ; newCellZ++) {
				if (newCellX == centerCellX && newCellZ == centerCellZ) continue;
				if (guess != null && guess.cellEquals(newCellX, newCellZ)) continue;
				int newCenterX = this.getCenterX(newCellX, newCellZ);
				int newCenterZ = this.getCenterZ(newCellX, newCellZ);
				long newCenterD = BigGlobeMath.squareL(newCenterX - blockX, newCenterZ - blockZ);
				if (newCenterD < closestCenterD) {
					closestCellX = newCellX;
					closestCellZ = newCellZ;
					closestCenterX = newCenterX;
					closestCenterZ = newCenterZ;
					closestCenterD = newCenterD;
				}
			}
		}

		if (guess != null && guess.cellEquals(closestCellX, closestCellZ)) return guess;
		else return new SeedPoint(closestCellX, closestCellZ, closestCenterX, closestCenterZ, this.seed.value);
	}

	public SeedPoint getNearestSeedPoint(int blockX, int blockZ) {
		return this.getNearestSeedPoint(blockX, blockZ, null);
	}

	/**
	adapted from https://github.com/jdiemke/delaunay-triangulator/blob/master/library/src/main/java/io/github/jdiemke/triangulation/Triangle2D.java
	the license for that method is: https://github.com/jdiemke/delaunay-triangulator/blob/master/LICENSE
	overflow-safe as much as possible, even if our distance is very large.
	this method assumes the points are ordered counter-clockwise.
	in other words, center.angleTo(left) <= center.angleTo(test) <= center.angleTo(right).
	*/
	public static boolean isInsideCircumCircle(SeedPoint center, SeedPoint left, SeedPoint test, SeedPoint right) {
		//apparently longs do not have enough scale to
		//avoid overflows for this math, so... double time!
		double a11 = center.centerX - test.centerX;
		double a21 = left  .centerX - test.centerX;
		double a31 = right .centerX - test.centerX;

		double a12 = center.centerZ - test.centerZ;
		double a22 = left  .centerZ - test.centerZ;
		double a32 = right .centerZ - test.centerZ;

		double a13 = BigGlobeMath.squareD(center.centerX - test.centerX, center.centerZ - test.centerZ);
		double a23 = BigGlobeMath.squareD(left  .centerX - test.centerX, left  .centerZ - test.centerZ);
		double a33 = BigGlobeMath.squareD(right .centerX - test.centerX, right .centerZ - test.centerZ);

		return (
			a13 * (a21 * a32 - a22 * a31) +
			a23 * (a12 * a31 - a11 * a32) +
			a33 * (a11 * a22 - a12 * a21)
		) > 0.0D;
	}

	/**
	algorithm:
	add the 20 nearest points to a list.
	why 20? because 20 is the minimum number sufficient
	to GUARANTEE that all the points we want are in there.
	however, there may be some points that we don't want too.
	so, we then remove all the points which do NOT belong in the list.
	the test for whether or not a point belongs in the list depends on its
	neighboring points in the list AFTER the list has been sorted by angle.
	as such, the act of removing a point from the list
	will also mean that its neighbors have a new neighbor.
	so, they are tested again to see if they now need to be removed too.

	example:
	A -> B -> C -> D -> E
	after C is removed, the list looks like this:
	A -> B -> D -> E
	notice: B used to have C as a neighbor, but now it has D instead.
	likewise, D now has B as a neighbor instead of C.
	it is entirely possible for isInsideCircumCircle(A, B, C) to
	return true while isInsideCircumCircle(A, B, D) returns false.
	that's why B and D need to be re-tested to see if they still belong in the list.

	after EVERY point passes the isInsideCircumCircle() check with
	its neighbors, the list is converted to a Cell, and returned.
	oh, and the list is also cyclic, so the point "before the first point" is
	the last point, and the point "after the last point" is the first point.

	things I've tried which don't work:

	just add the points that belong, so there's nothing to remove later.

	well, first you'll need to keep the list sorted every time
	it's modified. that's easy to do with a TreeSet or similar,
	but then access time becomes O(log(N)) instead of O(1), so it's
	debatable whether this actually improves performance or not.
	however, the real issue here is more subtle than that.
	imagine the reverse of the earlier example, where C is added, not removed.
	it is entirely possible for isInsideCircumCircle(A, B, D) to
	return true while isInsideCircumCircle(A, B, C) returns false.
	in other words, adding C *could* invalidate B and D.
	so there's ALWAYS something to remove at the end.

	well, maybe keep the removal stage, but now it's faster cause
	there's less points in the list which need to be tested, right?

	well, maybe, but there are still more edge cases. for example:
	start with A -> C.
	try to add B. assume this fails because isInsideCircumCircle(A, B, C) returns false.
	the list is still A -> C.
	now add D. assume this succeeds.
	the list is now A -> C -> D.
	isInsideCircumCircle(A, C, D) returns false, so C is now removed.
	the list is now A -> D.
	isInsideCircumCircle(A, B, D) returns true, so B SHOULD
	be in the list, but it was already ruled out earlier.
	so, the list will be incorrect when returned.
	*/
	public Cell getCellUncached(SeedPoint center) {
		//get all the candidate points.
		LinkedArrayList<AdjacentSeedPoint> adjacent = new LinkedArrayList<>();
		byte[] nearest = this.variation <= this.distance >> 1 ? ADJACENT_8 : ADJACENT_20;
		for (int index = 0, length = nearest.length; index < length;) {
			int newX = center.cellX + nearest[index++];
			int newZ = center.cellZ + nearest[index++];
			adjacent.addElementToEnd(this.getAdjacentSeedPoint(center, newX, newZ));
		}
		//sort the candidate points by angle.
		//a point will be removed if it is not inside the circumcircle of
		//the center point and the two adjacent points in the sorted list.
		adjacent.sortElements(Comparator.naturalOrder());

		//test all the points to see if they belong here.
		//the algorithm for this is:
		//go through the list. if the current point belongs there,
		//test the next point next. otherwise, remove the point
		//and test the previous point next. once another point
		//which belongs there is found, the next node will be tested.
		//because the list is cyclic, we need to keep track of
		//how many remaining points still need to be tested.
		//when we visit that number, we know it's time to stop testing.
		int remaining = adjacent.size();
		for (LinkedArrayList.Node<AdjacentSeedPoint> node = adjacent.getFirstNode(); remaining > 0;) {
			LinkedArrayList.Node<AdjacentSeedPoint> prev = node.prev, next = node.next;
			//cyclic list logic.
			if (prev == null) prev = adjacent.getLastNode();
			if (next == null) next = adjacent.getFirstNode();
			//the test!
			if (isInsideCircumCircle(center, prev.element, node.element, next.element)) {
				node = next;
				remaining--;
			}
			else {
				adjacent.removeNode(node);
				node = prev;
				//mark that we need to check the previous node and the next node.
				//however, if remaining >= 2, that means we were
				//already planning on checking the next node anyway,
				//and therefore remaining doesn't need to change.
				remaining = Math.max(remaining, 2);
			}
		}

		/*
		//old implementation:
		ObjectLinkedOpenHashSet<LinkedArrayList.Node<AdjacentSeedPoint>> test = new ObjectLinkedOpenHashSet<>(adjacent.nodes(), 1.0F);
		while (!test.isEmpty()) {
			LinkedArrayList.Node<AdjacentSeedPoint> node = test.removeFirst();
			LinkedArrayList.Node<AdjacentSeedPoint> prev = node.prev, next = node.next;
			//cyclic list logic
			if (prev == null) prev = adjacent.getLastNode();
			if (next == null) next = adjacent.getFirstNode();
			if (!isInsideCircumCircle(center, prev.element, node.element, next.element)) {
				//remove the point
				adjacent.removeNode(node);
				//removing a point might cause the prev and next points
				//to realize that they also don't belong here.
				//so, re-add them to test so that we can test them again.
				test.add(prev);
				test.add(next);
			}
		}
		*/
		return new Cell(center, adjacent.toElementArray(new AdjacentSeedPoint[adjacent.size()]));
	}

	public Cell getCellCached(SeedPoint center) {
		//this code is "pseudo-threadsafe". meaning that it is not synchronized or atomic,
		//and that the cache array may be in an inconsistent state at any given time.
		//HOWEVER, this method checks for these states and handles them sanely.
		//race conditions or inconsistent cache states will NOT
		//break this method or cause it to return the wrong Cell.
		Cell[] cache = this.cellCache;
		int i;
		for (i = 0; true; ++i) {
			Cell cell = cache[i];
			if (cell == null) break;
			if (cell.center.cellEquals(center)) {
				if (i != 0) {
					//move the cell to index 0 so that we can find it faster next time we need it.
					System.arraycopy(cache, 0, cache, 1, i);
					cache[0] = cell;
				}
				return cell;
			}
			//don't increment i if the result would be >= CACHE_SIZE.
			//allowing it to be >= CACHE_SIZE would break the
			//arraycopy call at the bottom of the method.
			if (i >= CACHE_SIZE - 1) break;
		}
		Cell cell = this.getCellUncached(center);
		//add new cell to index 0 so that we can find it faster next time we need it.
		if (i != 0) System.arraycopy(cache, 0, cache, 1, i);
		cache[0] = cell;
		return cell;
	}

	public Cell getCell(int cellX, int cellZ, @Nullable Cell guess) {
		return guess != null && guess.center.cellEquals(cellX, cellZ) ? guess : this.getCellCached(this.getSeedPoint(cellX, cellZ));
	}

	public Cell getCell(int cellX, int cellZ) {
		return this.getCellCached(this.getSeedPoint(cellX, cellZ));
	}

	public Cell getNearestCell(int blockX, int blockZ, @Nullable Cell guess) {
		SeedPoint guessPoint = guess != null ? guess.center : null;
		SeedPoint nearestPoint = this.getNearestSeedPoint(blockX, blockZ, guessPoint);
		return guessPoint == nearestPoint ? guess : this.getCellCached(nearestPoint);
	}

	public Cell getNearestCell(int blockX, int blockZ, @Nullable SeedPoint guess) {
		SeedPoint nearestPoint = this.getNearestSeedPoint(blockX, blockZ, guess);
		return this.getCellCached(nearestPoint);
	}

	public Cell getNearestCell(int blockX, int blockZ) {
		return this.getCellCached(this.getNearestSeedPoint(blockX, blockZ));
	}

	public static class SeedPoint {

		/** the coordinates of the square that this cell seeds from */
		public final int cellX, cellZ;
		/** the world location of the center of this cell */
		public final int centerX, centerZ;
		/** the seed of the VoronoiDiagram2D which created this SeedPoint */
		public final long seed;

		public SeedPoint(
			int cellX,
			int cellZ,
			int centerX,
			int centerZ,
			long seed
		) {
			this.cellX   = cellX;
			this.cellZ   = cellZ;
			this.centerX = centerX;
			this.centerZ = centerZ;
			this.seed    = seed;
		}

		/** permutes the seed based on this point's location. */
		public long getSeed(long salt) {
			return Permuter.permute(this.seed ^ salt, this.cellX, this.cellZ);
		}

		public long squareDistanceTo(int blockX, int blockZ) {
			return BigGlobeMath.squareL(this.centerX - blockX, this.centerZ - blockZ);
		}

		public double distanceTo(int blockX, int blockZ) {
			return Math.sqrt(this.squareDistanceTo(blockX, blockZ));
		}

		public long squareDistanceTo(SeedPoint other) {
			return this.squareDistanceTo(other.centerX, other.centerZ);
		}

		public double distanceTo(SeedPoint other) {
			return this.distanceTo(other.centerX, other.centerZ);
		}

		public double angleTo(int blockX, int blockZ) {
			return Math.atan2(blockZ - this.centerZ, blockX - this.centerX);
		}

		public double angleTo(SeedPoint other) {
			return this.angleTo(other.centerX, other.centerZ);
		}

		/**
		returns a double from 0 to 1 representing how far between this and end the block is.
		if the block is not on the line which intersects this and end,
		this method will act as if the block was first projected onto that line.
		if the block is at this, returns 0.
		if the block is at end, returns 1.
		if the block is half way between this and end, returns 0.5.
		if the block is not between this and end, returns a value outside the range 0 to 1.
		if end IS this, returns NaN.
		*/
		public double progressToD(SeedPoint end, int blockX, int blockZ) {
			long blockOffsetX = blockX - this.centerX;
			long blockOffsetZ = blockZ - this.centerZ;
			long cellOffsetX = end.centerX - this.centerX;
			long cellOffsetZ = end.centerZ - this.centerZ;
			double dotProduct = blockOffsetX * cellOffsetX + blockOffsetZ * cellOffsetZ;
			double factor = BigGlobeMath.squareL(cellOffsetX, cellOffsetZ);
			return dotProduct / factor;
		}

		public float progressToF(SeedPoint end, int blockX, int blockZ) {
			long blockOffsetX = blockX - this.centerX;
			long blockOffsetZ = blockZ - this.centerZ;
			long cellOffsetX = end.centerX - this.centerX;
			long cellOffsetZ = end.centerZ - this.centerZ;
			float dotProduct = blockOffsetX * cellOffsetX + blockOffsetZ * cellOffsetZ;
			float factor = BigGlobeMath.squareL(cellOffsetX, cellOffsetZ);
			return dotProduct / factor;
		}

		@Override
		public String toString() {
			return this.getClass().getName() + ": { cell: " + this.cellX + ", " + this.cellZ + ", center: " + this.centerX + ", " + this.centerZ + ", seed: " + Long.toHexString(this.seed) + " }";
		}

		public boolean cellEquals(int cellX, int cellZ) {
			return this.cellX == cellX && this.cellZ == cellZ;
		}

		public boolean cellEquals(SeedPoint that) {
			return this.cellEquals(that.cellX, that.cellZ);
		}

		public boolean centerEquals(int centerX, int centerZ) {
			return this.centerX == centerX && this.centerZ == centerZ;
		}

		public boolean centerEquals(SeedPoint that) {
			return this.centerEquals(that.centerX, that.centerZ);
		}
	}

	/**
	used internally by {@link #getCellUncached(SeedPoint)}, because points need to be sorted by angle.
	since sort() will *probably* need the angle for any given point more than once,
	we cache it here to avoid unnecessary {@link Math#atan2(double, double)} calls.
	*/
	public static class AdjacentSeedPoint extends SeedPoint implements Comparable<AdjacentSeedPoint> {

		public final double angleFromCenter;

		public AdjacentSeedPoint(
			int cellX,
			int cellZ,
			int centerX,
			int centerZ,
			long seed,
			double angleFromCenter
		) {
			super(cellX, cellZ, centerX, centerZ, seed);
			this.angleFromCenter = angleFromCenter;
		}

		@Override
		public int compareTo(AdjacentSeedPoint that) {
			return Double.compare(this.angleFromCenter, that.angleFromCenter);
		}

		@Override
		public String toString() {
			return this.getClass().getName() + ": { cell: " + this.cellX + ", " + this.cellZ + ", center: " + this.centerX + ", " + this.centerZ + ", seed: " + Long.toHexString(this.seed) + ", angle from center: " + this.angleFromCenter + " }";
		}
	}

	public static class Cell {

		//the seed point of this cell in the voronoi diagram.
		public final SeedPoint center;
		//the seed points of all the cells which are
		//adjacent to this one in the voronoi diagram.
		//in other words, all the seed points which are connected
		//to center in the equivalent delaunay triangulation.
		public final AdjacentSeedPoint[] adjacent;

		public Cell(SeedPoint center, AdjacentSeedPoint[] adjacent) {
			this.center = center;
			this.adjacent = adjacent;
		}

		public boolean contains(int blockX, int blockZ) {
			for (SeedPoint adjacent : this.adjacent) {
				float progress = this.center.progressToF(adjacent, blockX, blockZ);
				if (!(progress >= 0.0F && progress <= 1.0F)) {
					return false;
				}
			}
			return true;
		}

		public CrashException handleError(Throwable throwable, int blockX, int blockZ) {
			CrashReport report = CrashReport.create(throwable, "Getting distance squared to edge of voronoi cell");
			CrashReportSection category = report.addElement("Voronoi details:");
			category.add("Requested coordinates", blockX + ", " + blockZ);
			category.add("Center cell", this.center);
			for (SeedPoint adjacent : this.adjacent) {
				category.add("Adjacent cell", adjacent);
			}
			return new CrashException(report);
		}

		public double progressToEdgeSquaredD(int blockX, int blockZ) {
			try {
				double distance = 1.0D;
				for (SeedPoint adjacent : this.adjacent) {
					double progress = this.center.progressToD(adjacent, blockX, blockZ);
					if (progress > 0.0D) {
						progress *= 2.0D;
						if (progress > 1.0D) {
							throw new IllegalArgumentException("Position not inside cell");
						}
						distance *= 1.0D - progress * progress;
					}
				}
				return 1.0D - distance;
			}
			catch (Throwable throwable) {
				throw this.handleError(throwable, blockX, blockZ);
			}
		}

		public double progressToEdgeD(int blockX, int blockZ) {
			return Math.sqrt(this.progressToEdgeSquaredD(blockX, blockZ));
		}

		public double hardProgressToEdgeD(int blockX, int blockZ) {
			try {
				double distance = 0.0D;
				for (AdjacentSeedPoint adjacent : this.adjacent) {
					double progress = this.center.progressToD(adjacent, blockX, blockZ);
					if (progress > 0.5D) {
						throw new IllegalArgumentException("Position not inside cell");
					}
					distance = Math.max(distance, progress);
				}
				return distance * 2.0D;
			}
			catch (Throwable throwable) {
				throw this.handleError(throwable, blockX, blockZ);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = (
				new StringBuilder((this.adjacent.length + 1) << 6)
				.append(this.getClass().getName())
				.append(":\n\tCenter: ").append(this.center)
			);
			for (SeedPoint adjacent : this.adjacent) {
				builder.append("\n\tAdjacent: ").append(adjacent);
			}
			return builder.toString();
		}
	}
}