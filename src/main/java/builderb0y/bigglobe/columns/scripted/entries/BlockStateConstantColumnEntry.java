package builderb0y.bigglobe.columns.scripted.entries;

import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.BlockState2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateConstantColumnEntry extends ConstantColumnEntry {

	public final String value;

	public BlockStateConstantColumnEntry(String value) {
		this.value = value;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new BlockState2DAccessSchema();
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		BlockProperties block = BlockStateCoder.decodeState(context.root().registry.registries.getRegistry(RegistryKeyVersions.block()), this.value);
		Set<Property<?>> missing = block.missing();
		if (!missing.isEmpty()) {
			BigGlobeMod.LOGGER.warn("Missing properties: " + missing + " for column value " + memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
		}
		return_(ldc(block.state(), type(BlockState.class))).emitBytecode(getterMethod);
		getterMethod.endCode();
	}
}