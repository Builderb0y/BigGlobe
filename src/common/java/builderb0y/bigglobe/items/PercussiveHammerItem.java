package builderb0y.bigglobe.items;

import java.util.LinkedList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class PercussiveHammerItem extends Item {

	public static final List<SoundPulse> pulses = new LinkedList<>();

	static {
		ServerTickEvents.END_WORLD_TICK.register(PercussiveHammerItem::tick);
		ServerWorldEvents.UNLOAD.register((server, world) -> unload(world));
	}

	public PercussiveHammerItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = world.getBlockState(pos);
		if (isSolidOpaqueFullCube(world, pos, state)) {
			if (!world.isClientSide()) {
				world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 2.0F);
				pulses.add(new SoundPulse(
					world.dimension(),
					pos.mutable(),
					context.getClickedFace().getOpposite()
				));
				ItemStackVersions.damage(context.getItemInHand(), context.getPlayer(), context.getHand());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static void tick(ServerLevel world) {
		if (!pulses.isEmpty()) {
			pulses.removeIf(pulse -> {
				if (pulse.world == world.dimension()) {
					if (++pulse.distance > 32) return true;
					BlockPos.MutableBlockPos pos = pulse.position.move(pulse.direction);
					BlockState state = world.getBlockState(pos);
					if (!isSolidOpaqueFullCube(world, pos, state)) {
						world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 2.0F);
						return true;
					}
				}
				return false;
			});
		}
	}

	public static void unload(ServerLevel world) {
		if (!pulses.isEmpty()) {
			ResourceKey<Level> key = world.dimension();
			pulses.removeIf(pulse -> pulse.world == key);
		}
	}

	public static boolean isSolidOpaqueFullCube(BlockGetter world, BlockPos pos, BlockState state) {
		return BlockStateVersions.isOpaqueFullCube(state, world, pos);
	}

	public static class SoundPulse {

		public ResourceKey<Level> world;
		public BlockPos.MutableBlockPos position;
		public Direction direction;
		public int distance;

		public SoundPulse(ResourceKey<Level> world, BlockPos.MutableBlockPos position, Direction direction) {
			this.world = world;
			this.position = position;
			this.direction = direction;
		}
	}
}