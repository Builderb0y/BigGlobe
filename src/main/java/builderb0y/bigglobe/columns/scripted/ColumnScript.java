package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.objectweb.asm.Type;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.BiomeTagKey;
import builderb0y.bigglobe.scripting.wrappers.EntryWrapper;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnScript extends Script {

	public static abstract class BaseHolder<S extends ColumnScript> extends ScriptHolder<S> {

		public BaseHolder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = this.createScript(this.usage, registry);
		}

		public abstract Class<S> getScriptClass();

		public void addExtraFunctionsToEnvironment(MutableScriptEnvironment environment) {}

		public S createScript(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
			Class<S> type = this.getScriptClass();
			ClassCompileContext clazz = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(type) + '$' + (usage.debug_name != null ? usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				new TypeInfo[] { type(type) }
			);
			clazz.addNoArgConstructor(ACC_PUBLIC);
			Method implementingMethod = ScriptParser.findImplementingMethod(type);
			TypeInfo returnType = type(implementingMethod.getReturnType());
			int paramCount = implementingMethod.getParameterCount();
			LazyVarInfo[]
				bridgeParams = new LazyVarInfo[paramCount],
				actualParams = new LazyVarInfo[paramCount];
			LazyVarInfo
				bridgeColumn = new LazyVarInfo("column", type(ScriptedColumn.class)),
				actualColumn = new LazyVarInfo("column", registry.columnContext.columnType()),
				random       = new LazyVarInfo("random", type(RandomGenerator.class)),
				y            = new LazyVarInfo("y",      TypeInfos.INT);
			Class<?>[] paramClasses = implementingMethod.getParameterTypes();
			boolean haveRandom = false, haveY = false;
			for (int index = 0; index < paramCount; index++) {
				Class<?> paramType = paramClasses[index];
				if (paramType == ScriptedColumn.class) {
					bridgeParams[index] = bridgeColumn;
					actualParams[index] = actualColumn;
				}
				else if (paramType == RandomGenerator.class) {
					haveRandom = true;
					bridgeParams[index] = actualParams[index] = random;
				}
				else if (paramType == int.class) {
					haveY = true;
					bridgeParams[index] = actualParams[index] = y;
				}
				else {
					throw new RuntimeException("Unrecognized argument type: " + paramType + " on " + type);
				}
			}

			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, implementingMethod.getName(), returnType, actualParams);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, implementingMethod.getName(), returnType, bridgeParams);

			return_(
				invokeInstance(
					load("this", clazz.info),
					actualMethod.info,
					IntStream
					.range(0, paramCount)
					.mapToObj((int index) -> {
						LazyVarInfo
							from = bridgeParams[index],
							to   = actualParams[index];
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

			LoadInsnTree loadMainColumn = load("column", registry.columnContext.columnType());
			MutableScriptEnvironment environment = (
				new MutableScriptEnvironment()
				.addAll(MathScriptEnvironment.INSTANCE)
				.addAll(StatelessRandomScriptEnvironment.INSTANCE)
				.addVariableGetFields(loadMainColumn, ScriptedColumn.class, "x", "z", "distantHorizons")
				.addVariableRenamedGetField(loadMainColumn, "worldSeed", ScriptedColumn.INFO.seed)
				.addVariableRenamedInvoke(loadMainColumn, "columnSeed", ScriptedColumn.INFO.unsaltedSeed)
				.addFunctionInvoke("columnSeed", loadMainColumn, ScriptedColumn.INFO.saltedSeed)
			);
			if (haveY) environment.addVariableLoad(y);
			if (haveRandom) environment.addAll(RandomScriptEnvironment.create(load(random)));
			this.addExtraFunctionsToEnvironment(environment);
			registry.setupExternalEnvironment(environment, new ExternalEnvironmentParams().withColumn(loadMainColumn).withY(haveY ? load(y) : null));

			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(usage, clazz, actualMethod).addEnvironment(environment);
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(usage.findSource())).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				return type.cast(registry.loader.defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
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

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToIntScript> getScriptClass() {
				return ColumnToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnYToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToIntScript> implements ColumnYToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToIntScript> getScriptClass() {
				return ColumnYToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToLongScript> implements ColumnToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToLongScript> getScriptClass() {
				return ColumnToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnYToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToLongScript> implements ColumnYToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToLongScript> getScriptClass() {
				return ColumnYToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToFloatScript> implements ColumnToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToFloatScript> getScriptClass() {
				return ColumnToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnYToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToFloatScript> implements ColumnYToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToFloatScript> getScriptClass() {
				return ColumnYToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToDoubleScript> implements ColumnToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToDoubleScript> getScriptClass() {
				return ColumnToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnYToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToDoubleScript> getScriptClass() {
				return ColumnYToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnToBooleanScript> implements ColumnToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToBooleanScript> getScriptClass() {
				return ColumnToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnYToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToBooleanScript> implements ColumnYToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBooleanScript> getScriptClass() {
				return ColumnYToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnRandomToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToIntScript> implements ColumnRandomToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToIntScript> getScriptClass() {
				return ColumnRandomToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, RandomGenerator random) {
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnRandomYToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToIntScript> implements ColumnRandomYToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToIntScript> getScriptClass() {
				return ColumnRandomYToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, RandomGenerator random, int y) {
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnRandomToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToLongScript> implements ColumnRandomToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToLongScript> getScriptClass() {
				return ColumnRandomToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, RandomGenerator random) {
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnRandomYToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToLongScript> implements ColumnRandomYToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToLongScript> getScriptClass() {
				return ColumnRandomYToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, RandomGenerator random, int y) {
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnRandomToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToFloatScript> implements ColumnRandomToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToFloatScript> getScriptClass() {
				return ColumnRandomToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, RandomGenerator random) {
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnRandomYToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToFloatScript> implements ColumnRandomYToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToFloatScript> getScriptClass() {
				return ColumnRandomYToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, RandomGenerator random, int y) {
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnRandomToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToDoubleScript> implements ColumnRandomToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToDoubleScript> getScriptClass() {
				return ColumnRandomToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, RandomGenerator random) {
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnRandomYToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToDoubleScript> implements ColumnRandomYToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToDoubleScript> getScriptClass() {
				return ColumnRandomYToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, RandomGenerator random, int y) {
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnRandomToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, RandomGenerator random);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomToBooleanScript> implements ColumnRandomToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomToBooleanScript> getScriptClass() {
				return ColumnRandomToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, RandomGenerator random) {
				try {
					return this.script.get(column, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnRandomYToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, RandomGenerator random, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnRandomYToBooleanScript> implements ColumnRandomYToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnRandomYToBooleanScript> getScriptClass() {
				return ColumnRandomYToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, RandomGenerator random, int y) {
				try {
					return this.script.get(column, random, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnYToBiomeScript extends ColumnScript {

		public abstract BiomeEntry get(ScriptedColumn column, int y);

		@Wrapper
		public static class Holder extends BaseHolder<ColumnYToBiomeScript> implements ColumnYToBiomeScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBiomeScript> getScriptClass() {
				return ColumnYToBiomeScript.class;
			}

			@Override
			public BiomeEntry get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return BiomeEntry.of("minecraft:plains");
				}
			}

			@Override
			public void addExtraFunctionsToEnvironment(MutableScriptEnvironment environment) {
				super.addExtraFunctionsToEnvironment(environment);
				environment
				.addType("Biome", BiomeEntry.TYPE)
				.addType("BiomeTag", BiomeTagKey.TYPE)
				.addFieldInvoke(EntryWrapper.class, "id")
				.addFieldInvokes(BiomeEntry.class, "temperature", "downfall")
				.addMethodInvokeSpecific(BiomeEntry.class, "isIn", boolean.class, BiomeTagKey.class)
				.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class)
				.addMethodInvokeSpecific(BiomeTagKey.class, "random", BiomeEntry.class, long.class)
				.addCastConstant(BiomeEntry.CONSTANT_FACTORY, true)
				;
			}
		}
	}
}