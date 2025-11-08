package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.rendering.lods.SimpleLodRenderer.DefaultLodState;
import builderb0y.bigglobe.rendering.lods.LodPasses.Geometry;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class SidedSeparateLodRenderer extends AbstractLodRenderer {

	public DefaultLodState state = new DefaultLodState();
	public SeparateLodShader shader;
	public NativeMemory multiDrawStarts, multiDrawSizes, zeros;
	public int drawCount;

	@Override
	public void close() {
		ResourceTracker.closeAll(
			super::close,
			this.shader,
			this.multiDrawStarts,
			this.multiDrawSizes,
			this.zeros
		);
	}

	public SidedSeparateLodRenderer(int quadCount) {
		super(quadCount);
		try {
			this.shader = new SeparateLodShader();
			this.multiDrawStarts = new NativeMemory(5L * Integer.BYTES);
			this.multiDrawSizes = new NativeMemory(5L * Integer.BYTES);
			this.zeros = new NativeMemory(5L * Long.BYTES);
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
			if (this.state.inTranslucentPass) {
				this.depthBuffer.copyTo(RenderVersions.glID(MinecraftClient.getInstance().getFramebuffer()));
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
				return builder.build(SidedSeparateLodRenderer.this.heap);
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
				this.recordDraw(pass.all());
			}
			if (pass.posX() != null && modelOffsetX < 0.0F) {
				this.recordDraw(pass.posX());
			}
			if (pass.posZ() != null && modelOffsetZ < 0.0F) {
				this.recordDraw(pass.posZ());
			}
			if (pass.negX() != null && modelOffsetX + scale * (1 << LodQuadTree.MIN_LEVEL) > 0.0F) {
				this.recordDraw(pass.negX());
			}
			if (pass.negZ() != null && modelOffsetZ + scale * (1 << LodQuadTree.MIN_LEVEL) > 0.0F) {
				this.recordDraw(pass.negZ());
			}
			if (this.drawCount > 0) {
				glUniform4f(this.shader.modelOffset, modelOffsetX, modelOffsetY, modelOffsetZ, scale);
				nglMultiDrawElementsBaseVertex(
					GL_TRIANGLES,
					this.multiDrawSizes.address,
					GL_UNSIGNED_INT,
					this.zeros.address,
					this.drawCount,
					this.multiDrawStarts.address
				);
				GLException.check();
				this.multiDrawStarts.clear();
				this.multiDrawSizes.clear();
				this.zeros.clear();
				this.drawCount = 0;
			}
		}
	}

	public void recordDraw(Geometry geometry) {
		this.multiDrawStarts.appendInt(geometry.baseVertex());
		this.multiDrawSizes.appendInt(geometry.indexCount());
		this.elementBuffer.ensureCapacity(geometry.indexCount());
		this.zeros.appendLong(0L);
		this.drawCount++;
	}
}