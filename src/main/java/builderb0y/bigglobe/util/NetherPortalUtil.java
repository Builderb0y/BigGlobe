package builderb0y.bigglobe.util;

import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class NetherPortalUtil {

	public static BlockPos findBestPortalPosition(BlockGetter world, BlockPos startPos, Vec3i size) {
		BlockPos.MutableBlockPos mutable = startPos.mutable();
		int startX = startPos.getX();
		int startZ = startPos.getZ();
		int minY = HeightLimitViewVersions.getMinY(world);
		int maxY = HeightLimitViewVersions.getMaxY(world) - size.getY();
		for (int y = maxY; y >= minY; y--) {
			if (canPortalSpawnHere(world, mutable.set(startX, y, startZ), size)) {
				return mutable.set(startX, y, startZ);
			}
			for (int radius = 1; radius <= 12; radius++) {
				for (int offsetX = -radius; offsetX <= radius; offsetX++) {
					if (canPortalSpawnHere(world, mutable.set(startX + offsetX, y, startZ + radius), size)) {
						return mutable.set(startX + offsetX, y, startZ + radius);
					}
					if (canPortalSpawnHere(world, mutable.set(startX + offsetX, y, startZ - radius), size)) {
						return mutable.set(startX + offsetX, y, startZ - radius);
					}
				}
				for (int offsetZ = -radius; ++offsetZ < radius; offsetZ++) {
					if (canPortalSpawnHere(world, mutable.set(startX + radius, y, startZ + offsetZ), size)) {
						return mutable.set(startX + radius, y, startZ + offsetZ);
					}
					if (canPortalSpawnHere(world, mutable.set(startX - radius, y, startZ + offsetZ), size)) {
						return mutable.set(startX - radius, y, startZ + offsetZ);
					}
				}
			}
		}
		return null;
	}

	public static boolean canPortalSpawnHere(BlockGetter world, BlockPos.MutableBlockPos pos, Vec3i size) {
		int minX = pos.getX() - (size.getX() >> 1);
		int minY = pos.getY();
		int minZ = pos.getZ() - (size.getZ() >> 1);
		for (int offsetZ = 0; offsetZ < size.getZ(); offsetZ++) {
			for (int offsetX = 0; offsetX < size.getX(); offsetX++) {
				if (!world.getBlockState(pos.set(minX + offsetX, minY, minZ + offsetZ)).isRedstoneConductor(world, pos)) {
					return false;
				}
				for (int offsetY = 1; offsetY < size.getY(); offsetY++) {
					BlockState state = world.getBlockState(pos.set(minX + offsetX, minY + offsetY, minZ + offsetZ));
					if (!(BlockStateVersions.isReplaceable(state) && state.getFluidState().isEmpty())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static BoundingBox toBoundingBox(BlockPos pos, Vec3i size) {
		int minX = pos.getX() - (size.getX() >> 1);
		int minY = pos.getY();
		int minZ = pos.getZ() - (size.getZ() >> 1);
		int maxX = minX + size.getX() - 1;
		int maxY = minY + size.getY() - 1;
		int maxZ = minZ + size.getZ() - 1;
		return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}
}