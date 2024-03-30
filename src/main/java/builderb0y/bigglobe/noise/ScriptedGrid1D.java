package builderb0y.bigglobe.noise;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedGrid1D extends ScriptedGrid<Grid1D> implements Grid1D {

	public static final GridTypeInfo GRID_1D_TYPE_INFO = new GridTypeInfo(Grid1D.class, Parser.class, 1);

	public final transient Grid1D delegate;

	public ScriptedGrid1D(@MultiLine String script, Map<String, Grid1D> inputs, double min, double max) throws ScriptParsingException {
		super(script, inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_1D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_1D_TYPE_INFO))
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE);
		this.delegate = parser.parse();
	}

	@Override
	public Grid getDelegate() {
		return this.delegate;
	}

	@Override
	public double getValue(long seed, int x) {
		return this.delegate.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkX(seed, startX, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	public static class Parser extends ScriptedGrid.Parser<Grid1D> {

		@SuppressWarnings("MultipleVariablesInDeclaration")
		public static final MethodInfo
			GET_BULK_X = MethodInfo.getMethod(Grid1D.class, "getBulkX"),
			GET_BULK[] = { GET_BULK_X };

		public Parser(@MultiLine String script, LinkedHashMap<String, Input> inputs) {
			super(script, inputs, GRID_1D_TYPE_INFO);
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			LazyVarInfo self, seed, x, samples, sampleCount;
			MethodCompileContext getBulk = this.clazz.newMethod(
				ACC_PUBLIC,
				methodInfo.name,
				TypeInfos.VOID,
				seed = new LazyVarInfo("seed", TypeInfos.LONG),
				x = new LazyVarInfo("x", TypeInfos.INT),
				samples = new LazyVarInfo("samples", NUMBER_ARRAY_TYPE)
			);
			self = new LazyVarInfo("this", getBulk.clazz.info);
			sampleCount = new LazyVarInfo("sampleCount", TypeInfos.INT);
			Input input = this.gridInputs.values().iterator().next();

			//sampleCount = samples.length();
			store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
			//if (sampleCount <= 0) return;
			ifThen(
				le(
					this,
					load(sampleCount),
					ldc(0)
				),
				return_(noop)
			)
			.emitBytecode(getBulk);
			//fill samples with input.
				invokeInstance(
				getField(
					load(self),
					input.fieldInfo(getBulk)
				),
				methodInfo,
				load(seed),
				load(x),
				load(samples)
			)
			.emitBytecode(getBulk);
			getBulk.node.visitLabel(label());
			//replace samples with evaluation results.
			getBulk.scopes.pushScope();
			LazyVarInfo index = getBulk.scopes.addVariable("index", TypeInfos.INT);
			for_(
				new LoopName(null),
				store(index, ldc(0)),
				lt(this, load(index), load(sampleCount)),
				inc(index, 1),
				numberArrayStore(
					load(samples),
					load(index),
					invokeStatic(
						new MethodInfo(
							ACC_PUBLIC | INVOKESTATIC,
							getBulk.clazz.info,
							"evaluate",
							TypeInfos.DOUBLE,
							types("ID")
						),
						add(this, load(x), load(index)),
						numberArrayLoad(load(samples), load(index))
					)
				)
			)
			.emitBytecode(getBulk);
			getBulk.scopes.popScope();
			//return.
			return_(noop).emitBytecode(getBulk);
			getBulk.endCode();
		}

		@Override
		public void addGetBulkMany(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			LazyVarInfo self, seed, x, samples, sampleCount;
			MethodCompileContext getBulk = this.clazz.newMethod(
				ACC_PUBLIC,
				methodInfo.name,
				TypeInfos.VOID,
				seed = new LazyVarInfo("seed", TypeInfos.LONG),
				x = new LazyVarInfo("x", TypeInfos.INT),
				samples = new LazyVarInfo("samples", NUMBER_ARRAY_TYPE)
			);
			self = new LazyVarInfo("this", getBulk.clazz.info);
			sampleCount = getBulk.scopes.addVariable("sampleCount", TypeInfos.INT);

			//declare scratch arrays.
			LazyVarInfo[] scratches = new LazyVarInfo[this.gridInputs.size()];
			for (Input input : this.gridInputs.values()) {
				scratches[input.index] = getBulk.scopes.addVariable(input.name, NUMBER_ARRAY_TYPE);
			}
			//sampleCount = samples.length();
			store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
			//if (sampleCount <= 0) return;
			ifThen(
				le(
					this,
					load(sampleCount),
					ldc(0)
				),
				return_(noop)
			)
			.emitBytecode(getBulk);
			//allocate scratch arrays.
			for (Input input : this.gridInputs.values()) {
				store(
					scratches[input.index],
					newNumberArray(load(sampleCount))
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
			}
			//fill scratch arrays.
			for (Input input : this.gridInputs.values()) {
				invokeInstance(
					getField(load(self), input.fieldInfo(getBulk)),
					methodInfo,
					load(seed),
					load(x),
					load(scratches[input.index])
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
			}
			//fill samples.
			getBulk.scopes.pushScope();
			LazyVarInfo index = getBulk.scopes.addVariable("index", TypeInfos.INT);
			for_(
				new LoopName(null),
				store(index, ldc(0)),
				lt(this, load(index), load(sampleCount)),
				inc(index, 1),
				numberArrayStore(
					load(samples),
					load(index),
					invokeStatic(
						new MethodInfo(
							ACC_PUBLIC | ACC_STATIC,
							getBulk.clazz.info,
							"evaluate",
							TypeInfos.DOUBLE,
							types('I', 'D', this.gridInputs.size())
						),
						Stream.concat(
							Stream.of(
								add(this, load(x), load(index))
							),
							this.gridInputs.values().stream().map((Input input) -> (
								numberArrayLoad(load(scratches[input.index]), load(index))
							))
						)
						.toArray(InsnTree.ARRAY_FACTORY)
					)
				)
			)
			.emitBytecode(getBulk);
			getBulk.scopes.popScope();
			//return.
			return_(noop).emitBytecode(getBulk);
			getBulk.endCode();
		}
	}
}