package builderb0y.bigglobe.blocks;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ArtificialAutomataBlock extends AutomataBlock {

	public static final MapCodec<ArtificialAutomataBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ArtificialAutomataBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public ArtificialAutomataBlock(Properties settings) {
		super(settings);
	}

	@Override
	public boolean canActivateFromSpread(ServerLevel world, BlockPos pos, MutableBlockPos reuse) {
		int count = countFull(world, pos, reuse, 3);
		return count == 1 || count == 2;
	}

	@Override
	public void activate(ServerLevel world, BlockPos pos) {
		world.setBlock(pos, this.full, Block.UPDATE_ALL);
		Ticker.forWorld(world).addNeighbors(pos);
	}

	public static class Ticker {

		public static final Map<ServerLevel, Ticker> TICKERS = new WeakHashMap<>();
		static {
			ServerTickEvents.START_LEVEL_TICK.register((ServerLevel world) -> {
				Ticker ticker;
				synchronized (TICKERS) {
					ticker = TICKERS.get(world);
				}
				if (ticker != null) {
					ticker.tick();
				}
			});
			ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> {
				Ticker[] tickers;
				synchronized (TICKERS) {
					tickers = TICKERS.values().toArray(new Ticker[TICKERS.size()]);
					TICKERS.clear();
				}
				for (Ticker ticker : tickers) {
					ticker.stop();
				}
			});
		}

		public final ServerLevel world;
		public ObjectLinkedOpenHashSet<BlockPos> tickingPositions, swap;
		public Object2ObjectLinkedOpenHashMap<BlockPos, Replacement> replacements;

		public Ticker(ServerLevel world) {
			this.world = world;
			this.tickingPositions = new ObjectLinkedOpenHashSet<>(256);
			this.swap = new ObjectLinkedOpenHashSet<>();
			this.replacements = new Object2ObjectLinkedOpenHashMap<>(256);
		}

		public static Ticker forWorld(ServerLevel world) {
			synchronized (TICKERS) {
				return TICKERS.computeIfAbsent(world, Ticker::new);
			}
		}

		public void addNeighbors(BlockPos center) {
			addNeighbors(this.tickingPositions, center);
		}

		public static void addNeighbors(ObjectLinkedOpenHashSet<BlockPos> positions, BlockPos center) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				for (int offsetX = -1; offsetX <= 1; offsetX++) {
					for (int offsetY = -1; offsetY <= 1; offsetY++) {
						positions.add(center.offset(offsetX, offsetY, offsetZ));
					}
				}
			}
		}

		public void tick() {
			if ((this.world.getGameTime() & 1L) != 0) return;
			ObjectLinkedOpenHashSet<BlockPos> tickingPositions = this.tickingPositions;
			if (!tickingPositions.isEmpty()) {
				ObjectLinkedOpenHashSet<BlockPos> swap = this.swap;
				Object2ObjectLinkedOpenHashMap<BlockPos, Replacement> replacements = this.replacements;
				MutableBlockPos mutable = new MutableBlockPos();
				for (BlockPos position : tickingPositions) {
					BlockState state = this.world.getBlockState(position);
					if (state.getBlock() instanceof AutomataBlock automata) {
						if (this.world.shouldTickBlocksAt(position)) {
							if (state == automata.empty && automata.canActivateFromSpread(this.world, position, mutable)) {
								replacements.put(position, new Replacement(state, automata.full));
								addNeighbors(swap, position);
							}
							else if (state == automata.full) {
								replacements.put(position, new Replacement(state, automata.medium));
								swap.add(position);
							}
							else {
								replacements.put(position, new Replacement(state, automata.empty));
							}
						}
						else if (state != automata.empty) {
							replacements.put(position, new Replacement(state, automata.empty));
						}
					}
				}
				tickingPositions.clear();
				this.swap = tickingPositions;
				this.tickingPositions = swap;
				for (Map.Entry<BlockPos, Replacement> entry : replacements.entrySet()) {
					if (this.world.getBlockState(entry.getKey()) == entry.getValue().from) {
						this.world.setBlock(entry.getKey(), entry.getValue().to, Block.UPDATE_ALL);
					}
				}
				replacements.clear();
			}
		}

		public void stop() {
			for (BlockPos position : this.tickingPositions) {
				BlockState state = this.world.getBlockState(position);
				if (state.getBlock() instanceof AutomataBlock automata && state != automata.empty) {
					this.world.setBlock(position, automata.empty, Block.UPDATE_ALL);
				}
			}
			this.tickingPositions.clear();
		}
	}

	public static record Replacement(BlockState from, BlockState to) {}
}