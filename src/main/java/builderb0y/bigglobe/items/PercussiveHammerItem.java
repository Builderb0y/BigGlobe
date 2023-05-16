package builderb0y.bigglobe.items;

import java.util.LinkedList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class PercussiveHammerItem extends MiningToolItem {

	public static final List<SoundPulse> pulses = new LinkedList<>();
	static {
		ServerTickEvents.END_WORLD_TICK.register(PercussiveHammerItem::tick);
		ServerWorldEvents.UNLOAD.register((server, world) -> unload(world));
	}

	public PercussiveHammerItem(float attackDamage, float attackSpeed, ToolMaterial material, TagKey<Block> effectiveBlocks, Settings settings) {
		super(attackDamage, attackSpeed, material, effectiveBlocks, settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (isSolidOpaqueFullCube(world, pos, state)) {
			if (!world.isClient) {
				world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 2.0F, 2.0F);
				pulses.add(new SoundPulse(
					world.getRegistryKey(),
					pos.mutableCopy(),
					context.getSide().getOpposite()
				));
				context.getStack().damage(1, context.getPlayer(), player -> player.sendToolBreakStatus(context.getHand()));
			}
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	public static void tick(ServerWorld world) {
		if (!pulses.isEmpty()) {
			pulses.removeIf(pulse -> {
				if (pulse.world == world.getRegistryKey()) {
					if (++pulse.distance > 32) return true;
					BlockPos.Mutable pos = pulse.position.move(pulse.direction);
					BlockState state = world.getBlockState(pos);
					if (!isSolidOpaqueFullCube(world, pos, state)) {
						world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 2.0F, 2.0F);
						return true;
					}
				}
				return false;
			});
		}
	}

	public static void unload(ServerWorld world) {
		if (!pulses.isEmpty()) {
			RegistryKey<World> key = world.getRegistryKey();
			pulses.removeIf(pulse -> pulse.world == key);
		}
	}

	public static boolean isSolidOpaqueFullCube(BlockView world, BlockPos pos, BlockState state) {
		return state.getMaterial().isSolid() && state.getMaterial().blocksLight() && state.isFullCube(world, pos);
	}

	public static class SoundPulse {

		public RegistryKey<World> world;
		public BlockPos.Mutable position;
		public Direction direction;
		public int distance;

		public SoundPulse(RegistryKey<World> world, BlockPos.Mutable position, Direction direction) {
			this.world = world;
			this.position = position;
			this.direction = direction;
		}
	}
}