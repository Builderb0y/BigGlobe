package builderb0y.bigglobe.hyperspace;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public class HyperspaceStorageVersions {

	public static final int
		/**
		compound {
			list waypoints: [
				compound {
					String world,
					list pos: [
						double x,
						double y,
						double z
					]
					UUID uuid,
					nullable UUID owner
				}
				...
			]
		}
		*/
		V0_UUIDS = 0,
		/**
		compound {
			list waypoints: [
				compound {
					String world,
					list pos: [
						double x,
						double y,
						double z
					]
					int id,
					nullable UUID owner
				}
				...
			]
		}
		*/
		V1_VARINT_IDS = 1,
		CURRENT_VERSION = V1_VARINT_IDS;

	public static void update(NbtCompound nbt) {
		switch (nbt.getByte("version")) {
			case V0_UUIDS: convertUUIDsToIDs(nbt);
		}
	}

	public static void convertUUIDsToIDs(NbtCompound nbt) {
		NbtList waypoints = nbt.getList("waypoints", NbtElement.COMPOUND_TYPE);
		int size = waypoints.size();
		for (int index = 0; index < size; index++) {
			NbtCompound waypoint = waypoints.getCompound(index);
			waypoint.remove("uuid");
			waypoint.putInt("id", index);
		}
		nbt.putInt("nextID", size);
	}
}