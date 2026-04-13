package builderb0y.bigglobe.rendering.lods.flat;

import java.util.function.Supplier;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import builderb0y.bigglobe.rendering.NativeMemory;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.DrawRange;
import builderb0y.bigglobe.rendering.lods.QuadSorter.FlatNormalQuadSorter;
import builderb0y.bigglobe.rendering.lods.QuadSorter.LayerQuadSorter;
import builderb0y.bigglobe.rendering.lods.QuadSorter.UnsortedQuadSorter;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class FlatMesh implements SafeCloseable {

	public final GpuBuffer
		payload,
		modelOffset;
	public final DrawRange
		solidPosX,
		solidPosZ,
		solidNegX,
		solidNegZ,
		solidNone,
		cutoutPosX,
		cutoutPosZ,
		cutoutNegX,
		cutoutNegZ,
		cutoutNone,
		transPosX,
		transPosZ,
		transNegX,
		transNegZ,
		transNone,
		fullSize;

	@Override
	public void close() {
		ResourceTracker.closeAll(this.payload, this.modelOffset);
	}

	public FlatMesh(FlatQuadPacker packer, Supplier<String> name) {
		LayerQuadSorter
			layers = (LayerQuadSorter)(packer.sorter);
		FlatNormalQuadSorter
			solid  = (FlatNormalQuadSorter)(layers.solid),
			cutout = (FlatNormalQuadSorter)(layers.cutout),
			trans  = (FlatNormalQuadSorter)(layers.translucent);
		UnsortedQuadSorter
			solidPosX  = (UnsortedQuadSorter)(solid .posX),
			solidPosZ  = (UnsortedQuadSorter)(solid .posZ),
			solidNegX  = (UnsortedQuadSorter)(solid .negX),
			solidNegZ  = (UnsortedQuadSorter)(solid .negZ),
			solidNone  = (UnsortedQuadSorter)(solid .none),
			cutoutPosX = (UnsortedQuadSorter)(cutout.posX),
			cutoutPosZ = (UnsortedQuadSorter)(cutout.posZ),
			cutoutNegX = (UnsortedQuadSorter)(cutout.negX),
			cutoutNegZ = (UnsortedQuadSorter)(cutout.negZ),
			cutoutNone = (UnsortedQuadSorter)(cutout.none),
			transPosX  = (UnsortedQuadSorter)(trans .posX),
			transPosZ  = (UnsortedQuadSorter)(trans .posZ),
			transNegX  = (UnsortedQuadSorter)(trans .negX),
			transNegZ  = (UnsortedQuadSorter)(trans .negZ),
			transNone  = (UnsortedQuadSorter)(trans .none);
		int
			solidPosXSize  =  solidPosX.output.intUsed(),
			solidPosZSize  =  solidPosZ.output.intUsed(),
			solidNegXSize  =  solidNegX.output.intUsed(),
			solidNegZSize  =  solidNegZ.output.intUsed(),
			solidNoneSize  =  solidNone.output.intUsed(),
			cutoutPosXSize = cutoutPosX.output.intUsed(),
			cutoutPosZSize = cutoutPosZ.output.intUsed(),
			cutoutNegXSize = cutoutNegX.output.intUsed(),
			cutoutNegZSize = cutoutNegZ.output.intUsed(),
			cutoutNoneSize = cutoutNone.output.intUsed(),
			transPosXSize  =  transPosX.output.intUsed(),
			transPosZSize  =  transPosZ.output.intUsed(),
			transNegXSize  =  transNegX.output.intUsed(),
			transNegZSize  =  transNegZ.output.intUsed(),
			transNoneSize  =  transNone.output.intUsed();
		int size = 0;
		this.solidPosX  = DrawRange.fromBytes(size,  solidPosXSize); size = Math.addExact(size,  solidPosXSize);
		this.solidPosZ  = DrawRange.fromBytes(size,  solidPosZSize); size = Math.addExact(size,  solidPosZSize);
		this.solidNegX  = DrawRange.fromBytes(size,  solidNegXSize); size = Math.addExact(size,  solidNegXSize);
		this.solidNegZ  = DrawRange.fromBytes(size,  solidNegZSize); size = Math.addExact(size,  solidNegZSize);
		this.solidNone  = DrawRange.fromBytes(size,  solidNoneSize); size = Math.addExact(size,  solidNoneSize);
		this.cutoutPosX = DrawRange.fromBytes(size, cutoutPosXSize); size = Math.addExact(size, cutoutPosXSize);
		this.cutoutPosZ = DrawRange.fromBytes(size, cutoutPosZSize); size = Math.addExact(size, cutoutPosZSize);
		this.cutoutNegX = DrawRange.fromBytes(size, cutoutNegXSize); size = Math.addExact(size, cutoutNegXSize);
		this.cutoutNegZ = DrawRange.fromBytes(size, cutoutNegZSize); size = Math.addExact(size, cutoutNegZSize);
		this.cutoutNone = DrawRange.fromBytes(size, cutoutNoneSize); size = Math.addExact(size, cutoutNoneSize);
		this.transPosX  = DrawRange.fromBytes(size,  transPosXSize); size = Math.addExact(size,  transPosXSize);
		this.transPosZ  = DrawRange.fromBytes(size,  transPosZSize); size = Math.addExact(size,  transPosZSize);
		this.transNegX  = DrawRange.fromBytes(size,  transNegXSize); size = Math.addExact(size,  transNegXSize);
		this.transNegZ  = DrawRange.fromBytes(size,  transNegZSize); size = Math.addExact(size,  transNegZSize);
		this.transNone  = DrawRange.fromBytes(size,  transNoneSize); size = Math.addExact(size,  transNoneSize);
		this.fullSize   = DrawRange.fromBytes(0, size);

		try (NativeMemory combined = new NativeMemory(size)) {
			combined
			.append( solidPosX.output)
			.append( solidPosZ.output)
			.append( solidNegX.output)
			.append( solidNegZ.output)
			.append( solidNone.output)
			.append(cutoutPosX.output)
			.append(cutoutPosZ.output)
			.append(cutoutNegX.output)
			.append(cutoutNegZ.output)
			.append(cutoutNone.output)
			.append( transPosX.output)
			.append( transPosZ.output)
			.append( transNegX.output)
			.append( transNegZ.output)
			.append( transNone.output);

			this.payload = combined.isEmpty() ? null : RenderSystem.getDevice().createBuffer(
				() -> "FlatMesh: " + name.get() + " [payload]",
				GpuBuffer.USAGE_VERTEX,
				combined.toByteBuffer()
			);
			this.modelOffset = combined.isEmpty() ? null : RenderSystem.getDevice().createBuffer(
				() -> "FlatMesh: " + name.get() + " [modelOffset]",
				GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				FlatLodRenderer.TRANSFORM_SIZE
			);
		}
	}

	public boolean isEmpty() {
		return this.payload == null;
	}

	public DrawRange getPosX(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidPosX;
			case CUTOUT -> this.cutoutPosX;
			case TRANSLUCENT -> this.transPosX;
		};
	}

	public DrawRange getPosZ(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidPosZ;
			case CUTOUT -> this.cutoutPosZ;
			case TRANSLUCENT -> this.transPosZ;
		};
	}

	public DrawRange getNegX(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidNegX;
			case CUTOUT -> this.cutoutNegX;
			case TRANSLUCENT -> this.transNegX;
		};
	}

	public DrawRange getNegZ(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidNegZ;
			case CUTOUT -> this.cutoutNegZ;
			case TRANSLUCENT -> this.transNegZ;
		};
	}

	public DrawRange getNone(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidNone;
			case CUTOUT -> this.cutoutNone;
			case TRANSLUCENT -> this.transNone;
		};
	}
}