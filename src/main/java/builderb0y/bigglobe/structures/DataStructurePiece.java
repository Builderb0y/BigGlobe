package builderb0y.bigglobe.structures;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.math.BlockBox;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

/**
a StructurePiece which stores all of its NBT data in a single field,
typically a record. the AutoCoder associated with the field's type
is responsible for reading and writing NBT data.

this has the benefit of reducing verbose code to manually serialize
and deserialize NBT data, and it also reduces programmer error,
since the AutoCoder for the data field is typically created automatically.
this has bitten me on the ass once before and I don't want that to happen again.
*/
public abstract class DataStructurePiece<D> extends StructurePiece {

	public final @NotNull D data;

	public DataStructurePiece(StructurePieceType type, int length, BlockBox boundingBox, @NotNull D data) {
		super(type, length, boundingBox);
		this.data = data;
	}

	public DataStructurePiece(StructurePieceType type, NbtCompound nbt) {
		super(type, nbt);
		try {
			this.data = BigGlobeAutoCodec.AUTO_CODEC.decode(
				this.dataCoder(),
				nbt.getCompound("data"),
				RegistryOps.of(
					NbtOps.INSTANCE,
					BigGlobeMod.getCurrentServer().getRegistryManager()
				)
			);
		}
		catch (DecodeException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.put(
			"data",
			BigGlobeAutoCodec.AUTO_CODEC.encode(
				this.dataCoder(),
				this.data,
				RegistryOps.of(
					NbtOps.INSTANCE,
					context.registryManager()
				)
			)
		);
	}

	public abstract AutoCoder<D> dataCoder();
}