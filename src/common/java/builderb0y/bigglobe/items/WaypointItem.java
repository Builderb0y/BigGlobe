package builderb0y.bigglobe.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.hyperspace.HyperspaceConstants;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.ServerWaypointData;
import builderb0y.bigglobe.hyperspace.ServerWaypointManager;
import builderb0y.bigglobe.mixins.Entity_CurrentIdGetter;
import builderb0y.bigglobe.versions.GameProfileVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class WaypointItem extends Item {

	public final boolean isPrivate;

	public WaypointItem(Properties settings, boolean isPrivate) {
		super(settings);
		this.isPrivate = isPrivate;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel serverWorld)) {
			return InteractionResult.SUCCESS;
		}
		if (!BigGlobeConfig.INSTANCE.get().hyperspaceEnabled) {
			if (context.getPlayer() != null) {
				context.getPlayer().sendOverlayMessage(Component.translatable("bigglobe.hyperspace.disabled").withStyle(ChatFormatting.RED));
			}
			return InteractionResult.SUCCESS;
		}
		if (context.getLevel().dimension() == HyperspaceConstants.WORLD_KEY) {
			if (context.getPlayer() != null) {
				context.getPlayer().sendOverlayMessage(Component.translatable("bigglobe.hyperspace.cant_place_waypoint_here").withStyle(ChatFormatting.RED));
			}
			return InteractionResult.SUCCESS;
		}
		Direction side = context.getClickedFace();
		ServerWaypointManager manager = ServerWaypointManager.get(serverWorld);
		//it's not constant.
		//noinspection ConstantValue
		if (
			manager != null &&
			manager.addWaypoint(
				new ServerWaypointData(
					manager.nextID(),
					Entity_CurrentIdGetter.bigglobe_getCurrentID().incrementAndGet(),

					this.isPrivate && context.getPlayer() != null
						? GameProfileVersions.getUUID(context.getPlayer().getGameProfile())
						: null,

					new PackedWorldPos(
						serverWorld.dimension(),
						context.getClickedPos().getX() + (side.getStepX() << 1) + 0.5D,
						context.getClickedPos().getY() + (side.getStepY() << 1) + 0.5D,
						context.getClickedPos().getZ() + (side.getStepZ() << 1) + 0.5D
					),

					ItemStackVersions.getCustomName(context.getItemInHand()),
					CloudColor.BLANK
				),
				true
			)
		) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}