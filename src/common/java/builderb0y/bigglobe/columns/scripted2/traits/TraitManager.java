package builderb0y.bigglobe.columns.scripted2.traits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.classes.spec.TypeSpec;
import builderb0y.bigglobe.columns.scripted2.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.NonConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TraitManager {

	public final ColumnEntryRegistry columnEntryRegistry;
	public final Map<WorldTrait, Holder<WorldTrait>> traits;
	public final Map<Holder<WorldTrait>, TraitInfo> infos;
	public final ClassCompileContext baseTraitsClass;
	public WorldTraits baseTraits;

	public TraitManager(ColumnEntryRegistry columnEntryRegistry) {
		this.columnEntryRegistry = columnEntryRegistry;
		this.traits = (
			columnEntryRegistry
			.registries
			.getRegistry(BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY)
			.streamEntries()
			.collect(Collectors.toMap(Holder<WorldTrait>::value, Function.identity()))
		);
		this.infos = new HashMap<>();
		this.baseTraitsClass = new ClassCompileContext(
			ACC_PUBLIC | ACC_SUPER,
			ClassType.CLASS,
			Type.getInternalName(WorldTraits.class) + "$GeneratedBase_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			WorldTraits.TYPE,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.baseTraitsClass.addNoArgConstructor(ACC_PUBLIC);
		for (Holder<WorldTrait> entry : this.traits.values()) {
			TypeInfo traitType = entry.value().getTypeInfo(this);
			MethodCompileContext getter = this.baseTraitsClass.newMethod(
				ACC_PUBLIC,
				"get_" + ColumnCompileContext.internalName(
					UnregisteredObjectException.getID(entry),
					this.baseTraitsClass.memberUniquifier++
				),
				traitType,
				entry.value().schema().is_3d()
				? new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()), new LazyVarInfo("y", TypeInfos.INT) }
				: new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()) }
			);
			MethodCompileContext setter = this.baseTraitsClass.newMethod(
				ACC_PUBLIC,
				"set_" + ColumnCompileContext.internalName(
					UnregisteredObjectException.getID(entry),
					this.baseTraitsClass.memberUniquifier++
				),
				TypeInfos.VOID,
				entry.value().schema().is_3d()
				? new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()), new LazyVarInfo("y", TypeInfos.INT), new LazyVarInfo("value", traitType) }
				: new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()), new LazyVarInfo("value", traitType) }
			);
			this.infos.put(entry, new TraitInfo(getter, setter));
		}
	}

	public Holder<WorldTrait> entryOf(WorldTrait trait) {
		return this.traits.get(trait);
	}

	public Identifier idOf(WorldTrait trait) {
		return UnregisteredObjectException.getID(this.entryOf(trait));
	}

	public void compile() {
		for (Holder<WorldTrait> entry : this.traits.values()) {
			TraitInfo info = this.infos.get(entry);
			if (entry.value().fallback() != null) {
				info.getter.setCode(
					this.columnEntryRegistry.parserFlags(),
					entry.value().fallback().getSource(),
					(MutableScriptEnvironment environment) -> {
						environment
						.addAll(MathScriptEnvironment.INSTANCE)
						.addAll(StatelessRandomScriptEnvironment.INSTANCE)
						.configure(MinecraftScriptEnvironment.create())
						.configure(ScriptedColumn.baseEnvironment(
							load("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()),
							null,
							this.columnEntryRegistry.columnCompileContext.columnTypeInfo()
						))
						.addAll(ColorScriptEnvironment.ENVIRONMENT);
						this.columnEntryRegistry.setupEnvironment(
							environment,
							new ExternalEnvironmentParams()
							.withColumn(load("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()))
							.withY(entry.value().schema().is_3d() ? load("y", TypeInfos.INT) : null)
							.trackDependencies(info)
						);
					}
				);
			}
			else {
				throw_(
					newInstance(
						MethodInfo.findConstructor(TraitNotPresentException.class, String.class),
						ldc(UnregisteredObjectException.getID(entry).toString())
					)
				)
				.emitBytecode(info.getter);
				info.getter.endCode();
			}
			throw_(
				newInstance(
					MethodInfo.findConstructor(TraitNotSettableException.class, String.class),
					ldc(UnregisteredObjectException.getID(entry).toString())
				)
			)
			.emitBytecode(info.setter);
			info.setter.endCode();
		}
		try {
			this.baseTraits = (
				this
				.columnEntryRegistry
				.loader
				.defineClass(this.baseTraitsClass, ColumnEntryRegistry.CLASS_DUMP_DIRECTORY, null)
				.asSubclass(WorldTraits.class)
				.getDeclaredConstructor((Class<?>[])(null))
				.newInstance((Object[])(null))
			);
			this.baseTraits.dependenciesPerTrait = this.infos;
		}
		catch (Throwable throwable) {
			throw new RuntimeException("An exception occurred while trying to create the base world traits.", throwable);
		}
	}

	public WorldTraits createTraits(Map<Holder<WorldTrait>, WorldTraitProvider> implementations) {
		if (implementations == null || implementations.isEmpty()) {
			return this.baseTraits;
		}
		ClassCompileContext context = new ClassCompileContext(
			ACC_PUBLIC | ACC_SUPER,
			ClassType.CLASS,
			Type.getInternalName(WorldTraits.class) + "$GeneratedImpl_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			this.baseTraitsClass.info,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		context.addNoArgConstructor(ACC_PUBLIC);
		Map<Holder<WorldTrait>, SetBasedMutableDependencyView> dependencyMap = new HashMap<>(this.infos);
		for (Map.Entry<Holder<WorldTrait>, WorldTraitProvider> entry : implementations.entrySet()) {
			OverriddenDependencyView dependencies = new OverriddenDependencyView();
			dependencyMap.put(entry.getKey(), dependencies);
			TraitInfo info = this.infos.get(entry.getKey());
			LazyVarInfo column = new LazyVarInfo("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo());
			LazyVarInfo y = entry.getKey().value().schema().is_3d() ? new LazyVarInfo("y", TypeInfos.INT) : null;
			MethodCompileContext implGetter = context.newMethod(
				ACC_PUBLIC,
				info.getter.info.name,
				info.getter.info.returnType,
				y != null
				? new LazyVarInfo[] { column, y }
				: new LazyVarInfo[] { column }
			);
			implGetter.setCode(
				this.columnEntryRegistry.parserFlags(),
				entry.getValue().get().getSource(),
				(MutableScriptEnvironment environment) -> {
					environment
						.addAll(MathScriptEnvironment.INSTANCE)
						.addAll(StatelessRandomScriptEnvironment.INSTANCE)
						.configure(MinecraftScriptEnvironment.create())
						.configure(ScriptedColumn.baseEnvironment(load(column), null, column.type))
						.addAll(ColorScriptEnvironment.ENVIRONMENT);
					if (y != null) environment.addVariableLoad(y);
					this.columnEntryRegistry.setupEnvironment(
						environment,
						new ExternalEnvironmentParams()
						.withColumn(load("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()))
						.withY(entry.getKey().value().schema().is_3d() ? load("y", TypeInfos.INT) : null)
						.trackDependencies(dependencies)
					);
				}
			);
			if (entry.getValue().set() != null) {
				LazyVarInfo value = new LazyVarInfo("value", info.getter.info.returnType);
				MethodCompileContext implSetter = context.newMethod(
					ACC_PUBLIC,
					info.setter.info.name,
					TypeInfos.VOID,
					y != null
					? new LazyVarInfo[] { column, y, value }
					: new LazyVarInfo[] { column, value }
				);
				implSetter.setCode(
					this.columnEntryRegistry.parserFlags(),
					entry.getValue().set().getSource(),
					(MutableScriptEnvironment environment) -> {
						environment
						.addAll(MathScriptEnvironment.INSTANCE)
						.addAll(StatelessRandomScriptEnvironment.INSTANCE)
						.configure(MinecraftScriptEnvironment.create())
						.configure(ScriptedColumn.baseEnvironment(load(column), null, column.type))
						.addAll(ColorScriptEnvironment.ENVIRONMENT)
						.addVariableLoad(value);
						if (y != null) environment.addVariableLoad(y);
						this.columnEntryRegistry.setupEnvironment(
							environment,
							new ExternalEnvironmentParams()
							.withColumn(load("column", this.columnEntryRegistry.columnCompileContext.columnTypeInfo()))
							.withY(entry.getKey().value().schema().is_3d() ? load("y", TypeInfos.INT) : null)
							.mutable()
							.trackDependencies(dependencies)
						);
					}
				);
			}
		}
		try {
			WorldTraits traits = (
				this
				.columnEntryRegistry
				.loader
				.defineClass(context, ColumnEntryRegistry.CLASS_DUMP_DIRECTORY, null)
				.asSubclass(WorldTraits.class)
				.getDeclaredConstructor((Class<?>[])(null))
				.newInstance((Object[])(null))
			);
			traits.dependenciesPerTrait = dependencyMap;
			return traits;
		}
		catch (Throwable throwable) {
			throw new RuntimeException("An exception occurred while trying to create the base world traits.", throwable);
		}
	}

	public void setupEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		environment
		.addVariableConstant("world_traits", this.baseTraitsClass.info)
		.addField(
			this.columnEntryRegistry.columnCompileContext.columnTypeInfo(),
			"world_traits",
			new FieldHandler.Named("column.world_traits", (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
				return switch (mode) {
					case NORMAL, NULLABLE -> new WorldTraitInsnTreeHack(receiver);
					case RECEIVER, NULLABLE_RECEIVER -> receiver;
				};
			})
		);
		for (Holder<WorldTrait> entry : this.traits.values()) {
			String name = UnregisteredObjectException.getID(entry).toString();
			TraitInfo info = this.infos.get(entry);
			MethodInfo getter = info.getter.info;
			MethodInfo setter = info.setter.info;
			boolean is3D = entry.value().schema().is_3d();
			environment.addMethod(WorldTraitClassHack.TYPE, name, new MethodHandler.Named(
				"column.world_traits.`" + name + '`' + params.getPossibleArguments(is3D, true),
				(ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
					if (receiver.getConstantValue() instanceof WorldTraitConstantValueHack hack) {
						if (params.dependencies != null) params.dependencies.addDependency(entry);
						return params.resolveColumn(parser, name1, is3D, true, getter, setter, hack.column, arguments);
					}
					else {
						throw new ScriptParsingException("Somehow obtained a WorldTraitClassHack in an unexpected way...", parser.input);
					}
				}
			));
			if (params.requiresNoArguments(is3D, true)) {
				environment.addField(WorldTraitClassHack.TYPE, name, new FieldHandler.Named(
					"column.world_traits.`" + name + '`' + params.getPossibleArguments(is3D, true),
					(ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
						if (receiver.getConstantValue() instanceof WorldTraitConstantValueHack hack) {
							if (params.dependencies != null) params.dependencies.addDependency(entry);
							return params.resolveColumn(parser, name1, is3D, true, getter, setter, hack.column).tree();
						}
						else {
							throw new ScriptParsingException("Somehow obtained a WorldTraitClassHack in an unexpected way...", parser.input);
						}
					}
				));
			}
			environment.addQualifiedFunction(
				this.baseTraitsClass.info,
				name,
				new FunctionHandler.Named(
					"world_traits.`" + name + '`' + params.getPossibleArguments(is3D, false),
					(ExpressionParser parser, String name1, InsnTree... arguments) -> {
						if (params.dependencies != null) params.dependencies.addDependency(entry);
						return params.resolveColumn(parser, name1, is3D, true, getter, setter, null, arguments);
					}
				)
			);
			if (params.requiresNoArguments(is3D, false)) {
				environment.addQualifiedVariable(
					this.baseTraitsClass.info,
					name,
					new VariableHandler.Named(
						"world_traits.`" + name + '`',
						(ExpressionParser parser, String name1) -> {
							if (params.dependencies != null) params.dependencies.addDependency(entry);
							return params.resolveColumn(parser, name1, is3D, true, getter, setter, null).tree();
						}
					)
				);
			}
		}
	}

	public static class WorldTraitClassHack {

		public static final TypeInfo TYPE = TypeInfo.of(WorldTraitClassHack.class);
	}

	//must be able to continue working when wrapped in a group or scope or line number or whatever.
	public static class WorldTraitConstantValueHack extends NonConstantValue {

		public final InsnTree column;

		public WorldTraitConstantValueHack(InsnTree column) {
			this.column = column;
		}
	}

	public static class WorldTraitInsnTreeHack implements InsnTree {

		public final WorldTraitConstantValueHack constantValueHack;

		public WorldTraitInsnTreeHack(InsnTree column) {
			this.constantValueHack = new WorldTraitConstantValueHack(column);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			method.node.visitInsn(ACONST_NULL);
		}

		@Override
		public TypeInfo getTypeInfo() {
			return WorldTraitClassHack.TYPE;
		}

		@Override
		public ConstantValue getConstantValue() {
			return this.constantValueHack;
		}
	}

	public static class TraitInfo implements SetBasedMutableDependencyView {

		public final MethodCompileContext getter, setter;
		public final Set<Holder<? extends DependencyView>> dependencies;

		public TraitInfo(MethodCompileContext getter, MethodCompileContext setter) {
			this.getter = getter;
			this.setter = setter;
			this.dependencies = new HashSet<>();
		}

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}

	public static class OverriddenDependencyView implements SetBasedMutableDependencyView {

		public final Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}
}