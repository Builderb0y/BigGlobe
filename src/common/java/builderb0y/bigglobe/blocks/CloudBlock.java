package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.items.AuraBottleItem;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.ActionResultVersions;

public class CloudBlock extends Block {

	public static final MapCodec<CloudBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CloudBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public final CloudColor color;
	public final boolean isVoid;

	public CloudBlock(Properties settings, CloudColor color, boolean isVoid) {
		super(settings);
		this.color = color;
		this.isVoid = isVoid;
	}

	@Override

	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

		if (!player.isShiftKeyDown()) {
			if (this.color != CloudColor.BLANK) {
				if (stack.getItem() == Items.GLASS_BOTTLE) {
					if (!world.isClientSide()) {
						world.setBlockAndUpdate(pos, (this.isVoid ? BigGlobeBlocks.VOID_CLOUDS : BigGlobeBlocks.CLOUDS).get(CloudColor.BLANK).defaultBlockState());
						player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(BigGlobeItems.AURA_BOTTLES.get(this.color))));
					}
					return ActionResultVersions.ITEM_SUCCESS;
				}
			}
			else {
				if (stack.getItem() instanceof AuraBottleItem bottle) {
					if (!world.isClientSide()) {
						world.setBlockAndUpdate(pos, (this.isVoid ? BigGlobeBlocks.VOID_CLOUDS : BigGlobeBlocks.CLOUDS).get(bottle.color).defaultBlockState());
						player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
					}
					return ActionResultVersions.ITEM_SUCCESS;
				}
			}
		}
		return ActionResultVersions.ITEM_PASS;
	}

	@Override
	public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		//don't apply fall damage.
	}
}