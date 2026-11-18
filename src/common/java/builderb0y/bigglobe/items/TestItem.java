package builderb0y.bigglobe.items;

import org.jetbrains.annotations.TestOnly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

@TestOnly
@Deprecated
public class TestItem extends Item {

	public TestItem() {
		super(new Item.Properties());
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel world) {
			//haven't had anything to "do" here in a long time.
		}
		return InteractionResult.SUCCESS;
	}
}