package builderb0y.bigglobe.mixinInterfaces;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;

public interface ColumnEntryRegistryHolder {

	public abstract ColumnEntryRegistry bigglobe_getColumnEntryRegistry();

	public abstract void bigglobe_delayCompile(DelayedCompileable compileable);
}