package builderb0y.bigglobe.columns.restrictions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.bigglobe.scripting.ScriptErrorCatcher;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ScriptParsingException;

public abstract class PropertyColumnRestriction extends ScriptErrorCatcher.Impl implements ColumnRestriction, DelayedCompileable {

	public final Holder<ColumnEntry> property;
	public MethodHandle getter;

	public PropertyColumnRestriction(Holder<ColumnEntry> property) {
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
		TypeInfo type = this.property.value().typeInfo(registry);
		Class<?> fromClass = switch (type.getSort()) {
			case FLOAT -> float.class;
			case DOUBLE -> double.class;
			default -> throw new IllegalArgumentException("Property should point to a float or double-typed column value, but " + this.property + " points to a column value of type " + type);
		};
		MethodHandle handle;
		String getterName = registry.columnCompileContext.getCompileContext(this.property.value()).mainGetter.node.name;
		try {
			if (this.property.value().params.is_3d()) {
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