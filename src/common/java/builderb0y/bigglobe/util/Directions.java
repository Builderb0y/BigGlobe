package builderb0y.bigglobe.util;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import builderb0y.bigglobe.math.BigGlobeMath;

public class Directions {

	public static final Direction[]
		ALL = Direction.values(),
		HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST },
		VERTICAL = { Direction.DOWN, Direction.UP };
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
	public static final Axis[]
		AXES = Axis.values();
	public static final Rotation[]
		ROTATIONS = Rotation.values();
	public static final Mirror[]
		MIRRORS = Mirror.values();

	/**
	returns the {@link Rotation} which,
	when applied to (from), will produce (to).
	*/
	public static Rotation rotationOf(Direction from, Direction to) {
		//todo: this can be optimized better.
		for (Rotation rotation : ROTATIONS) {
			if (rotation.rotate(from) == to) return rotation;
		}
		throw new IllegalArgumentException("Can't rotate from " + from + " to " + to);
	}

	/**
	returns the {@link Mirror} which,
	when applied to (from), will produce (to).
	*/
	public static Mirror mirrorOf(Direction from, Direction to) {
		//todo: this can be optimized better.
		for (Mirror mirror : MIRRORS) {
			if (mirror.mirror(from) == to) return mirror;
		}
		throw new IllegalArgumentException("Can't mirror from " + from + " to " + to);
	}

	public static Rotation scriptRotation(int rotation) {
		rotation = BigGlobeMath.modulus_BP(rotation, 360);
		return switch (rotation) {
			default -> Rotation.NONE;
			case 90 -> Rotation.CLOCKWISE_90;
			case 180 -> Rotation.CLOCKWISE_180;
			case 270 -> Rotation.COUNTERCLOCKWISE_90;
		};
	}

	public static int reverseScriptRotation(Rotation rotation) {
		return switch (rotation) {
			case NONE -> 0;
			case CLOCKWISE_90 -> 90;
			case CLOCKWISE_180 -> 180;
			case COUNTERCLOCKWISE_90 -> 270;
		};
	}

	public static Mirror scriptMirror(String axis) {
		if (axis.length() == 1) {
			char c = axis.charAt(0);
			if (c == 'x') return Mirror.FRONT_BACK;
			if (c == 'z') return Mirror.LEFT_RIGHT;
		}
		return Mirror.NONE;
	}

	public static String reverseScriptMirror(Mirror mirror) {
		return switch (mirror) {
			case FRONT_BACK -> "x";
			case LEFT_RIGHT -> "z";
			case NONE -> "none";
		};
	}
}