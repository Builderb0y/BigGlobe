package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.render.VertexConsumerProvider;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.lods.DefaultLodRenderer.DefaultCapturedState;
import builderb0y.bigglobe.lods.LodPasses.Geometry;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class SidedLodRenderer extends AbstractLodRenderer {

	public DefaultCapturedState state = new DefaultCapturedState();
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

	public SidedLodRenderer(int quadCount) {
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
		return this.state;
	}

	@Override
	public VertexConsumerProvider beginMeshing() {
		return new SidedLodPasses.Builder(LodVertexFormat.FORMAT, LodGenerator.RENDER_AREA /* expected columns */ * 2 /* expected quads per column */ * 4 /* vertices per quad */);
	}

	@Override
	public MeshUploader finishMeshing() {
		SafeCloseable unbinder = this.heap.bind();
		return new MeshUploader() {

			@Override
			public SafeCloseable upload(VertexConsumerProvider provider) {
				SidedLodPasses.Builder builder = (SidedLodPasses.Builder)(provider);
				return builder.build(SidedLodRenderer.this.heap);
			}

			@Override
			public void close() {
				unbinder.close();
			}
		};
	}

	@Override
	public void endMeshing(VertexConsumerProvider provider) {
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