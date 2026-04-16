package builderb0y.bigglobe.blocks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class NaturalAutomataBlock extends AutomataBlock {

	public static final MapCodec<NaturalAutomataBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NaturalAutomataBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public NaturalAutomataBlock(Properties settings) {
		super(settings);
	}

	@Override
	public void attack(BlockState state, Level world, BlockPos pos, Player player) {
		super.attack(state, world, pos, player);
		if (world instanceof ServerLevel serverWorld) {
			this.activate(serverWorld, pos);
		}
	}

	@Override
	public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		super.fallOn(world, state, pos, entity, fallDistance);
		if (world instanceof ServerLevel serverWorld && !entity.isSuppressingBounce()) {
			this.activate(serverWorld, pos);
		}
	}

	@Override
	public boolean canActivateFromSpread(ServerLevel world, BlockPos pos, MutableBlockPos reuse) {
		return countFull(world, pos, reuse, 1) != 0;
	}

	@Override
	public void activate(ServerLevel world, BlockPos pos) {
		world.setBlock(pos, this.full, Block.UPDATE_ALL);
		Spreader.forWorld(world).add(new Spreader(world, pos));
	}

	public static class Spreader {

		public static final Map<ServerLevel, List<Spreader>> SPREADERS = new WeakHashMap<>();
		static {
			ServerTickEvents.START_LEVEL_TICK.register((ServerLevel world) -> {
				if ((world.getGameTime() & 1L) == 0L) {
					List<Spreader> spreaders;
					synchronized (SPREADERS) {
						spreaders = SPREADERS.get(world);
					}
					if (spreaders != null) {
						spreaders.forEach(Spreader::preTick);
						spreaders.removeIf(Spreader::postTick);
					}
				}
			});
			ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> {
				List<Spreader>[] spreaders;
				synchronized (SPREADERS) {
					spreaders = SPREADERS.values().toArray(new List[SPREADERS.size()]);
					SPREADERS.clear();
				}
				for (List<Spreader> list : spreaders) {
					list.forEach(Spreader::stop);
					list.clear();
				}
			});
		}

		public final ServerLevel world;
		public final ObjectOpenHashSet<BlockPos>
			emptyPositions  = new ObjectOpenHashSet<>(1024),
			mediumPositions = new ObjectOpenHashSet<>(256),
			fullPositions   = new ObjectOpenHashSet<>(256),
			nextPositions   = new ObjectOpenHashSet<>(256);

		public Spreader(ServerLevel world, BlockPos origin) {
			this.world = world;
			this.fullPositions.add(origin.immutable());
		}

		public static List<Spreader> forWorld(ServerLevel world) {
			synchronized (SPREADERS) {
				return SPREADERS.computeIfAbsent(world, (ServerLevel _) -> new LinkedList<>());
			}
		}

		public void preTick() {
			MutableBlockPos mutable = new MutableBlockPos();
			MutableBlockPos mutable2 = new MutableBlockPos();
			for (BlockPos position : this.fullPositions) {
				for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
					for (int offsetX = -1; offsetX <= 1; offsetX++) {
						for (int offsetY = -1; offsetY <= 1; offsetY++) {
							mutable.setWithOffset(position, offsetX, offsetY, offsetZ);
							if (!this.emptyPositions.contains(mutable) && !this.mediumPositions.contains(mutable) && !this.fullPositions.contains(mutable)) {
								if (this.world.shouldTickBlocksAt(mutable)) {
									BlockState state = this.world.getBlockState(mutable);
									if (state.getBlock() instanceof AutomataBlock automata && state == automata.empty && automata.canActivateFromSpread(this.world, mutable, mutable2)) {
										this.nextPositions.add(mutable.immutable());
									}
								}
							}
						}
					}
				}
			}
		}

		public boolean postTick() {
			if (this.nextPositions.isEmpty() && this.fullPositions.isEmpty() && this.mediumPositions.isEmpty()) {
				return true;
			}
			for (BlockPos position : this.mediumPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state == automata.medium) {
					this.world.setBlock(position, automata.empty, Block.UPDATE_ALL);
				}
				this.emptyPositions.add(position);
			}
			this.mediumPositions.clear();
			for (BlockPos position : this.fullPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state == automata.full) {
					this.world.setBlock(position, automata.medium, Block.UPDATE_ALL);
				}
				this.mediumPositions.add(position);
			}
			this.fullPositions.clear();
			for (BlockPos position : this.nextPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state == automata.empty) {
					this.world.setBlock(position, automata.full, Block.UPDATE_ALL);
				}
				this.fullPositions.add(position);
			}
			this.nextPositions.clear();
			return false;
		}

		public void stop() {
			this.emptyPositions.clear();
			for (BlockPos position : this.mediumPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state != automata.empty) {
					this.world.setBlock(position, automata.empty, Block.UPDATE_ALL);
				}
			}
			this.mediumPositions.clear();
			for (BlockPos position : this.fullPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state != automata.empty) {
					this.world.setBlock(position, automata.empty, Block.UPDATE_ALL);
				}
			}
			this.fullPositions.clear();
			this.nextPositions.clear();
		}
	}
}