package builderb0y.bigglobe.hyperspace;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.joml.Vector3dc;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public record PackedWorldPos(RegistryKey<World> world, PackedPos pos) {

	public static final PackedWorldPos ZERO = new PackedWorldPos(World.OVERWORLD, PackedPos.ZERO);

	public PackedWorldPos(RegistryKey<World> world, double x, double y, double z) {
		this(world, new PackedPos(x, y, z));
	}

	public PackedWorldPos(RegistryKey<World> world, int x, int y, int z) {
		this(world, new PackedPos(x, y, z));
	}

	public PackedWorldPos(RegistryKey<World> world, Vec3d vector) {
		this(world, new PackedPos(vector));
	}

	public PackedWorldPos(RegistryKey<World> world, Vector3dc vector) {
		this(world, new PackedPos(vector));
	}

	public double x() { return this.pos.x(); }
	public double y() { return this.pos.y(); }
	public double z() { return this.pos.z(); }

	public int packedX() { return this.pos.packedX(); }
	public int packedY() { return this.pos.packedY(); }
	public int packedZ() { return this.pos.packedZ(); }

	public void writePositionOnly(PacketByteBuf buffer) {
		this.pos.write(buffer);
	}

	public static PackedWorldPos readPositionOnly(PacketByteBuf buffer, RegistryKey<World> world) {
		return new PackedWorldPos(world, PackedPos.read(buffer));
	}

	public void write(PacketByteBuf buffer) {
		buffer.writeRegistryKey(this.world);
		this.writePositionOnly(buffer);
	}

	public static PackedWorldPos read(PacketByteBuf buffer) {
		return new PackedWorldPos(buffer.readRegistryKey(RegistryKeyVersions.world()), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public void writeBulk(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worlds) {
		buffer.writeVarInt(worlds.getInt(this.world));
		this.writePositionOnly(buffer);
	}

	public static PackedWorldPos readBulk(PacketByteBuf buffer, List<RegistryKey<World>> worlds) {
		return new PackedWorldPos(worlds.get(buffer.readVarInt()), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("world", this.world.getValue().toString());
		nbt.put("pos", this.pos.toNbt());
		return nbt;
	}

	public static PackedWorldPos fromNbt(NbtCompound nbt) {
		String worldString = nbt.getString("world");
		RegistryKey<World> world;
		gotWorld: {
			if (!worldString.isEmpty()) try {
				world = RegistryKey.of(RegistryKeyVersions.world(), IdentifierVersions.create(worldString));
				break gotWorld;
			}
			catch (InvalidIdentifierException exception) {}
			world = World.OVERWORLD;
		}
		PackedPos pos;
		NbtList posNbt = nbt.getList("pos", NbtElement.DOUBLE_TYPE);
		if (posNbt.size() == 3) {
			pos = PackedPos.fromNbt(posNbt);
		}
		else {
			pos = PackedPos.ZERO;
		}
		return new PackedWorldPos(world, pos);
	}
}