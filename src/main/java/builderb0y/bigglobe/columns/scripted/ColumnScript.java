package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.ExternalData;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage.ColorScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnScript extends Script {

	public static class ImplParameters {

		public final ColumnEntryRegistry registry;
		public final Class<?> implClass;
		public final Method implMethod;
		public final TypeInfo returnType;
		public final int paramCount;
		public final LazyVarInfo[] bridgeParams, actualParams;
		public final LazyVarInfo bridgeColumn, actualColumn;
		public final @Nullable LazyVarInfo random, y;

		public ImplParameters(ColumnEntryRegistry registry, Class<?> implClass) {
			this.registry = registry;
			this.implClass = implClass;
			this.implMethod = ScriptParser.findImplementingMethod(implClass);
			this.returnType = type(this.implMethod.getReturnType());
			this.paramCount = this.implMethod.getParameterCount();
			this.bridgeParams = new LazyVarInfo[this.paramCount];
			this.actualParams = new LazyVarInfo[this.paramCount];
			Parameter[] parameters = this.implMethod.getParameters();
			LazyVarInfo bridgeColumn = null, actualColumn = null, random = null, y = null;
			for (int paramIndex = 0; paramIndex < this.paramCount; paramIndex++) {
				Parameter parameter = parameters[paramIndex];
				if (!parameter.isNamePresent()) throw new IllegalStateException(this.implMethod + " lacks parameter names!");
				Class<?> paramType = parameter.getType();
				if (paramType == ScriptedColumn.class) {
					if (bridgeColumn == null) {
						this.bridgeParams[paramIndex] = bridgeColumn = new LazyVarInfo(parameter.getName(), type(ScriptedColumn.class));
						this.actualParams[paramIndex] = actualColumn = new LazyVarInfo(parameter.getName(), registry.columnContext.columnType());
					}
					else {
						throw new IllegalStateException(this.implMethod + " takes more than one column!");
					}
				}
				else if (paramType == RandomGenerator.class) {
					if (random == null) {
						this.bridgeParams[paramIndex] = this.actualParams[paramIndex] = random = new LazyVarInfo(parameter.getName(), type(RandomGenerator.class));
					}
					else {
						throw new IllegalStateException(this.implMethod + " takes more than one RandomGenerator!");
					}
				}
				else if (paramType == int.class) {
					if (y == null) {
						this.bridgeParams[paramIndex] = this.actualParams[paramIndex] = y = new LazyVarInfo(parameter.getName(), TypeInfos.INT);
					}
					else {
						throw new IllegalStateException(this.implMethod + " takes more than one int (assuming Y level)!");
					}
				}
				else {
					this.bridgeParams[paramIndex] = this.actualParams[paramIndex] = new LazyVarInfo(parameter.getName(), type(paramType));
				}
			}
			if (bridgeColumn == null) {
				throw new IllegalStateException("Column script does not take a column parameter: " + this.implMethod);
			}
			this.bridgeColumn = bridgeColumn;
			this.actualColumn = actualColumn;
			this.random = random;
			this.y = y;
		}
	}

	public static abstract class BaseHolder<S extends ColumnScript> extends ScriptHolder<S> implements SetBasedMutableDependencyView {

		public final Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>(64);

		public BaseHolder(ScriptUsage usage) {
			super(usage);
			this.addAllDependencies(usage);
		}

		@Override
		public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = this.createScript(this.usage, registry);
		}

		public abstract Class<S> getScriptClass();

		public void addExtraFunctionsToEnvironment(ImplParameters parameters, MutableScriptEnvironment environment) {
			environment
			.addAll(MathScriptEnvironment.INSTANCE)
			.addAll(StatelessRandomScriptEnvironment.INSTANCE)
			.configure(GridScriptEnvironment.createWithSeed(ScriptedColumn.INFO.baseSeed(load(parameters.actualColumn))))
			.configure(
				parameters.random != null
				? MinecraftScriptEnvironment.createWithRandom(load(parameters.random))
				: MinecraftScriptEnvironment.create()
			)
			.configure(ScriptedColumn.baseEnvironment(load(parameters.actualColumn)))
			.addAll(ColorScriptEnvironment.ENVIRONMENT)
			.addAll(ExternalImage.ENVIRONMENT)
			.addAll(ExternalData.ENVIRONMENT);
			if (parameters.y != null) environment.addVariableLoad(parameters.y);
			if (parameters.random != null) environment.configure(RandomScriptEnvironment.create(load(parameters.random)));
		}

		public boolean isColumnMutable() {
			return false;
		}

		public S createScript(ScriptUsage usage, ColumnEntryRegistry registry) throws ScriptParsingException {
			Class<S> type = this.getScriptClass();
			ImplParameters parameters = new ImplParameters(registry, type);
			ClassCompileContext clazz = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(type) + '$' + (usage.debug_name != null ? usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				new TypeInfo[] { type(type) }
			);
			clazz.addNoArgConstructor(ACC_PUBLIC);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, parameters.implMethod.getName(), parameters.returnType, parameters.bridgeParams);
			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, parameters.implMethod.getName(), parameters.returnType, parameters.actualParams);

			return_(
				invokeInstance(
					load("this", clazz.info),
					actualMethod.info,
					IntStream
					.range(0, parameters.paramCount)
					.mapToObj((int index) -> {
						LazyVarInfo
							from = parameters.bridgeParams[index],
							to   = parameters.actualParams[index];
						InsnTree loader = load(from);
						if (!from.equals(to)) {
							loader = new DirectCastInsnTree(loader, to.type);
						}
						return loader;
					})
					.toArray(InsnTree.ARRAY_FACTORY)
				)
			)
			.emitBytecode(bridgeMethod);
			bridgeMethod.endCode();

			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(usage, clazz, actualMethod).configureEnvironment((MutableScriptEnvironment environment) -> {
				this.addExtraFunctionsToEnvironment(parameters, environment);
				registry.setupExternalEnvironment(
					environment,
					new ExternalEnvironmentParams()
					.withColumn(load(parameters.actualColumn))
					.withY(parameters.y != null ? load(parameters.y) : null)
					.mutable(this.isColumnMutable())
					.trackDependencies(this)
				);
			});
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(clazz.newConstant(usage.getSource(), TypeInfos.STRING))).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				return type.cast(new ScriptClassLoader(registry.loader).defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
			}
		}
	}

	public static interface ColumnToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToIntScript> implements ColumnToIntScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToIntScript> getScriptClass() {
				return ColumnToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToIntScript> implements ColumnYToIntScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToIntScript> getScriptClass() {
				return ColumnYToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToLongScript> implements ColumnToLongScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToLongScript> getScriptClass() {
				return ColumnToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToLongScript> implements ColumnYToLongScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToLongScript> getScriptClass() {
				return ColumnYToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToFloatScript> implements ColumnToFloatScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToFloatScript> getScriptClass() {
				return ColumnToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToFloatScript> implements ColumnYToFloatScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToFloatScript> getScriptClass() {
				return ColumnYToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToDoubleScript> implements ColumnToDoubleScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToDoubleScript> getScriptClass() {
				return ColumnToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToDoubleScript> getScriptClass() {
				return ColumnYToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToBooleanScript> implements ColumnToBooleanScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToBooleanScript> getScriptClass() {
				return ColumnToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToBooleanScript> implements ColumnYToBooleanScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBooleanScript> getScriptClass() {
				return ColumnYToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToIntScript> implements ColumnRandomToIntScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToIntScript> getScriptClass() {
				return ColumnRandomToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, RandomGenerator random) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomYToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToIntScript> implements ColumnRandomYToIntScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToIntScript> getScriptClass() {
				return ColumnRandomYToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, RandomGenerator random, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToLongScript> implements ColumnRandomToLongScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToLongScript> getScriptClass() {
				return ColumnRandomToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, RandomGenerator random) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomYToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToLongScript> implements ColumnRandomYToLongScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToLongScript> getScriptClass() {
				return ColumnRandomYToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, RandomGenerator random, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToFloatScript> implements ColumnRandomToFloatScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToFloatScript> getScriptClass() {
				return ColumnRandomToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, RandomGenerator random) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomYToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToFloatScript> implements ColumnRandomYToFloatScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToFloatScript> getScriptClass() {
				return ColumnRandomYToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, RandomGenerator random, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToDoubleScript> implements ColumnRandomToDoubleScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToDoubleScript> getScriptClass() {
				return ColumnRandomToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, RandomGenerator random) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomYToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToDoubleScript> implements ColumnRandomYToDoubleScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToDoubleScript> getScriptClass() {
				return ColumnRandomYToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, RandomGenerator random, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToBooleanScript> implements ColumnRandomToBooleanScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToBooleanScript> getScriptClass() {
				return ColumnRandomToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, RandomGenerator random) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnRandomYToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToBooleanScript> implements ColumnRandomYToBooleanScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToBooleanScript> getScriptClass() {
				return ColumnRandomYToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, RandomGenerator random, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnToBlockStateScript extends ColumnScript {

		public abstract BlockState get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToBlockStateScript> implements ColumnToBlockStateScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToBlockStateScript> getScriptClass() {
				return ColumnToBlockStateScript.class;
			}

			@Override
			public BlockState get(ScriptedColumn column) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return BlockStates.AIR;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToBlockStateScript extends ColumnScript {

		public abstract BlockState get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToBlockStateScript> implements ColumnYToBlockStateScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBlockStateScript> getScriptClass() {
				return ColumnYToBlockStateScript.class;
			}

			@Override
			public BlockState get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return BlockStates.AIR;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static interface ColumnYToBiomeScript extends ColumnScript {

		public abstract BiomeEntry get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToBiomeScript> implements ColumnYToBiomeScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBiomeScript> getScriptClass() {
				return ColumnYToBiomeScript.class;
			}

			@Override
			public BiomeEntry get(ScriptedColumn column, int y) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return BiomeEntry.of("minecraft:plains");
				}
				finally {
					manager.used = used;
				}
			}
		}
	}
}