package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.rendering.TextureState;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class SimpleLodRenderer extends AbstractLodRenderer {

	public DefaultLodState state = new DefaultLodState();
	public SeparateLodShader shader;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.shader);
	}

	public SimpleLodRenderer(int quadCount) {
		super(quadCount);
		try {
			this.shader = new SeparateLodShader();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public SafeCloseable bind(WorldRenderContext context, FogParams fog, boolean translucent) {
		if (translucent) {
			this.state.inTranslucentPass = true;
			this.state.setBlend(true);
			this.state.setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
		}
		else {
			this.state.capture();

			this.state.setVao(this.vao);
			this.state.setElementBuffer(this.elementBuffer.glID);

			this.setupOpaqueState(this.state);

			this.state.setProgram(this.shader.program);
			this.shader.bindTextures();

			this.setupUniforms(context, this.shader, fog);
		}
		return () -> {
			if (this.state.inTranslucentPass) {
				this.depthBuffer.copyTo(RenderVersions.glID(MinecraftClient.getInstance().getFramebuffer()));
				this.state.restore();
			}
		};
	}

	@Override
	public VersionedVertexConsumerProvider beginMeshing() {
		return new LodPasses.Builder(LodVertexFormat.FORMAT, LodGenerator.RENDER_AREA /* expected columns */ * 6 /* expected quads per column */ * 4 /* vertices per quad */);
	}

	@Override
	public MeshUploader finishMeshing() {
		SafeCloseable unbinder = this.heap.bind();
		return new MeshUploader() {

			@Override
			public SafeCloseable upload(VersionedVertexConsumerProvider provider) {
				LodPasses.Builder builder = (LodPasses.Builder)(provider);
				return builder.build(SimpleLodRenderer.this.heap);
			}

			@Override
			public void close() {
				unbinder.close();
			}
		};
	}

	@Override
	public void endMeshing(VersionedVertexConsumerProvider provider) {
		LodPasses.Builder builder = (LodPasses.Builder)(provider);
		builder.close();
	}

	@Override
	public void draw(
		SafeCloseable token,
		float modelOffsetX,
		float modelOffsetY,
		float modelOffsetZ,
		float scale
	) {
		LodPasses passes = (LodPasses)(token);
		LodPasses.Geometry geometry = passes.getGeometry(this.state.inTranslucentPass);
		if (geometry != null) {
			glUniform4f(this.shader.modelOffset, modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			this.elementBuffer.ensureCapacity(geometry.indexCount());
			nglDrawElementsBaseVertex(GL_TRIANGLES, geometry.indexCount(), GL_UNSIGNED_INT, 0L, geometry.baseVertex());
		}
	}

	public static class DefaultLodState extends LodGlState {

		public TextureState
			texture0 = TextureState._2D(GL_TEXTURE0),
			texture2 = TextureState._2D(GL_TEXTURE2);

		@Override
		public void capture() {
			super.capture();
			this.texture0.capture();
			this.texture2.capture();
		}

		@Override
		public void restore() {
			this.texture2.restore();
			this.texture0.restore();
			super.restore();
		}
	}
}