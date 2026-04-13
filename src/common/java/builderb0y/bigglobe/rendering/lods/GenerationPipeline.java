package builderb0y.bigglobe.rendering.lods;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LoadMode;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public abstract class GenerationPipeline extends Thread implements SafeCloseable {

	public static final long CHUNKLOAD_DELAY_MS = 2000L;

	public final LodSystem system;
	public final LodGenerator<?> generator;
	public final LodMesher mesher;
	public final LinkedBlockingQueue<Request> requests;
	public final ConcurrentLinkedQueue<Supply> supply;
	public final AtomicInteger activeMeshers;
	public volatile boolean running = true;

	public GenerationPipeline(LodSystem system, LodGenerator<?> generator, LodMesher mesher) {
		super("Big Globe LOD generation pipeline");
		this.system = system;
		this.generator = generator;
		this.mesher = mesher;
		this.requests = new LinkedBlockingQueue<>();
		this.supply = new ConcurrentLinkedQueue<>();
		this.activeMeshers = new AtomicInteger(0);
		this.setDaemon(true);
	}

	public String f3Message() {
		int
			loadOnly = 0,
			loadOrGen = 0,
			genOnly = 0,
			total = 0;
		for (Request request : this.requests) {
			switch (request.mode()) {
				case LOAD_ONLY -> loadOnly++;
				case LOAD_OR_GENERATE -> loadOrGen++;
				case GENERATE_ONLY -> genOnly++;
			}
			total++;
		}
		return "[BG] LOD Req L: " + loadOnly + ", G: " + genOnly + ", LG: " + loadOrGen + ", T: " + total;
	}

	public void request(LodNode node, LoadMode mode) {
		assert !node.isQueued() : "attempt to request already-queued tree";
		this.requests.add(new Request(node, mode, this.system.getRenderer().createPacker()));
		node.rebuildTime = Long.MAX_VALUE;
		node.setQueued(true);
	}

	@Override
	public void run() {
		while (this.running) try {
			Request request = null;
			try {
				request = this.requests.take();
			}
			catch (InterruptedException ignored) {}

			if (request != null) {
				if (request.node.isQueued()) {
					this.processRequest(request);
				}
				else {
					this.supply.add(new Supply(request.node, request.packer, false));
				}
			}
		}
		catch (Throwable problem) {
			BigGlobeMod.LOGGER.error("Exception in LOD generation pipeline thread:", problem);
			break;
		}
	}

	@Environment(EnvType.CLIENT)
	public static record Request(
		LodNode node,
		LoadMode mode,
		QuadPacker<?> packer
	)
	implements SafeCloseable {

		@Override
		public void close() {
			this.packer.close();
		}
	}

	@Environment(EnvType.CLIENT)
	public static record Supply(
		LodNode node,
		QuadPacker<?> packer,
		boolean success
	)
	implements SafeCloseable {

		@Override
		public void close() {
			this.packer.close();
		}
	}

	public void processRequest(Request request) {
		ColumnBlockGetter generatedArea;
		try {
			generatedArea = this.generateWithPadding(this.system.getTree().getBounds(request.node), request.node.level, request.mode);
		}
		catch (Throwable throwable) {
			this.supply.add(new Supply(request.node, request.packer, false));
			throw AutoCodecUtil.rethrow(throwable);
		}
		if (generatedArea == null) {
			this.supply.add(new Supply(request.node, request.packer, false));
			return;
		}
		this.activeMeshers.incrementAndGet();
		BigGlobeThreadPool.lodExecutor().execute(() -> {
			try (generatedArea) {
				this.mesher.mesh(generatedArea, request.packer);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception in LOD meshing:", throwable);
				this.supply.add(new Supply(request.node, request.packer, false));
				this.running = false;
				return;
			}
			finally {
				this.activeMeshers.decrementAndGet();
			}
			this.supply.add(new Supply(request.node, request.packer, true));
		});
	}

	public abstract @Nullable ColumnBlockGetter generateWithPadding(BoundingBox area, byte lod, LoadMode mode);

	public void processSupply() {
		byte maxLoadLevel = this.generator.maxLoadLevel;
		for (Supply supply; (supply = this.supply.poll()) != null;) {
			LodNode node = supply.node();
			try {
				if (node.isQueued()) {
					if (supply.success()) {
						if (node.mesh != null) {
							node.mesh.close();
							node.mesh = null;
						}
						else if (node.level < maxLoadLevel) {
							node.rebuildTime = System.currentTimeMillis() + GenerationPipeline.CHUNKLOAD_DELAY_MS;
						}
						node.mesh = supply.packer().build(node::toString);
					}
					node.setQueued(false);
				}
			}
			catch (Throwable throwable) {
				if (node.isQueued()) {
					node.setQueued(false); //try again later.
				}
				throw AutoCodecUtil.rethrow(throwable);
			}
			finally {
				supply.close();
			}
		}
	}

	@Override
	public void close() {
		if (Thread.currentThread() == this) {
			throw new IllegalThreadStateException("This thread cannot close itself.");
		}
		this.running = false;
		try {
			this.interrupt();
			this.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.warn("Who's trying to interrupt the shutdown process?", exception);
		}
		long nextLog = System.currentTimeMillis() + 5000L;
		for (int meshers; (meshers = this.activeMeshers.get()) > 0;) {
			Thread.onSpinWait();
			if (System.currentTimeMillis() >= nextLog) {
				BigGlobeMod.LOGGER.info("Waiting for " + meshers + " meshing task(s) to complete...");
				nextLog += 5000L;
			}
		}
		ResourceTracker.closeAll(this.requests);
		ResourceTracker.closeAll(this.supply);
	}
}