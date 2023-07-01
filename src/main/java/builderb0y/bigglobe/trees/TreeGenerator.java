package builderb0y.bigglobe.trees;

import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.block.MushroomBlock;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.StructureWorldAccess;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.features.BlockQueue;
import builderb0y.bigglobe.features.BlockQueueStructureWorldAccess;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.branches.BranchConfig;
import builderb0y.bigglobe.trees.branches.BranchesConfig;
import builderb0y.bigglobe.trees.branches.ThickBranchConfig;
import builderb0y.bigglobe.trees.decoration.*;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;
import builderb0y.bigglobe.versions.MaterialVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class TreeGenerator {

	public final BlockQueueStructureWorldAccess worldQueue;
	public final Permuter random;
	public final WoodPalette palette;
	public final Map<BlockState, BlockState> groundReplacements;
	public final TrunkConfig trunk;
	public final BranchesConfig branches;
	public final DecoratorConfig decorators;
	public final WorldColumn centerColumn, anywhereColumn;

	public TreeGenerator(
		StructureWorldAccess world,
		BlockQueue queue,
		Permuter random,
		WoodPalette palette,
		Map<BlockState, BlockState> groundReplacements,
		TrunkConfig trunk,
		BranchesConfig branches,
		DecoratorConfig decorators,
		WorldColumn centerColumn
	) {
		this.worldQueue         = queue.createWorld(world);
		this.random             = random;
		this.palette            = palette;
		this.groundReplacements = groundReplacements;
		this.trunk              = trunk;
		this.branches           = branches;
		this.decorators         = decorators;
		this.centerColumn       = centerColumn;
		this.anywhereColumn     = centerColumn.blankCopy();
	}

	public boolean generate() {
		try {
			this.generateQueue();
		}
		catch (NotEnoughSpaceException fail) {
			return false;
		}
		this.worldQueue.commit();
		return true;
	}

	public void queueAndDecorateLeaf(BlockPos pos, BlockState state) {
		this.worldQueue.setBlockState(pos, state);
		for (BlockDecorator decorator : this.decorators.leafBlock) {
			decorator.decorate(this, pos, state);
		}
	}

	public void generateQueue() throws NotEnoughSpaceException {
		this.generateTrunk();
		this.generateBranches();
	}

	public void generateTrunk() throws NotEnoughSpaceException {
		int startY = this.trunk.startY;
		int height = this.trunk.height;
		if (startY + height >= this.worldQueue.getTopY()) {
			throw NotEnoughSpaceException.INSTANCE;
		}
		for (int offsetY = height; true; offsetY--) {
			if (offsetY < -4) {
				//probably generated on a cliff or overhang.
				throw NotEnoughSpaceException.INSTANCE;
			}
			this.trunk.setOffset(offsetY);
			if (!this.generateTrunkLayer(startY + offsetY)) {
				if (offsetY <= 0) break;
				else throw NotEnoughSpaceException.INSTANCE;
			}
		}
		for (TrunkDecorator decorator : this.decorators.trunk) {
			decorator.decorate(this, this.trunk);
		}
	}

	public boolean generateTrunkLayer(int y) throws NotEnoughSpaceException {
		Map<BlockState, BlockState> groundReplacements = this.groundReplacements;
		double radius = this.trunk.currentRadius;
		double radius2 = squareD(radius);
		double centerX = this.trunk.currentX;
		double centerZ = this.trunk.currentZ;
		int minX = ceilI(centerX - radius), maxX = floorI(centerX + radius);
		int minZ = ceilI(centerZ - radius), maxZ = floorI(centerZ + radius);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		mutablePos.setY(y);
		boolean placedAny = false;
		for (int blockX = minX; blockX <= maxX; blockX++) {
			mutablePos.setX(blockX);
			double offsetX2 = squareD(blockX - centerX);
			for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
				if (offsetX2 + squareD(blockZ - centerZ) < radius2) {
					mutablePos.setZ(blockZ);
					BlockState existingState = this.worldQueue.getWorldState(mutablePos);
					boolean workaroundForBushes = false;
					if (this.canTrunkReplace(existingState) || (workaroundForBushes = this.canTrunkReplaceBush(mutablePos, existingState))) {
						BlockState logState = this.palette.logState(this.random, Axis.Y);
						if (workaroundForBushes) {
							this.worldQueue.queue.queueReplacement(mutablePos, existingState, logState);
						}
						else {
							this.worldQueue.setBlockState(mutablePos, logState);
						}
						for (BlockDecorator decorator : this.decorators.trunkBlock) {
							decorator.decorate(this, mutablePos, this.palette.logState(this.random, Axis.Y));
						}
						placedAny = true;
					}
					else {
						mutablePos.setY(y + 1);
						boolean logAboveToo = this.worldQueue.getBlockState(mutablePos) == this.palette.logState(this.random, Axis.Y);
						mutablePos.setY(y);
						if (logAboveToo) {
							BlockState replacement = groundReplacements.get(existingState);
							if (replacement != null) {
								if (replacement != existingState) {
									this.worldQueue.queue.queueReplacement(mutablePos, existingState, replacement);
								}
							}
							else if (this.trunk.requireValidGround) {
								throw NotEnoughSpaceException.INSTANCE;
							}
						}
					}
				}
			}
		}
		if (placedAny) {
			for (TrunkLayerDecorator decorator : this.decorators.trunkLayer) {
				decorator.decorate(this, this.trunk, y);
			}
		}
		return placedAny;
	}

	public void generateBranches() throws NotEnoughSpaceException {
		for (int branchIndex = 0, count = this.branches.branchCount; branchIndex < count; branchIndex++) {
			double branchFracY = ((double)(branchIndex)) / ((double)(count));
			double trunkFracY = Interpolator.mixLinear(this.branches.startFracY, 1.0D, branchFracY);
			this.trunk.setFrac(trunkFracY);
			this.branches.updateBranch(this, branchIndex);
			this.generateBranch();
		}
	}

	public void generateBranch() throws NotEnoughSpaceException {
		if (this.branches.thickBranches) this.generateThickBranch();
		else this.generateThinBranch();
	}

	public void generateThinBranch() throws NotEnoughSpaceException {
		BranchConfig branch = this.branches.currentBranch;
		if (!(branch.length > 0.0D)) return;
		double absDX = Math.abs(branch.nx);
		double absDZ = Math.abs(branch.nz);
		BlockState logState = this.palette.woodState(this.random, absDZ > absDX ? Axis.Z : Axis.X);

		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		for (double offset = this.random.nextDouble(); offset <= branch.length; offset += 1.0D) {
			branch.setLength(this, offset);
			this.generateBranchBlock(mutablePos.set(branch.currentX, branch.currentY, branch.currentZ), logState);
		}
		for (BranchDecorator decorator : this.decorators.branch) {
			decorator.decorate(this, branch);
		}
	}

	public void generateThickBranch() throws NotEnoughSpaceException {
		ThickBranchConfig branch = (ThickBranchConfig)(this.branches.currentBranch);
		if (!(branch.length > 0.0D)) return;
		double absDX = Math.abs(branch.nx);
		double absDZ = Math.abs(branch.nz);

		branch.setFracLength(this, 0.0D);
		double x1 = branch.currentX, z1 = branch.currentZ, r1 = branch.currentRadius;
		branch.setFracLength(this, 1.0D);
		double x2 = branch.currentX, z2 = branch.currentZ, r2 = branch.currentRadius;
		int minX = floorI(Math.min(x1 - r1, x2 - r2)) + 1;
		int minZ = floorI(Math.min(z1 - r1, z2 - r2)) + 1;
		int maxX =  ceilI(Math.max(x1 + r1, x2 + r2)) - 1;
		int maxZ =  ceilI(Math.max(z1 + r1, z2 + r2)) - 1;
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		for (int x = minX; x <= maxX; x++) {
			mutablePos.setX(x);
			for (int z = minZ; z <= maxZ; z++) {
				mutablePos.setZ(z);
				branch.project(this, x, z);
				double xzLengthSquared = squareD(x - branch.currentX, z - branch.currentZ);
				double radiusSquared = squareD(branch.currentRadius);
				int startY = floorI(branch.currentY);
				for (int y = startY; xzLengthSquared + squareD(y - branch.currentY) < radiusSquared; y--) {
					mutablePos.setY(y);
					this.generateBranchBlock(mutablePos, this.palette.woodState(this.random, absDZ > absDX ? Axis.Z : Axis.X));
				}
				for (int y = startY + 1; xzLengthSquared + squareD(y - branch.currentY) < radiusSquared; y++) {
					mutablePos.setY(y);
					this.generateBranchBlock(mutablePos, this.palette.woodState(this.random, absDZ > absDX ? Axis.Z : Axis.X));
				}
			}
		}
		for (BranchDecorator decorator : this.decorators.branch) {
			decorator.decorate(this, branch);
		}
	}

	public void generateBranchBlock(BlockPos pos, BlockState branchState) throws NotEnoughSpaceException {
		if (this.canLogReplace(this.worldQueue.getWorldState(pos))) {
			if (this.canLogReplace(this.worldQueue.queue.getBlockState(pos))) { //don't overwrite logs with other logs.
				this.worldQueue.setBlockState(pos, branchState);
				for (BlockDecorator decorator : this.decorators.branchBlock) {
					decorator.decorate(this, pos, branchState);
				}
			}
		}
		else {
			throw NotEnoughSpaceException.INSTANCE;
		}
	}

	public boolean canTrunkReplace(BlockState existingState) {
		return this.canLogReplace(existingState);
	}

	public boolean canTrunkReplaceBush(BlockPos.Mutable mutablePos, BlockState existingState) {
		if (this.palette.woodBlocks().contains(existingState.getBlock())) {
			//hacky workaround for bushes.
			int oldY = mutablePos.getY();
			mutablePos.setY(oldY - 1);
			existingState = this.worldQueue.getWorldState(mutablePos);
			mutablePos.setY(oldY);
			return this.groundReplacements.containsKey(existingState);
		}
		else {
			return false;
		}
	}

	public boolean canLogReplace(BlockState state) {
		if (state.getFluidState().isStill()) return this.trunk.canGenerateInLiquid;
		return MaterialVersions.isReplaceableOrPlant(state) || state.isIn(BlockTags.LEAVES) || state.getBlock() instanceof MushroomBlock;
	}

	public boolean canLeavesReplace(BlockState state) {
		if (state.getFluidState().isStill()) return false;
		return MaterialVersions.isReplaceableOrPlant(state);
	}

	public static class NotEnoughSpaceException extends Exception {

		public static final NotEnoughSpaceException INSTANCE = new NotEnoughSpaceException();

		public NotEnoughSpaceException() {
			super(null, null, false, false);
		}
	}
}