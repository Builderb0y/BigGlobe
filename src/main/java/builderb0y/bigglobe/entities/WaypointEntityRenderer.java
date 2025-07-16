package builderb0y.bigglobe.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.WaypointEntity.Orbit;
import builderb0y.bigglobe.hyperspace.HyperspaceRendering;
import builderb0y.bigglobe.math.BigGlobeMath;

@Environment(EnvType.CLIENT)
public class WaypointEntityRenderer extends BigGlobeEntityRenderer<WaypointEntity, WaypointEntityRenderer.State> {

	public static final Identifier TEXTURE = BigGlobeMod.mcID("textures/particle/flash.png");
	public static RenderLayer LIGHTNING_LAYER;
	static {
		RenderLayer layer = RenderLayer.getLightning();
		got:
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			try {
				layer = (RenderLayer)(Class.forName("net.irisshaders.iris.pathways.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using new iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {}
			try {
				layer = (RenderLayer)(Class.forName("net.coderbot.iris.pipeline.LightningHandler").getDeclaredField("IRIS_LIGHTNING").get(null));
				BigGlobeMod.LOGGER.info("Using old iris lightning render layer.");
				break got;
			}
			catch (Exception ignored) {}
			BigGlobeMod.LOGGER.info("Could not locate iris lightning render layer...");
		}
		LIGHTNING_LAYER = layer;
	}

	public WaypointEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void doRender(WaypointEntityRenderer.State state, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light) {
		VertexConsumer buffer = vertexConsumerProvider.getBuffer(LIGHTNING_LAYER);
		Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
		Vector3f cameraToPos = new Vector3f(
			(float)(state.x - camera.x),
			(float)(state.y - camera.y + 1.0D),
			(float)(state.z - camera.z)
		);
		Vector4f
			transform    = new Vector4f();
		Vector3f
			position     = new Vector3f(),
			tangent      = new Vector3f(),
			side         = new Vector3f(),
			prevPosition = new Vector3f(),
			prevTangent  = new Vector3f(),
			prevSide     = new Vector3f();
		float
			size         = 0.0F,
			prevSize     = 0.0F;
		int maxOrbits = BigGlobeMath.roundI(state.health / WaypointEntity.MAX_HEALTH * state.orbits.length);
		for (int orbitIndex = 0; orbitIndex < maxOrbits; orbitIndex++) {
			Orbit orbit = state.orbits[orbitIndex];
			for (int history = 0; history <= 32; history++) {
				if (history != 0) {
					prevPosition.set(position);
					prevTangent.set(tangent);
					prevSide.set(side);
					prevSize = size;
				}
				float historyF = history * 0.03125F;
				orbit.getPositionAndVelocity(position, tangent, historyF * historyF, state.partialTicks);
				position.add(cameraToPos).cross(tangent, side).normalize();
				size = ((float)(Math.sqrt(historyF))) * BigGlobeMath.squareF(1.0F - historyF) * 0.25F;
				if (history != 0) {
					vertex(
						buffer,
						matrices,
						prevPosition.x,
						prevPosition.y,
						prevPosition.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						prevPosition.x + prevSide.x * prevSize,
						prevPosition.y + prevSide.y * prevSize,
						prevPosition.z + prevSide.z * prevSize,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x + side.x * size,
						position.y + side.y * size,
						position.z + side.z * size,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x,
						position.y,
						position.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);



					vertex(
						buffer,
						matrices,
						prevPosition.x - prevSide.x * prevSize,
						prevPosition.y - prevSide.y * prevSize,
						prevPosition.z - prevSide.z * prevSize,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						prevPosition.x,
						prevPosition.y,
						prevPosition.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x,
						position.y,
						position.z,
						orbit.r * 0.5F + 0.5F,
						orbit.g * 0.5F + 0.5F,
						orbit.b * 0.5F + 0.5F,
						1.0F,
						transform
					);
					vertex(
						buffer,
						matrices,
						position.x - side.x * size,
						position.y - side.y * size,
						position.z - side.z * size,
						orbit.r * 0.5F,
						orbit.g * 0.5F,
						orbit.b * 0.5F,
						0.0F,
						transform
					);
				}
			}
		}
		#if MC_VERSION < MC_1_21_2
			HyperspaceRendering.markWaypointVisible(state.x, state.y, state.z, state.age, state.health);
		#endif
	}

	public static void vertex(
		VertexConsumer buffer,
		MatrixStack matrices,
		float x,
		float y,
		float z,
		float r,
		float g,
		float b,
		float a,
		Vector4f scratch
	) {
		#if MC_VERSION < MC_1_20_5
			//use w=0 instead of w=1 because the translation
			//component of the matrices offsets from being relative
			//to the entity to being relative to the camera.
			//however, our positions are already relative to the camera,
			//so offset is necessary.
			matrices.peek().getPositionMatrix().transform(scratch.set(x, y, z, 0.0F));
			x = scratch.x;
			y = scratch.y;
			z = scratch.z;
		#endif
		buffer.vertex(x, y, z).color(r, g, b, a) #if MC_VERSION < MC_1_21_0 .next() #endif;
	}

	#if MC_VERSION < MC_1_20_4
		//1.20.4 has this built in, so this override is only necessary in older versions.
		@Override
		public boolean hasLabel(WaypointEntity entity) {
			return (
				entity.shouldRenderName() || (
					entity.hasCustomName() &&
					MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hit &&
					hit.getEntity() == entity
				)
			);
		}
	#endif

	#if MC_VERSION < MC_1_21_2

		@Override
		public Identifier getTexture(WaypointEntity entity) {
			return TEXTURE;
		}

	#endif

	@Override
	public WaypointEntityRenderer.State createState() {
		return new State();
	}

	@Override
	public void updateState(WaypointEntity entity, WaypointEntityRenderer.State state, float partialTicks) {
		state.age = entity.age;
		state.health = entity.health;
		state.partialTicks = partialTicks;
		state.orbits = Orbit.copy(entity.orbits, state.orbits);
	}

	public static class State extends BigGlobeEntityRenderer.State {

		#if MC_VERSION <= MC_1_21_1
			public int age;
		#endif
		public float health, partialTicks;
		public WaypointEntity.Orbit[] orbits;
	}
}