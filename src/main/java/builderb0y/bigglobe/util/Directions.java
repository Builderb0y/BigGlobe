package builderb0y.bigglobe.util;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

public class Directions {

	public static final Direction[]
		ALL        = Direction.values(),
		HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST },
		VERTICAL   = { Direction.DOWN, Direction.UP };
	/**
	aliases for existing Direction's which might be
	more convenient to work with in certain cases.
	*/
	public static final Direction
		POSITIVE_X = Direction.EAST,
		POSITIVE_Y = Direction.UP,
		POSITIVE_Z = Direction.SOUTH,
		NEGATIVE_X = Direction.WEST,
		NEGATIVE_Y = Direction.DOWN,
		NEGATIVE_Z = Direction.NORTH;
	public static final BlockRotation[]
		ROTATIONS  = BlockRotation.values();
	public static final BlockMirror[]
		MIRRORS    = BlockMirror.values();

	/**
	returns the {@link BlockRotation} which,
	when applied to (from), will produce (to).
	*/
	public static BlockRotation rotationOf(Direction from, Direction to) {
		//todo: this can be optimized better.
		for (BlockRotation rotation : ROTATIONS) {
			if (rotation.rotate(from) == to) return rotation;
		}
		throw new IllegalArgumentException("Can't rotate from " + from + " to " + to);
	}

	/**
	returns the {@link BlockMirror} which,
	when applied to (from), will produce (to).
	*/
	public static BlockMirror mirrorOf(Direction from, Direction to) {
		//todo: this can be optimized better.
		for (BlockMirror mirror : MIRRORS) {
			if (mirror.apply(from) == to) return mirror;
		}
		throw new IllegalArgumentException("Can't mirror from " + from + " to " + to);
	}
}