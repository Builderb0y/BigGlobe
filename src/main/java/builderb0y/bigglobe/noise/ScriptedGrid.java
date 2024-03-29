package builderb0y.bigglobe.noise;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.autocodec.logging.TaskLogger;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.ScriptedGridTemplate.ScriptedGridTemplateUsage;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.ScopeLocal;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.PutFieldInsnTree;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.ScriptTemplate.RequiredInput;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class ScriptedGrid<G extends Grid> implements Grid {

	public static final TypeInfo NUMBER_ARRAY = type(NumberArray.class);
	public static final ScopeLocal<WorldColumn> SECRET_COLUMN = new ScopeLocal<>();
	public static final InsnTree GET_SECRET_COLUMN = invokeStatic(MethodInfo.getMethod(ScriptedGrid.class, "getSecretColumn"));
	public static final InsnTree GET_WORLD_SEED = invokeStatic(MethodInfo.getMethod(ScriptedGrid.class, "getWorldSeed"));

	public final ScriptUsage<ScriptedGridTemplateUsage<G>> script;
	public final @DefaultEmpty Map<@UseVerifier(name = "verifyInputName", in = ScriptedGrid.class, usage = MemberUsage.METHOD_IS_HANDLER) String, G> inputs;
	public final double min;
	public final @VerifySorted(greaterThanOrEqual = "min") double max;
	public transient long nextWarning = Long.MIN_VALUE;

	public ScriptedGrid(ScriptUsage<ScriptedGridTemplateUsage<G>> script, Map<String, G> inputs, double min, double max) {
		this.script = script;
		this.inputs = inputs;
		this.min = min;
		this.max = max;
	}

	public static WorldColumn getSecretColumn() {
		return SECRET_COLUMN.getCurrent();
	}

	public static long getWorldSeed() {
		WorldColumn column = SECRET_COLUMN.getCurrent();
		return column != null ? column.seed : 0L;
	}

	public abstract Grid getDelegate();

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
	}

	public void onError(Throwable throwable) {
		long time = System.currentTimeMillis();
		if (time >= this.nextWarning) {
			this.nextWarning = time + 5000L;
			StringBuilder mainMessage = new StringBuilder().append("Caught exception from ").append(this.getClass().getName());
			if (this.script.debug_name != null) mainMessage.append(" (").append(this.script.debug_name).append(')');
			mainMessage.append(": ").append(throwable).append("; Check your logs for more info.");
			BuiltinScriptEnvironment.PRINTER.println(mainMessage.toString());
			ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.script.findSource()));
			ScriptLogger.LOGGER.error("Exception was: ", throwable);
		}
	}

	public static <T_Encoded> void verifyInputName(VerifyContext<T_Encoded, String> context) throws VerifyException {
		String inputName = context.object;
		if (inputName != null) {
			if (inputName.isEmpty()) throw new VerifyException(() -> context.pathToStringBuilder().append(" cannot be an empty string.").toString());
			if (inputName.equals("_")) throw new VerifyException(() -> context.pathToStringBuilder().append(" cannot be _ as it is a reserved name.").toString());
			char c = inputName.charAt(0);
			if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_')) {
				throw new VerifyException(() -> context.pathToStringBuilder().append(" must start with an alphabetic character or an underscore.").toString());
			}
			for (int index = 1, length = inputName.length(); index < length; index++) {
				c = inputName.charAt(index);
				if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) {
					throw new VerifyException(() -> context.pathToStringBuilder().append(" must contain only alphabetic characters, numeric characters, and underscores.").toString());
				}
			}
		}
	}

	public static LinkedHashMap<String, Input> processInputs(Map<String, ? extends Grid> inputs, GridTypeInfo info) {
		LinkedHashMap<String, Input> processedInputs = new LinkedHashMap<>(inputs.size());
		int index = 0;
		for (Map.Entry<String, ? extends Grid> entry : inputs.entrySet()) {
			processedInputs.put(entry.getKey(), new Input(entry.getKey(), index++, entry.getValue(), info));
		}
		return processedInputs;
	}

	public static class GridTypeInfo {

		public final Class<? extends Grid> gridClass;
		public final Class<? extends ScriptedGrid.Parser> parserClass;
		public final String name;
		public final String desc;
		public final TypeInfo type;
		public final int dimensions;

		public GridTypeInfo(
			Class<? extends Grid> gridClass,
			Class<? extends ScriptedGrid.Parser> parserClass,
			int dimensions
		) {
			this.gridClass   = gridClass;
			this.parserClass = parserClass;
			this.name        = Type.getInternalName(gridClass);
			this.desc        = Type.getDescriptor(gridClass);
			this.type        = TypeInfo.of(gridClass);
			this.dimensions  = dimensions;
		}
	}

	public static class Input {

		public final String name;
		public final int index;
		public final Grid grid;
		public final GridTypeInfo info;

		public Input(String name, int index, Grid grid, GridTypeInfo info) {
			this.name = name;
			this.index = index;
			this.grid = grid;
			this.info = info;
		}

		@SuppressWarnings("unchecked")
		public <G extends Grid> G grid() {
			return (G)(this.grid);
		}

		public FieldInfo fieldInfo(MethodCompileContext method) {
			return new FieldInfo(ACC_PUBLIC | ACC_FINAL, method.clazz.info, this.name, this.info.type);
		}

		public FieldNode fieldNode() {
			return new FieldNode(ACC_PUBLIC | ACC_FINAL, this.name, this.info.desc, null, null);
		}

		public VarInfo newGridParameter(MethodCompileContext method) {
			return method.newParameter(this.name, this.info.type);
		}

		public VarInfo newDoubleParameter(MethodCompileContext method) {
			return method.newParameter(this.name, TypeInfos.DOUBLE);
		}
	}

	public static abstract class Parser<G extends Grid> extends ExpressionParser {

		public static final MethodInfo
			CHECK_NAN = MethodInfo.getMethod(Parser.class, "checkNaN"),
			OBJECT_CONSTRUCTOR = MethodInfo.getConstructor(Object.class),

			GETD = MethodInfo.getMethod(NumberArray.class, "getD"),
			SETD = MethodInfo.getMethod(NumberArray.class, "setD"),
			ALLOCATED = MethodInfo.getMethod(NumberArray.class, "allocateDoublesDirect"),
			LENGTH = MethodInfo.getMethod(NumberArray.class, "length");

		public ScriptUsage<ScriptedGridTemplateUsage<G>> usage;
		public LinkedHashMap<String, Input> gridInputs;
		public GridTypeInfo gridTypeInfo;

		public Parser(
			ScriptUsage<ScriptedGridTemplateUsage<G>> usage,
			LinkedHashMap<String, Input> gridInputs,
			GridTypeInfo gridTypeInfo,
			ClassCompileContext clazz
		) {
			super(
				usage.findSource(),
				clazz,
				clazz.newMethod(
					ACC_PUBLIC | ACC_STATIC,
					"evaluate",
					TypeInfos.DOUBLE,
					types(WorldColumn.class, 'I', gridTypeInfo.dimensions, 'D', gridInputs.size())
				)
			);
			this.usage = usage;
			this.gridInputs = gridInputs;
			this.gridTypeInfo = gridTypeInfo;
		}

		public Parser(ScriptUsage<ScriptedGridTemplateUsage<G>> usage, LinkedHashMap<String, Input> gridInputs, GridTypeInfo gridTypeInfo) {
			this(
				usage,
				gridInputs,
				gridTypeInfo,
				new ClassCompileContext(
					ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
					ClassType.CLASS,
					Type.getInternalName(gridTypeInfo.parserClass) + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
					TypeInfos.OBJECT,
					new TypeInfo[] { TypeInfo.of(gridTypeInfo.gridClass), TypeInfo.of(Script.class) }
				)
			);
		}

		public static InsnTree newNumberArray(InsnTree length) {
			return invokeStatic(ALLOCATED, length);
		}

		public static InsnTree numberArrayLoad(InsnTree array, InsnTree index) {
			return invokeInstance(array, GETD, index);
		}

		public static InsnTree numberArrayStore(InsnTree array, InsnTree index, InsnTree value) {
			return invokeInstance(array, SETD, index, value);
		}

		public static InsnTree numberArrayLength(InsnTree array) {
			return invokeInstance(array, LENGTH);
		}

		public G parse() throws ScriptParsingException {
			this.addConstructor();
			this.addGetValue();
			int dimensions = this.gridTypeInfo.dimensions;
			if (this.gridInputs.size() == 1) {
				for (int dimension = 0; dimension < dimensions; dimension++) {
					this.addGetBulkOne(dimension);
				}
			}
			else {
				for (int dimension = 0; dimension < dimensions; dimension++) {
					this.addGetBulkMany(dimension);
				}
			}
			this.addEvaluate();
			this.addSource();
			this.addToString();
			return this.instantiate();
		}

		public abstract void addGetBulkOne(int methodDimension);

		public abstract void addGetBulkMany(int methodDimension);

		@SuppressWarnings("unchecked")
		public G instantiate() throws ScriptParsingException {
			try {
				Class<?> clazz = this.compile();
				Class<?>[] parameterTypes = new Class<?>[this.gridInputs.size()];
				Arrays.fill(parameterTypes, this.gridTypeInfo.gridClass);
				Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
				Object[] arguments = new Object[this.gridInputs.size()];
				for (Input input : this.gridInputs.values()) {
					arguments[input.index] = input.grid;
				}
				return (G)(constructor.newInstance(arguments));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(this.fatalError().toString(), throwable, null);
			}
		}

		public void addConstructor() {
			for (Input input : this.gridInputs.values()) {
				this.clazz.node.fields.add(input.fieldNode());
			}
			this
			.clazz
			.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				repeat(this.gridTypeInfo.type, this.gridInputs.size())
			)
			.scopes
			.withScope((MethodCompileContext constructor) -> {
				VarInfo thisVar = constructor.addThis();
				invokeInstance(load(thisVar), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				constructor.node.visitLabel(label());
				for (Input input : this.gridInputs.values()) {
					VarInfo parameter = input.newGridParameter(constructor);
					assert parameter.index == input.index + 1 : "Parameters out of order!";
					new PutFieldInsnTree(
						new LoadInsnTree(thisVar),
						input.fieldInfo(constructor), new LoadInsnTree(parameter)
					)
					.emitBytecode(constructor);
					constructor.node.visitLabel(label());
				}
				constructor.node.visitInsn(RETURN);
			});
		}

		public void addGetValue() {
			int dimensions = this.gridTypeInfo.dimensions;
			this.clazz.newMethod(
				ACC_PUBLIC,
				"getValue",
				TypeInfos.DOUBLE,
				types('J', 'I', dimensions)
			)
			.scopes.withScope((MethodCompileContext getValue) -> {
				VarInfo thisVar = getValue.addThis();
				VarInfo seed = getValue.newParameter("seed", TypeInfos.LONG);
				VarInfo[] coordinates = new VarInfo[dimensions];
				for (int dimension = 0; dimension < dimensions; dimension++) {
					coordinates[dimension] = getValue.newParameter(coordName(dimension), TypeInfos.INT);
				}
				GET_SECRET_COLUMN.emitBytecode(getValue);
				for (int dimension = 0; dimension < dimensions; dimension++) {
					load(coordinates[dimension]).emitBytecode(getValue);
				}
				for (Input input : this.gridInputs.values()) {
					invokeInstance(
						getField(
							load(thisVar),
							input.fieldInfo(getValue)
						),
						new MethodInfo(
							ACC_PUBLIC | ACC_INTERFACE,
							this.gridTypeInfo.type,
							"getValue",
							TypeInfos.DOUBLE,
							types('J', 'I', dimensions)
						),
						Stream.concat(
							Stream.of(load(seed)),
							Arrays.stream(coordinates).map(InsnTrees::load)
						)
						.toArray(InsnTree[]::new)
					)
					.emitBytecode(getValue);
				}
				getValue.node.visitMethodInsn(INVOKESTATIC, getValue.clazz.info.getInternalName(), "evaluate", '(' + type(WorldColumn.class).getDescriptor() + "I".repeat(dimensions) + "D".repeat(this.gridInputs.size()) + ")D", false);
				getValue.node.visitInsn(DRETURN);
			});
		}

		public void addEvaluate() throws ScriptParsingException {
			int dimensions = this.gridTypeInfo.dimensions;
			this.method.scopes.pushScope();
			this.method.newParameter("column", type(WorldColumn.class));
			for (int dimension = 0; dimension < dimensions; dimension++) {
				this.method.newParameter(coordName(dimension), TypeInfos.INT);
			}
			for (Input input : this.gridInputs.values()) {
				input.newDoubleParameter(this.method);
			}
			this.parseEntireInput().emitBytecode(this.method);
			this.method.scopes.popScope();
		}

		@Override
		public InsnTree parseEntireInput() throws ScriptParsingException {
			for (Input input : this.gridInputs.values()) {
				this.environment.mutable().addVariableConstant(input.name + "Min", input.grid.minValue());
				this.environment.mutable().addVariableConstant(input.name + "Max", input.grid.maxValue());
			}
			if (this.usage.isTemplate()) {
				ScriptedGridTemplateUsage<G> gridUsage = this.usage.getTemplate();
				gridUsage.validateInputs(this.gridInputs.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue().<G>grid())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), message -> new ScriptParsingException(message.get(), null));
				ArrayBuilder<InsnTree> initializers = new ArrayBuilder<>();
				for (RequiredInput input : gridUsage.actualTemplate.getRequiredInputs()) {
					String inputSource = gridUsage.getProvidedInputs().get(input.name());
					assert inputSource != null;
					ClassCompileContext classCopy = new ClassCompileContext(this.clazz.node.access, this.clazz.info);
					MethodCompileContext methodCopy = new MethodCompileContext(classCopy, new MethodNode(), this.method.info);
					ExpressionParser parserCopy = new ExpressionParser(inputSource, classCopy, methodCopy);
					parserCopy.environment.mutable(new MutableScriptEnvironment().addAll(this.environment.mutable()));
					FunctionHandler handler = new FunctionHandler.Named("invalid", (parser, name, arguments) -> {
						throw new ScriptParsingException(name + " is not allowed in script inputs", parser.input);
					});
					parserCopy.environment.mutable().functions.put("return", Collections.singletonList(handler));
					TypeInfo type = parserCopy.environment.getType(this, input.type());
					if (type == null) {
						throw new ScriptParsingException("Unknown type: " + input.type(), null);
					}
					InsnTree inputTree = parserCopy.nextScript().cast(parserCopy, type, CastMode.IMPLICIT_THROW);
					VarInfo declaration = this.environment.user().newVariable(input.name(), type);
					InsnTree initializer = new VariableDeclareAssignInsnTree(declaration, inputTree);
					this.environment.mutable()
						.addVariable(input.name(), load(declaration))
						.addVariable('$' + input.name(), inputTree);
					initializers.add(initializer);
				}
				initializers.add(super.parseEntireInput());
				return seq(initializers.toArray(InsnTree.ARRAY_FACTORY));
			}
			else {
				return super.parseEntireInput();
			}
		}

		public void addSource() {
			this.clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING).scopes.withScope((MethodCompileContext getSource) -> {
				getSource.addThis();
				return_(ldc(this.input.getSource())).emitBytecode(getSource);
			});
		}

		public void addToString() {
			this.clazz.addToString(this.getClass().getEnclosingClass().getSimpleName() + ".evaluate(): " + this.input.getSource());
		}

		@Override
		public InsnTree createReturn(InsnTree value) {
			return return_(invokeStatic(CHECK_NAN, value.cast(this, TypeInfos.DOUBLE, CastMode.IMPLICIT_THROW)));
		}

		public static String coordName(int dimension) {
			return String.valueOf((char)('x' + dimension));
		}

		public static InsnTree maybeAdd(ExpressionParser parser, VarInfo variable, VarInfo index, int variableDimension, int methodDimension) {
			return variableDimension == methodDimension ? add(parser, load(variable), load(index)) : load(variable);
		}

		public static double checkNaN(double result) {
			return result == result ? result : 0.0D;
		}
	}

	public static class Environment implements ScriptEnvironment {

		public final LinkedHashMap<String, Input> inputs;
		public final GridTypeInfo gridTypeInfo;

		public Environment(LinkedHashMap<String, Input> inputs, GridTypeInfo gridTypeInfo) {
			this.inputs = inputs;
			this.gridTypeInfo = gridTypeInfo;
		}

		@Override
		public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
			if (name.equals("worldSeed")) {
				return GET_WORLD_SEED;
			}
			if (name.length() == 1) {
				char c = name.charAt(0);
				if (c >= 'x' && c < 'x' + this.gridTypeInfo.dimensions) {
					return load(name, c - 'x' + 1, TypeInfos.INT);
				}
			}
			Input input = this.inputs.get(name);
			return input == null ? null : load(name, (input.index << 1) + this.gridTypeInfo.dimensions + 1, TypeInfos.DOUBLE);
		}

		@Override
		public Stream<IdentifierDescriptor> listIdentifiers() {
			return Stream.of(
				Stream.of(new IdentifierDescriptor("worldSeed", "long worldSeed: seed of the world")),

				IntStream.range('x', 'x' + this.gridTypeInfo.dimensions).mapToObj((int c) -> {
					return new IdentifierDescriptor(String.valueOf((char)(c)), TaskLogger.lazyMessage(() -> {
						return "int " + (char)(c) + ": the " + (char)(c) + " coordinate of the current position";
					}));
				}),

				this.inputs.values().stream().map((Input input) -> {
					return new IdentifierDescriptor(input.name, TaskLogger.lazyMessage(() -> {
						return "double " + input.name + ": user-defined input @ " + input.index;
					}));
				})
			)
			.flatMap(Function.identity());
		}
	}
}