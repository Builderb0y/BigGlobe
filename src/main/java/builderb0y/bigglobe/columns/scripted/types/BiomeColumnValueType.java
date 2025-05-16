package builderb0y.bigglobe.columns.scripted.types;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@RecordLike({})
public class BiomeColumnValueType extends AbstractColumnValueType {

	@Override
	public TypeInfo getTypeInfo() {
		return BiomeEntry.TYPE;
	}

	@Override
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		if (data.isEmpty()) return ldc(null, this.getTypeInfo());
		StringData stringData = data.tryAsString();
		if (stringData == null) throw new ClassCastException("Not a String: " + data);
		//create the entry early so that if it doesn't exist, the world will fail to load.
		RegistryEntry<Biome> biome = context.registry.registries.getRegistry(RegistryKeys.BIOME).requireByName(stringData.value);
		return ldc(new BiomeEntry(biome), BiomeEntry.TYPE);
	}

	@Override
	public String toString() {
		return "biome";
	}
}