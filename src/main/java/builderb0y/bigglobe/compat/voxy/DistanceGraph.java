package builderb0y.bigglobe.compat.voxy;

public class DistanceGraph {

	public Node root;
	public Query query;

	public DistanceGraph(int minX, int minZ, int maxX, int maxZ) {
		this.root = new LeafNode(minX, maxX, minZ, maxZ); //permute arguments.
		this.query = new Query();
	}

	public Query query(int x, int z) {
		if (this.root.isFull()) return null;
		this.query.init(x, z);
		this.root.getClosestEmpty(this.query);
		this.root = this.root.setFull(this.query.closestX, this.query.closestZ);
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

	public static abstract class Node {

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

		public abstract boolean isFull();

		public abstract Node setFull(int x, int z);

		public abstract void getClosestEmpty(Query query);
	}

	public static class LeafNode extends Node {

		public boolean full;

		public LeafNode(int minX, int maxX, int minZ, int maxZ) {
			super(minX, maxX, minZ, maxZ);
		}

		public LeafNode(Node bounds) {
			super(bounds);
		}

		@Override
		public boolean isFull() {
			return this.full;
		}

		@Override
		public Node setFull(int x, int z) {
			if (this.full) return this;
			if (x == this.minX && z == this.minZ && x == this.midX && z == this.midZ) {
				//this node's area is 1x1.
				this.full = true;
				return this;
			}
			return new PartialNode(this).setFull(x, z);
		}

		@Override
		public void getClosestEmpty(Query query) {
			if (this.full) return;
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

	public static class PartialNode extends Node {

		public Node node00, node01, node10, node11;

		public PartialNode(int minX, int maxX, int minZ, int maxZ) {
			super(minX, maxX, minZ, maxZ);
			this.node00 = new LeafNode(minX,      this.midX, minZ,      this.midZ);
			this.node01 = new LeafNode(minX,      this.midX, this.midZ, maxZ     );
			this.node10 = new LeafNode(this.midX, maxX,      minZ,      this.midZ);
			this.node11 = new LeafNode(this.midX, maxX,      this.midZ, maxZ     );
		}

		public PartialNode(Node bounds) {
			super(bounds);
			this.node00 = new LeafNode(this.minX, this.midX, this.minZ, this.midZ);
			this.node01 = new LeafNode(this.minX, this.midX, this.midZ, this.maxZ);
			this.node10 = new LeafNode(this.midX, this.maxX, this.minZ, this.midZ);
			this.node11 = new LeafNode(this.midX, this.maxX, this.midZ, this.maxZ);
		}

		@Override
		public boolean isFull() {
			return false;
		}

		@Override
		public Node setFull(int x, int z) {
			if (x >= this.midX) {
				if (z >= this.midZ) {
					this.node11 = this.node11.setFull(x, z);
				}
				else {
					this.node10 = this.node10.setFull(x, z);
				}
			}
			else {
				if (z >= this.midZ) {
					this.node01 = this.node01.setFull(x, z);
				}
				else {
					this.node00 = this.node00.setFull(x, z);
				}
			}
			if (
				this.node00.isFull() &&
				this.node01.isFull() &&
				this.node10.isFull() &&
				this.node11.isFull()
			) {
				LeafNode result = new LeafNode(this);
				result.full = true;
				return result;
			}
			else {
				return this;
			}
		}

		@Override
		public void getClosestEmpty(Query query) {
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
				n0.getClosestEmpty(query);
				n1.getClosestEmpty(query);
				n2.getClosestEmpty(query);
				n3.getClosestEmpty(query);
			}
		}
	}
}