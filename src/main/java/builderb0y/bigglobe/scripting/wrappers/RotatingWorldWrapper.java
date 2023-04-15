package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import builderb0y.bigglobe.noise.Permuter;

public class RotatingWorldWrapper extends WorldWrapper {

	public final BlockPos origin;
	public final BlockRotation rotation;

	public RotatingWorldWrapper(StructureWorldAccess world, Permuter permuter, BlockPos origin, BlockRotation rotation) {
		super(world, permuter);
		this.origin = origin.toImmutable();
		this.rotation = rotation;
	}

	@Override
	public BlockPos.Mutable pos(int x, int y, int z) {
		return switch (this.rotation) {
			case NONE                -> this.pos.set(x, y, z);
			case CLOCKWISE_90        -> this.pos.set(this.origin.getZ() - z + this.origin.getX(), y, x - this.origin.getX() + this.origin.getZ());
			case CLOCKWISE_180       -> this.pos.set((this.origin.getX() << 1) - x, y, (this.origin.getZ() << 1) - z);
			case COUNTERCLOCKWISE_90 -> this.pos.set(z - this.origin.getZ() + this.origin.getX(), y, this.origin.getX() - x + this.origin.getZ());
		};
	}

	@Override
	public BlockState getBlockState(int x, int y, int z) {
		return super.getBlockState(x, y, z).rotate(this.oppositeRotation());
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		super.setBlockState(x, y, z, state.rotate(this.rotation));
	}

	@Override
	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		return super.placeBlockState(x, y, z, state.rotate(this.rotation));
	}

	@Override
	public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		super.fillBlockState(minX, minY, minZ, maxX, maxY, maxZ, state.rotate(this.rotation));
	}

	@Override
	public BiomeEntry getBiome(int x, int y, int z) {
		BlockPos.Mutable pos = this.pos(x, y, z);
		this.biomeColumn.setPos(pos.getX(), pos.getZ());
		return new BiomeEntry(this.biomeColumn.getBiome(pos.getY()));
	}

	public BlockRotation oppositeRotation() {
		return switch (this.rotation) {
			case NONE                -> BlockRotation.NONE;
			case CLOCKWISE_90        -> BlockRotation.COUNTERCLOCKWISE_90;
			case CLOCKWISE_180       -> BlockRotation.CLOCKWISE_180;
			case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
		};
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { world: " + this.world + ", origin: " + this.origin + ", rotation: " + this.rotation + " }";
	}
}