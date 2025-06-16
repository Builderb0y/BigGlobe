package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.classes.TypeSpec.CompileState;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;

public class ClassHierarchy {

	public final ColumnEntryRegistry registry;
	public final Map<TypeSpec, RegistryEntry<ElementSpec>> typeLookup;

	public ClassHierarchy(ColumnEntryRegistry registry) {
		this.registry = registry;
		this.typeLookup = null; /*(
			registry
			.registries
			.getRegistry(BigGlobeDynamicRegistries.ELEMENT_SPEC_REGISTRY_KEY)
			.streamEntries()
			.filter((RegistryEntry<ElementSpec> entry) -> entry.value() instanceof TypeSpec)
			.collect(Collectors.toMap(RegistryEntry<ElementSpec>::value, Function.identity()))
		);*/
	}

	public void assemble() throws CustomClassFormatException {
		ObjectOpenCustomHashSet<TypeSpec> names = new ObjectOpenCustomHashSet<>(TypeSpec.NAME_STRATEGY);
		for (TypeSpec type : this.typeLookup.keySet()) {
			TypeSpec existing = names.addOrGet(type);
			if (existing != type) {
				throw new CustomClassFormatException("Duplicate class: " + type.name() + " (provided by " + UnregisteredObjectException.getKey(this.entryOf(existing)) + " and " + UnregisteredObjectException.getKey(this.entryOf(type)));
			}
		}
		for (CompileState state : CompileState.EXCEPT_FRESH) {
			CustomClassFormatException root = null;
			for (Map.Entry<TypeSpec, RegistryEntry<ElementSpec>> entry : this.typeLookup.entrySet()) try {
				entry.getKey().doProgressTo(state, this);
			}
			catch (Exception exception) {
				if (root == null) root = new CustomClassFormatException("Exception " + state.description);
				root.addSuppressed(new CustomClassFormatException("Exception " + state.description + " for " + UnregisteredObjectException.getID(entry.getValue()), exception));
			}
			if (root != null) throw root;
		}
	}

	public void link(ScriptClassLoader loader) {
		for (TypeSpec type : this.typeLookup.keySet()) {
			type.link(loader);
		}
	}

	public void setupEnvironment(MutableScriptEnvironment environment, ClassCompileContext caller) {
		for (TypeSpec type : this.typeLookup.keySet()) {
			type.setupEnvironment(environment, caller);
		}
	}

	public RegistryEntry<ElementSpec> entryOf(TypeSpec spec) {
		return this.typeLookup.get(spec);
	}
}