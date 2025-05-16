package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
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
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		StringData stringData = data.tryAsString();
		if (stringData == null) throw new ClassCastException("Not a String: " + data);
		RegistryEntry<WoodPalette> entry = ConstantFactory.getEntryServerOnly(
			BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY,
			stringData.value,
			context.registry.constantFlags(),
			WoodPaletteEntry.CLIENT_EMPTY
		);
		return ldc(new WoodPaletteEntry(entry), WoodPaletteEntry.INFO.type);
	}

	@Override
	public String toString() {
		return "wood_palette";
	}
}