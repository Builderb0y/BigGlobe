package builderb0y.bigglobe.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.Item;

@Mixin(DispenserBlock.class)
public interface DispenserBlock_BehaviorsAccess {

	@Accessor("BEHAVIORS")
	public static Map<Item, DispenserBehavior> bigglobe_getBehaviors() {
		throw new Error("Mixin not applied.");
	}
}