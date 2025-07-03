package builderb0y.bigglobe.columns.scripted2;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.columns.scripted.classes.ClassHierarchy;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.columns.scripted.classes.TypeSpec;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.input.ScriptUsage;

public record Valid(
	@VerifyNullable ScriptUsage where,
	@VerifyNullable ScriptUsage min_y,
	@VerifyNullable ScriptUsage max_y,
	Data fallback
) {

	public boolean isUseful(boolean _3D) {
		return (
			_3D
			? (this.where != null || this.min_y != null || this.max_y != null)
			: (this.where != null)
		);
	}

	public InsnTree getFallback(ClassHierarchy hierarchy, TypeSpec type, InsnTree loadColumn) {
		return type.parseConstant(hierarchy, this.fallback, loadColumn);
	}

	public InsnTree getFallback(ClassHierarchy hierarchy, RegistryEntry<ElementSpec> type, InsnTree loadColumn) {
		return this.getFallback(hierarchy, ElementSpec.asType(type), loadColumn);
	}
}