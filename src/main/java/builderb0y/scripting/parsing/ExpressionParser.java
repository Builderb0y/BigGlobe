package builderb0y.scripting.parsing;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.util.CheckClassAdapter;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.ThrowingFunction;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.InsnTree.UpdateOp;
import builderb0y.scripting.bytecode.tree.InsnTree.UpdateOrder;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarePostAssignInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.LineNumberInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ScopedInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.RootScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.*;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.CommaSeparatedExpressions;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.MultiDeclaration;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.ParenthesizedScript;
import builderb0y.scripting.parsing.UserMethodDefiner.UserExtensionMethodDefiner;
import builderb0y.scripting.parsing.UserMethodDefiner.UserFunctionDefiner;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.StringSimilarity;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/**
higher-level script-parsing logic than {@link ExpressionReader}.
handles expressions, user-defined variables/methods/classes,
order of operations, etc... and building an abstract syntax tree out of them.
this abstract syntax tree is represented with {@link InsnTree}.
*/
@SuppressWarnings("ErrorNotRethrown")
public class ExpressionParser {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bytecode.dumpScripts", "bigglobe_scripts");

	public static void clinit() {}

	public final ExpressionReader input;
	public int currentLine;
	public final ClassCompileContext clazz;
	public final MethodCompileContext method;
	public final RootScriptEnvironment environment;
	public final List<DelayedMethod> delayedMethods;

	public ExpressionParser(String input, ClassCompileContext clazz, MethodCompileContext method) {
		this.input = new ExpressionReader(input);
		this.clazz = clazz;
		this.method = method;
		this.environment = new RootScriptEnvironment();
		this.environment.user().parser = this;
		this.delayedMethods = new ArrayList<>(4);
	}

	/**
	this constructor is intended for user-defined functions only.
	see {@link UserMethodDefiner}.
	*/
	public ExpressionParser(ExpressionParser from) {
		this.input = from.input;
		this.clazz = from.clazz;
		this.method = from.method;
		this.currentLine = from.currentLine;
		this.environment = new RootScriptEnvironment(from.environment);
		this.delayedMethods = from.delayedMethods;
	}

	/**
	this constructor is intended for template scripts only.
	see {@link TemplateScriptParser#parseEntireInput()}.
	*/
	public ExpressionParser(ExpressionParser from, String newInput) {
		this.input = new ExpressionReader(newInput);
		this.clazz = from.clazz;
		this.method = from.method;
		this.environment = new RootScriptEnvironment(from.environment);
		this.environment.user().parser = this;
		this.delayedMethods = from.delayedMethods;
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

	public Class<?> compile(ScriptClassLoader loader) throws Throwable {
		if (CLASS_DUMP_DIRECTORY != null) try {
			String baseName = this.clazz.info.getSimpleName();
			Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-src.txt"), this.input.input, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), this.clazz.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), this.clazz.toByteArray(), StandardOpenOption.CREATE_NEW);
		}
		catch (IOException exception) {
			ScriptLogger.LOGGER.error("", exception);
		}
		return loader.defineClass(this.clazz);
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
		return this.parseRemainingInput(false, true);
	}

	public InsnTree parseRemainingInput(boolean expectClose, boolean return_) throws ScriptParsingException {
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
			if (return_ && !tree.jumpsUnconditionally()) {
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
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case ",", ":" -> { //indicates the end of this statement list.
						return left;
					}
					case "", "++", "++:", ":++", "--", "--:", ":--", "!", "~" -> {} //indicates that there's another statement to read.
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
					case "<"    -> { this.input.onCharsRead(operator); left = bool(        lt(this, left, this.nextSum()) ); }
					case "<="   -> { this.input.onCharsRead(operator); left = bool(        le(this, left, this.nextSum()) ); }
					case ">"    -> { this.input.onCharsRead(operator); left = bool(        gt(this, left, this.nextSum()) ); }
					case ">="   -> { this.input.onCharsRead(operator); left = bool(        ge(this, left, this.nextSum()) ); }
					case "=="   -> { this.input.onCharsRead(operator); left = bool(        eq(this, left, this.nextSum()) ); }
					case "!="   -> { this.input.onCharsRead(operator); left = bool(        ne(this, left, this.nextSum()) ); }
					case "==="  -> { this.input.onCharsRead(operator); left = bool(identityEq(this, left, this.nextSum()) ); }
					case "!=="  -> { this.input.onCharsRead(operator); left = bool(identityNe(this, left, this.nextSum()) ); }
					case "!>"   -> { this.input.onCharsRead(operator); left = bool(    not(gt(this, left, this.nextSum()))); }
					case "!<"   -> { this.input.onCharsRead(operator); left = bool(    not(lt(this, left, this.nextSum()))); }
					case "!>="  -> { this.input.onCharsRead(operator); left = bool(    not(ge(this, left, this.nextSum()))); }
					case "!<="  -> { this.input.onCharsRead(operator); left = bool(    not(le(this, left, this.nextSum()))); }

					case ".<"   -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        lt(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".<="  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        le(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".>"   -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        gt(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".>="  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        ge(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".=="  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        eq(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".!="  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(        ne(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".===" -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(identityEq(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".!==" -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(identityNe(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right) ); }
					case ".!>"  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(    not(gt(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right))); }
					case ".!<"  -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(    not(lt(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right))); }
					case ".!>=" -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(    not(ge(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right))); }
					case ".!<=" -> { this.input.onCharsRead(operator); InsnTree right = this.nextSum(); left = bool(    not(le(this, left.cast(this, right.getTypeInfo(), CastMode.EXPLICIT_THROW), right))); }

					case "<."   -> { this.input.onCharsRead(operator); left = bool(        lt(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "<=."  -> { this.input.onCharsRead(operator); left = bool(        le(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case ">."   -> { this.input.onCharsRead(operator); left = bool(        gt(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case ">=."  -> { this.input.onCharsRead(operator); left = bool(        ge(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "==."  -> { this.input.onCharsRead(operator); left = bool(        eq(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "!=."  -> { this.input.onCharsRead(operator); left = bool(        ne(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "===." -> { this.input.onCharsRead(operator); left = bool(identityEq(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "!==." -> { this.input.onCharsRead(operator); left = bool(identityNe(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)) ); }
					case "!>."  -> { this.input.onCharsRead(operator); left = bool(    not(gt(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)))); }
					case "!<."  -> { this.input.onCharsRead(operator); left = bool(    not(lt(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)))); }
					case "!>=." -> { this.input.onCharsRead(operator); left = bool(    not(ge(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)))); }
					case "!<=." -> { this.input.onCharsRead(operator); left = bool(    not(le(this, left, this.nextSum().cast(this, left.getTypeInfo(), CastMode.EXPLICIT_THROW)))); }

					default     -> { return left; }
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
			if (this.input.hasOperatorAfterWhitespace("?:")) {
				left = left.elvis(this, this.nextElvis());
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

	public InsnTree nextMember() throws ScriptParsingException {
		try {
			return this.finishNextMember(this.nextPrefixOperator());
		}
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public InsnTree finishNextMember(InsnTree left) throws ScriptParsingException {
		while (true) {
			String operator = this.input.peekOperatorAfterWhitespace();
			if (!operator.isEmpty() && operator.charAt(0) == '.') {
				this.input.onCharsRead(operator);
				boolean isAssign   = false;
				boolean isNullable = false;
				boolean isReceiver = false;
				for (int index = 1, length = operator.length(); index < length; index++) {
					switch (operator.charAt(index)) {
						case '=' -> {
							if (isAssign) throw new ScriptParsingException("Duplicate assignment character", this.input);
							isAssign = true;
						}
						case '?' -> {
							if (isNullable) throw new ScriptParsingException("Duplicate nullable character", this.input);
							isNullable = true;
						}
						case '$' -> {
							if (isReceiver) throw new ScriptParsingException("Duplicate receiver character", this.input);
							isReceiver = true;
						}
						default -> {
							throw new ScriptParsingException("Unrecognized character in member lookup operator", this.input);
						}
					}
				}
				//note: memberName can be the empty String, "".
				//this is intentional to support array/list-lookup syntax:
				//array.(index)
				String memberName = this.input.readIdentifierAfterWhitespace();
				CommonMode mode = isNullable ? (isReceiver ? CommonMode.NULLABLE_RECEIVER : CommonMode.NULLABLE) : (isReceiver ? CommonMode.RECEIVER : CommonMode.NORMAL);
				if (isAssign) {
					InsnTree assignable = this.environment.getField(this, left, memberName, GetFieldMode.from(mode));
					if (assignable == null) {
						throw new ScriptParsingException(this.listCandidates(memberName, "Unknown field: " + memberName, "Actual form: " + left.describe() + '.' + memberName), this.input);
					}
					this.beginCodeBlock();
					InsnTree value = this.nextScript();
					if (this.endCodeBlock()) {
						value = new ScopedInsnTree(value);
					}
					left = assignable.update(this, UpdateOp.ASSIGN, UpdateOrder.VOID, value);
				}
				else {
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
			else {
				return left;
			}
		}
	}

	public InsnTree nextPrefixOperator() throws ScriptParsingException {
		try {
			String prefixOperator = this.input.peekOperatorAfterWhitespace();
			return switch (prefixOperator) {
				case "+" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextPrefixTerm(NegateMode.NONE, tree -> {
						if (!tree.getTypeInfo().isNumber()) {
							throw new ScriptParsingException("Non-numeric term for unary '+': " + tree.getTypeInfo(), this.input);
						}
						return tree;
					});
				}
				case "-" -> {
					this.input.onCharsRead(prefixOperator);
					//must special handle Integer.MIN_VALUE and Long.MIN_VALUE,
					//because otherwise it would try to parse them as positive numbers,
					//and then negate them, but the positive form is not representable
					//in the same precision as the negative form.
					yield this.nextPrefixTerm(NegateMode.ARITHMETIC, InsnTrees::neg);
				}
				case "~" -> {
					this.input.onCharsRead(prefixOperator);
					yield this.nextPrefixTerm(NegateMode.BITWISE, tree -> {
						//it is safe to use tree.getTypeInfo() directly
						//without sanity checking that it is numeric here,
						//because bxor() will check that immediately afterwards.
						return bxor(this, tree, ldc(-1, tree.getTypeInfo()));
					});
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

	public InsnTree nextPrefixTerm(NegateMode negateMode, ThrowingFunction<InsnTree, InsnTree, ScriptParsingException> nonNumber) throws ScriptParsingException {
		if (isNumber(this.input.peekAfterWhitespace())) {
			return this.nextNumber(negateMode);
		}
		else {
			return nonNumber.apply(this.nextMember());
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
					yield this.nextNumber(NegateMode.NONE);
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
					return concat(string.toString(), arguments.toArray(InsnTree[]::new));
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

	public static enum NegateMode {
		NONE,
		ARITHMETIC,
		BITWISE;
	}

	public InsnTree nextNumber(NegateMode negateMode) throws ScriptParsingException {
		try {
			BigDecimal number = NumberParser.parse(this.input);
			switch (negateMode) {
				case NONE -> {}
				case ARITHMETIC -> number = number.negate();
				case BITWISE -> {
					if (number.scale() > 0) throw new ScriptParsingException("Can't bitwise negate non-integer", this.input);
					number = new BigDecimal(number.toBigIntegerExact().not());
				}
			}
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
					throw new ScriptParsingException("This isn't a C-family language. Doubles are suffixed by 'L', and floats are suffixed by 'I'.", this.input);
				}
				case 'l', 'L' -> {
					this.input.onCharRead(suffix);
					if (number.scale() > 0) {
						if (unsigned) throw new ScriptParsingException("Unsigned double literals not supported", this.input);
						double value = number.doubleValue();
						if (negateMode == NegateMode.ARITHMETIC && value == 0.0D) value = -0.0D;
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
						if (negateMode == NegateMode.ARITHMETIC && value == 0.0F) value = -0.0F;
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
					if (number.scale() > 0) {
						throw new ScriptParsingException("Half-precision floats not supported", this.input);
					}
					if (unsigned) {
						yield ldc(BigGlobeMath.toUnsignedShortExact(number.intValueExact()));
					}
					else {
						yield ldc(number.shortValueExact());
					}
				}
				default -> {
					if (number.scale() > 0) {
						if (unsigned) throw new ScriptParsingException("Unsigned floating point literals not supported", this.input);
						double doubleValue = number.doubleValue();
						if (negateMode == NegateMode.ARITHMETIC && doubleValue == 0.0D) doubleValue = -0.0D;
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
							if (longValue == (longValue & 0xFFFF_FFFFL)) {
								int intValue = (int)(longValue);
								if (intValue == (intValue & 0xFFFF)) {
									if (intValue == (intValue & 0xFF)) {
										yield ldc((byte)(intValue));
									}
									yield ldc((short)(intValue));
								}
								yield ldc(intValue);
							}
							yield ldc(longValue);
						}
						else {
							longValue = number.longValueExact();
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
			InsnTree result = this.environment.parseKeyword(this, name);
			if (result != null) return result;
			//not keyword.
			TypeInfo type = this.environment.getType(this, name);
			if (type != null) {
				if (this.input.hasOperatorAfterWhitespace("*")) {
					if (type.getSort() == Sort.VOID) {
						throw new ScriptParsingException("void-typed variables are not allowed.", this.input);
					}
					return MultiDeclaration.parse(this, type).sequence();
				}
				else if (this.input.peekAfterWhitespace() == '(') { //casting.
					return ParenthesizedScript.parse(this).maybeWrapContents().cast(this, type, CastMode.EXPLICIT_THROW);
				}
				else { //not casting. (variable or method declaration or ldc class)
					String varName = this.input.readIdentifierAfterWhitespace();
					if (!varName.isEmpty()) { //variable or method declaration.
						if (this.input.hasOperatorAfterWhitespace("=")) { //variable declaration.
							if (type.getSort() == Sort.VOID) {
								throw new ScriptParsingException("void-typed variables are not allowed.", this.input);
							}
							this.verifyName(varName, "variable");
							this.checkVariable(varName);
							this.environment.user().reserveVariable(varName, type);
							InsnTree initializer = this.nextVariableInitializer(type, true);
							this.environment.user().assignVariable(varName);
							LazyVarInfo variable = new LazyVarInfo(varName, type);
							return new VariableDeclareAssignInsnTree(variable, initializer);
						}
						else if (this.input.hasOperatorAfterWhitespace(":=")) { //also variable declaration.
							if (type.getSort() == Sort.VOID) {
								throw new ScriptParsingException("void-typed variables are not allowed.", this.input);
							}
							this.verifyName(varName, "variable");
							this.checkVariable(varName);
							this.environment.user().reserveVariable(varName, type);
							InsnTree initializer = this.nextVariableInitializer(type, true);
							this.environment.user().assignVariable(varName);
							LazyVarInfo variable = new LazyVarInfo(varName, type);
							return new VariableDeclarePostAssignInsnTree(variable, initializer);
						}
						else if (this.input.hasAfterWhitespace('(')) { //function declaration.
							this.verifyName(varName, "method");
							boolean empty = this.delayedMethods.isEmpty();
							new UserFunctionDefiner(this, varName, type).parse();
							if (empty) this.finishDelayedMethods();
							return noop;
						}
						else if (this.input.hasOperatorAfterWhitespace(".")) { //extension method declaration.
							TypeInfo typeBeingExtended = this.environment.getType(this, varName);
							if (typeBeingExtended == null) {
								throw new ScriptParsingException("Unknown type: " + varName, this.input);
							}
							varName = this.input.readIdentifierAfterWhitespace();
							this.verifyName(varName, "extension method");
							this.input.expectAfterWhitespace('(');
							boolean empty = this.delayedMethods.isEmpty();
							new UserExtensionMethodDefiner(this, varName, type, typeBeingExtended).parse();
							if (empty) this.finishDelayedMethods();
							return noop;
						}
						else {
							throw new ScriptParsingException("Expected '=', '(', or '.'", this.input);
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
		catch (RuntimeException exception) {
			throw new ScriptParsingException(exception, this.input);
		}
		catch (StackOverflowError error) {
			throw new ScriptParsingException("Script too long or too complex", error, this.input);
		}
	}

	public void finishDelayedMethods() {
		MethodCompileContext[] methodCompileContexts = this.delayedMethods.stream().map((DelayedMethod method) -> method.createMethod(this)).toArray(MethodCompileContext[]::new);
		for (int index = 0, length = methodCompileContexts.length; index < length; index++) {
			this.delayedMethods.get(index).emitMethod(methodCompileContexts[index]);
		}
		this.delayedMethods.clear();
	}

	public InsnTree nextVariableInitializer(TypeInfo variableType, boolean cast) throws ScriptParsingException {
		if (this.input.hasIdentifierAfterWhitespace("new")) {
			CommaSeparatedExpressions arguments = CommaSeparatedExpressions.parse(this);
			InsnTree expression = this.environment.getMethod(this, ldc(variableType), "new", GetMethodMode.NORMAL, arguments.arguments());
			if (expression == null) {
				throw new ScriptParsingException(this.listCandidates("new", "Incorrect arguments for new()", Arrays.stream(arguments.arguments()).map(InsnTree::describe).collect(Collectors.joining(", ", "Actual form: " + ldc(variableType).describe() + ".new(", ")"))), this.input);
			}
			return this.finishNextMember(arguments.maybeWrap(expression));
		}
		else {
			InsnTree tree = this.nextSingleExpression();
			if (cast) {
				tree = tree.cast(this, variableType, CastMode.IMPLICIT_THROW);
			}
			return tree;
		}
	}

	public TypeInfo getMainReturnType() {
		return this.method.info.returnType;
	}

	public InsnTree createReturn(InsnTree value) {
		return return_(value.cast(this, this.getMainReturnType(), CastMode.IMPLICIT_THROW));
	}

	public String listCandidates(String identifier, String prefix, String suffix) {
		record DescribedStringSimilarity(IdentifierDescriptor descriptor, StringSimilarity similarity) implements Comparable<DescribedStringSimilarity> {

			@Override
			public int compareTo(@NotNull DescribedStringSimilarity that) {
				return this.similarity.compareTo(that.similarity);
			}
		}
		return (
			this
			.environment
			.listIdentifiers()
			.map((IdentifierDescriptor descriptor) -> {
				StringSimilarity similarity = StringSimilarity.compare(identifier, descriptor.name());
				return similarity.compareTo(StringSimilarity.NO_MATCH) > 0 ? new DescribedStringSimilarity(descriptor, similarity) : null;
			})
			.filter(Objects::nonNull)
			.sorted(Comparator.reverseOrder())
			.limit(10L)
			.map((DescribedStringSimilarity described) -> "\t" + described.descriptor.value())
			.collect(Collectors.joining(
				"\n",
				prefix + "\nCandidates:\n",
				'\n' + suffix
			))
		);
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

	public static <T_Encoded> void verifyName(VerifyContext<T_Encoded, String> context) throws VerifyException {
		String name = context.object;
		if (name == null) return;
		if (name.isEmpty()) throw new VerifyException(() -> context.pathToStringBuilder().append(" must not be empty").toString());
		if (name.equals("_")) throw new VerifyException(() -> context.pathToStringBuilder().append(" must not be _ as it is a reserved word in java").toString());
		if (!isLetter(name.charAt(0))) throw new VerifyException(() -> context.pathToStringBuilder().append(" must start with an ASCII letter or underscore").toString());
		for (int index = 1, length = name.length(); index < length; index++) {
			if (!isLetterOrNumber(name.charAt(index))) throw new VerifyException(() -> context.pathToStringBuilder().append(" must contain only ASCII letters, numbers, and underscores").toString());
		}
	}

	@Mirror(UseVerifier.class)
	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	@UseVerifier(name = "verifyName", in = ExpressionParser.class, usage = MemberUsage.METHOD_IS_HANDLER)
	public static @interface IdentifierName {}

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