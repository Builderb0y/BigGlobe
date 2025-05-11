package builderb0y.bigglobe.hyperspace;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseFixer;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.data.AbstractNumberData;
import builderb0y.autocodec.data.ListData;
import builderb0y.autocodec.data.MapData;
import builderb0y.autocodec.fixers.AutoFixer;
import builderb0y.autocodec.fixers.AutoFixer.NamedFixer;
import builderb0y.autocodec.fixers.DataFixContext;
import builderb0y.autocodec.fixers.DataFixException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@UseFixer(name = "FIXER", in = PackedWorldPos.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public record PackedWorldPos(RegistryKey<World> world, @EncodeInline PackedPos pos) {

	public static final AutoFixer<PackedWorldPos> FIXER = new NamedFixer<>("PackedWorldPos.FIXER") {

		@Override
		@OverrideOnly
		public @NotNull <T_Encoded> DataFixContext<T_Encoded> fixData(@NotNull DataFixContext<T_Encoded> context) throws DataFixException {
			MapData map = context.data.tryAsMap();
			if (map != null) {
				ListData pos = map.get("pos").tryAsList();
				if (pos != null && pos.size() == 3) {
					map = map.without("pos");
					AbstractNumberData x = pos.get(0).tryAsNumber();
					AbstractNumberData y = pos.get(1).tryAsNumber();
					AbstractNumberData z = pos.get(2).tryAsNumber();
					if (x != null) map.put("x", x);
					if (y != null) map.put("y", y);
					if (z != null) map.put("z", z);
					context = context.withData(map);
				}
			}
			return context;
		}
	};
	//codec depends on FIXER, which must be initialized first.
	public static class CoderHolder {

		public static final AutoCoder<PackedWorldPos> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(PackedWorldPos.class);
	}

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
		return new PackedWorldPos(buffer.readRegistryKey(RegistryKeys.WORLD), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}

	public void writeBulk(PacketByteBuf buffer, Object2IntMap<RegistryKey<World>> worlds) {
		buffer.writeVarInt(worlds.getInt(this.world));
		this.writePositionOnly(buffer);
	}

	public static PackedWorldPos readBulk(PacketByteBuf buffer, List<RegistryKey<World>> worlds) {
		return new PackedWorldPos(worlds.get(buffer.readVarInt()), buffer.readInt(), buffer.readInt(), buffer.readInt());
	}
}