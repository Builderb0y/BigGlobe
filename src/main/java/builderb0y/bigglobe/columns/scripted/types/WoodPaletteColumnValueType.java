package builderb0y.bigglobe.columns.scripted.types;

import com.mojang.datafixers.util.Unit;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.WoodPaletteEntry;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class WoodPaletteColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return WoodPaletteEntry.TYPE;
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		if (object == Unit.INSTANCE) return ldc(null, this.getTypeInfo());
		String string = (String)(object);
		context.registry.registries.getRegistry(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY).getOrCreateEntry(RegistryKey.of(BigGlobeDynamicRegistries.WOOD_PALETTE_REGISTRY_KEY, IdentifierVersions.create(string)));
		return WoodPaletteEntry.CONSTANT_FACTORY.createConstant(constant(string));
	}

	@Override
	public String toString() {
		return "wood_palette";
	}
}