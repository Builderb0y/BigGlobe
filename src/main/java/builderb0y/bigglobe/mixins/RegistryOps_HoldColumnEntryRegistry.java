package builderb0y.bigglobe.mixins;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.registry.RegistryOps;

import builderb0y.bigglobe.codecs.registries.BetterRegistryLookupCoder;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.mixinInterfaces.ColumnEntryRegistryHolder;
import builderb0y.scripting.parsing.ScriptParsingException;

@Mixin(RegistryOps.class)
public class RegistryOps_HoldColumnEntryRegistry implements ColumnEntryRegistryHolder {

	public ColumnEntryRegistry bigglobe_columnEntryRegistry;
	public List<DelayedCompileable> bigglobe_compileables;

	@Override
	public ColumnEntryRegistry bigglobe_getColumnEntryRegistry() {
		this.bigglobe_initializeColumnEntryRegistry();
		return this.bigglobe_columnEntryRegistry;
	}

	@Override
	public void bigglobe_delayCompile(DelayedCompileable compileable) {
		if (this.bigglobe_columnEntryRegistry != null) {
			try {
				compileable.compile(this.bigglobe_columnEntryRegistry);
			}
			catch (ScriptParsingException exception) {
				throw new RuntimeException(exception);
			}
		}
		else {
			if (this.bigglobe_compileables == null) {
				this.bigglobe_compileables = new ArrayList<>(64);
			}
			this.bigglobe_compileables.add(compileable);
		}
	}

	public void bigglobe_initializeColumnEntryRegistry() {
		if (this.bigglobe_columnEntryRegistry == null) return;
		RegistryOps<?> ops = (RegistryOps<?>)(Object)(this);
		BetterRegistry.Lookup lookup = BetterRegistryLookupCoder.fromOps(ops);
		try {
			this.bigglobe_columnEntryRegistry = new ColumnEntryRegistry(lookup);
			if (this.bigglobe_compileables != null) {
				for (DelayedCompileable compileable : this.bigglobe_compileables) {
					compileable.compile(this.bigglobe_columnEntryRegistry);
				}
				this.bigglobe_compileables = null;
			}
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception);
		}
	}
}