package builderb0y.bigglobe.columns.scripted.traits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage.ColorScriptEnvironment;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TraitManager {

	public final ColumnEntryRegistry columnEntryRegistry;
	public final BetterRegistry<WorldTrait> traitRegistry;
	public final Map<RegistryEntry<WorldTrait>, TraitInfo> infos;
	public final ClassCompileContext baseTraitsClass;
	public WorldTraits baseTraits;

	public TraitManager(ColumnEntryRegistry columnEntryRegistry) {
		this.columnEntryRegistry = columnEntryRegistry;
		this.traitRegistry = columnEntryRegistry.registries.getRegistry(BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY);
		this.infos = new HashMap<>();
		this.baseTraitsClass = new ClassCompileContext(
			ACC_PUBLIC,
			ClassType.CLASS,
			Type.getInternalName(WorldTraits.class) + "$GeneratedBase_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			WorldTraits.TYPE,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.baseTraitsClass.addNoArgConstructor(ACC_PUBLIC);
		this.traitRegistry.streamEntries().sequential().forEach((RegistryEntry<WorldTrait> entry) -> {
			MethodCompileContext method = this.baseTraitsClass.newMethod(
				ACC_PUBLIC,
				"get_" + DataCompileContext.internalName(
					UnregisteredObjectException.getID(entry),
					this.baseTraitsClass.memberUniquifier++
				),
				this.columnEntryRegistry.columnContext.getTypeContext(entry.value().schema().type()).type(),
				entry.value().schema().is_3d()
				? new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnContext.selfType()), new LazyVarInfo("y", TypeInfos.INT) }
				: new LazyVarInfo[] { new LazyVarInfo("column", this.columnEntryRegistry.columnContext.selfType()) }
			);
			this.infos.put(entry, new TraitInfo(method));
		});
	}

	public void compile() {
		this.traitRegistry.streamEntries().forEach((RegistryEntry<WorldTrait> entry) -> {
			TraitInfo info = this.infos.get(entry);
			if (entry.value().fallback() != null) {
				info.method.setCode(entry.value().fallback().findSource(), (MutableScriptEnvironment environment) -> {
					this.columnEntryRegistry.setupExternalEnvironment(
						environment,
						new ExternalEnvironmentParams()
						.withColumn(load("column", this.columnEntryRegistry.columnContext.selfType()))
						.withY(entry.value().schema().is_3d() ? load("y", TypeInfos.INT) : null)
						.trackDependencies(info)
					);
				});
			}
			else {
				throw_(
					newInstance(
						MethodInfo.findConstructor(TraitNotPresentException.class, String.class),
						ldc(UnregisteredObjectException.getID(entry).toString())
					)
				)
				.emitBytecode(info.method);
				info.method.endCode();
			}
		});
		try {
			this.baseTraits = (
				this
				.columnEntryRegistry
				.loader
				.defineClass(this.baseTraitsClass)
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

	public WorldTraits createTraits(Map<RegistryEntry<WorldTrait>, ScriptUsage> implementations) {
		if (implementations == null || implementations.isEmpty()) {
			return this.baseTraits;
		}
		ClassCompileContext context = new ClassCompileContext(
			ACC_PUBLIC,
			ClassType.CLASS,
			Type.getInternalName(WorldTraits.class) + "$GeneratedImpl_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			this.baseTraitsClass.info,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		context.addNoArgConstructor(ACC_PUBLIC);
		for (Map.Entry<RegistryEntry<WorldTrait>, ScriptUsage> entry : implementations.entrySet()) {
			TraitInfo info = this.infos.get(entry.getKey());
			LazyVarInfo column = new LazyVarInfo("column", this.columnEntryRegistry.columnContext.selfType());
			LazyVarInfo y = entry.getKey().value().schema().is_3d() ? new LazyVarInfo("y", TypeInfos.INT) : null;
			MethodCompileContext implMethod = context.newMethod(
				ACC_PUBLIC,
				info.method.info.name,
				info.method.info.returnType,
				y != null
				? new LazyVarInfo[] { column, y }
				: new LazyVarInfo[] { column }
			);
			implMethod.setCode(entry.getValue().findSource(), (MutableScriptEnvironment environment) -> {
				environment
				.addAll(MathScriptEnvironment.INSTANCE)
				.addAll(StatelessRandomScriptEnvironment.INSTANCE)
				.configure(MinecraftScriptEnvironment.create())
				.configure(ScriptedColumn.baseEnvironment(load(column)))
				.addAll(ColorScriptEnvironment.ENVIRONMENT);
				if (y != null) environment.addVariableLoad(y);
				this.columnEntryRegistry.setupExternalEnvironment(
					environment,
					new ExternalEnvironmentParams()
					.withColumn(load("column", this.columnEntryRegistry.columnContext.selfType()))
					.withY(entry.getKey().value().schema().is_3d() ? load("y", TypeInfos.INT) : null)
					.trackDependencies(info)
				);
			});
		}
		try {
			WorldTraits traits = (
				this
				.columnEntryRegistry
				.loader
				.defineClass(context)
				.asSubclass(WorldTraits.class)
				.getDeclaredConstructor((Class<?>[])(null))
				.newInstance((Object[])(null))
			);
			traits.dependenciesPerTrait = this.infos;
			return traits;
		}
		catch (Throwable throwable) {
			throw new RuntimeException("An exception occurred while trying to create the base world traits.", throwable);
		}
	}

	public void setupInternalEnvironment(MutableScriptEnvironment environment, InsnTree loadColumn, @Nullable InsnTree loadY, MutableDependencyView dependencies) {
		environment
		.addVariableConstant("world_traits", this.baseTraitsClass.info)
		.addFieldInvoke(ScriptedColumn.INFO.worldTraits);
		this.traitRegistry.streamEntries().forEach((RegistryEntry<? extends WorldTrait> entry) -> {
			String name = UnregisteredObjectException.getID(entry).toString();
			TraitInfo info = this.infos.get(entry);
			if (entry.value().schema().is_3d()) {
				environment.addMethod(
					this.baseTraitsClass.info,
					name,
					new MethodHandler.Named(
						"world_traits.`" + name + "`(y)",
						(ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
							InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name1, types("I"), CastMode.IMPLICIT_NULL, arguments);
							if (castArguments == null) return null;
							dependencies.addDependency(entry);
							return new CastResult(mode.makeInvoker(parser, ScriptedColumn.INFO.worldTraits(loadColumn), info.method.info, loadColumn, castArguments[0]), castArguments != arguments);
						}
					)
				);
				if (loadY != null) {
					environment.addField(
						this.baseTraitsClass.info,
						name,
						new FieldHandler.Named(
							"world_traits.`" + name + '`',
							(ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
								dependencies.addDependency(entry);
								return mode.makeInvoker(parser, ScriptedColumn.INFO.worldTraits(loadColumn), info.method.info, loadColumn, loadY);
							}
						)
					);
				}
			}
			else {
				environment.addField(
					this.baseTraitsClass.info,
					name,
					new FieldHandler.Named(
						"world_traits.`" + name + '`',
						(ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
							dependencies.addDependency(entry);
							return mode.makeInvoker(parser, ScriptedColumn.INFO.worldTraits(loadColumn), info.method.info, loadColumn);
						}
					)
				);
			}
		});
	}

	public void setupExternalEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		environment
		.addVariableConstant("world_traits", this.baseTraitsClass.info)
		.addFieldInvoke(ScriptedColumn.INFO.worldTraits);
		this.traitRegistry.streamEntries().forEach((RegistryEntry<? extends WorldTrait> entry) -> {
			String name = UnregisteredObjectException.getID(entry).toString();
			MethodInfo getter = this.infos.get(entry).method.info;
			boolean is3D = entry.value().schema().is_3d();
			environment.addMethod(
				TypeInfos.CLASS,
				name,
				new MethodHandler.Named(
					"world_traits.`" + name + '`' + params.getPossibleArguments(is3D),
					(ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
						if (receiver.getConstantValue().isConstant() && receiver.getConstantValue().asJavaObject().equals(this.baseTraitsClass.info)) {
							if (params.dependencies != null) params.dependencies.addDependency(entry);
							return params.resolveColumn(parser, name1, is3D, true, getter, arguments);
						}
						else {
							return null;
						}
					}
				)
			);
			if (params.requiresNoArguments(is3D)) {
				environment.addField(
					TypeInfos.CLASS,
					name,
					new FieldHandler.Named(
						"world_traits.`" + name + '`',
						(ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
							if (receiver.getConstantValue().isConstant() && receiver.getConstantValue().asJavaObject().equals(this.baseTraitsClass.info)) {
								if (params.dependencies != null) params.dependencies.addDependency(entry);
								return params.resolveColumn(parser, name1, is3D, true, getter).tree();
							}
							else {
								return null;
							}
						}
					)
				);
			}
		});
	}

	public static class TraitInfo implements SetBasedMutableDependencyView {

		public final MethodCompileContext method;
		public final Set<RegistryEntry<? extends DependencyView>> dependencies;

		public TraitInfo(MethodCompileContext method) {
			this.method = method;
			this.dependencies = new HashSet<>();
		}

		@Override
		public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}
}