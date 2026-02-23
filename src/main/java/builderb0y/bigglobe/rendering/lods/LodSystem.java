package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.math.FastMath;
import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.OutOfVramException;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LoadMode;
import builderb0y.bigglobe.rendering.lods.LodRenderer.LodRenderState;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LodSupply;
import builderb0y.bigglobe.rendering.lods.LodRenderer.MeshUploader;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

@Environment(EnvType.CLIENT)
public class LodSystem implements SafeCloseable {

	public static final long CHUNK_REBUILD_DELAY = 2000L;

	static {
		#if MC_VERSION >= MC_1_21_9
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_EXTRACTION.register(context -> {
				LodSystem system = LodSystemHolder.of(context.worldRenderer()).bigglobe_getLodSystem();
				if (system != null) {
					system.renderState.setup(context);
					system.renderingThisFrame = shouldRenderLods(context.camera());
				}
			});
			net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.START_MAIN.register(context -> {
				LodSystem system = LodSystemHolder.of(context.worldRenderer()).bigglobe_getLodSystem();
				if (system != null) system.draw();
			});
		#else
			net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_SETUP.register(
				context -> {
					LodSystem system = LodSystemHolder.of(context.worldRenderer()).bigglobe_getLodSystem();
					if (system != null) {
						system.renderState.setup(context);
						system.renderingThisFrame = shouldRenderLods(context.camera());
						system.draw();
					}
				}
			);
		#endif
	}

	public static void init() {}

	/**
	I would normally use {@link WorldRenderer#hasBlindnessOrDarkness(Camera)} for this,
	but sodium mixes into that to also cancel when the camera is underwater.
	*/
	public static boolean shouldRenderLods(Camera camera) {
		return !(camera.getFocusedEntity() instanceof LivingEntity entity && (entity.hasStatusEffect(StatusEffects.BLINDNESS) || entity.hasStatusEffect(StatusEffects.DARKNESS)));
	}

	public ClientWorld world;
	public LodGenerator generator;
	public LodQuadTree tree;
	public LodRenderer renderer;
	public LodRenderState renderState;
	public double currentQuality, qualityLimit, loadDistance;
	public int levelLimit = LodQuadTree.MAX_LEVEL;
	public boolean renderingThisFrame;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.generator, this.tree, this.renderer);
	}

	public LodSystem(ClientWorld world, ClientGeneratorParams generator) {
		String message = GLException.checkMessage();
		if (message != null) BigGlobeMod.LOGGER.warn("A GL exception occurred just before LOD system initialization: " + message);
		try {
			this.world = world;
			this.qualityLimit = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
			this.renderer = BigGlobeConfig.INSTANCE.get().lodRendering.createRendererBackend();
			this.generator = new LodGenerator(this, generator);
			this.tree = new LodQuadTree(-(1 << (LodQuadTree.MAX_LEVEL - 1)), -(1 << (LodQuadTree.MAX_LEVEL - 1)), LodQuadTree.MAX_LEVEL);
			this.renderState = new LodRenderState();
			GLException.check();
			this.generator.start();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	public static void reload(LodSystemHolder holder, ClientWorld world) {
		ClientState state = ClientState.get(world);
		reload(holder, world, state != null ? state.generatorParams : null);
	}

	public static void reload(LodSystemHolder holder, ClientWorld world, ClientGeneratorParams params) {
		LodSystem system = holder.bigglobe_getLodSystem();
		if (system != null) {
			system.close();
			holder.bigglobe_setLodSystem(null);
		}
		if (world != null && params != null && params.layer != null && BigGlobeConfig.INSTANCE.get().lodRendering.renderingEnabled()) {
			try {
				holder.bigglobe_setLodSystem(new LodSystem(world, params));
			}
			catch (Exception exception) {
				BigGlobeMod.LOGGER.error("Failed to setup LOD renderer:", exception);
			}
		}
	}

	public static int countTotalNodes(LodQuadTree root) {
		return root == null ? 0 : (
			+ countTotalNodes(root.x0z0)
			+ countTotalNodes(root.x1z0)
			+ countTotalNodes(root.x0z1)
			+ countTotalNodes(root.x1z1)
			+ 1
		);
	}

	public static int countRenderingNodes(LodQuadTree root) {
		if (root != null) {
			if (root.canRender()) {
				return 1;
			}
			else if (root.isTraversableForRender()) {
				return (
					+ countRenderingNodes(root.x0z0)
					+ countRenderingNodes(root.x0z1)
					+ countRenderingNodes(root.x1z0)
					+ countRenderingNodes(root.x1z1)
				);
			}
		}
		return 0;
	}

	public static int countMeshes(LodQuadTree root) {
		return root == null ? 0 : (
			+ (root.passes != null ? 1 : 0)
			+ countMeshes(root.x0z0)
			+ countMeshes(root.x0z1)
			+ countMeshes(root.x1z0)
			+ countMeshes(root.x1z1)
		);
	}

	public static int countDirty(LodQuadTree root) {
		return root == null ? 0 : (
			+ (root.rebuildTime != Long.MAX_VALUE ? 1 : 0)
			+ countDirty(root.x0z0)
			+ countDirty(root.x0z1)
			+ countDirty(root.x1z0)
			+ countDirty(root.x1z1)
		);
	}

	public void oom() {
		this.currentQuality = Math.min(this.currentQuality, this.qualityLimit -= 0.5D);

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		Text text = Text.translatable("bigglobe.lod.oom", this.qualityLimit);
		if (player != null) {
			player.sendMessage(text, false);
		}
		else {
			BigGlobeMod.LOGGER.warn(text.getString());
		}
		this.makeRequests(this.tree, System.currentTimeMillis(), false);
		this.renderer.oom();
	}

	public boolean canRender() {
		return this.renderingThisFrame;
	}

	public void draw() {
		String existingMessage = GLException.checkMessage();
		if (existingMessage != null) {
			BigGlobeMod.LOGGER.warn("Caught GL exception from some other unknown mod right before LOD rendering: " + existingMessage);
		}
		if ((this.levelLimit > LodQuadTree.MIN_LEVEL || this.currentQuality == 0.0D) && BigGlobeConfig.INSTANCE.get().lodRendering.showProgress) {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) {
				int percent = 100 - (this.levelLimit - LodQuadTree.MIN_LEVEL) * 100 / (LodQuadTree.MAX_LEVEL - LodQuadTree.MIN_LEVEL);
				player.sendMessage(Text.translatable("bigglobe.lod.generating", percent), true);
			}
		}
		try {
			this.doDraw();
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("An exception occurred while rendering LODs. LOD rendering will now disable itself to prevent further problems. You can press F3+A to attempt to restart it.", exception);
			this.close();
			this.renderState.lodSystemHolder.bigglobe_setLodSystem(null);
		}
	}

	public void doDraw() {
		if (!this.generator.running) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to exception in mesh generator thread. Press F3+A to restart it.");
			this.close();
			this.renderState.lodSystemHolder.bigglobe_setLodSystem(null);
			return;
		}

		GLException failure = null;

		#if MC_VERSION >= MC_1_21_2
			Profiler profiler = net.minecraft.util.profiler.Profilers.get();
		#else
			Profiler profiler = this.renderState.profiler;
		#endif

		profiler.push("BG LODs");

		LodChunkCache cache = this.generator.chunkCache;
		if (cache != null) {
			profiler.push("Process Dirty Chunks");
			cache.processDirtyChunks(this);
			profiler.pop();
		}

		if (this.generator.hasSupply()) {
			profiler.push("Process Supply");
			try {
				try (MeshUploader uploader = this.renderer.finishMeshing()) {
					for (LodSupply supply; (supply = this.generator.getSupply()) != null;) {
						supply.apply(uploader, this.generator.maxLoadLevel);
					}
				}
				GLException.check();
			}
			catch (OutOfVramException exception) {
				this.oom();
			}
			catch (GLException exception) {
				failure = exception;
			}
			profiler.pop();
		}
		LodQuadTree atPlayer = this.getTreeAtPlayerForDowngradeChecking();
		if (atPlayer != null) {
			if (atPlayer.level > this.levelLimit) {
				this.currentQuality = 0.0D;
			}
			else if (this.generator.requests.isEmpty() && this.tree.passes != null) {
				this.currentQuality = Math.min(this.currentQuality + 0.0625D, this.qualityLimit);
				if (this.currentQuality == this.qualityLimit) {
					this.loadDistance = Math.min(this.loadDistance + 16.0D, this.renderState.frustum.generationBuffer);
				}
			}
			this.levelLimit = Math.max(atPlayer.level - 1, LodQuadTree.MIN_LEVEL);
		}
		else {
			if (this.generator.requests.isEmpty() && this.tree.passes != null) {
				if (this.levelLimit > LodQuadTree.MIN_LEVEL) {
					this.levelLimit--;
				}
				else {
					this.currentQuality = Math.min(this.currentQuality + 0.0625D, this.qualityLimit);
					if (this.currentQuality == this.qualityLimit) {
						this.loadDistance = Math.min(this.loadDistance + 16.0D, this.renderState.frustum.generationBuffer);
					}
				}
			}
		}

		if (failure == null && this.canRender()) {
			if (this.tree.passes != null) {
				profiler.swap("Request");
				this.makeRequests(this.tree, System.currentTimeMillis(), true);
				profiler.swap("Visibility");
				this.computeVisibility(this.tree, null);
				profiler.swap("Opaque");

				//must ensure either both passes are bound and unbound, or neither are.
				try (var $ = this.renderer.bind(this.renderState, false)) {
					GLException.check();
					this.drawTree(this.tree);
					GLException.check();
				}
				catch (GLException exception) {
					failure = exception;
				}
				profiler.swap("Translucent");
				try (var $ = this.renderer.bind(this.renderState, true)) {
					GLException.check();
					if (failure == null) {
						this.drawTree(this.tree);
						GLException.check();
					}
				}
				catch (GLException exception) {
					if (failure == null) failure = exception;
					else failure.addSuppressed(exception);
				}
				profiler.pop();
			}
			else if (!this.tree.isQueued()) {
				this.generator.request(this.tree, LoadMode.GENERATE_ONLY);
				this.tree.split();
			}
		}

		profiler.pop();

		if (failure != null) {
			BigGlobeMod.LOGGER.error("LOD rendering encountered an unexpected exception and will now stop. Press F3+A to restart it.", failure);
			this.close();
			this.renderState.lodSystemHolder.bigglobe_setLodSystem(null);
		}
	}

	public LodQuadTree getTreeAtPlayerForDowngradeChecking() {
		LodFrustum frustum = this.renderState.frustum;
		//don't downgrade anything if we're above the world,
		//because the LodQuadTree at the player's horizontal position
		//will not match our current quality in this case no matter how long we wait.
		if (frustum.y > HeightLimitViewVersions.getMaxY(this.world)) {
			return null;
		}
		//using previous frame data is fine here.
		LodQuadTree atPlayer = this.tree;
		while (!atPlayer.canRender() && atPlayer.isTraversableForRender()) {
			LodQuadTree next;
			if (frustum.x >= atPlayer.midX()) {
				if (frustum.z >= atPlayer.midZ()) {
					next = atPlayer.x1z1;
				}
				else {
					next = atPlayer.x1z0;
				}
			}
			else {
				if (frustum.z >= atPlayer.midZ()) {
					next = atPlayer.x0z1;
				}
				else {
					next = atPlayer.x0z0;
				}
			}
			if (next != null) atPlayer = next;
			else break;
		}
		return atPlayer;
	}

	public static final int NONE = 0, SOME = 1, ALL = 2;

	public int computeVisibility(LodQuadTree tree, Boolean frustumStatus) {
		if (frustumStatus == null) {
			frustumStatus = this.renderState.frustum.test(
				tree.minX(),
				this.generator.generatorParams.minY,
				tree.minZ(),
				tree.maxX(),
				this.generator.generatorParams.maxY,
				tree.maxZ()
			);
		}
		int x1z1 = tree.x1z1 != null ? this.computeVisibility(tree.x1z1, frustumStatus) : NONE;
		int x0z1 = tree.x0z1 != null ? this.computeVisibility(tree.x0z1, frustumStatus) : NONE;
		int x1z0 = tree.x1z0 != null ? this.computeVisibility(tree.x1z0, frustumStatus) : NONE;
		int x0z0 = tree.x0z0 != null ? this.computeVisibility(tree.x0z0, frustumStatus) : NONE;
		int childrenState;
		if (x0z0 == NONE && x1z0 == NONE && x0z1 == NONE && x1z1 == NONE) {
			childrenState = NONE;
		}
		else if (x0z0 == ALL && x1z0 == ALL && x0z1 == ALL && x1z1 == ALL) {
			childrenState = ALL;
		}
		else {
			childrenState = SOME;
		}
		tree.setTraversableForRender(childrenState != NONE && frustumStatus != Boolean.FALSE);
		tree.setCanRender(childrenState != ALL && tree.passes != null && frustumStatus != Boolean.FALSE);
		return tree.passes != null || !tree.isInRange() ? ALL : childrenState;
	}

	public void drawTree(LodQuadTree tree) {
		if (tree.canRender()) {
			assert tree.passes != null : "renderable tree has no passes";
			LodFrustum frustum = this.renderState.frustum;
			this.renderer.draw(
				tree.passes,
				(float)(tree.minX() - frustum.x),
				(float)(            - frustum.y),
				(float)(tree.minZ() - frustum.z),
				(float)(1 << (tree.level - LodQuadTree.MIN_LEVEL))
			);
		}
		else if (tree.isTraversableForRender()) {
			assert tree.x0z0 != null && tree.x0z1 != null && tree.x1z0 != null && tree.x1z1 != null : "tree with missing children is traversable";
			this.drawTree(tree.x0z0);
			this.drawTree(tree.x0z1);
			this.drawTree(tree.x1z0);
			this.drawTree(tree.x1z1);
		}
	}

	public void makeRequests(LodQuadTree tree, long time, boolean canSplit) {
		double squareDistance = this.squareDistanceTo(tree);
		tree.setInRange(squareDistance < BigGlobeMath.squareD(this.renderState.frustum.farClippingPlane));
		double idealQuality = this.computeIdealQuality(squareDistance, tree);
		if (idealQuality > this.qualityLimit + 0.5D) {
			tree.merge();
		}
		else {
			if (!tree.isQueued() && canSplit) {
				if (tree.level > this.levelLimit && idealQuality < this.currentQuality) {
					tree.split();
				}
				boolean request = false;
				LoadMode loadMode = null;
				if (tree.passes == null && squareDistance < BigGlobeMath.squareD(this.renderState.frustum.generationBuffer)) {
					request = true;
					loadMode = squareDistance < BigGlobeMath.squareD(this.loadDistance) ? LoadMode.LOAD_OR_GENERATE : LoadMode.GENERATE_ONLY;
				}
				if (time > tree.rebuildTime && squareDistance < BigGlobeMath.squareD(this.loadDistance)) {
					request = true;
					loadMode = tree.passes == null ? LoadMode.LOAD_OR_GENERATE : LoadMode.LOAD_ONLY;
				}
				if (request) {
					this.generator.request(tree, loadMode);
				}
			}
			if (tree.level > LodQuadTree.MIN_LEVEL) {
				if (tree.x0z0 != null) this.makeRequests(tree.x1z1, time, canSplit);
				if (tree.x0z1 != null) this.makeRequests(tree.x0z1, time, canSplit);
				if (tree.x1z0 != null) this.makeRequests(tree.x1z0, time, canSplit);
				if (tree.x1z1 != null) this.makeRequests(tree.x0z0, time, canSplit);
			}
		}
	}

	public double squareDistanceTo(LodQuadTree tree) {
		LodFrustum frustum = this.renderState.frustum;
		double closestX = MathHelper.clamp(frustum.x, tree.minX(), tree.maxX());
		double closestY = MathHelper.clamp(frustum.y, this.generator.generatorParams.minY, this.generator.generatorParams.maxY);
		double closestZ = MathHelper.clamp(frustum.z, tree.minZ(), tree.maxZ());
		return BigGlobeMath.squareD(closestX - frustum.x, closestY - frustum.y, closestZ - frustum.z);
	}

	public double computeIdealQuality(double squareDistance, LodQuadTree tree) {
		return FastMath.Log.fastLog2(squareDistance) * 0.5D - tree.level;
	}

	public void invalidateChunkLater(ChunkPos chunkPos) {
		LodChunkCache cache = this.generator.chunkCache;
		if (cache != null) cache.invalidateChunkLater(chunkPos);
	}

	public void invalidateChunkNow(ChunkPos chunkPos) {
		if (this.tree != null) {
			this.recursiveInvalidateBlock(this.tree, System.currentTimeMillis() + CHUNK_REBUILD_DELAY, chunkPos.x << 4, chunkPos.z << 4);
		}
	}

	public void recursiveInvalidateBlock(LodQuadTree tree, long time, int blockX, int blockZ) {
		if (tree.level - LodQuadTree.MIN_LEVEL < this.generator.maxLoadLevel) {
			tree.rebuildTime = time;
		}
		if (blockZ >= tree.midZ()) {
			if (blockX >= tree.midX()) {
				if (tree.x1z1 != null) this.recursiveInvalidateBlock(tree.x1z1, time, blockX, blockZ);
			}
			else {
				if (tree.x0z1 != null) this.recursiveInvalidateBlock(tree.x0z1, time, blockX, blockZ);
			}
		}
		else {
			if (blockX >= tree.midX()) {
				if (tree.x1z0 != null) this.recursiveInvalidateBlock(tree.x1z0, time, blockX, blockZ);
			}
			else {
				if (tree.x0z0 != null) this.recursiveInvalidateBlock(tree.x0z0, time, blockX, blockZ);
			}
		}
	}
}