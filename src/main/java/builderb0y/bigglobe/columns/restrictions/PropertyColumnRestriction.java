package builderb0y.bigglobe.columns.restrictions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.scripting.ScriptErrorCatcher;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class PropertyColumnRestriction extends ScriptErrorCatcher.Impl implements ColumnRestriction, DelayedCompileable {

	public final RegistryEntry<ColumnEntry> property;
	public MethodHandle getter;

	public PropertyColumnRestriction(RegistryEntry<ColumnEntry> property) {
		this.property = property;
	}

	@Override
	public @Nullable String getDebugName() {
		return UnregisteredObjectException.getID(this.property).toString();
	}

	@Override
	public @Nullable String getSource() {
		return null;
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
		ColumnEntryMemory memory = registry.columnContext.memories.get(this.property.value());
		if (memory == null) throw new IllegalStateException("Unknown or voronoi-enabled property: " + this.property);
		TypeInfo type = memory.getTyped(ColumnEntryMemory.TYPE_CONTEXT).type();
		Class<?> fromClass = switch (type.getSort()) {
			case FLOAT -> float.class;
			case DOUBLE -> double.class;
			default -> throw new IllegalArgumentException("Property should point to a float or double-typed column value, but " + this.property + " points to a column value of type " + type);
		};
		MethodHandle handle;
		String getterName = memory.getTyped(ColumnEntryMemory.GETTER).node.name;
		try {
			if (memory.getTyped(ColumnEntryMemory.ENTRY).getAccessSchema().is_3d()) {
				handle = registry.columnLookup.findVirtual(registry.columnClass, getterName, MethodType.methodType(fromClass, int.class));
			}
			else {
				handle = registry.columnLookup.findVirtual(registry.columnClass, getterName, MethodType.methodType(fromClass));
				handle = MethodHandles.dropArguments(handle, 1, int.class);
			}
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
		this.getter = handle.asType(MethodType.methodType(double.class, ScriptedColumn.class, int.class));
	}
}