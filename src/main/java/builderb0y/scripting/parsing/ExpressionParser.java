package builderb0y.scripting.parsing;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import it.unimi.dsi.fastutil.HashCommon;
import org.objectweb.asm.util.CheckClassAdapter;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.CastingSupport.CastProvider;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.InsnTree.UpdateOp;
import builderb0y.scripting.bytecode.tree.MethodDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.LineNumberInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.StoreInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment2;
import builderb0y.scripting.environments.RootScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.CommaSeparatedExpressions;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.ParenthesizedScript;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.UserParameterList.UserParameter;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ExpressionParser {

	public static final boolean DUMP_GENERATED_CLASSES  = Boolean.getBoolean("builderb0y.bytecode.dumpGeneratedClasses");

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
	}

	public ExpressionParser addEnvironment(MutableScriptEnvironment2 environment) {
		this.environment.mutable().addAll(environment);
		return this;
	}

	public ExpressionParser addEnvironment(ScriptEnvironment environment) {
		this.environment.environments.add(environment);
		return this;
	}

	public ExpressionParser addCastProvider(CastProvider castProvider) {
		this.environment.castProviders.add(castProvider);
		return this;
	}

	public Class<?> compile() {
		if (DUMP_GENERATED_CLASSES) {
			ScriptLogger.LOGGER.info("Compiling script with source:");
			ScriptLogger.LOGGER.info(this.input.input);
			ScriptLogger.LOGGER.info(this.clazz.dump());
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
			.append("Script source:\n").append(this.input.getSource()).append('\n')
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
			if (!tree.returnsUnconditionally()) {
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
				if (this.input.peek() == ')') {
					return left;
				}
				//another operator (except ++ and --) indicates that said
				//operator didn't get processed sooner when it should have.
				String operator = this.input.peekOperator();
				switch (operator) {
					case ",", ":" -> { //indicates the end of this statement list.
						return left;
					}
					case "", "++", "--" -> {} //indicates that there's another statement to read.
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
				if (left.returnsUnconditionally()) {
					throw new ScriptParsingException("Unreachable statement", this.input);
				}
				left = left.then(this, next);
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
				if (left.returnsUnconditionally()) {
					throw new ScriptParsingException("Unreachable statement", this.input);
				}
				left = left.then(this, next);
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
			UpdateOp op = switch (operator) {
				case "=" -> UpdateOp.ASSIGN;
				case "+=" -> UpdateOp.ADD;
				case "-=" -> UpdateOp.SUBTRACT;
				case "*=" -> UpdateOp.MULTIPLY;
				case "/=" -> UpdateOp.DIVIDE;
				case "%=" -> UpdateOp.MODULO;
				case "^=" -> UpdateOp.POWER;
				case "&=" -> UpdateOp.BITWISE_AND;
				case "|=" -> UpdateOp.BITWISE_OR;
				case "#=" -> UpdateOp.BITWISE_XOR;
				case "&&=" -> UpdateOp.AND;
				case "||=" -> UpdateOp.OR;
				case "##=" -> UpdateOp.XOR;
				case "<<=" -> UpdateOp.SIGNED_LEFT_SHIFT;
				case ">>=" -> UpdateOp.SIGNED_RIGHT_SHIFT;
				case "<<<=" -> UpdateOp.UNSIGNED_LEFT_SHIFT;
				case ">>>=" -> UpdateOp.UNSIGNED_RIGHT_SHIFT;
				default -> null;
			};
			if (op != null) {
				this.input.onCharsRead(operator);
				left = left.update(this, op, this.nextSingleExpression());
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

	public InsnTree nextBoolean() throws ScriptParsingException {
		try {
			InsnTree left = this.nextCompare();
			while (true) {
				String operator = this.input.peekOperatorAfterWhitespace();
				switch (operator) {
					case "&&" -> {
						this.input.onCharsRead(operator);
						if (left.getTypeInfo().getSort() != Sort.BOOLEAN) {
							throw new ScriptParsingException("Expected boolean before &&", this.input);
						}
						left = and(this, left, this.nextCompare());
					}
					case "||" -> {
						this.input.onCharsRead(operator);
						if (left.getTypeInfo().getSort() != Sort.BOOLEAN) {
							throw new ScriptParsingException("Expected boolean before ||", this.input);
						}
						left = or(this, left, this.nextCompare());
					}
					case "##" -> {
						this.input.onCharsRead(operator);
						if (left.getTypeInfo().getSort() != Sort.BOOLEAN) {
							throw new ScriptParsingException("Expected boolean before ##", this.input);
						}
						left = xor(this, left, this.nextCompare());
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
					case "<"  -> { this.input.onCharsRead(operator); left = bool(lt(this, left, this.nextSum())); }
					case "<=" -> { this.input.onCharsRead(operator); left = bool(le(this, left, this.nextSum())); }
					case ">"  -> { this.input.onCharsRead(operator); left = bool(gt(this, left, this.nextSum())); }
					case ">=" -> { this.input.onCharsRead(operator); left = bool(ge(this, left, this.nextSum())); }
					case "==" -> { this.input.onCharsRead(operator); left = bool(eq(this, left, this.nextSum())); }
					case "!=" -> { this.input.onCharsRead(operator); left = bool(ne(this, left, this.nextSum())); }
					default   -> { return left; }
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
			InsnTree left = this.nextMember();
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

	public InsnTree nextMember() throws ScriptParsingException {
		try {
			InsnTree left = this.nextPrefixOperator();
			while (true) {
				if (this.input.hasAfterWhitespace('.')) {
					//note: can be the empty String, "".
					//this is intentional to support array/list-lookup syntax:
					//array.(index)
					String memberName = this.input.readIdentifierAfterWhitespace();

					InsnTree result = this.environment.parseMemberKeyword(this, left, memberName);
					if (result == null) {
						if (this.input.peekAfterWhitespace() == '(') {
							result = this.environment.getMethod(this, left, memberName, CommaSeparatedExpressions.parse(this).arguments());
							if (result == null) {
								throw new ScriptParsingException("Unknown method: " + memberName, this.input);
							}
						}
						else {
							result = this.environment.getField(this, left, memberName);
							if (result == null) {
								throw new ScriptParsingException("Unknown field: " + memberName, this.input);
							}
						}
					}
					left = result;
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
					InsnTree term = this.nextMember();
					yield term.update(this, UpdateOp.ADD, ldc(1));
				}
				case "--" -> {
					this.input.onCharsRead(prefixOperator);
					InsnTree term = this.nextMember();
					yield term.update(this, UpdateOp.SUBTRACT, ldc(1));
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
					//String string = this.input.readWhile((char c) -> c != first);
					//this.input.expect(first);
					//yield ldc(string);
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

	public static final MethodInfo STRING_CONCAT_FACTORY = method(
		ACC_PUBLIC | ACC_STATIC,
		StringConcatFactory.class,
		"makeConcatWithConstants",
		CallSite.class,
		MethodHandles.Lookup.class,
		String.class,
		MethodType.class,
		String.class,
		Object[].class
	);

	public InsnTree nextString(char end) throws ScriptParsingException {
		StringBuilder string = new StringBuilder();
		ArrayBuilder<InsnTree> arguments = new ArrayBuilder<>();
		while (true) {
			char c = this.input.read();
			//todo: handle when c == 1 or c == 2.
			if (c == 0) {
				throw new ScriptParsingException("Un-terminated string", this.input);
			}
			else if (c == end) {
				if (arguments.isEmpty()) {
					return ldc(string.toString());
				}
				else {
					return invokeDynamic(
						STRING_CONCAT_FACTORY,
						method(
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
				char escaped = this.input.peek();
				if (escaped == '$') {
					this.input.onCharRead('$');
					string.append('$');
				}
				else {
					string.append((char)(1));
					arguments.add(this.nextTerm());
				}
			}
			else {
				string.append(c);
			}
		}
	}

	public InsnTree nextNumber(boolean negated) throws ScriptParsingException {
		try {
			BigDecimal number = NumberParser.parse(this.input);
			if (negated) number = number.negate();
			char suffix = this.input.peek();
			return switch (suffix) {
				case 's', 'S' -> {
					this.input.onCharRead(suffix);
					if (number.scale() > 0) {
						float value = number.floatValue();
						if (negated && value == 0.0F) value = -0.0F;
						yield ldc(value);
					}
					else {
						yield ldc(number.intValueExact());
					}
				}
				case 'l', 'L' -> {
					this.input.onCharRead(suffix);
					if (number.scale() > 0) {
						double value = number.doubleValue();
						if (negated && value == 0.0D) value = -0.0D;
						yield ldc(value);
					}
					else {
						yield ldc(number.longValueExact());
					}
				}
				default -> {
					if (number.scale() > 0) {
						double doubleValue = number.doubleValue();
						if (negated && doubleValue == 0.0D) doubleValue = -0.0D;
						float floatValue = (float)(doubleValue);
						if (doubleValue == floatValue) {
							yield ldc(floatValue);
						}
						yield ldc(doubleValue);
					}
					else {
						long longValue = number.longValueExact();
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
				String varName = this.input.expectIdentifierAfterWhitespace();
				this.input.expectOperatorAfterWhitespace("=");
				InsnTree initializer = this.nextSingleExpression();
				VariableDeclarationInsnTree declaration = this.environment.user().newVariable(varName, initializer.getTypeInfo());
				return declaration.then(this, new StoreInsnTree(declaration.loader.variable, initializer));
			}
			else if (name.equals("class")) {
				String className = this.input.expectIdentifierAfterWhitespace();
				return this.nextUserDefinedClass(className);
			}
			else { //not var.
				TypeInfo type = this.environment.getType(this, name);
				if (type != null) {
					if (this.input.peekAfterWhitespace() == '(') { //casting.
						return ParenthesizedScript.parse(this).maybeWrapContents().cast(this, type, CastMode.EXPLICIT_THROW);
					}
					else { //not casting. (variable or method declaration or ldc class)
						String varName = this.input.readIdentifierAfterWhitespace();
						if (!varName.isEmpty()) { //variable or method declaration.
							if (this.input.hasOperatorAfterWhitespace("=")) { //variable declaration.
								InsnTree initializer = this.nextSingleExpression().cast(this, type, CastMode.IMPLICIT_THROW);
								VariableDeclarationInsnTree declaration = this.environment.user().newVariable(varName, type);
								return declaration.then(this, new StoreInsnTree(declaration.loader.variable, initializer));
							}
							else if (this.input.hasAfterWhitespace('(')) { //method declaration.
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
					InsnTree result = this.environment.parseKeyword(this, name);
					if (result != null) return result;
					if (this.input.peekAfterWhitespace() == '(') { //function call.
						CommaSeparatedExpressions arguments = CommaSeparatedExpressions.parse(this);
						result = this.environment.getFunction(this, name, arguments.arguments());
						if (result != null) return arguments.maybeWrap(result);
						throw new ScriptParsingException("Unknown function: " + name, this.input);
					}
					else { //variable.
						InsnTree variable = this.environment.getVariable(this, name);
						if (variable != null) return variable;
						throw new ScriptParsingException("Unknown variable: " + name, this.input);
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
		MutableScriptEnvironment2 userParametersEnvironment = new MutableScriptEnvironment2();
		for (VarInfo captured : this.environment.user().getVariables()) {
			VarInfo added = new VarInfo(captured.name, currentOffset, captured.type);
			newParameters.add(added);
			userParametersEnvironment.addVariableLoad(added);
			currentOffset += added.type.getSize();
		}
		for (UserParameter userParameter : userParameters.parameters()) {
			VarInfo variable = new VarInfo(userParameter.name(), currentOffset, userParameter.type());
			newParameters.add(variable);
			userParametersEnvironment.addVariableLoad(variable);
			currentOffset += variable.type.getSize();
		}
		MethodCompileContext newMethod = this.clazz.newMethod(
			this.method.info.access(),
			methodName + '_' + this.functionUniquifier++,
			returnType,
			newParameters
			.stream()
			.map(var -> var.type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
		ExpressionParser newParser = new ExpressionParser(this, newMethod);
		newParser.environment.environments.add(1, userParametersEnvironment);
		InsnTree result = newParser.parseRemainingInput(true);
		//newMethod.scopes.popScope();

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
		this.environment.user().addFunction(methodName, (parser, name, arguments) -> {
			InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, name, expectedTypes, CastMode.IMPLICIT_THROW, arguments);
			InsnTree[] concatenatedArguments = ObjectArrays.concat(implicitParameters, castArguments, InsnTree.class);
			if (this.method.info.isStatic()) {
				return invokeStatic(newMethodInfo, concatenatedArguments);
			}
			else {
				return invokeVirtual(load("this", 0, this.clazz.info), newMethodInfo, concatenatedArguments);
			}
		});
		return new MethodDeclarationInsnTree(newMethod, result, newParameters.toArray(VarInfo.ARRAY_FACTORY));
	}

	public static final MethodInfo OBJECT_CONSTRUCTOR = method(ACC_PUBLIC, TypeInfos.OBJECT, "<init>", TypeInfos.VOID);

	public InsnTree nextUserDefinedClass(String className) throws ScriptParsingException {
		this.input.expectAfterWhitespace('(');
		ClassCompileContext innerClass = this.clazz.newInnerClass(
			ACC_PUBLIC | ACC_STATIC,
			this.clazz.innerClassName(className),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		List<FieldCompileContext> fields = new ArrayList<>();
		while (!this.input.hasAfterWhitespace(')')) {
			String typeName = this.input.expectIdentifier();
			TypeInfo type = this.environment.getType(this, typeName);
			if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, this.input);

			String fieldName = this.input.expectIdentifierAfterWhitespace();
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

			this.input.hasOperatorAfterWhitespace(",,");
		}
		//add constructors.
		innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID).scopes.withScope((MethodCompileContext constructor) -> {
			VarInfo constructorThis = constructor.addThis();
			invokeSpecial(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
			for (FieldCompileContext field : fields) {
				if (field.initializer != null) {
					putField(
						load(constructorThis),
						field(ACC_PUBLIC, innerClass.info, field.name(), field.info.type),
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
				invokeSpecial(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorThis),
						field(ACC_PUBLIC, innerClass.info, field.name(), field.info.type),
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
				invokeSpecial(load(constructorThis), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorThis),
						field(ACC_PUBLIC, innerClass.info, field.info.name, field.info.type),
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
					method(ACC_PUBLIC | ACC_STATIC, StringConcatFactory.class, "makeConcatWithConstants", CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, Object[].class),
					method(ACC_PUBLIC | ACC_STATIC, TypeInfos.OBJECT, "toString", TypeInfos.STRING, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)),
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
						ArrayExtensions.computeHashCode(
							add(
								this,
								getFromStack(TypeInfos.INT),
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
				return_(instanceOf(load(object), innerClass.info)).emitBytecode(method);
			}
			else {
				VarInfo that = method.newVariable("that", innerClass.info);
				ifThen(
					this,
					not(condition(this, instanceOf(load(object), innerClass.info))),
					return_(ldc(false))
				)
				.emitBytecode(method);
				store(that, load(object).cast(this, innerClass.info, CastMode.EXPLICIT_THROW)).emitBytecode(method);
				for (FieldCompileContext field : fields) {
					ifThen(
						this,
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
		this.environment.user().types.put(className, innerClass.info);
		this.environment.user().addConstructor(innerClass.info, method(ACC_PUBLIC, innerClass.info, "<init>", TypeInfos.VOID));
		if (!fields.isEmpty()) {
			this.environment.user().addConstructor(innerClass.info, method(ACC_PUBLIC, innerClass.info, "<init>", TypeInfos.VOID, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			if (nonDefaulted.size() != fields.size()) {
				this.environment.user().addConstructor(innerClass.info, method(ACC_PUBLIC, innerClass.info, "<init>", TypeInfos.VOID, nonDefaulted.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			}
		}
		fields.stream().map(field -> field.info).forEach(this.environment.user()::addField);
		return noop;
	}

	public static final MethodInfo HASH_MIX = method(ACC_PUBLIC | ACC_STATIC | ACC_PURE, HashCommon.class, "mix", int.class, int.class);

	public TypeInfo getMainReturnType() {
		return this.method.info.returnType;
	}

	public InsnTree createReturn(InsnTree value) {
		return return_(value.cast(this, this.getMainReturnType(), CastMode.IMPLICIT_THROW));
	}

	public static boolean isNumber(char c) {
		return c >= '0' && c <= '9';
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