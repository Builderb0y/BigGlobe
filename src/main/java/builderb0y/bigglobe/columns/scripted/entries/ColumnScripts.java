package builderb0y.bigglobe.columns.scripted.entries;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnScripts extends Script {

	public static class BaseHolder<S extends ColumnScripts> extends ScriptHolder<S> {

		public BaseHolder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry, Class<S> type) throws ScriptParsingException {
			super(usage, createScript(usage, registry, type));
		}

		public static <S extends ColumnScripts> S createScript(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry, Class<S> type) throws ScriptParsingException {
			ClassCompileContext clazz = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(type) + "$Generated$" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				new TypeInfo[] { type(type) }
			);
			clazz.addNoArgConstructor(ACC_PUBLIC);
			Method implementingMethod = ScriptParser.findImplementingMethod(type);
			TypeInfo returnType = type(implementingMethod.getReturnType());
			TypeInfo[] bridgeTypes = types(implementingMethod.getParameterTypes());
			TypeInfo[] actualTypes = bridgeTypes.clone();
			actualTypes[0] = registry.columnContext.mainClass.info;
			boolean requiresY = bridgeTypes.length == 2;
			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, implementingMethod.getName(), returnType, actualTypes);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC | ACC_BRIDGE, implementingMethod.getName(), returnType, bridgeTypes);
			bridgeMethod.scopes.withScope((MethodCompileContext bridge) -> {
				VarInfo self = bridge.addThis();
				VarInfo column = bridge.newParameter("column", type(ScriptedColumn.class));
				if (requiresY) {
					VarInfo y = bridge.newParameter("y", TypeInfos.INT);
					return_(invokeInstance(load(self), actualMethod.info, new DirectCastInsnTree(load(column), actualTypes[0]), load(y))).emitBytecode(bridge);
				}
				else {
					return_(invokeInstance(load(self), actualMethod.info, new DirectCastInsnTree(load(column), actualTypes[0]))).emitBytecode(bridgeMethod);
				}
			});
			if (requiresY) actualMethod.prepareParameters("column", "y");
			else actualMethod.prepareParameters("column");
			new ScriptColumnEntryParser(usage, clazz, actualMethod)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(registry.columnContext.environment)
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				if (requiresY) environment.addVariableLoad(actualMethod.getParameter("y"));
			})
			.parseEntireInput()
			.emitBytecode(actualMethod);
			actualMethod.endCode();
			try {
				return type.cast(registry.loader.defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Exception exception) {
				throw new ScriptParsingException("An exception occurred while trying to define the script class", exception, null);
			}
		}
	}

	public static interface ColumnToIntScript extends ColumnScripts {

		public abstract int get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToIntScript> implements ColumnToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnToIntScript.class);
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

	public static interface ColumnYToIntScript extends ColumnScripts {

		public abstract int get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToIntScript> implements ColumnYToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnYToIntScript.class);
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

	public static interface ColumnToLongScript extends ColumnScripts {

		public abstract long get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToLongScript> implements ColumnToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnToLongScript.class);
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

	public static interface ColumnYToLongScript extends ColumnScripts {

		public abstract long get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToLongScript> implements ColumnYToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnYToLongScript.class);
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

	public static interface ColumnToFloatScript extends ColumnScripts {

		public abstract float get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToFloatScript> implements ColumnToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnToFloatScript.class);
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

	public static interface ColumnYToFloatScript extends ColumnScripts {

		public abstract float get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToFloatScript> implements ColumnYToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnYToFloatScript.class);
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

	public static interface ColumnToDoubleScript extends ColumnScripts {

		public abstract double get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToDoubleScript> implements ColumnToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnToDoubleScript.class);
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

	public static interface ColumnYToDoubleScript extends ColumnScripts {

		public abstract double get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnYToDoubleScript.class);
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

	public static interface ColumnToBooleanScript extends ColumnScripts {

		public abstract boolean get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToBooleanScript> implements ColumnToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnToBooleanScript.class);
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

	public static interface ColumnYToBooleanScript extends ColumnScripts {

		public abstract boolean get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToBooleanScript> implements ColumnYToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
				super(usage, registry, ColumnYToBooleanScript.class);
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
}