package builderb0y.bigglobe.rendering.lods.flat;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPass.Draw;
import com.mojang.blaze3d.systems.RenderPass.UniformUploader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.*;

@Environment(EnvType.CLIENT)
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
	public final List<LodQuadNode> sortedNodesToRender;
	public final List<Draw<Void>> drawList;
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
		this.sortedNodesToRender = new ArrayList<>(1024);
		this.drawList = new ArrayList<>();
	}

	@Override
	public LodSystem getSystem() {
		return this.system;
	}

	@Override
	public void uploadBuffers(LevelTerrainRenderContext context) {
		super.uploadBuffers(context);
		this.uploadTransformations();
	}

	public void uploadTransformations() {
		this.sortedNodesToRender.clear();
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
				this.sortedNodesToRender.add(node);
			}
		});
		this.sortedNodesToRender.sort((LodQuadNode a, LodQuadNode b) -> Byte.compare(a.level, b.level));
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
		for (LodQuadNode node : this.sortedNodesToRender) {
			FlatMesh mesh = (FlatMesh)(node.mesh);
			BiConsumer<Void, UniformUploader> uniforms = (Void _, UniformUploader uploader) -> {
				uploader.upload("ModelOffset", mesh.modelOffset.slice());
			};
			DrawRange draw = mesh.getNone(layer);
			if (draw.canDraw()) {
				this.drawList.add(new Draw<>(0, mesh.payload, null, null, 0, draw.indexCount(), draw.firstVertex(), uniforms));
			}
			draw = mesh.getPosX(layer);
			if (draw.canDraw() && x > node.minX()) {
				this.drawList.add(new Draw<>(0, mesh.payload, null, null, 0, draw.indexCount(), draw.firstVertex(), uniforms));
			}
			draw = mesh.getPosZ(layer);
			if (draw.canDraw() && z > node.minZ()) {
				this.drawList.add(new Draw<>(0, mesh.payload, null, null, 0, draw.indexCount(), draw.firstVertex(), uniforms));
			}
			draw = mesh.getNegX(layer);
			if (draw.canDraw() && x < node.maxX()) {
				this.drawList.add(new Draw<>(0, mesh.payload, null, null, 0, draw.indexCount(), draw.firstVertex(), uniforms));
			}
			draw = mesh.getNegZ(layer);
			if (draw.canDraw() && z < node.maxZ()) {
				this.drawList.add(new Draw<>(0, mesh.payload, null, null, 0, draw.indexCount(), draw.firstVertex(), uniforms));
			}
		}
		AutoStorageIndexBuffer buffers = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer indexBuffer = buffers.getBuffer(this.maxIndices);
		IndexType indexType = buffers.type();
		pass.drawMultipleIndexed(this.drawList, indexBuffer, indexType, requiredUniforms, null);
		this.drawList.clear();
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