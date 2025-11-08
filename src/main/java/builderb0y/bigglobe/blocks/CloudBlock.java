package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.items.AuraBottleItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.ActionResultVersions;

public class CloudBlock extends Block {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<CloudBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CloudBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public final CloudColor color;
	public final boolean isVoid;

	public CloudBlock(Settings settings, CloudColor color, boolean isVoid) {
		super(settings);
		this.color = color;
		this.isVoid = isVoid;
	}

	@Override
	#if MC_VERSION >= MC_1_21_2
		public net.minecraft.util.ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
	#elif MC_VERSION >= MC_1_20_5 && MC_VERSION < MC_1_21_2
		public net.minecraft.util.ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
	#else
		public net.minecraft.util.ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemStack stack = player.getStackInHand(hand);
	#endif
		if (!player.isSneaking()) {
			if (this.color != CloudColor.BLANK) {
				if (stack.getItem() == Items.GLASS_BOTTLE) {
					if (!world.isClient()) {
						world.setBlockState(pos, (this.isVoid ? BigGlobeBlocks.VOID_CLOUDS : BigGlobeBlocks.CLOUDS).get(CloudColor.BLANK).getDefaultState());
						player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(BigGlobeItems.AURA_BOTTLES.get(this.color))));
					}
					return ActionResultVersions.ITEM_SUCCESS;
				}
			}
			else {
				if (stack.getItem() instanceof AuraBottleItem bottle) {
					if (!world.isClient()) {
						world.setBlockState(pos, (this.isVoid ? BigGlobeBlocks.VOID_CLOUDS : BigGlobeBlocks.CLOUDS).get(bottle.color).getDefaultState());
						player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
					}
					return ActionResultVersions.ITEM_SUCCESS;
				}
			}
		}
		return ActionResultVersions.ITEM_PASS;
	}

	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, #if MC_VERSION >= MC_1_21_5 double #else float #endif fallDistance) {
		//don't apply fall damage.
	}
}