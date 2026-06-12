package builderb0y.bigglobe.classes.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.classes.compile.StagedCompileable.BulkStagedCompiler;
import builderb0y.bigglobe.classes.spec.BaseClassSpec;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;

public class ClassHierarchy extends BulkStagedCompiler<ClassHierarchy, ElementSpec> {

	public static final Hash.Strategy<TypeSpec>
		TYPE_INFO_STRATEGY = HashStrategies.map(
			HashStrategies.defaultStrategy(),
			TypeSpec::getTypeInfo
		);

	public final ColumnEntryRegistry registry;
	public final BetterRegistry<ElementSpec> elementRegistry;
	public final Map<ElementSpec, Holder<ElementSpec>> elements;
	public final List<Holder<ElementSpec>> rootTypes;
	public final ObjectOpenCustomHashSet<TypeSpec> names;
	public final ObjectOpenCustomHashSet<TypeSpec> typeInfos;

	public ClassHierarchy(ColumnEntryRegistry registry) {
		this.registry = registry;
		this.elementRegistry = registry.registries.getRegistry(BigGlobeDynamicRegistries.ELEMENT_SPEC_REGISTRY_KEY);
		this.elements = this.elementRegistry.streamEntries().collect(Collectors.toMap(Holder<ElementSpec>::value, Function.identity()));
		this.rootTypes = new ArrayList<>();
		this.names = new ObjectOpenCustomHashSet<>(TypeSpec.NAME_STRATEGY);
		this.typeInfos = new ObjectOpenCustomHashSet<>(TYPE_INFO_STRATEGY);
	}

	@Override
	public ClassHierarchy getSelf() {
		return this;
	}

	@Override
	public Collection<? extends Holder<? extends ElementSpec>> getElementsToCompileForStep(CompileStep step) {
		return switch (step) {
			case FRESH -> throw new UnsupportedOperationException();
			case REFERENCE -> this.elements.values();
			case RESOLVE, VERIFY, REPRESENT, COMPILE -> this.rootTypes;
		};
	}

	public void checkName(TypeSpec type) throws DetailedException {
		TypeSpec existing = this.names.addOrGet(type);
		if (existing != type) {
			throw new CustomClassFormatException("Duplicate class name: " + type.name() + " (provided by " + this.idOf(existing) + " and " + this.idOf(type) + ")");
		}
		existing = this.typeInfos.addOrGet(type);
		if (existing != type) {
			throw new CustomClassFormatException("Duplicate class: " + type.name() + " (provided by " + this.idOf(existing) + " and " + this.idOf(type) + ")");
		}
	}

	public void link(ScriptClassLoader loader) {
		for (Holder<ElementSpec> type : this.rootTypes) {
			if (type.value() instanceof BaseClassSpec clazz) {
				clazz.define(loader);
			}
		}
	}

	public void setupEnvironment(ExpressionParser parser, ExternalEnvironmentParams params) {
		for (Holder<ElementSpec> type : this.elements.values()) {
			type.value().setupEnvironment(type, parser, params);
		}
	}

	public Holder<ElementSpec> entryOf(ElementSpec spec) {
		return this.elements.get(spec);
	}

	public Identifier idOf(ElementSpec spec) {
		return UnregisteredObjectException.getID(this.entryOf(spec));
	}
}