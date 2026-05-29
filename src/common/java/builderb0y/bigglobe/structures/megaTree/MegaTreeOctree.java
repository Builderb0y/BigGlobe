package builderb0y.bigglobe.structures.megaTree;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import builderb0y.bigglobe.math.BigGlobeMath;

public class MegaTreeOctree {

	public Node root;
	public Query query;

	public MegaTreeOctree(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.root = new EmptyNode(minX, minY, minZ, maxX, maxY, maxZ);
		this.query = new Query(null);
	}

	public MegaTreeOctree(AABB box) {
		this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public void addBall(Ball ball) {
		this.root = this.root.addBall(ball);
	}

	public @Nullable Ball findClosestBall(Ball target) {
		Query query = this.query;
		query.target = target;
		query.found = null;
		query.distanceSquared = Double.POSITIVE_INFINITY;
		this.root.findClosest(query);
		return query.found;
	}

	public static class Query {

		public Ball target, found;
		public double distanceSquared;

		public Query(Ball target) {
			this.target = target;
			this.distanceSquared = Double.POSITIVE_INFINITY;
		}

		public void accept(Ball ball) {
			double newDistanceSquared = BigGlobeMath.squareD(
				ball.x() - this.target.x(),
				ball.y() - this.target.y(),
				ball.z() - this.target.z()
			);
			if (newDistanceSquared < this.distanceSquared) {
				this.found = ball;
				this.distanceSquared = newDistanceSquared;
			}
		}
	}

	public static abstract class Node extends AABB {

		public final double midX, midY, midZ;

		public Node(double x1, double y1, double z1, double x2, double y2, double z2) {
			super(x1, y1, z1, x2, y2, z2);
			this.midX = (x1 + x2) * 0.5D;
			this.midY = (y1 + y2) * 0.5D;
			this.midZ = (z1 + z2) * 0.5D;
		}

		public Node(AABB box) {
			this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
		}

		public abstract Node addBall(Ball ball);

		public abstract void findClosest(Query query);

		/*
		@Override
		public boolean contains(double x, double y, double z) {
			return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
		}
		*/
	}

	public static class EmptyNode extends Node {

		public EmptyNode(double x1, double y1, double z1, double x2, double y2, double z2) {
			super(x1, y1, z1, x2, y2, z2);
		}

		public EmptyNode(AABB box) {
			super(box);
		}

		@Override
		public Node addBall(Ball ball) {
			//assert this.contains(ball.data.x(), ball.data.y(), ball.data.z());
			return new SingleNode(this, ball);
		}

		@Override
		public void findClosest(Query query) {
			//no-op.
		}
	}

	public static class SingleNode extends Node {

		public final Ball ball;

		public SingleNode(double x1, double y1, double z1, double x2, double y2, double z2, Ball ball) {
			super(x1, y1, z1, x2, y2, z2);
			this.ball = ball;
		}

		public SingleNode(AABB box, Ball ball) {
			super(box);
			this.ball = ball;
		}

		@Override
		public Node addBall(Ball ball) {
			//assert this.contains(ball.data.x(), ball.data.y(), ball.data.z());
			//assert !this.ball.data.position().equals(ball.data.position()) : "Duplicate ball: " + ball;
			return new OctNode(this).addBall(this.ball).addBall(ball);
		}

		@Override
		public void findClosest(Query query) {
			query.accept(this.ball);
		}
	}

	public static class OctNode extends Node {

		public double
			effectiveMinX = Double.POSITIVE_INFINITY,
			effectiveMinY = Double.POSITIVE_INFINITY,
			effectiveMinZ = Double.POSITIVE_INFINITY,
			effectiveMaxX = Double.NEGATIVE_INFINITY,
			effectiveMaxY = Double.NEGATIVE_INFINITY,
			effectiveMaxZ = Double.NEGATIVE_INFINITY;

		public Node
			n000 = new EmptyNode(this.minX, this.minY, this.minZ, this.midX, this.midY, this.midZ),
			n001 = new EmptyNode(this.minX, this.minY, this.midZ, this.midX, this.midY, this.maxZ),
			n010 = new EmptyNode(this.minX, this.midY, this.minZ, this.midX, this.maxY, this.midZ),
			n011 = new EmptyNode(this.minX, this.midY, this.midZ, this.midX, this.maxY, this.maxZ),
			n100 = new EmptyNode(this.midX, this.minY, this.minZ, this.maxX, this.midY, this.midZ),
			n101 = new EmptyNode(this.midX, this.minY, this.midZ, this.maxX, this.midY, this.maxZ),
			n110 = new EmptyNode(this.midX, this.midY, this.minZ, this.maxX, this.maxY, this.midZ),
			n111 = new EmptyNode(this.midX, this.midY, this.midZ, this.maxX, this.maxY, this.maxZ);

		public OctNode(double x1, double y1, double z1, double x2, double y2, double z2) {
			super(x1, y1, z1, x2, y2, z2);
		}

		public OctNode(AABB box) {
			super(box);
		}

		public Node getNode(int corner) {
			return switch (corner) {
				case 0b000 -> this.n000;
				case 0b001 -> this.n001;
				case 0b010 -> this.n010;
				case 0b011 -> this.n011;
				case 0b100 -> this.n100;
				case 0b101 -> this.n101;
				case 0b110 -> this.n110;
				case 0b111 -> this.n111;
				default -> throw new IllegalArgumentException(Integer.toString(corner));
			};
		}

		public void setNode(int corner, Node node) {
			switch (corner) {
				case 0b000 -> this.n000 = node;
				case 0b001 -> this.n001 = node;
				case 0b010 -> this.n010 = node;
				case 0b011 -> this.n011 = node;
				case 0b100 -> this.n100 = node;
				case 0b101 -> this.n101 = node;
				case 0b110 -> this.n110 = node;
				case 0b111 -> this.n111 = node;
				default -> throw new IllegalArgumentException(Integer.toString(corner));
			}
		}

		public int getClosestCorner(Ball ball) {
			int x = (int)(Double.doubleToRawLongBits(this.midX - ball.x()) >>> 63);
			int y = (int)(Double.doubleToRawLongBits(this.midY - ball.y()) >>> 63);
			int z = (int)(Double.doubleToRawLongBits(this.midZ - ball.z()) >>> 63);
			return (x << 2) | (y << 1) | z;
		}

		@Override
		public Node addBall(Ball ball) {
			//assert this.contains(ball.data.x(), ball.data.y(), ball.data.z());
			int corner = this.getClosestCorner(ball);
			this.setNode(corner, this.getNode(corner).addBall(ball));
			double x = ball.x(), y = ball.y(), z = ball.z();
			if (x > this.effectiveMaxX) this.effectiveMaxX = x;
			if (y > this.effectiveMaxY) this.effectiveMaxY = y;
			if (z > this.effectiveMaxZ) this.effectiveMaxZ = z;
			if (x < this.effectiveMinX) this.effectiveMinX = x;
			if (y < this.effectiveMinY) this.effectiveMinY = y;
			if (z < this.effectiveMinZ) this.effectiveMinZ = z;
			return this;
		}

		@Override
		public void findClosest(Query query) {
			Ball ballData = query.target;
			double clampX = Mth.clamp(ballData.x(), this.effectiveMinX, this.effectiveMaxX);
			double clampY = Mth.clamp(ballData.y(), this.effectiveMinY, this.effectiveMaxY);
			double clampZ = Mth.clamp(ballData.z(), this.effectiveMinY, this.effectiveMaxZ);
			if (BigGlobeMath.squareD(ballData.x() - clampX, ballData.y() - clampY, ballData.z() - clampZ) < query.distanceSquared) {
				//containing corner first, then corners connected by a face,
				//then corners connected by an edge, then the corner connected by a corner.
				int containingCorner = this.getClosestCorner(query.target);
				this.getNode(containingCorner).findClosest(query); //containing
				this.getNode(containingCorner ^ 0b100).findClosest(query); //connected by X face
				this.getNode(containingCorner ^ 0b010).findClosest(query); //connected by Y face
				this.getNode(containingCorner ^ 0b001).findClosest(query); //connected by Z face
				this.getNode(containingCorner ^ 0b110).findClosest(query); //connected by XY edge
				this.getNode(containingCorner ^ 0b101).findClosest(query); //connected by XZ edge
				this.getNode(containingCorner ^ 0b011).findClosest(query); //connected by YZ edge
				this.getNode(containingCorner ^ 0b111).findClosest(query); //connected by corner
			}
		}
	}
}