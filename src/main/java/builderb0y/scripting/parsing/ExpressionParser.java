package builderb0y.scripting.parsing;

import java.io.IOException;
import java.lang.invoke.StringConcatFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.file.PathUtils;
import org.objectweb.asm.util.CheckClassAdapter;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.*;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.InsnTree.UpdateOp;
import builderb0y.scripting.bytecode.tree.InsnTree.UpdateOrder;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.LineNumberInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.RootScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.CommonMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.environments.UserScriptEnvironment;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.CommaSeparatedExpressions;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.ParenthesizedScript;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList.UserParameter;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/**
higher-level script-parsing logic than {@link ExpressionReader}.
handles expressions, user-defined variables/methods/classes,
order of operations, etc... and building an abstract syntax tree out of them.
this abstract syntax tree is represented with {@link InsnTree}.
*/
@SuppressWarnings("ErrorNotRethrown")
public class ExpressionParser {

	public static final Path CLASS_DUMP_DIRECTORY;
	static {
		Path classDumpDirectory;
		if (Boolean.getBoolean("builderb0y.bytecode.dumpGeneratedClasses")) {
			classDumpDirectory = FabricLoader.getInstance().getGameDir().resolve("builderb0y_bytecode_class_dump");
			if (Files.isDirectory(classDumpDirectory)) try {
				PathUtils.cleanDirectory(classDumpDirectory);
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error(
					"""
					An error occurred while trying to clean the previous session's script dump output.
					Dumping of generated classes has been disabled to prevent ambiguity over which file is from which session.
					Please empty the class dump directory manually when you get a chance.
					""",
					exception
				);
				classDumpDirectory = null;
			}
			else try {
				Files.createDirectory(classDumpDirectory);
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error(
					"""
					An error occurred while trying to create the script dump directory.
					Dumping of generated classes has been disabled as there is nowhere to put them.
					""",
					exception
				);
				classDumpDirectory = null;
			}
		}
		else {
			classDumpDirectory = null;
		}
		CLASS_DUMP_DIRECTORY = classDumpDirectory;
		ScriptLogger.LOGGER.info("Class dumping is " + (classDumpDirectory != null ? "enabled" : "disabled") + '.');
	}

	public static final MethodInfo
		OBJECT_CONSTRUCTOR = MethodInfo.getConstructor(Object.class),
		MAKE_CONCAT_WITH_CONSTANTS = MethodInfo.getMethod(StringConcatFactory.class, "makeConcatWithConstants"),
		HASH_MIX = MethodInfo.findMethod(HashCommon.class, "mix", int.class, int.class).pure();

	public static void clinit() {}

	public final ExpressionReader input;
	public int currentLine;
	public final ClassCompileContext clazz;
	public final MethodCompileContext method;
	public final RootScriptEnvironment environment;
	public int functionUniquifier;

	public ExpressionParser(String input, ClassCompileContext clazz, MethodCompileContext method) {
		this.input = new ExpressionReader(input);
		this.clazz = clazz;
		this.method = method;
		this.environment = new RootScriptEnvironment();
		this.environment.user().parser = this;
	}

	/**
	this constructor is intended for user-defined functions only.
	see {@link #nextUserDefinedFunction(TypeInfo, String)}.
	*/
	public ExpressionParser(ExpressionParser from, MethodCompileContext method) {
		this.input = from.input;
		this.clazz = from.clazz;
		this.method = method;
		this.currentLine = from.currentLine;
		this.environment = new RootScriptEnvironment(from.environment);
		this.environment.user().parser = this;
	}

	public ExpressionParser addEnvironment(ScriptEnvironment environment) {
		if (environment instanceof MutableScriptEnvironment mutable) {
			this.environment.mutable().addAll(mutable);
		}
		else {
			this.environment.environments.add(environment);
		}
		return this;
	}

	public ExpressionParser configureEnvironment(Consumer<MutableScriptEnvironment> configurator) {
		this.environment.mutable().configure(configurator);
		return this;
	}

	public void checkVariable(String name) throws ScriptParsingException {
		if (this.environment.getVariable(this, name) != null) {
			throw new ScriptParsingException("Variable '" + name + "' is already defined in this scope", this.input);
		}
	}

	public void checkType(String name) throws ScriptParsingException {
		if (this.environment.getType(this, name) != null) {
			throw new ScriptParsingException("Type '" + name + "' is already defined in this scope", this.input);
		}
	}

	public static void dump(ClassCompileContext context) throws IOException {
		String baseName = context.info.getSimpleName();
		Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), context.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), context.toByteArray(), StandardOpenOption.CREATE_NEW);
	}

	public Class<?> compile() throws Throwable {
		if (CLASS_DUMP_DIRECTORY != null) try {
			String baseName = this.clazz.info.getSimpleName();
			Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-src.txt"), this.input.input, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			dump(this.clazz);
		}
		catch (IOException exception) {
			ScriptLogger.LOGGER.error("", exception);
		}
		return new ScriptClassLoader(this.clazz).defineMainClass();
	}

	public StringBuilder fatalError() {
		return (
			new StringBuilder(
				"""
				Congrats! You found a bug in my script parser!
				Normally this error would've been caught earlier,
				and you would've received a more helpful error message
				telling you where the problem in your script was.
				But in this case the script parser
				doesn't know what went wrong or where.
				Go ask Builderb0y for help instead,
				and tell him the following details:
				"""
			)
			.append('\n')
			.append("Script source:\n").append(ScriptLogger.addLineNumbers(this.input.getSource())).append('\n')
			.append("Compiled bytecode:\n").append(this.clazz.dump()).append('\n')
			.append("ASM errors: ").append(this.testForASMErrors()).append('\n')
			.append("Parser class: ").append(this.getClass().getName()).append('\n')
			.append("Environment: ").append(this.environment).append('\n')
		);
	}

	public String testForASMErrors() {
		try {
			this.clazz.node.accept(new CheckClassAdapter(null));
			return "No errors.";
		}
		catch (Throwable throwable) {
			return throwable.toString();
		}
	}

	public InsnTree parseEntireInput() throws ScriptParsingException {
		return this.parseRemainingInput(false);
	}

	public InsnTree parseRemainingInput(boolean expectClose) throws ScriptParsingException {
		try {
			int expectedUserStackSize = this.environment.user().getStackSize();
			this.environment.user().push();
			InsnTree tree = this.nextScript();
			this.input.skipWhitespace();
			if (expectClose ? !this.input.has(')') : this.input.skip()) {
				throw new ScriptParsingException("Unexpected trailing character: " + this.input.getChar(this.input.cursor - 1), this.input);
			}
			this.environment.user().pop();
			if (this.environment.user().getStackSize() != expectedUserStackSize) {
				throw new IllegalStateException("User defined variable scope out of sync!");
			}
			if (!tree.jumpsUnconditionally()) {
				tree = this.createReturn(tree);
			}
			return tree;
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextScript() throws ScriptParsingException {
		return this.nextStatementList();
	}

	public InsnTree nextStatementList() throws ScriptParsingException {
		try {
			InsnTree left = this.nextCompoundExpression();
			while (true) {
				//end of input, ')', ',', and ':' mark the end of the script.
				if (!this.input.canReadAfterWhitespace()) {
					return left;
				}
				switch (this.input.peek()) {
					case ')', ']', '}' -> { return left; }
					default -> {}
				}
				//another operator (except ++ and --) indicates that said
				//operator didn't get processed sooner when it should have.
				String operator = this.input.peekOperator();
				switch (operator) {
					case ",", ":" -> { //indicates the end of this statement list.
						return left;
					}
					case "", "++", "++:", ":++", "--", "--:", ":--", "!" -> {} //indicates that there's another statement to read.
					default -> { //indicates that there's an operator which didn't get consumed properly.
						this.input.onCharsRead(operator);
						throw new ScriptParsingException("Unknown or unexpected operator: " + operator, this.input);
					}
				}
				//if we get to this point, we are expecting another statement in this script.
				if (!left.canBeStatement()) {
					throw new ScriptParsingException("Not a statement", this.input);
				}
				InsnTree next = this.nextCompoundExpression();
				if (left.jumpsUnconditionally()) {
					throw new ScriptParsingException("Unreachable statement", this.input);
				}
				left = seq(left, next);
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextCompoundExpression() throws ScriptParsingException {
		try {
			InsnTree left = this.nextSingleExpression();
			while (this.input.hasOperatorAfterWhitespace(",,")) {
				if (!left.canBeStatement()) {
					throw new ScriptParsingException("Not a statement", this.input);
				}
				InsnTree next = this.nextSingleExpression();
				if (left.jumpsUnconditionally()) {
					throw new ScriptParsingException("Unreachable statement", this.input);
				}
				left = seq(left, next);
			}
			return left;
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextSingleExpression() throws ScriptParsingException {
		return this.nextAssignment();
	}

	public InsnTree nextAssignment() throws ScriptParsingException {
		try {
			InsnTree left = this.nextTernary();
			String operator = this.input.peekOperatorAfterWhitespace();
			UpdateOp op;
			UpdateOrder order;
			switch (operator) {
				case "="    -> { op = UpdateOp.ASSIGN;               order = UpdateOrder.VOID; }
				case "+="   -> { op = UpdateOp.ADD;                  order = UpdateOrder.VOID; }
				case "-="   -> { op = UpdateOp.SUBTRACT;             order = UpdateOrder.VOID; }
				case "*="   -> { op = UpdateOp.MULTIPLY;             order = UpdateOrder.VOID; }
				case "/="   -> { op = UpdateOp.DIVIDE;               order = UpdateOrder.VOID; }
				case "%="   -> { op = UpdateOp.MODULO;               order = UpdateOrder.VOID; }
				case "^="   -> { op = UpdateOp.POWER;                order = UpdateOrder.VOID; }
				case "&="   -> { op = UpdateOp.BITWISE_AND;          order = UpdateOrder.VOID; }
				case "|="   -> { op = UpdateOp.BITWISE_OR;           order = UpdateOrder.VOID; }
				case "#="   -> { op = UpdateOp.BITWISE_XOR;          order = UpdateOrder.VOID; }
				case "&&="  -> { op = UpdateOp.AND;                  order = UpdateOrder.VOID; }
				case "||="  -> { op = UpdateOp.OR;                   order = UpdateOrder.VOID; }
				case "##="  -> { op = UpdateOp.XOR;                  order = UpdateOrder.VOID; }
				case "<<="  -> { op = UpdateOp.SIGNED_LEFT_SHIFT;    order = UpdateOrder.VOID; }
				case ">>="  -> { op = UpdateOp.SIGNED_RIGHT_SHIFT;   order = UpdateOrder.VOID; }
				case "<<<=" -> { op = UpdateOp.UNSIGNED_LEFT_SHIFT;  order = UpdateOrder.VOID; }
				case ">>>=" -> { op = UpdateOp.UNSIGNED_RIGHT_SHIFT; order = UpdateOrder.VOID; }

				case ":="   -> { op = UpdateOp.ASSIGN;               order = UpdateOrder.POST; }
				case ":+"   -> { op = UpdateOp.ADD;                  order = UpdateOrder.POST; }
				case ":-"   -> { op = UpdateOp.SUBTRACT;             order = UpdateOrder.POST; }
				case ":*"   -> { op = UpdateOp.MULTIPLY;             order = UpdateOrder.POST; }
				case ":/"   -> { op = UpdateOp.DIVIDE;               order = UpdateOrder.POST; }
				case ":%"   -> { op = UpdateOp.MODULO;               order = UpdateOrder.POST; }
				case ":^"   -> { op = UpdateOp.POWER;                order = UpdateOrder.POST; }
				case ":&"   -> { op = UpdateOp.BITWISE_AND;          order = UpdateOrder.POST; }
				case ":|"   -> { op = UpdateOp.BITWISE_OR;           order = UpdateOrder.POST; }
				case ":#"   -> { op = UpdateOp.BITWISE_XOR;          order = UpdateOrder.POST; }
				case ":&&"  -> { op = UpdateOp.AND;                  order = UpdateOrder.POST; }
				case ":||"  -> { op = UpdateOp.OR;                   order = UpdateOrder.POST; }
				case ":##"  -> { op = UpdateOp.XOR;                  order = UpdateOrder.POST; }
				case ":<<"  -> { op = UpdateOp.SIGNED_LEFT_SHIFT;    order = UpdateOrder.POST; }
				case ":>>"  -> { op = UpdateOp.SIGNED_RIGHT_SHIFT;   order = UpdateOrder.POST; }
				case ":<<<" -> { op = UpdateOp.UNSIGNED_LEFT_SHIFT;  order = UpdateOrder.POST; }
				case ":>>>" -> { op = UpdateOp.UNSIGNED_RIGHT_SHIFT; order = UpdateOrder.POST; }

				case "=:"   -> { op = UpdateOp.ASSIGN;               order = UpdateOrder.PRE; }
				case "+:"   -> { op = UpdateOp.ADD;                  order = UpdateOrder.PRE; }
				case "-:"   -> { op = UpdateOp.SUBTRACT;             order = UpdateOrder.PRE; }
				case "*:"   -> { op = UpdateOp.MULTIPLY;             order = UpdateOrder.PRE; }
				case "/:"   -> { op = UpdateOp.DIVIDE;               order = UpdateOrder.PRE; }
				case "%:"   -> { op = UpdateOp.MODULO;               order = UpdateOrder.PRE; }
				case "^:"   -> { op = UpdateOp.POWER;                order = UpdateOrder.PRE; }
				case "&:"   -> { op = UpdateOp.BITWISE_AND;          order = UpdateOrder.PRE; }
				case "|:"   -> { op = UpdateOp.BITWISE_OR;           order = UpdateOrder.PRE; }
				case "#:"   -> { op = UpdateOp.BITWISE_XOR;          order = UpdateOrder.PRE; }
				case "&&:"  -> { op = UpdateOp.AND;                  order = UpdateOrder.PRE; }
				case "||:"  -> { op = UpdateOp.OR;                   order = UpdateOrder.PRE; }
				case "##:"  -> { op = UpdateOp.XOR;                  order = UpdateOrder.PRE; }
				case "<<:"  -> { op = UpdateOp.SIGNED_LEFT_SHIFT;    order = UpdateOrder.PRE; }
				case ">>:"  -> { op = UpdateOp.SIGNED_RIGHT_SHIFT;   order = UpdateOrder.PRE; }
				case "<<<:" -> { op = UpdateOp.UNSIGNED_LEFT_SHIFT;  order = UpdateOrder.PRE; }
				case ">>>:" -> { op = UpdateOp.UNSIGNED_RIGHT_SHIFT; order = UpdateOrder.PRE; }
				default     -> { op = null; order = null; }
			}
			if (op != null) {
				this.input.onCharsRead(operator);
				left = left.update(this, op, order, this.nextVariableInitializer(left.getTypeInfo(), false));
			}
			return left;
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextTernary() throws ScriptParsingException {
		try {
			InsnTree left = this.nextBoolean();
			if (this.input.hasOperatorAfterWhitespace("?")) {
				ConditionTree condition = condition(this, left);
				InsnTree trueBody = this.nextSingleExpression();
				this.input.expectOperatorAfterWhitespace(":");
				InsnTree falseBody = this.nextSingleExpression();
				return ifElse(this, condition, trueBody, falseBody);
			}
			else {
				return left;
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public void checkBoolean(InsnTree left, String operator) throws ScriptParsingException {
		if (left.getTypeInfo().getSort() != Sort.BOOLEAN) {
			throw new ScriptParsingException("Expected boolean before " + operator, this.input);
		}
	}

	public InsnTree nextBoolean() throws ScriptParsingException {
		try {
			InsnTree left = this.nextCompare();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case "&&" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = and(this, left, this.nextCompare());
					}
					case "||" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = or(this, left, this.nextCompare());
					}
					case "##" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = xor(this, left, this.nextCompare());
					}
					case "!&&" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = not(this, and(this, left, this.nextCompare()));
					}
					case "!||" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = not(this, or(this, left, this.nextCompare()));
					}
					case "!##" -> {
						this.input.onCharsRead(operator);
						this.checkBoolean(left, operator);
						left = not(this, xor(this, left, this.nextCompare()));
					}
					default -> {
						return left;
					}
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextCompare() throws ScriptParsingException {
		try {
			InsnTree left = this.nextSum();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case "<"   -> { this.input.onCharsRead(operator); left = bool(        lt(this, left, this.nextSum()) ); }
					case "<="  -> { this.input.onCharsRead(operator); left = bool(        le(this, left, this.nextSum()) ); }
					case ">"   -> { this.input.onCharsRead(operator); left = bool(        gt(this, left, this.nextSum()) ); }
					case ">="  -> { this.input.onCharsRead(operator); left = bool(        ge(this, left, this.nextSum()) ); }
					case "=="  -> { this.input.onCharsRead(operator); left = bool(        eq(this, left, this.nextSum()) ); }
					case "!="  -> { this.input.onCharsRead(operator); left = bool(        ne(this, left, this.nextSum()) ); }
					case "===" -> { this.input.onCharsRead(operator); left = bool(identityEq(this, left, this.nextSum()) ); }
					case "!==" -> { this.input.onCharsRead(operator); left = bool(identityNe(this, left, this.nextSum()) ); }
					case "!>"  -> { this.input.onCharsRead(operator); left = bool(    not(gt(this, left, this.nextSum()))); }
					case "!<"  -> { this.input.onCharsRead(operator); left = bool(    not(lt(this, left, this.nextSum()))); }
					case "!>=" -> { this.input.onCharsRead(operator); left = bool(    not(ge(this, left, this.nextSum()))); }
					case "!<=" -> { this.input.onCharsRead(operator); left = bool(    not(le(this, left, this.nextSum()))); }
					default    -> { return left; }
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextSum() throws ScriptParsingException {
		try {
			InsnTree left = this.nextProduct();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case "+" -> { this.input.onCharsRead(operator); left =  add(this, left, this.nextProduct()); }
					case "-" -> { this.input.onCharsRead(operator); left =  sub(this, left, this.nextProduct()); }
					case "&" -> { this.input.onCharsRead(operator); left = band(this, left, this.nextProduct()); }
					case "|" -> { this.input.onCharsRead(operator); left =  bor(this, left, this.nextProduct()); }
					case "#" -> { this.input.onCharsRead(operator); left = bxor(this, left, this.nextProduct()); }
					default  -> { return left; }
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextProduct() throws ScriptParsingException {
		try {
			InsnTree left = this.nextExponent();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case "*"   -> { this.input.onCharsRead(operator); left =  mul(this, left, this.nextExponent()); }
					case "<<"  -> { this.input.onCharsRead(operator); left =  shl(this, left, this.nextExponent()); }
					case "<<<" -> { this.input.onCharsRead(operator); left = ushl(this, left, this.nextExponent()); }
					case "/"   -> { this.input.onCharsRead(operator); left =  div(this, left, this.nextExponent()); }
					case ">>"  -> { this.input.onCharsRead(operator); left =  shr(this, left, this.nextExponent()); }
					case ">>>" -> { this.input.onCharsRead(operator); left = ushr(this, left, this.nextExponent()); }
					case "%"   -> { this.input.onCharsRead(operator); left =  mod(this, left, this.nextExponent()); }
					default    -> { return left; }
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextExponent() throws ScriptParsingException {
		try {
			InsnTree left = this.nextElvis();
			if (this.input.hasOperatorAfterWhitespace("^")) {
				left = pow(this, left, this.nextExponent());
			}
			return left;
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextElvis() throws ScriptParsingException {
		try {
			InsnTree left = this.nextMember();
			while (true) {
				if (this.input.hasOperatorAfterWhitespace("?:")) {
					InsnTree right = this.nextMember();
					left = left.elvis(this, right);
				}
				else {
					return left;
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextMember() throws ScriptParsingException {
		try {
			InsnTree left = this.nextPrefixOperator();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				CommonMode mode = switch (operator) {
					case "."  -> { this.input.onCharsRead(operator); yield CommonMode.NORMAL; }
					case ".?" -> { this.input.onCharsRead(operator); yield CommonMode.NULLABLE; }
					case ".$" -> { this.input.onCharsRead(operator); yield CommonMode.RECEIVER; }
					case ".$?", ".?$" -> { this.input.onCharsRead(operator); yield CommonMode.NULLABLE_RECEIVER; }
					default -> null;
				};
				if (mode == null) return left;

				//note: memberName can be the empty String, "".
				//this is intentional to support array/list-lookup syntax:
				//array.(index)
				String memberName = this.input.readIdentifierAfterWhitespace();
				InsnTree result = this.environment.parseMemberKeyword(this, left, memberName, MemberKeywordMode.from(mode));
				if (result == null) {
					if (this.input.peekAfterWhitespace() == '(') {
						CommaSeparatedExpressions arguments = CommaSeparatedExpressions.parse(this);
						result = this.environment.getMethod(this, left, memberName, GetMethodMode.from(mode), arguments.arguments());
						if (result == null) {
							throw new ScriptParsingException(this.listCandidates(memberName, "Unknown method or incorrect arguments: " + memberName, Arrays.stream(arguments.arguments()).map(InsnTree::describe).collect(Collectors.joining(", ", "Actual form: " + left.describe() + '.' + memberName + "(", ")"))), this.input);
						}
						result = arguments.maybeWrap(result);
					}
					else {
						result = this.environment.getField(this, left, memberName, GetFieldMode.from(mode));
						if (result == null) {
							throw new ScriptParsingException(this.listCandidates(memberName, "Unknown field: " + memberName, "Actual form: " + left.describe() + '.' + memberName), this.input);
						}
					}
				}
				left = result;
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextPrefixOperator() throws ScriptParsingException {
		try {
			String prefixOperator = this.input.peekOperatorAfterWhitespace();
			return switch (prefixOperator) {
				case "+" -> {
					this.input.onCharsRead(prefixOperator);
					InsnTree tree = this.nextProduct();
					if (!tree.getTypeInfo().isNumber()) {
						throw new ScriptParsingException("Non-numeric term for unary '+': " + tree.getTypeInfo(), this.input);
					}
					yield tree;
				}
				case "-" -> {
					this.input.onCharsRead(prefixOperator);
					//must special handle Integer.MIN_VALUE and Long.MIN_VALUE,
					//because otherwise it would try to parse them as positive numbers,
					//and then negate them, but the positive form is not representable
					//in the same precision as the negative form.
					if (isNumber(this.input.peekAfterWhitespace())) {
						yield this.nextNumber(true);
					}
					yield neg(this.nextProduct());
				}
				case "~" -> {
					this.input.onCharsRead(prefixOperator);
					InsnTree term = this.nextMember();
					//it is safe to use term.getTypeInfo() directly
					//without sanity checking that it is numeric here,
					//because bxor() will check that immediately afterwards.
					yield bxor(this, term, ldc(-1, term.getTypeInfo()));
				}
				case "!" -> {
					this.input.onCharsRead(prefixOperator);
					yield not(this, this.nextMember());
				}
				case "++" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.ADD, UpdateOrder.VOID, ldc(1));
				}
				case "--" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.SUBTRACT, UpdateOrder.VOID, ldc(1));
				}
				case ":++" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.ADD, UpdateOrder.POST, ldc(1));
				}
				case ":--" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.SUBTRACT, UpdateOrder.POST, ldc(1));
				}
				case "++:" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.ADD, UpdateOrder.PRE, ldc(1));
				}
				case "--:" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextMember().update(this, UpdateOp.SUBTRACT, UpdateOrder.PRE, ldc(1));
				}
				case "" -> {
					yield this.nextTerm();
				}
				default -> {
					this.input.onCharsRead(prefixOperator);
					throw new ScriptParsingException("Unknown prefix operator: " + prefixOperator, this.input);
				}
			};
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextTerm() throws ScriptParsingException {
		try {
			char first = this.input.peekAfterWhitespace();
			int line = this.input.line;
			InsnTree resultTree = switch (first) {
				case 0 -> {
					throw new ScriptParsingException("Unexpected end of input", this.input);
				}
				case '(' -> {
					yield ParenthesizedScript.parse(this).maybeWrapContents();
				}
				case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
					yield this.nextNumber(false);
				}
				case
					'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
					'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
					'_', '`'
				-> {
					yield this.nextIdentifier(this.input.readIdentifier());
				}
				case '\'', '"' -> {
					this.input.onCharRead(first);
					yield this.nextString(first);
				}
				default -> {
					this.input.onCharRead(first);
					throw new ScriptParsingException("Unexpected character: " + first, this.input);
				}
			};
			if (this.currentLine != line) {
				this.currentLine = line;
				resultTree = new LineNumberInsnTree(resultTree, line);
			}
			return resultTree;
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}


	public InsnTree nextString(char quote) throws ScriptParsingException {
		StringBuilder string = new StringBuilder();
		ArrayBuilder<InsnTree> arguments = new ArrayBuilder<>();
		while (true) {
			char c = this.input.read();
			//todo: handle when c == 1 or c == 2.
			if (c == 0) {
				throw new ScriptParsingException("Un-terminated string", this.input);
			}
			else if (c == quote) {
				if (arguments.isEmpty()) {
					return ldc(string.toString());
				}
				else {
					return invokeDynamic(
						MAKE_CONCAT_WITH_CONSTANTS,
						new MethodInfo(
							ACC_PUBLIC | ACC_STATIC,
							TypeInfos.OBJECT, //ignored
							"concat",
							TypeInfos.STRING,
							arguments
							.stream()
							.map(InsnTree::getTypeInfo)
							.toArray(TypeInfo.ARRAY_FACTORY)
						),
						new ConstantValue[] {
							constant(string.toString())
						},
						arguments.toArray(InsnTree.ARRAY_FACTORY)
					);
				}
			}
			else if (c == '$') {
				if (this.input.has('$')) {
					string.append('$');
				}
				else if (this.input.has(':')) {
					boolean member = this.input.has('.');
					this.input.skipWhitespace();
					int start = this.input.cursor;
					arguments.add(member ? this.nextMember() : this.nextTerm());
					int end = this.input.cursor;
					while (Character.isWhitespace(this.input.input.charAt(end - 1))) end--;
					string.append(this.input.input, start, end).append(": ").append((char)(1));
					this.addSkippedWhitespace(string);
				}
				else {
					boolean member = this.input.has('.');
					string.append((char)(1));
					arguments.add(member ? this.nextMember() : this.nextTerm());
					this.addSkippedWhitespace(string);
				}
			}
			else {
				string.append(c);
			}
		}
	}

	public void addSkippedWhitespace(StringBuilder builder) {
		//in some cases, input.skipWhitespace() may
		//be called after the next term has ended.
		//this is problematic because if the input
		//is, for example, "String a = 'a',, '$a b'",
		//then the output would be "ab", without
		//a space between. so, here we add any
		//whitespace which got skipped over.
		int skippedWhitespace = this.input.cursor - 1;
		while (Character.isWhitespace(this.input.getChar(skippedWhitespace))) {
			skippedWhitespace--;
		}
		builder.append(this.input.input, skippedWhitespace + 1, this.input.cursor);
	}

	public InsnTree nextNumber(boolean negated) throws ScriptParsingException {
		try {
			BigDecimal number = NumberParser.parse(this.input);
			if (negated) number = number.negate();
			char suffix = this.input.peek();
			boolean unsigned = false;
			if (suffix == 'u' || suffix == 'U') {
				unsigned = true;
				this.input.onCharRead(suffix);
				suffix = this.input.peek();
			}
			return switch (suffix) {
				case 'f', 'F', 'd', 'D' -> {
					this.input.onCharRead(suffix);
					throw new ScriptParsingException("This isn't a C-family language. Doubles are suffixed by 'L', and floats are not suffixed by anything.", this.input);
				}
				case 'l', 'L' -> {
					this.input.onCharRead(suffix);
					if (number.scale() > 0) {
						if (unsigned) throw new ScriptParsingException("Unsigned double literals not supported", this.input);
						double value = number.doubleValue();
						if (negated && value == 0.0D) value = -0.0D;
						yield ldc(value);
					}
					else {
						if (unsigned) {
							BigInteger integer = number.toBigIntegerExact();
							if (integer.signum() >= 0 && integer.bitLength() <= 64) {
								yield ldc(integer.longValue());
							}
							else {
								throw new ScriptParsingException("Overflow", this.input);
							}
						}
						else {
							yield ldc(number.longValueExact());
						}
					}
				}
				case 'i', 'I' -> {
					this.input.onCharRead(suffix);
					if (number.scale() > 0) {
						if (unsigned) throw new ScriptParsingException("Unsigned float literals not supported", this.input);
						float value = number.floatValue();
						if (negated && value == 0.0F) value = -0.0F;
						yield ldc(value);
					}
					else {
						if (unsigned) {
							yield ldc(BigGlobeMath.toUnsignedIntExact(number.longValueExact()));
						}
						else {
							yield ldc(number.intValueExact());
						}
					}
				}
				case 's', 'S' -> {
					this.input.onCharRead(suffix);
					if (unsigned) throw new ScriptParsingException("Unsigned shorts not supported", this.input);
					if (number.scale() > 0) {
						throw new ScriptParsingException("Half-precision floats not supported", this.input);
					}
					yield ldc(number.shortValueExact());
				}
				default -> {
					if (number.scale() > 0) {
						if (unsigned) throw new ScriptParsingException("Unsigned floating point literals not supported", this.input);
						double doubleValue = number.doubleValue();
						if (negated && doubleValue == 0.0D) doubleValue = -0.0D;
						float floatValue = (float)(doubleValue);
						if (doubleValue == floatValue) {
							yield ldc(floatValue);
						}
						yield ldc(doubleValue);
					}
					else {
						long longValue;
						if (unsigned) {
							BigInteger integer = number.toBigIntegerExact();
							if (integer.signum() >= 0 && integer.bitLength() <= 64) {
								longValue = integer.longValue();
							}
							else {
								throw new ScriptParsingException("Overflow", this.input);
							}
						}
						else {
							longValue = number.longValueExact();
						}
						int intValue = (int)(longValue);
						if (intValue == longValue) {
							if (intValue == (short)(intValue)) {
								if (intValue == (byte)(intValue)) {
									yield ldc((byte)(intValue));
								}
								yield ldc((short)(intValue));
							}
							yield ldc(intValue);
						}
						yield ldc(longValue);
					}
				}
			};
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextIdentifier(String name) throws ScriptParsingException {
		try {
			if (name.equals("var")) {
				String varName = this.verifyName(this.input.expectIdentifierAfterWhitespace(), "variable");
				boolean reuse;
				if (this.input.hasOperatorAfterWhitespace("=")) reuse = false;
				else if (this.input.hasOperatorAfterWhitespace(":=")) reuse = true;
				else throw new ScriptParsingException("Expected '=' or ':='", this.input);
				InsnTree initializer = this.nextSingleExpression();
				VarInfo variable = this.environment.user().newVariable(varName, initializer.getTypeInfo());
				return (
					reuse
					? new VariableDeclarePostAssignInsnTree(variable, initializer)
					: new VariableDeclareAssignInsnTree(variable, initializer)
				);
			}
			else if (name.equals("class")) {
				String className = this.verifyName(this.input.expectIdentifierAfterWhitespace(), "class");
				return this.nextUserDefinedClass(className);
			}
			else { //not var or class.
				InsnTree result = this.environment.parseKeyword(this, name);
				if (result != null) return result;
				//not keyword.
				TypeInfo type = this.environment.getType(this, name);
				if (type != null) {
					if (this.input.peekAfterWhitespace() == '(') { //casting.
						return ParenthesizedScript.parse(this).maybeWrapContents().cast(this, type, CastMode.EXPLICIT_THROW);
					}
					else { //not casting. (variable or method declaration or ldc class)
						String varName = this.input.readIdentifierAfterWhitespace();
						if (!varName.isEmpty()) { //variable or method declaration.
							if (this.input.hasOperatorAfterWhitespace("=")) { //variable declaration.
								this.verifyName(varName, "variable");
								InsnTree initializer = this.nextVariableInitializer(type, true);
								VarInfo variable = this.environment.user().newVariable(varName, type);
								return new VariableDeclareAssignInsnTree(variable, initializer);
							}
							else if (this.input.hasOperatorAfterWhitespace(":=")) {
								this.verifyName(varName, "variable");
								InsnTree initializer = this.nextVariableInitializer(type, true);
								VarInfo variable = this.environment.user().newVariable(varName, type);
								return new VariableDeclarePostAssignInsnTree(variable, initializer);
							}
							else if (this.input.hasAfterWhitespace('(')) { //method declaration.
								this.verifyName(varName, "method");
								return this.nextUserDefinedFunction(type, varName);
							}
							else {
								throw new ScriptParsingException("Expected '=' or '('", this.input);
							}
						}
						else { //ldc class.
							return ldc(type);
						}
					}
				}
				else { //not a type.
					if (this.input.peekAfterWhitespace() == '(') { //function call.
						CommaSeparatedExpressions arguments = CommaSeparatedExpressions.parse(this);
						result = this.environment.getFunction(this, name, arguments.arguments());
						if (result != null) return arguments.maybeWrap(result);
						throw new ScriptParsingException(this.listCandidates(name, "Unknown function or incorrect arguments: " + name, Arrays.stream(arguments.arguments()).map(InsnTree::describe).collect(Collectors.joining(", ", "Actual form: " + name + '(', ")"))), this.input);
					}
					else { //variable.
						InsnTree variable = this.environment.getVariable(this, name);
						if (variable != null) return variable;
						throw new ScriptParsingException(this.listCandidates(name, "Unknown variable: " + name, "Actual form: " + name), this.input);
					}
				}
			}
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree nextVariableInitializer(TypeInfo variableType, boolean cast) throws ScriptParsingException {
		if (this.input.hasIdentifierAfterWhitespace("new")) {
			CommaSeparatedExpressions arguments = CommaSeparatedExpressions.parse(this);
			InsnTree expression = this.environment.getMethod(this, ldc(variableType), "new", GetMethodMode.NORMAL, arguments.arguments());
			if (expression == null) {
				throw new ScriptParsingException(this.listCandidates("new", "Incorrect arguments for new()", Arrays.stream(arguments.arguments()).map(InsnTree::describe).collect(Collectors.joining(", ", "Actual form: " + ldc(variableType).describe() + ".new(", ")"))), this.input);
			}
			return arguments.maybeWrap(expression);
		}
		else {
			InsnTree tree = this.nextSingleExpression();
			if (cast) {
				tree = tree.cast(this, variableType, CastMode.IMPLICIT_THROW);
			}
			return tree;
		}
	}

	public InsnTree nextUserDefinedFunction(TypeInfo returnType, String methodName) throws ScriptParsingException {
		UserParameterList userParameters = UserParameterList.parse(this);
		List<VarInfo> newParameters = new ArrayList<>(this.method.parameters.size() + userParameters.parameters().length);
		int currentOffset = this.method.info.isStatic() ? 0 : 1;
		for (VarInfo builtin : this.method.parameters.values()) {
			if (builtin.index != currentOffset) {
				throw new IllegalStateException("Builtin parameter has incorrect offset: " + builtin + " should be at index " + currentOffset);
			}
			newParameters.add(builtin);
			currentOffset += builtin.type.getSize();
		}
		//System.out.println(methodName + " builtin: " + newParameters);
		MutableScriptEnvironment userParametersEnvironment = new MutableScriptEnvironment().addAll(this.environment.mutable());
		UserScriptEnvironment userVariablesEnvironment = new UserScriptEnvironment(this.environment.user());
		userVariablesEnvironment.variables.clear();
		for (VarInfo captured : this.environment.user().getVariables()) {
			VarInfo added = new VarInfo(captured.name, currentOffset, captured.type);
			newParameters.add(added);
			InsnTree loader = load(added);
			//must force put in backing map directly,
			//as normally this variable is "already defined".
			userParametersEnvironment.variables.put(added.name, (parser, name) -> loader);
			currentOffset += added.type.getSize();
		}
		//System.out.println(methodName + " builtin + captured: " + newParameters);
		for (UserParameter userParameter : userParameters.parameters()) {
			VarInfo variable = new VarInfo(userParameter.name(), currentOffset, userParameter.type());
			newParameters.add(variable);
			userParametersEnvironment.addVariableLoad(variable);
			currentOffset += variable.type.getSize();
		}
		//System.out.println(methodName + " builtin + captured + user: " + newParameters);
		MethodCompileContext newMethod = this.clazz.newMethod(
			this.method.info.access(),
			methodName + '_' + this.functionUniquifier++,
			returnType,
			newParameters
			.stream()
			.map(var -> var.type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
		newMethod.scopes.pushScope();
		if (!newMethod.info.isStatic()) {
			newMethod.addThis();
		}
		for (VarInfo parameter : newParameters) {
			VarInfo added = newMethod.newParameter(parameter.name, parameter.type);
			if (added.index != parameter.index) {
				throw new IllegalStateException("Parameter index mismatch: " + parameter + " -> " + added);
			}
		}

		MethodInfo newMethodInfo = newMethod.info;
		InsnTree[] implicitParameters = (
			Stream.concat(
				this.method.parameters.values().stream(),
				this.environment.user().streamVariables()
			)
			.map(InsnTrees::load)
			.toArray(InsnTree.ARRAY_FACTORY)
		);
		TypeInfo[] expectedTypes = (
			Arrays
			.stream(userParameters.parameters())
			.map(UserParameter::type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
		FunctionHandler handler = (parser, name, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
			InsnTree[] concatenatedArguments = ObjectArrays.concat(implicitParameters, castArguments, InsnTree.class);
			if (this.method.info.isStatic()) {
				return new CastResult(invokeStatic(newMethodInfo, concatenatedArguments), castArguments != arguments);
			}
			else {
				return new CastResult(invokeInstance(load("this", 0, this.clazz.info), newMethodInfo, concatenatedArguments), castArguments != arguments);
			}
		};
		this.environment.user().addFunction(methodName, handler);
		userVariablesEnvironment.addFunction(methodName, handler);

		ExpressionParser newParser = new ExpressionParser(this, newMethod);
		userVariablesEnvironment.parser = newParser;
		newParser.environment.user(userVariablesEnvironment).mutable(userParametersEnvironment);
		InsnTree result = newParser.parseRemainingInput(true);

		return new MethodDeclarationInsnTree(newMethod, result);
	}

	public InsnTree nextUserDefinedClass(String className) throws ScriptParsingException {
		this.input.expectAfterWhitespace('(');
		ClassCompileContext innerClass = this.clazz.newInnerClass(
			ACC_PUBLIC | ACC_STATIC,
			this.clazz.innerClassName(className),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		TypeInfo innerClassType = innerClass.info;
		List<FieldCompileContext> fields = new ArrayList<>(8);
		while (!this.input.hasAfterWhitespace(')')) {
			String typeName = this.input.expectIdentifier();
			TypeInfo type = this.environment.getType(this, typeName);
			if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, this.input);

			String fieldName = this.verifyName(this.input.expectIdentifierAfterWhitespace(), "field");
			FieldCompileContext field = innerClass.newField(ACC_PUBLIC, fieldName, type);
			fields.add(field);

			if (this.input.hasOperatorAfterWhitespace("=")) {
				ConstantValue initializer = this.nextSingleExpression().cast(this, type, CastMode.IMPLICIT_THROW).getConstantValue();
				if (initializer.isConstant()) {
					field.initializer = initializer;
				}
				else {
					throw new ScriptParsingException("Field initializer must be constant", this.input);
				}
			}
			FieldInfo fieldInfo = field.info;
			innerClass.newMethod(ACC_PUBLIC, fieldName, type).scopes.withScope((MethodCompileContext getter) -> {
				return_(getField(load("this", 0, innerClassType), fieldInfo)).emitBytecode(getter);
			});
			innerClass.newMethod(ACC_PUBLIC, fieldName, TypeInfos.VOID, fieldInfo.type).scopes.withScope((MethodCompileContext setter) -> {
				putField(load("this", 0, innerClassType), fieldInfo, load(fieldInfo.name, 1, fieldInfo.type)).emitBytecode(setter);
				return_(noop).emitBytecode(setter);
			});

			this.input.hasOperatorAfterWhitespace(",,");
		}
		//add constructors.
		innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID).scopes.withScope((MethodCompileContext constructor) -> {
			VarInfo constructorThis = constructor.addThis();
			invokeInstance(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
			for (FieldCompileContext field : fields) {
				if (field.initializer != null) {
					putField(
						load(constructorThis),
						new FieldInfo(ACC_PUBLIC, innerClassType, field.name(), field.info.type),
						ldc(field.initializer)
					)
					.emitBytecode(constructor);
				}
			}
			return_(noop).emitBytecode(constructor);
		});
		if (!fields.isEmpty()) {
			innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)).scopes.withScope((MethodCompileContext constructor) -> {
				VarInfo constructorThis = constructor.addThis();
				invokeInstance(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorThis),
						new FieldInfo(ACC_PUBLIC, innerClassType, field.name(), field.info.type),
						load(constructor.newParameter(field.name(), field.info.type))
					)
					.emitBytecode(constructor);
				}
				return_(noop).emitBytecode(constructor);
			});
		}
		List<FieldCompileContext> nonDefaulted = fields.stream().filter(field -> field.initializer == null).collect(Collectors.toList());
		if (nonDefaulted.size() != fields.size() && !nonDefaulted.isEmpty()) {
			innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID, nonDefaulted.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)).scopes.withScope((MethodCompileContext constructor) -> {
				VarInfo constructorThis = constructor.addThis();
				invokeInstance(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorThis),
						new FieldInfo(ACC_PUBLIC, innerClassType, field.info.name, field.info.type),
						field.initializer != null ? ldc(field.initializer) : load(constructor.newParameter(field.name(), field.info.type))
					)
					.emitBytecode(constructor);
				}
				return_(noop).emitBytecode(constructor);
			});
		}
		//add toString().
		innerClass.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodThis = method.addThis();
			StringBuilder pattern = new StringBuilder(className).append('(');
			for (FieldCompileContext field : fields) {
				pattern.append(field.name()).append(": ").append('\u0001').append(", ");
			}
			pattern.setLength(pattern.length() - 2);
			pattern.append(')');
			return_(
				invokeDynamic(
					MAKE_CONCAT_WITH_CONSTANTS,
					new MethodInfo(
						ACC_PUBLIC | ACC_STATIC,
						TypeInfos.OBJECT,
						"toString",
						TypeInfos.STRING,
						fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)
					),
					new ConstantValue[] {
						constant(pattern.toString())
					},
					fields
					.stream()
					.map(field -> getField(load(methodThis), field.info))
					.toArray(InsnTree.ARRAY_FACTORY)
				)
			)
			.emitBytecode(method);
		});
		//add hashCode().
		innerClass.newMethod(ACC_PUBLIC, "hashCode", TypeInfos.INT).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodThis = method.addThis();
			if (fields.isEmpty()) {
				return_(ldc(0)).emitBytecode(method);
			}
			else {
				invokeStatic(
					HASH_MIX,
					ArrayExtensions.computeHashCode(
						getField(load(methodThis), fields.get(0).info)
					)
				)
				.emitBytecode(method);
				for (int index = 1, size = fields.size(); index < size; index++) {
					invokeStatic(
						HASH_MIX,
						add(
							this,
							getFromStack(TypeInfos.INT),
							ArrayExtensions.computeHashCode(
								getField(load(methodThis), fields.get(index).info)
							)
						)
					)
					.emitBytecode(method);
				}
				return_(getFromStack(TypeInfos.INT)).emitBytecode(method);
			}
		});
		//add equals().
		innerClass.newMethod(ACC_PUBLIC, "equals", TypeInfos.BOOLEAN, TypeInfos.OBJECT).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodThis = method.addThis();
			VarInfo object = method.newParameter("object", TypeInfos.OBJECT);
			if (fields.isEmpty()) {
				return_(instanceOf(load(object), innerClassType)).emitBytecode(method);
			}
			else {
				VarInfo that = method.newVariable("that", innerClassType);
				ifThen(
					not(condition(this, instanceOf(load(object), innerClassType))),
					return_(ldc(false))
				)
				.emitBytecode(method);
				store(that, load(object).cast(this, innerClassType, CastMode.EXPLICIT_THROW)).emitBytecode(method);
				for (FieldCompileContext field : fields) {
					ifThen(
						not(
							condition(
								this,
								ArrayExtensions.computeEquals(
									this,
									getField(load(methodThis), field.info),
									getField(load(that), field.info)
								)
							)
						),
						return_(ldc(false))
					)
					.emitBytecode(method);
				}
				return_(ldc(true)).emitBytecode(method);
			}
		});
		//setup user definitions.
		this.checkType(className);
		this.environment.user().types.put(className, innerClassType);
		this.environment.user().addConstructor(innerClassType, new MethodInfo(ACC_PUBLIC, innerClassType, "<init>", TypeInfos.VOID));
		if (!fields.isEmpty()) {
			this.environment.user().addConstructor(innerClassType, new MethodInfo(ACC_PUBLIC, innerClassType, "<init>", TypeInfos.VOID, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			if (nonDefaulted.size() != fields.size()) {
				this.environment.user().addConstructor(innerClassType, new MethodInfo(ACC_PUBLIC, innerClassType, "<init>", TypeInfos.VOID, nonDefaulted.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			}
		}
		fields.stream().map(field -> field.info).forEach(this.environment.user()::addFieldGetterAndSetter);
		return noop;
	}

	public TypeInfo getMainReturnType() {
		return this.method.info.returnType;
	}

	public InsnTree createReturn(InsnTree value) {
		return return_(value.cast(this, this.getMainReturnType(), CastMode.IMPLICIT_THROW));
	}

	public String listCandidates(String identifier, String prefix, String suffix) {
		return this.environment.listCandidates(identifier).map("\t"::concat).collect(Collectors.joining("\n", prefix + "\nCandidates:\n", '\n' + suffix));
	}

	public static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
	}

	public static boolean isNumber(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isLetterOrNumber(char c) {
		return isLetter(c) || isNumber(c);
	}

	public String verifyName(String name, String type) throws ScriptParsingException {
		if (name.isEmpty()) throw new ScriptParsingException(type + " name must not be empty", this.input);
		if (name.equals("_")) throw new ScriptParsingException(type + " name must not be _ as it is a reserved word in java", this.input);
		if (!isLetter(name.charAt(0))) throw new ScriptParsingException(type + " name must start with an ASCII letter or underscore", this.input);
		for (int index = 1, length = name.length(); index < length; index++) {
			if (!isLetterOrNumber(name.charAt(index))) throw new ScriptParsingException(type + " name must contain only ASCII letters, numbers, and underscores", this.input);
		}
		return name;
	}

	public void beginCodeBlock() throws ScriptParsingException {
		this.input.expectAfterWhitespace('(');
		this.environment.user().push();
	}

	public boolean endCodeBlock() throws ScriptParsingException {
		this.input.expectAfterWhitespace(')');
		boolean newVariables = this.environment.user().hasNewVariables();
		this.environment.user().pop();
		return newVariables;
	}
}