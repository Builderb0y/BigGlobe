package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import net.minecraft.text.Text;

import builderb0y.autocodec.annotations.DefaultObject;
import builderb0y.autocodec.annotations.DefaultObject.DefaultObjectMode;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.Entity_CurrentIdGetter;

/**
data about a specific waypoint that is known to the server.
the displayed position of a server waypoint is
always the same as its destination position.
*/
public record ServerWaypointData(
	int id,
	@Hidden int entityId,
	@VerifyNullable UUID owner,
	@EncodeInline PackedWorldPos pos,
	@VerifyNullable Text name,
	@DefaultObject(name = "BLANK", in = CloudColor.class, mode = DefaultObjectMode.FIELD) CloudColor color
)
implements WaypointData {

	public ServerWaypointData(
		int id,
		@VerifyNullable UUID owner,
		PackedWorldPos pos,
		@VerifyNullable Text name,
		CloudColor color
	) {
		this(id, Entity_CurrentIdGetter.bigglobe_getCurrentID().incrementAndGet(), owner, pos, name, color);
	}

	@Override
	public PackedWorldPos destinationPosition() {
		return this.pos;
	}

	@Override
	public PackedWorldPos displayPosition() {
		return this.pos;
	}

	public ServerWaypointData withName(Text name) {
		return new ServerWaypointData(this.id, this.entityId, this.owner, this.pos, name, this.color);
	}

	public ServerWaypointData withColor(CloudColor color) {
		return new ServerWaypointData(this.id, this.entityId, this.owner, this.pos, this.name, color);
	}

	public PlayerWaypointData relativize(PackedPos entrance) {
		double x = this.pos.x() - entrance.x();
		double y = this.pos.y() - entrance.y();
		double z = this.pos.z() - entrance.z();
		if (x != 0.0D || y != 0.0D || z != 0.0D) {
			double scalar = 1.0D / Math.sqrt(Math.sqrt(BigGlobeMath.squareD(x, y, z)));
			x *= scalar;
			y *= scalar;
			z *= scalar;
		}
		return new PlayerWaypointData(this, new PackedWorldPos(HyperspaceConstants.WORLD_KEY, x, y, z));
	}

	public PlayerWaypointData absolutize() {
		return new PlayerWaypointData(this, this.pos);
	}

	public PlayerWaypointData toClientData(PackedPos entrance) {
		return entrance != null ? this.relativize(entrance) : this.absolutize();
	}
}