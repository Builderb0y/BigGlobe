package builderb0y.bigglobe.platform.fabric.fabric.api.event.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AttackEntityCallback {

	InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult);

	class Event {
		public final List<AttackEntityCallback> listeners = new ArrayList<>();

		public void register(AttackEntityCallback listener) {
			this.listeners.add(listener);
		}

		public InteractionResult invoker(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
			for (AttackEntityCallback listener : this.listeners) {
				InteractionResult result = listener.interact(player, world, hand, entity, hitResult);
				if (result != InteractionResult.PASS) return result;
			}
			return InteractionResult.PASS;
		}
	}

	Event EVENT = new Event();
}
