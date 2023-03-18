package builderb0y.bigglobe.blocks;

import java.util.Map;

import com.google.common.base.Predicates;

import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.items.BigGlobeItems;

public class SoulCauldronBlock extends AbstractCauldronBlock {

	public static final Map<Item, CauldronBehavior> BEHAVIOR_MAP = CauldronBehavior.createMap();
	public static final CauldronBehavior
		FILL_WITH_SOUL_LAVA = (state, world, pos, player, hand, stack) -> {
			return CauldronBehavior.fillCauldron(world, pos, player, hand, stack, BigGlobeBlocks.SOUL_CAULDRON.getDefaultState(), SoundEvents.ITEM_BUCKET_EMPTY_LAVA);
		},
		EMPTY_SOUL_LAVA_CAULDRON = (state, world, pos, player, hand, stack) -> {
			return CauldronBehavior.emptyCauldron(state, world, pos, player, hand, stack, new ItemStack(BigGlobeItems.SOUL_LAVA_BUCKET), Predicates.alwaysTrue(), SoundEvents.ITEM_BUCKET_FILL_LAVA);
		};

	public SoulCauldronBlock(Settings settings) {
		super(settings, BEHAVIOR_MAP);
	}

	public static void init() {
		CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR.put(BigGlobeItems.SOUL_LAVA_BUCKET, FILL_WITH_SOUL_LAVA);
		BEHAVIOR_MAP.put(Items.BUCKET, EMPTY_SOUL_LAVA_CAULDRON);
	}

	@Override
	public double getFluidHeight(BlockState state) {
		return 0.9375D;
	}


	@Override
	public boolean isFull(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (this.isEntityTouchingFluid(state, pos, entity)) {
			entity.setOnFireFromLava();
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return 3;
	}
}