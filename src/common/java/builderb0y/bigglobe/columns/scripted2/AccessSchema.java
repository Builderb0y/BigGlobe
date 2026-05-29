package builderb0y.bigglobe.columns.scripted2;

import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.scripting.bytecode.TypeInfo;

import net.minecraft.core.Holder;

public record AccessSchema(Holder<ElementSpec> type, boolean is_3d) {

	public TypeSpec typeSpec(ColumnEntryRegistry registry, ColumnEntry entry) {
		return ElementSpec.requireType(this.type, TypeSpec.class, () -> registry.idOf(entry) + " > params > type");
	}

	public TypeInfo typeInfo(ColumnEntryRegistry registry, ColumnEntry entry) {
		return this.typeSpec(registry, entry).getTypeInfo();
	}
}