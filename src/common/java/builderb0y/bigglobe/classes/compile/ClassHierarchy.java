package builderb0y.bigglobe.classes.compile;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec.CompileStep;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;

public class ClassHierarchy {

	public final ColumnEntryRegistry registry;
	public final Map<ElementSpec, Holder<ElementSpec>> elements;
	public final List<TypeSpec> types;

	public ClassHierarchy(ColumnEntryRegistry registry) throws CustomClassFormatException {
		this.registry = registry;
		this.elements = null; /*(
			registry
			.registries
			.getRegistry(BigGlobeDynamicRegistries.ELEMENT_SPEC_REGISTRY_KEY)
			.streamEntries()
			.collect(Collectors.toMap(RegistryEntry<ElementSpec>::value, Function.identity()))
		);*/
		this.types = null; /*(
			this
			.elements
			.keySet()
			.stream()
			.filter(TypeSpec.class::isInstance)
			.map(TypeSpec.class::cast)
			.toList()
		)*/
		ObjectOpenCustomHashSet<TypeSpec> names = new ObjectOpenCustomHashSet<>(TypeSpec.NAME_STRATEGY);
		for (TypeSpec type : this.types) {
			TypeSpec existing = names.addOrGet(type);
			if (existing != type) {
				throw new CustomClassFormatException("Duplicate class: " + type.name() + " (provided by " + UnregisteredObjectException.getKey(this.entryOf(existing)) + " and " + UnregisteredObjectException.getKey(this.entryOf(type)));
			}
		}
	}

	public void progressTo(CompileStep state) throws CustomClassFormatException {
		CustomClassFormatException root = null;
		for (TypeSpec type : this.types) try {
			type.doProgressTo(state, this);
		}
		catch (Exception exception) {
			if (root == null) root = new CustomClassFormatException("Exception " + state.description);
			root.addSuppressed(new CustomClassFormatException("Exception " + state.description + " for " + UnregisteredObjectException.getID(this.entryOf(type)), exception));
		}
		if (root != null) throw root;
	}

	public void link(ScriptClassLoader loader) {
		for (TypeSpec type : this.types) {
			type.link(loader);
		}
	}

	public void setupEnvironment(MutableScriptEnvironment environment, @Nullable InsnTree loadCustomClass) {
		for (TypeSpec type : this.types) {
			type.setupEnvironment(environment, loadCustomClass);
		}
	}

	public Holder<ElementSpec> entryOf(ElementSpec spec) {
		return this.elements.get(spec);
	}

	public Identifier idOf(ElementSpec spec) {
		return UnregisteredObjectException.getID(this.entryOf(spec));
	}
}