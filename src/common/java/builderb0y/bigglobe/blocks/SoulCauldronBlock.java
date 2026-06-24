package builderb0y.bigglobe.blocks;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteraction.Dispatcher;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blockdefs.NetherBlocks;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;

public class SoulCauldronBlock extends AbstractCauldronBlock {

	public static final CauldronInteraction.Dispatcher BEHAVIOR_MAP = new Dispatcher();

	public static final CauldronInteraction
		FILL_WITH_SOUL_LAVA = (BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) -> {
			return CauldronInteractions.emptyBucket(world, pos, player, hand, stack, NetherBlocks.SOUL_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA);
		},
		EMPTY_SOUL_LAVA_CAULDRON = (BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) -> {
			return CauldronInteractions.fillBucket(state, world, pos, player, hand, stack, new ItemStack(BigGlobeItems.SOUL_LAVA_BUCKET), Predicates.alwaysTrue(), SoundEvents.BUCKET_FILL_LAVA);
		};

	public static final MapCodec<SoulCauldronBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SoulCauldronBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public SoulCauldronBlock(Properties settings) {
		super(settings, BEHAVIOR_MAP);
	}

	public static void init() {
		CauldronInteractions.EMPTY.put(BigGlobeItems.SOUL_LAVA_BUCKET, FILL_WITH_SOUL_LAVA);
		BEHAVIOR_MAP.put(Items.BUCKET, EMPTY_SOUL_LAVA_CAULDRON);
	}

	@Override
	public double getContentHeight(BlockState state) {
		return 0.9375D;
	}

	@Override
	public boolean isFull(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(
		BlockState state,
		Level world,
		BlockPos pos,
		Entity entity,
		InsideBlockEffectApplier handler,
		boolean movingFastOrBlockPosIsInsideDestinationBox
	) {
		super.entityInside(
			state,
			world,
			pos,
			entity,
			handler,
			movingFastOrBlockPosIsInsideDestinationBox
		);
		entity.lavaHurt();
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
		return 3;
	}
}