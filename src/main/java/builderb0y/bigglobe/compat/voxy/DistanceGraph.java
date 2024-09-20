package builderb0y.bigglobe.compat.voxy;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.MathHelper;

/**
this class used to be used for generating chunks for voxy
while making the closest ones to the player generate first.
this task comes with the obvious problem
that you can't just maintain a sorted queue,
because the player can move, and that would invalidate the queue.
you also can't re-sort the queue or pick the min
element each time, because that would be slow.
my solution uses a quadtree which tries to keep itself
in the most "simple" state possible at all times.
this mostly just means that there are no nodes with 4 identical children.
if a node is completely "full" or completely "empty", it becomes a leaf node.
the tree is traversed in a clever way to find the closest
"empty" leaf to any given target position quickly.
once an empty leaf is found, it gets marked as "full" so
that the chunk it represents won't be generated again.

this class has been unused ever since I added {@link GeneratingStorageBackend}.
*/
public class DistanceGraph {

	public static final int
		WORLD_SIZE_IN_BLOCKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000),
		WORLD_SIZE_IN_CHUNKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000 >>> 4);

	public Node root;
	public Query query;

	public DistanceGraph(int minX, int minZ, int maxX, int maxZ, boolean full) {
		this.root = new LeafNode(minX, maxX, minZ, maxZ, full); //permute arguments.
		this.query = new Query();
	}

	public static DistanceGraph worldOfBlocks(boolean initiallyFull) {
		return new DistanceGraph(-WORLD_SIZE_IN_BLOCKS, -WORLD_SIZE_IN_BLOCKS, +WORLD_SIZE_IN_BLOCKS, +WORLD_SIZE_IN_BLOCKS, initiallyFull);
	}

	public static DistanceGraph worldOfChunks(boolean initiallyFull) {
		return new DistanceGraph(-WORLD_SIZE_IN_CHUNKS, -WORLD_SIZE_IN_CHUNKS, +WORLD_SIZE_IN_CHUNKS, +WORLD_SIZE_IN_CHUNKS, initiallyFull);
	}

	public DistanceGraph(Node root) {
		this.root = root;
		this.query = new Query();
	}

	public boolean get(int x, int z) {
		return this.root.get(x, z);
	}

	public void set(int x, int z, boolean full) {
		this.root = this.root.setFull(x, z, full);
	}

	public @Nullable Query current(int x, int z, boolean full) {
		if (this.root.matches(!full)) return null;
		this.query.init(x, z);
		this.root.getClosest(this.query, full);
		return this.query;
	}

	public @Nullable Query next(int x, int z, boolean full) {
		if (this.root.matches(!full)) return null;
		this.query.init(x, z);
		this.root.getClosest(this.query, full);
		this.root = this.root.setFull(this.query.closestX, this.query.closestZ, !full);
		return this.query;
	}

	public static long square(int offset) {
		long o = (long)(offset);
		return o * o;
	}

	public static class Query {

		public int targetX, targetZ;
		public int closestX, closestZ;
		public long distanceSquared;

		public void init(int targetX, int targetZ) {
			this.targetX = targetX;
			this.targetZ = targetZ;
			this.distanceSquared = Long.MAX_VALUE;
		}
	}

	public static abstract sealed class Node {

		public final int minX, midX, maxX;
		public final int minZ, midZ, maxZ;

		public Node(int minX, int maxX, int minZ, int maxZ) {
			this.minX = minX;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxZ = maxZ;
			this.midX = (minX + maxX) >> 1;
			this.midZ = (minZ + maxZ) >> 1;
		}

		public Node(Node bounds) {
			this.minX = bounds.minX;
			this.midX = bounds.midX;
			this.maxX = bounds.maxX;
			this.minZ = bounds.minZ;
			this.midZ = bounds.midZ;
			this.maxZ = bounds.maxZ;
		}

		public int clampX(int x) {
			return Math.max(Math.min(x, this.maxX - 1), this.minX);
		}

		public int clampZ(int z) {
			return Math.max(Math.min(z, this.maxZ - 1), this.minZ);
		}

		public boolean contains(int x, int z) {
			return x >= this.minX && x < this.maxX && z >= this.minZ && z < this.maxZ;
		}

		public abstract boolean get(int x, int z);

		public abstract boolean matches(boolean full);

		public abstract Node setFull(int x, int z, boolean full);

		public abstract void getClosest(Query query, boolean full);
	}

	public static non-sealed class LeafNode extends Node {

		public boolean full;

		public LeafNode(int minX, int maxX, int minZ, int maxZ, boolean full) {
			super(minX, maxX, minZ, maxZ);
			this.full = full;
		}

		public LeafNode(Node bounds, boolean full) {
			super(bounds);
			this.full = full;
		}

		@Override
		public boolean get(int x, int z) {
			return this.full;
		}

		@Override
		public boolean matches(boolean full) {
			return this.full == full;
		}

		@Override
		public Node setFull(int x, int z, boolean full) {
			if (this.matches(full)) return this;
			if (x == this.minX && z == this.minZ && x == this.midX && z == this.midZ) {
				//this node's area is 1x1.
				this.full = full;
				return this;
			}
			return new PartialNode(this, !full).setFull(x, z, full);
		}

		@Override
		public void getClosest(Query query, boolean full) {
			if (this.matches(full)) {
				int x = this.clampX(query.targetX);
				int z = this.clampZ(query.targetZ);
				long radiusSquared = square(x - query.targetX) + square(z - query.targetZ);
				if (radiusSquared < query.distanceSquared) {
					query.closestX = x;
					query.closestZ = z;
					query.distanceSquared = radiusSquared;
				}
			}
		}
	}

	public static non-sealed class PartialNode extends Node {

		public Node node00, node01, node10, node11;

		public PartialNode(int minX, int maxX, int minZ, int maxZ, boolean full) {
			super(minX, maxX, minZ, maxZ);
			this.node00 = new LeafNode(minX,      this.midX, minZ,      this.midZ, full);
			this.node01 = new LeafNode(minX,      this.midX, this.midZ, maxZ,      full);
			this.node10 = new LeafNode(this.midX, maxX,      minZ,      this.midZ, full);
			this.node11 = new LeafNode(this.midX, maxX,      this.midZ, maxZ,      full);
		}

		public PartialNode(Node bounds, boolean full) {
			super(bounds);
			this.node00 = new LeafNode(this.minX, this.midX, this.minZ, this.midZ, full);
			this.node01 = new LeafNode(this.minX, this.midX, this.midZ, this.maxZ, full);
			this.node10 = new LeafNode(this.midX, this.maxX, this.minZ, this.midZ, full);
			this.node11 = new LeafNode(this.midX, this.maxX, this.midZ, this.maxZ, full);
		}

		public PartialNode(int minX, int maxX, int minZ, int maxZ, Void ignored) {
			super(minX, maxX, minZ, maxZ);
		}

		@Override
		public boolean get(int x, int z) {
			if (x >= this.midX) {
				if (z >= this.midZ) {
					return this.node11.get(x, z);
				}
				else {
					return this.node10.get(x, z);
				}
			}
			else {
				if (z >= this.midZ) {
					return this.node01.get(x, z);
				}
				else {
					return this.node00.get(x, z);
				}
			}
		}

		@Override
		public boolean matches(boolean full) {
			return false;
		}

		@Override
		public Node setFull(int x, int z, boolean full) {
			if (x >= this.midX) {
				if (z >= this.midZ) {
					this.node11 = this.node11.setFull(x, z, full);
				}
				else {
					this.node10 = this.node10.setFull(x, z, full);
				}
			}
			else {
				if (z >= this.midZ) {
					this.node01 = this.node01.setFull(x, z, full);
				}
				else {
					this.node00 = this.node00.setFull(x, z, full);
				}
			}
			if (
				this.node00.matches(full) &&
				this.node01.matches(full) &&
				this.node10.matches(full) &&
				this.node11.matches(full)
			) {
				return new LeafNode(this, full);
			}
			else {
				return this;
			}

		}

		@Override
		public void getClosest(Query query, boolean full) {
			int x = this.clampX(query.targetX);
			int z = this.clampZ(query.targetZ);
			long distance = square(query.targetX - x) + square(query.targetZ - z);
			if (distance < query.distanceSquared) {
				//sort nodes such that we query the likely closest one first.
				Node n0, n1, n2, n3;
				if (x >= this.midX) {
					if (z >= this.midZ) {
						n0 = this.node11;
						if (x - this.midX > z - this.midZ) {
							n1 = this.node10;
							n2 = this.node01;
						}
						else {
							n1 = this.node01;
							n2 = this.node10;
						}
						n3 = this.node00;
					}
					else {
						n0 = this.node10;
						if (this.midX - x > z - this.midZ) {
							n1 = this.node00;
							n2 = this.node11;
						}
						else {
							n1 = this.node11;
							n2 = this.node00;
						}
						n3 = this.node01;
					}
				}
				else {
					if (z >= this.midZ) {
						n0 = this.node01;
						if (this.midX - x > z - this.midZ) {
							n1 = this.node00;
							n2 = this.node11;
						}
						else {
							n1 = this.node11;
							n2 = this.node00;
						}
						n3 = this.node10;
					}
					else {
						n0 = this.node00;
						if (x - this.midX > z - this.midZ) {
							n1 = this.node10;
							n2 = this.node01;
						}
						else {
							n1 = this.node01;
							n2 = this.node10;
						}
						n3 = this.node11;
					}
				}
				n0.getClosest(query, full);
				n1.getClosest(query, full);
				n2.getClosest(query, full);
				n3.getClosest(query, full);
			}
		}
	}
}