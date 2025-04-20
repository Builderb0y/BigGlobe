package builderb0y.bigglobe.hyperspace;

import org.jetbrains.annotations.NotNull;

import builderb0y.autocodec.data.*;
import builderb0y.autocodec.fixers.DataFixContext;
import builderb0y.autocodec.fixers.DataFixException;
import builderb0y.autocodec.fixers.VersionedFixer;

public class HyperspaceStorageVersions extends VersionedFixer<ServerWaypointManager> {

	public static final HyperspaceStorageVersions INSTANCE = new HyperspaceStorageVersions();

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

		/**
		compound {
			list waypoints: [
				compound {
					String world,
					double x,
					double y,
					double z,
					int id,
					nullable UUID owner
				}
			]
		}
		*/
		V2_INLINE_POS = 2,

		CURRENT_VERSION = V2_INLINE_POS;

	public HyperspaceStorageVersions() {
		super("HyperspaceStorageVersions", CURRENT_VERSION);
	}

	@Override
	public @NotNull <T_Encoded> DataFixContext<T_Encoded> fixData(@NotNull DataFixContext<T_Encoded> context, int version) throws DataFixException {
		switch (version) {
			default: throw new DataFixException(() -> "Unknown hyperspace storage version: " + version);
			case V0_UUIDS: this.convertUUIDsToIDs(context);
			case V1_VARINT_IDS: this.inlinePositions(context);
			case V2_INLINE_POS:
		}
		return context;
	}

	public <T_Encoded> void convertUUIDsToIDs(DataFixContext<T_Encoded> context) throws DataFixException {
		ListData waypoints = context.forceGetMember("waypoints").forceAsList();
		int size = waypoints.value.size();
		for (int index = 0; index < size; index++) {
			MapData waypoint = context.fork(index, waypoints.get(index)).forceAsMap();
			waypoint.remove("uuid");
			waypoint.putInt("id", index);
		}
		context.putInt("nextID", size);
	}

	public <T_Encoded> void inlinePositions(DataFixContext<T_Encoded> context) throws DataFixException {
		//no-op, this is handled in PackedWorldPos#FIXER.
	}
}