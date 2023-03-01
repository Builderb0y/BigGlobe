package builderb0y.bigglobe.structures;

import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.Directions;

/**
generator for semi-3D labyrinth-like structures.
labyrinths are composed of rooms on a grid connected by hallways.
they are semi-3D because the halls can go up and down,
and rooms can spawn on top of each other.
*/
public abstract class LabyrinthLayout {

	public static final Direction[] POSITIVE_HORIZONTALS = { Directions.POSITIVE_X, Directions.POSITIVE_Z };

	public final ObjectOpenCustomHashSet<RoomPiece> rooms;
	public final ObjectArrayList<RoomPiece> activeRooms;
	public final ObjectArrayList<HallPiece> halls;
	public final ObjectArrayList<DecorationPiece> decorations;
	public final Direction[] horizontals;
	public final RandomGenerator random;
	public final int maxRooms;

	public LabyrinthLayout(RandomGenerator random, int maxRooms) {
		this.rooms       = new ObjectOpenCustomHashSet<>(maxRooms, RoomPiece.HACKY_OVERLAP_STRATEGY);
		this.activeRooms = new ObjectArrayList<>(maxRooms);
		this.halls       = new ObjectArrayList<>(maxRooms + (maxRooms >> 1)); //= maxRooms * 1.5
		this.decorations = new ObjectArrayList<>(maxRooms << 1);
		this.horizontals = Directions.HORIZONTAL.clone();
		this.random      = random;
		this.maxRooms    = maxRooms;
	}

	/**
	creates a new room.
	implementors of this method do not need to set
	the position of the room to anything meaningful;
	it will be overridden after this method returns.
	implementors also do not need to add the newly
	created room to {@link #rooms} or {@link #activeRooms}.
	if it needs to be in either of these collections,
	then it will be added after this method returns.
	*/
	public abstract RoomPiece newRoom();

	/**
	creates a new hallway connecting the two rooms.
	the "direction" parameter indicates the direction from from to to.
	implementors of this method should set the position of the newly created hall to
	wherever is applicable. the position will not be overwritten after this method returns.
	implementors of this method do not need to add the newly created hall to {@link #halls},
	as that will be done automatically after this method returns.
	*/
	public abstract HallPiece newHall(RoomPiece from, RoomPiece to, Direction direction);

	/** returns the horizontal distance between rooms. */
	public abstract int distanceBetweenRooms();

	/**
	returns true if the room is in a "valid" position; false otherwise.
	rooms in invalid positions will not be added to {@link #rooms}
	or {@link #activeRooms}. implementing this method allows subclasses
	to place restrictions on how big or small labyrinths can generate.
	it also allows more creative restrictions on the shape of the labyrinth.
	for example, implementors could limit the area to a square, or a circle.
	*/
	public abstract boolean isValidPosition(RoomPiece room);

	/**
	returns the maximum amount of vertical separation between 2 adjacent
	rooms which still allows them to connect to each other with a hallway.
	*/
	public abstract int maxHeightDifference();

	/**
	returns the chance that 2 rooms will connect to each other,
	even if neither was created as an extension of the other.
	note that this connection will only happen if the two rooms' vertical
	separation is less than or equal to {@link #maxHeightDifference()}.
	*/
	public double mergeChance() {
		return 0.5D;
	}

	public void generate() {
		this.spreadRooms();
		this.mergeSomeRooms();
		this.makeHalls();
		this.addDecorations();
	}

	public void spreadRooms() {
		ObjectArrayList<RoomPiece> activeRooms = this.activeRooms;
		while (!activeRooms.isEmpty() && this.rooms.size() < this.maxRooms) {
			int index = this.random.nextInt(activeRooms.size());
			if (!this.spreadRoom(activeRooms.get(index))) {
				activeRooms.set(index, activeRooms.get(activeRooms.size() - 1));
				activeRooms.size(activeRooms.size() - 1);
			}
		}
	}

	public boolean spreadRoom(RoomPiece from) {
		RoomPiece next = this.newRoom();
		for (Direction direction : this.horizontals()) {
			if (from.getConnectedRoom(direction) != null) continue;
			int heightOffset = this.random.nextInt(-this.maxHeightDifference(), this.maxHeightDifference() + 1);
			next.setPos(
				from.x() + direction.getOffsetX() * this.distanceBetweenRooms(),
				from.y() + heightOffset,
				from.z() + direction.getOffsetZ() * this.distanceBetweenRooms()
			);
			if (this.isValidPosition(next) && this.rooms.add(next)) {
				this.activeRooms.add(next);
				from.setConnectedRoom(direction, next);
				next.setConnectedRoom(direction.getOpposite(), from);
				return true;
			}
		}
		return false;
	}

	public void mergeSomeRooms() {
		RoomPiece test = this.newRoom();
		for (RoomPiece from : this.rooms) {
			if (!Permuter.nextChancedBoolean(this.random, this.mergeChance())) continue;
			for (Direction direction : POSITIVE_HORIZONTALS) {
				if (from.getConnectedRoom(direction) != null) continue;
				test.setPos(
					from.x() + direction.getOffsetX() * this.distanceBetweenRooms(),
					from.y(),
					from.z() + direction.getOffsetZ() * this.distanceBetweenRooms()
				);
				RoomPiece intersection = this.rooms.get(test);
				if (intersection != null && intersection.getConnectedRoom(direction.getOpposite()) == null) {
					int diff = intersection.y() - from.y();
					if (diff >= -this.maxHeightDifference() && diff <= this.maxHeightDifference()) {
						from.setConnectedRoom(direction, intersection);
						intersection.setConnectedRoom(direction.getOpposite(), from);
					}
				}
			}
		}
	}

	public void makeHalls() {
		for (RoomPiece room : this.rooms) {
			for (Direction direction : POSITIVE_HORIZONTALS) {
				RoomPiece connection = room.getConnectedRoom(direction);
				if (connection != null) {
					this.halls.add(this.newHall(room, connection, direction));
				}
			}
		}
	}

	public void addDecorations() {
		for (RoomPiece room : this.rooms) {
			room.addDecorations(this);
		}
		for (HallPiece hall : this.halls) {
			hall.addDecorations(this);
		}
	}

	/**
	returns the 4 horizontal {@link Direction}'s, in a random order.
	note that this method uses the shared array {@link #horizontals},
	and therefore is not applicable for recursive or multi-threaded use.
	*/
	public Direction[] horizontals() {
		RandomGenerator random = this.random;
		Direction[] horizontals = this.horizontals;
		for (int index = 4; index != 0;) {
			ObjectArrays.swap(horizontals, random.nextInt(index), --index);
		}
		return horizontals;
	}

	public static interface LabyrinthPiece {

		public abstract BlockBox boundingBox();

		public default int x() {
			return (this.boundingBox().getMinX() + this.boundingBox().getMaxX() + 1) >> 1;
		}

		public default int y() {
			return this.boundingBox().getMinY();
		}

		public default int z() {
			return (this.boundingBox().getMinZ() + this.boundingBox().getMaxZ() + 1) >> 1;
		}
	}

	public static interface RoomPiece extends LabyrinthPiece {

		/** this is hacky because it uses hash collisions to model spacial collisions. */
		public static final Hash.Strategy<RoomPiece> HACKY_OVERLAP_STRATEGY = HashStrategies.of(
			piece -> piece.x() * 31 + piece.z(),
			(piece1, piece2) -> piece1.x() == piece2.x() && piece1.z() == piece2.z() && piece1.intersectsY(piece2)
		);

		public abstract void setPos(int x, int y, int z);

		public default void setPos(BlockPos pos) {
			this.setPos(pos.getX(), pos.getY(), pos.getZ());
		}

		public abstract RoomPiece getConnectedRoom(Direction direction);

		public abstract void setConnectedRoom(Direction direction, RoomPiece piece);

		public default boolean intersectsY(LabyrinthPiece that) {
			return this.boundingBox().getMinY() < that.boundingBox().getMaxY() && this.boundingBox().getMaxY() > that.boundingBox().getMinY();
		}

		public default void addDecorations(LabyrinthLayout layout) {}
	}

	public static interface HallPiece extends LabyrinthPiece {

		public default void addDecorations(LabyrinthLayout layout) {}
	}

	public static interface DecorationPiece extends LabyrinthPiece {}
}