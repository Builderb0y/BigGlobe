package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.OutOfVramException;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.SafeCloseable;
import builderb0y.bigglobe.rendering.lods.LodGenerator.LodSupply;
import builderb0y.bigglobe.rendering.lods.LodRenderer.FogParams;
import builderb0y.bigglobe.rendering.lods.LodRenderer.MeshUploader;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.FastMath.Log;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;

@Environment(EnvType.CLIENT)
public class LodSystem implements SafeCloseable {

	static {
		WorldRenderEvents.AFTER_SETUP.register((WorldRenderContext context) -> {
			LodSystem system = ((LodSystemHolder)(context.worldRenderer())).bigglobe_getLodSystem();
			if (system != null && !context.worldRenderer().hasBlindnessOrDarkness(context.camera())) {
				system.render(context);
			}
		});
	}

	public static void init() {}

	public ClientWorld world;
	public LodGenerator generator;
	public LodQuadTree tree;
	public LodRenderer renderer;
	public LodFrustum frustum;
	public FogParams fog;
	public double currentQuality, qualityLimit;

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
			this.frustum = new LodFrustum();
			this.fog = new FogParams();
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
		this.makeRequests(this.tree, false);
		this.renderer.oom();
	}

	public void render(WorldRenderContext context) {
		if (!this.generator.running) {
			BigGlobeMod.LOGGER.error("LOD system shutting down due to exception in mesh generator thread. Press F3+A to restart it.");
			this.close();
			((LodSystemHolder)(context.worldRenderer())).bigglobe_setLodSystem(null);
			return;
		}

		GLException failure = null;

		#if MC_VERSION >= MC_1_21_2
			Profiler profiler = net.minecraft.util.profiler.Profilers.get();
		#else
			Profiler profiler = context.profiler();
		#endif

		profiler.push("BG LODs");

		if (this.generator.hasSupply()) {
			profiler.push("Process Supply");
			try {
				try (MeshUploader uploader = this.renderer.finishMeshing()) {
					for (LodSupply supply; (supply = this.generator.getSupply()) != null;) {
						supply.apply(uploader);
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
		else if (this.generator.requests.isEmpty() && this.tree.passes != null) {
			this.currentQuality = Math.min(this.currentQuality + 0.0625D, this.qualityLimit);
		}

		if (failure == null) {
			if (this.tree.passes != null) {
				profiler.push("Setup");
				this.frustum.setup(context);
				this.fog.farPlaneDistance = this.frustum.farClippingPlane;
				profiler.swap("Request");
				this.makeRequests(this.tree, true);
				profiler.swap("Visibility");
				this.computeVisibility(this.tree, null);
				profiler.swap("Opaque");

				//must ensure either both passes are bound and unbound, or neither are.
				try (var $ = this.renderer.bind(context, this.fog, false)) {
					GLException.check();
					this.drawTree(context, this.tree);
					GLException.check();
				}
				catch (GLException exception) {
					failure = exception;
				}
				profiler.swap("Translucent");
				try (var $ = this.renderer.bind(context, this.fog, true)) {
					GLException.check();
					if (failure == null) {
						this.drawTree(context, this.tree);
						GLException.check();
					}
				}
				catch (GLException exception) {
					if (failure == null) failure = exception;
					else failure.addSuppressed(exception);
				}
				profiler.swap("Restore");
				this.frustum.restore(context);
				profiler.pop();
			}
			else if (!this.tree.isQueued()) {
				this.generator.request(this.tree);
				this.tree.split();
			}
		}

		profiler.pop();

		if (failure != null) {
			BigGlobeMod.LOGGER.error("LOD rendering encountered an unexpected exception and will now stop. Press F3+A to restart it.", failure);
			this.close();
			((LodSystemHolder)(context.worldRenderer())).bigglobe_setLodSystem(null);
		}
	}

	public static final int NONE = 0, SOME = 1, ALL = 2;

	public int computeVisibility(LodQuadTree tree, Boolean frustumStatus) {
		if (frustumStatus == null) {
			frustumStatus = this.frustum.test(
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

	public void drawTree(WorldRenderContext context, LodQuadTree tree) {
		if (tree.canRender()) {
			assert tree.passes != null : "renderable tree has no passes";
			Vec3d cameraPos = context.camera().getPos();
			this.renderer.draw(
				tree.passes,
				(float)(tree.minX() - cameraPos.x),
				(float)(            - cameraPos.y),
				(float)(tree.minZ() - cameraPos.z),
				(float)(1 << (tree.level - LodQuadTree.MIN_LEVEL))
			);
		}
		else if (tree.isTraversableForRender()) {
			assert tree.x0z0 != null && tree.x0z1 != null && tree.x1z0 != null && tree.x1z1 != null : "tree with missing children is traversable";
			this.drawTree(context, tree.x0z0);
			this.drawTree(context, tree.x0z1);
			this.drawTree(context, tree.x1z0);
			this.drawTree(context, tree.x1z1);
		}
	}

	public void makeRequests(LodQuadTree tree, boolean canSplit) {
		double squareDistance = this.squareDistanceTo(tree);
		tree.setInRange(squareDistance < BigGlobeMath.squareD(this.frustum.farClippingPlane));
		double idealQuality = this.computeIdealQuality(squareDistance, tree);
		if (tree.isQueued()) {
			if (idealQuality > this.currentQuality + 0.5D) {
				tree.merge();
				return;
			}
		}
		else if (canSplit) {
			if (idealQuality < this.currentQuality) {
				if (squareDistance < BigGlobeMath.squareD(this.frustum.generationBuffer)) {
					this.generator.request(tree);
				}
				if (tree.level > LodQuadTree.MIN_LEVEL) {
					tree.split();
				}
			}
		}
		if (tree.level > LodQuadTree.MIN_LEVEL) {
			if (tree.x0z0 != null) this.makeRequests(tree.x1z1, canSplit);
			if (tree.x0z1 != null) this.makeRequests(tree.x0z1, canSplit);
			if (tree.x1z0 != null) this.makeRequests(tree.x1z0, canSplit);
			if (tree.x1z1 != null) this.makeRequests(tree.x0z0, canSplit);
		}
	}

	public double squareDistanceTo(LodQuadTree tree) {
		double closestX = MathHelper.clamp(this.frustum.x, tree.minX(), tree.maxX());
		double closestY = MathHelper.clamp(this.frustum.y, this.generator.generatorParams.minY, this.generator.generatorParams.maxY);
		double closestZ = MathHelper.clamp(this.frustum.z, tree.minZ(), tree.maxZ());
		return BigGlobeMath.squareD(closestX - this.frustum.x, closestY - this.frustum.y, closestZ - this.frustum.z);
	}

	public double computeIdealQuality(double squareDistance, LodQuadTree tree) {
		return Log.fastLog2(squareDistance) * 0.5D - tree.level;
	}
}