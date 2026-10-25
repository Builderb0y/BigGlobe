package builderb0y.bigglobe.columns.scripted2.entries;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.columns.scripted.classes.ConstantFormatException;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantColumnEntry extends ColumnEntry {

	public final Data value;

	public ConstantColumnEntry(AccessSchema params, Data value) {
		super(params);
		this.value = value;
	}

	@Override
	public boolean hasFieldSetterAndFlag() {
		return false;
	}

	@Override
	public void createContext(ColumnEntryRegistry registry) throws ColumnValueException {
		ColumnEntryContext context = new ColumnEntryContext();
		context.uniquifier = registry.columnCompileContext.clazz.memberUniquifier++;
		context.internalName = ColumnCompileContext.internalName(UnregisteredObjectException.getID(registry.entryOf(this)), context.uniquifier);
		context.mainGetter = registry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			"get_" + context.internalName,
			ElementSpec.asType(this.params.type()).getTypeInfo()
		);
		registry.columnCompileContext.setCompileContext(this, context);
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ColumnValueException, ScriptParsingException {
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		try {
			ElementSpec.asType(this.params.type()).parseConstant(registry.classHierarchy, this.value, load("this", registry.columnCompileContext.columnTypeInfo())).emitBytecode(context.mainGetter);
		}
		catch (ConstantFormatException exception) {
			throw new ColumnValueException(exception);
		}
		context.mainGetter.endCode();
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies(Holder<? extends DependencyView> self, WorldTraits traits) {
		return Stream.empty();
	}
}