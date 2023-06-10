package builderb0y.bigglobe.noise;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.util.ScopeLocal;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.PutFieldInsnTree;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@AddPseudoField(name = "script", getter = "getScriptSource")
public abstract class ScriptedGrid<G extends Grid> implements Grid {

	public static final TypeInfo DOUBLE_ARRAY = type(double[].class);
	public static final ScopeLocal<WorldColumn> SECRET_COLUMN = new ScopeLocal<>();
	public static final InsnTree GET_SECRET_COLUMN = invokeStatic(MethodInfo.getMethod(ScriptedGrid.class, "getSecretColumn"));
	public static final InsnTree GET_WORLD_SEED = invokeStatic(MethodInfo.getMethod(ScriptedGrid.class, "getWorldSeed"));

	public final Map<@UseVerifier(name = "verifyInputName", in = ScriptedGrid.class, usage = MemberUsage.METHOD_IS_HANDLER) String, G> inputs;
	public final double min;
	public final @VerifySorted(greaterThanOrEqual = "min") double max;

	public ScriptedGrid(Map<String, G> inputs, double min, double max) {
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

	public @MultiLine String getScriptSource() {
		return ((Script)(this.getDelegate())).getSource();
	}

	@Override
	public double minValue() {
		return this.min;
	}

	@Override
	public double maxValue() {
		return this.max;
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

	public static abstract class Parser extends ExpressionParser {

		public static final MethodInfo CHECK_NAN = method(
			ACC_PUBLIC | ACC_STATIC | ExtendedOpcodes.ACC_PURE,
			TypeInfo.of(Parser.class),
			"checkNaN",
			TypeInfos.DOUBLE,
			TypeInfos.DOUBLE
		);

		public LinkedHashMap<String, Input> inputs;
		public GridTypeInfo gridTypeInfo;

		public Parser(
			String input,
			LinkedHashMap<String, Input> inputs,
			GridTypeInfo gridTypeInfo,
			ClassCompileContext clazz
		) {
			super(
				input,
				clazz,
				clazz.newMethod(
					ACC_PUBLIC | ACC_STATIC,
					"evaluate",
					TypeInfos.DOUBLE,
					types(WorldColumn.class, 'I', gridTypeInfo.dimensions, 'D', inputs.size())
				)
			);
			this.inputs = inputs;
			this.gridTypeInfo = gridTypeInfo;
		}

		public Parser(String input, LinkedHashMap<String, Input> inputs, GridTypeInfo gridTypeInfo) {
			this(
				input,
				inputs,
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

		public Grid parse() throws ScriptParsingException {
			this.addConstructor();
			this.addGetValue();
			int dimensions = this.gridTypeInfo.dimensions;
			if (this.inputs.size() == 1) {
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

		public Grid instantiate() throws ScriptParsingException {
			try {
				Class<?> clazz = this.compile();
				Class<?>[] parameterTypes = new Class<?>[this.inputs.size()];
				Arrays.fill(parameterTypes, this.gridTypeInfo.gridClass);
				Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
				Object[] arguments = new Object[this.inputs.size()];
				for (Input input : this.inputs.values()) {
					arguments[input.index] = input.grid;
				}
				return (Grid)(constructor.newInstance(arguments));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(this.fatalError().toString(), throwable, null);
			}
		}

		public void addConstructor() {
			for (Input input : this.inputs.values()) {
				this.clazz.node.fields.add(input.fieldNode());
			}
			this
			.clazz
			.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				repeat(this.gridTypeInfo.type, this.inputs.size())
			)
			.scopes
			.withScope((MethodCompileContext constructor) -> {
				VarInfo thisVar = constructor.addThis();
				invokeSpecial(
					load(thisVar),
					constructor(ACC_PUBLIC, Object.class)
				)
				.emitBytecode(constructor);
				constructor.node.visitLabel(label());
				for (Input input : this.inputs.values()) {
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
				for (Input input : this.inputs.values()) {
					invokeInterface(
						getField(
							load(thisVar),
							input.fieldInfo(getValue)
						),
						method(
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
				getValue.node.visitMethodInsn(INVOKESTATIC, getValue.clazz.info.getInternalName(), "evaluate", '(' + type(WorldColumn.class).getDescriptor() + "I".repeat(dimensions) + "D".repeat(this.inputs.size()) + ")D", false);
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
			for (Input input : this.inputs.values()) {
				input.newDoubleParameter(this.method);
			}
			this.parseEntireInput().emitBytecode(this.method);
			this.method.scopes.popScope();
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
		public Stream<String> listCandidates(String name) {
			return Stream.of(
				Stream.of("worldSeed"),
				IntStream.range('x', 'x' + this.gridTypeInfo.dimensions).mapToObj((int c) -> String.valueOf((char)(c))),
				Stream.ofNullable(this.inputs.get(name)).map(input -> "Input " + input.name + " @ " + input.index)
			)
			.flatMap(Function.identity());
		}
	}
}