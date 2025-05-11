package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.scripting.wrappers.entries.WoodPaletteEntry;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class WoodPaletteColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return WoodPaletteEntry.INFO.type;
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		if (object == null) return ldc(null, this.getTypeInfo());
		String string = (String)(object);
		RegistryEntry<WoodPalette> entry = ConstantFactory.getEntryServerOnly(
			BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY,
			string,
			context.registry.constantFlags(),
			WoodPaletteEntry.CLIENT_EMPTY
		);
		return WoodPaletteEntry.CONSTANT_FACTORY.createConstant(constant(string), context.registry.constantFlags());
	}

	@Override
	public String toString() {
		return "wood_palette";
	}
}