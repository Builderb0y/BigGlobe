package builderb0y.bigglobe.columns.scripted.classes;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class ClassSpec extends BaseClassSpec {

	public ClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable RegistryEntry<ElementSpec> parent,
		DelayedEntryList<ElementSpec> members
	) {
		super(name, isAbstract, parent, members);
	}

	@Override
	public FieldInfo baseColumnField() {
		return ObjectBase.INFO.column;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedField("column");
	}
}