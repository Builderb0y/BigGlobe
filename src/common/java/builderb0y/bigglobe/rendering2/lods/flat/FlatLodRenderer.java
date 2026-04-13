package builderb0y.bigglobe.rendering2.lods.flat;

import java.nio.ByteOrder;
import java.util.function.Consumer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering2.NativeMemory;
import builderb0y.bigglobe.rendering2.ResourceTracker;
import builderb0y.bigglobe.rendering2.lods.*;

public class FlatLodRenderer extends LodRenderer {

	public static final @Nullable RenderPipeline SOLID, CUTOUT, TRANSLUCENT;
	static {
		if (LodVertexFormat.FORMAT != null) {
			Snippet snippet = (
				RenderPipeline
				.builder()
				.withFragmentShader(BigGlobeMod.modID("core/lod"))
				.withVertexShader(BigGlobeMod.modID("core/lod"))
				.withUniform("ModelOffset", UniformType.UNIFORM_BUFFER)
				.withUniform("Fog", UniformType.UNIFORM_BUFFER)
				.withUniform("ExtraFog", UniformType.UNIFORM_BUFFER)
				.withUniform("Matrices", UniformType.UNIFORM_BUFFER)
				.withSampler("blockAtlas")
				.withSampler("lightmap")
				.withShaderDefine("REGION_SIZE", LodNode.SIZE)
				.withVertexFormat(LodVertexFormat.FORMAT, VertexFormat.Mode.QUADS)
				.withDepthStencilState(DepthStencilState.DEFAULT)
				.buildSnippet()
			);
			SOLID = RenderPipeline.builder(snippet).withLocation(BigGlobeMod.modID("pipeline/lods/flat/solid")).build();
			CUTOUT = RenderPipeline.builder(snippet).withLocation(BigGlobeMod.modID("pipeline/lods/flat/cutout")).withShaderDefine("ALPHA_CUTOUT", 0.5F).build();
			TRANSLUCENT = RenderPipeline.builder(snippet).withLocation(BigGlobeMod.modID("pipeline/lods/flat/translucent")).withShaderDefine("ALPHA_CUTOUT", 0.01F).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
		}
		else {
			SOLID = CUTOUT = TRANSLUCENT = null;
		}
	}
	public static final int TRANSFORM_SIZE = 16;

	public final FlatLodSystem system;
	public final NativeMemory modelOffsetCpuBuffer;
	public int maxIndices;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.fogGpuBuffer, this.fogCpuBuffer, this.modelOffsetCpuBuffer);
	}

	public FlatLodRenderer(FlatLodSystem system) {
		if (
			!RenderSystem.getDevice().precompilePipeline(SOLID).isValid() ||
			!RenderSystem.getDevice().precompilePipeline(CUTOUT).isValid() ||
			!RenderSystem.getDevice().precompilePipeline(TRANSLUCENT).isValid()
		) {
			throw new UnsupportedOperationException("FlatLodRenderer.PIPELINE is not valid.");
		}
		super(system);
		this.system = system;
		this.modelOffsetCpuBuffer = new NativeMemory(TRANSFORM_SIZE);
	}

	@Override
	public LodSystem getSystem() {
		return this.system;
	}

	@Override
	public void uploadBuffers() {
		super.uploadBuffers();
		this.uploadTransformations();
	}

	public void uploadTransformations() {
		this.maxIndices = 0;
		double
			x = this.frustum.x,
			y = this.frustum.y,
			z = this.frustum.z;
		this.traverseVisible((LodQuadNode node) -> {
			FlatMesh mesh = (FlatMesh)(node.mesh);
			if (!mesh.isEmpty()) {
				this
				.modelOffsetCpuBuffer
				.appendFloat((float)(node.minX() - x), ByteOrder.nativeOrder())
				.appendFloat((float)(            - y), ByteOrder.nativeOrder())
				.appendFloat((float)(node.minZ() - z), ByteOrder.nativeOrder())
				.appendFloat(Math.scalb(1.0F, node.level), ByteOrder.nativeOrder());
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(mesh.modelOffset.slice(), this.modelOffsetCpuBuffer.toByteBuffer());
				this.modelOffsetCpuBuffer.clear();
				this.maxIndices = Math.max(this.maxIndices, mesh.fullSize.indexCount());
			}
		});
		this.modelOffsetCpuBuffer.clear();
	}

	@Override
	public void beginRendering(RenderPass pass) {
		super.beginRendering(pass);
		AutoStorageIndexBuffer buffers = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		pass.setIndexBuffer(buffers.getBuffer(this.maxIndices), buffers.type());
	}

	@Override
	public void render(RenderPass pass, ChunkSectionLayer layer) {
		pass.setPipeline(switch (layer) {
			case SOLID -> SOLID;
			case CUTOUT -> CUTOUT;
			case TRANSLUCENT -> TRANSLUCENT;
		});
		double x = this.frustum.x, z = this.frustum.z;
		this.traverseVisible((LodQuadNode node) -> {
			FlatMesh mesh = (FlatMesh)(node.mesh);
			if (!mesh.isEmpty()) {
				boolean uploaded = false;
				DrawRange draw = mesh.getNone(layer);
				if (draw.canDraw()) {
					if (!uploaded) {
						pass.setVertexBuffer(0, mesh.payload);
						pass.setUniform("ModelOffset", mesh.modelOffset);
						uploaded = true;
					}
					pass.drawIndexed(draw.firstVertex(), 0, draw.indexCount(), 1);
				}
				draw = mesh.getPosX(layer);
				if (draw.canDraw() && x > node.minX()) {
					if (!uploaded) {
						pass.setVertexBuffer(0, mesh.payload);
						pass.setUniform("ModelOffset", mesh.modelOffset);
						uploaded = true;
					}
					pass.drawIndexed(draw.firstVertex(), 0, draw.indexCount(), 1);
				}
				draw = mesh.getPosZ(layer);
				if (draw.canDraw() && z > node.minZ()) {
					if (!uploaded) {
						pass.setVertexBuffer(0, mesh.payload);
						pass.setUniform("ModelOffset", mesh.modelOffset);
						uploaded = true;
					}
					pass.drawIndexed(draw.firstVertex(), 0, draw.indexCount(), 1);
				}
				draw = mesh.getNegX(layer);
				if (draw.canDraw() && x < node.maxX()) {
					if (!uploaded) {
						pass.setVertexBuffer(0, mesh.payload);
						pass.setUniform("ModelOffset", mesh.modelOffset);
						uploaded = true;
					}
					pass.drawIndexed(draw.firstVertex(), 0, draw.indexCount(), 1);
				}
				draw = mesh.getNegZ(layer);
				if (draw.canDraw() && z < node.maxZ()) {
					if (!uploaded) {
						pass.setVertexBuffer(0, mesh.payload);
						pass.setUniform("ModelOffset", mesh.modelOffset);
						uploaded = true;
					}
					pass.drawIndexed(draw.firstVertex(), 0, draw.indexCount(), 1);
				}
			}
		});
	}

	public void traverseVisible(Consumer<LodQuadNode> action) {
		this.traverseVisible(this.system.tree.root, action);
	}

	public void traverseVisible(LodQuadNode node, Consumer<LodQuadNode> action) {
		if (node != null && node.isInRange() && node.isInFrustum()) {
			if (node.getAncestorDepth() == 1) {
				assert node.mesh != null : "renderable node has no mesh";
				action.accept(node);
			}
			else {
				this.traverseVisible(node.x0z0, action);
				this.traverseVisible(node.x1z0, action);
				this.traverseVisible(node.x0z1, action);
				this.traverseVisible(node.x1z1, action);
			}
		}
	}

	@Override
	public QuadPacker<?> createPacker() {
		return new FlatQuadPacker();
	}
}