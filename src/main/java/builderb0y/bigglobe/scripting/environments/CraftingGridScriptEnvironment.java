package builderb0y.bigglobe.scripting.environments;

import net.minecraft.item.ItemStack;

import builderb0y.bigglobe.scripting.wrappers.CraftingGrid;
import builderb0y.scripting.environments.MutableScriptEnvironment;

public class CraftingGridScriptEnvironment {

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("CraftingGrid", CraftingGrid.class)
		.addFieldGets(CraftingGrid.class, "width", "height")
		.addMethodInvoke(CraftingGrid.class, "get")
		.addMethodInvokeSpecific(CraftingGrid.class, "set", ItemStack.class, int.class, int.class, ItemStack.class)
	);
}