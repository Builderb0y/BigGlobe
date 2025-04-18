package builderb0y.bigglobe.versions;

import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;
import net.minecraft.state.property.EnumProperty;

public class WallBlockVersions {

	public static final EnumProperty<WallShape> NORTH_SHAPE = WallBlock.#if MC_VERSION >= MC_1_21_5 NORTH_WALL_SHAPE #else NORTH_SHAPE #endif;
	public static final EnumProperty<WallShape>  EAST_SHAPE = WallBlock.#if MC_VERSION >= MC_1_21_5  EAST_WALL_SHAPE #else  EAST_SHAPE #endif;
	public static final EnumProperty<WallShape> SOUTH_SHAPE = WallBlock.#if MC_VERSION >= MC_1_21_5 SOUTH_WALL_SHAPE #else SOUTH_SHAPE #endif;
	public static final EnumProperty<WallShape>  WEST_SHAPE = WallBlock.#if MC_VERSION >= MC_1_21_5  WEST_WALL_SHAPE #else  WEST_SHAPE #endif;
}