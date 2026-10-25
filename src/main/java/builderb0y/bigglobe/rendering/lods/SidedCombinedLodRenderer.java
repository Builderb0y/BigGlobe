package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.*;
import builderb0y.bigglobe.rendering.lods.LodPasses.Geometry;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class SidedCombinedLodRenderer extends AbstractLodRenderer {

	public SidedMultiDrawLodState state = new SidedMultiDrawLodState();
	public CombinedLodShader shader;
	public TextureBuffer transformationBuffer;
	public NativeMemory multiDrawStarts, multiDrawSizes, zeros;
	public int drawCount;
	public int transformationTexture;

	@Override
	public void close() {
		if (this.transformationTexture != 0) {
			glDeleteTextures(this.transformationTexture);
			this.transformationTexture = 0;
		}
		ResourceTracker.closeAll(
			super::close,
			this.shader,
			this.transformationBuffer,
			this.multiDrawStarts,
			this.multiDrawSizes,
			this.zeros
		);
	}

	public SidedCombinedLodRenderer(int quadCount) {
		super(quadCount);
		try {
			this.shader = new CombinedLodShader();
			this.transformationBuffer = new TextureBuffer();
			this.transformationTexture = glGenTextures();
			this.multiDrawStarts = new NativeMemory();
			this.multiDrawSizes = new NativeMemory();
			this.zeros = new NativeMemory();
			GLException.check();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public SafeCloseable bind(LodRenderState state, boolean translucent) {
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

			this.setupUniforms(state, this.shader);
		}
		return () -> {
			if (this.drawCount > 0) {
				try (var $ = this.transformationBuffer.bind()) {
					GLException.check();
					this.transformationBuffer.uploadAndClear();
					GLException.check();
					glActiveTexture(GL_TEXTURE1);
					GLException.check();
					glBindTexture(GL_TEXTURE_BUFFER, this.transformationTexture);
					GLException.check();
					glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, this.transformationBuffer.glID);
					GLException.check();
					nglMultiDrawElementsBaseVertex(
						GL_TRIANGLES,
						this.multiDrawSizes.address,
						GL_UNSIGNED_INT,
						this.zeros.address,
						this.drawCount,
						this.multiDrawStarts.address
					);
					GLException.check();
				}
				GLException.check(); //handles exceptions from this.transformationBuffer.bind().close().
				this.multiDrawStarts.clear();
				this.multiDrawSizes.clear();
				this.zeros.clear();
				this.drawCount = 0;
			}
			if (this.state.inTranslucentPass) {
				this.depthBuffer.copyTo(RenderVersions.glID(Minecraft.getInstance().getMainRenderTarget()));
				this.state.restore();
			}
		};
	}

	@Override
	public VersionedVertexConsumerProvider beginMeshing() {
		return new SidedLodPasses.Builder(LodVertexFormat.FORMAT, LodGenerator.RENDER_AREA /* expected columns */ * 2 /* expected quads per column */ * 4 /* vertices per quad */);
	}

	@Override
	public MeshUploader finishMeshing() {
		SafeCloseable unbinder = this.heap.bind();
		return new MeshUploader() {

			@Override
			public SafeCloseable upload(VersionedVertexConsumerProvider provider) {
				SidedLodPasses.Builder builder = (SidedLodPasses.Builder)(provider);
				return builder.build(SidedCombinedLodRenderer.this.heap);
			}

			@Override
			public void close() {
				unbinder.close();
			}
		};
	}

	@Override
	public void endMeshing(VersionedVertexConsumerProvider provider) {
		SidedLodPasses.Builder builder = (SidedLodPasses.Builder)(provider);
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
		SidedLodPasses passes = (SidedLodPasses)(token);
		SidedLodPasses.Pass pass = passes.getPass(this.state.inTranslucentPass);
		if (pass != null) {
			if (pass.all() != null) {
				this.recordDraw(pass.all(), modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			}
			if (pass.posX() != null && modelOffsetX < 0.0F) {
				this.recordDraw(pass.posX(), modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			}
			if (pass.posZ() != null && modelOffsetZ < 0.0F) {
				this.recordDraw(pass.posZ(), modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			}
			if (pass.negX() != null && modelOffsetX + scale * (1 << LodQuadTree.MIN_LEVEL) > 0.0F) {
				this.recordDraw(pass.negX(), modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			}
			if (pass.negZ() != null && modelOffsetZ + scale * (1 << LodQuadTree.MIN_LEVEL) > 0.0F) {
				this.recordDraw(pass.negZ(), modelOffsetX, modelOffsetY, modelOffsetZ, scale);
			}
		}
	}

	public void recordDraw(Geometry geometry, float x, float y, float z, float scale) {
		NativeMemory transformations = this.transformationBuffer.cpuBuffer;
		transformations.appendFloat(x);
		transformations.appendFloat(y);
		transformations.appendFloat(z);
		transformations.appendFloat(scale);
		this.multiDrawStarts.appendInt(geometry.baseVertex());
		this.multiDrawSizes.appendInt(geometry.indexCount());
		this.elementBuffer.ensureCapacity(geometry.indexCount());
		this.zeros.appendLong(0L);
		this.drawCount++;
	}

	public static class SidedMultiDrawLodState extends LodGlState {

		public TextureState
			texture1 = TextureState.buffer(GL_TEXTURE1);
		public FilteredTextureState
			texture0 = TextureState._2D(GL_TEXTURE0),
			texture2 = TextureState._2D(GL_TEXTURE2);

		@Override
		public void capture() {
			super.capture();
			this.texture0.capture();
			this.texture1.capture();
			this.texture2.capture();
		}

		@Override
		public void restore() {
			this.texture2.restore();
			this.texture1.restore();
			this.texture0.restore();
			super.restore();
		}
	}
}