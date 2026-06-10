package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.special.CommaSeparatedExpressions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class TagParser {

	public final String tagTypeName, elementTypeName;
	public final TypeInfo tagType, elementType;
	public final MethodInfo bootstrapConstant, nonConstant, isIn;

	public TagParser(String tagTypeName, Class<?> tagClass, String elementTypeName, MethodInfo isIn) {
		this.tagTypeName = tagTypeName;
		this.elementTypeName = elementTypeName;
		this.tagType = isIn.getInvokeTypes()[1];
		this.elementType = isIn.getInvokeTypes()[0];
		this.bootstrapConstant = MethodInfo.findMethod(tagClass, "of", tagClass, MethodHandles.Lookup.class, String.class, Class.class, int.class, String[].class);
		this.nonConstant = MethodInfo.findMethod(tagClass, "of", tagClass, int.class, String[].class);
		this.isIn = isIn;
	}

	public void configure(MutableScriptEnvironment environment, UsageCallback callback) {
		environment
		.addCast(type(String.class), this.tagType, true, this.makeCaster())
		.addKeyword(this.makeKeyword(callback))
		.addMethod(this.makeIsIn(callback));
	}

	public Consumer<MutableScriptEnvironment> configurator(UsageCallback callback) {
		return (MutableScriptEnvironment environment) -> this.configure(environment, callback);
	}

	public CastHandler.Named makeCaster() {
		return new CastHandler.Named(
			"String -> " + this.tagTypeName,
			(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit, boolean nullable) -> {
				if (value.getConstantValue().isConstant()) {
					return ldc(
						this.bootstrapConstant,
						constant(AbstractConstantFactory.flags(parser, nullable)),
						value.getConstantValue()
					);
				}
				else {
					if (implicit) {
						ScriptLogger.LOGGER.warn(ScriptParsingException.appendContext("Non-constant tag; this will be worse on performance. Use an explicit cast to suppress this warning.", parser.input));
					}
					return invokeStatic(
						this.nonConstant,
						ldc(AbstractConstantFactory.flags(parser, nullable)),
						newArrayWithContents(parser, type(String[].class), value)
					);
				}
			}
		);
	}

	public KeywordHandler.Named makeKeyword(UsageCallback callback) {
		return new KeywordHandler.Named(
			this.tagTypeName,
			this.tagTypeName + "(element1, element2, ...)",
			callback,
			(ExpressionParser parser, String name) -> {
				boolean nullable = parser.input.hasOperatorAfterWhitespace("?");
				if (parser.input.peekAfterWhitespace() != '(') {
					if (nullable) throw new ScriptParsingException('\'' + name + "?' must be followed by parentheses for nullable cast. If a nullable cast was not intended, remove the question mark.", parser.input);
					return null;
				}
				CommaSeparatedExpressions expressions = CommaSeparatedExpressions.parse(parser);
				return switch (expressions.arguments().length) {
					case 0 -> {
						yield ldc(
							this.bootstrapConstant,
							constant(AbstractConstantFactory.flags(parser, nullable))
						);
					}
					case 1 -> {
						yield expressions.maybeWrap(expressions.arguments()[0].cast(parser, this.tagType, CastMode.EXPLICIT_THROW, nullable));
					}
					default -> {
						InsnTree[] strings = Arrays.stream(expressions.arguments()).map((InsnTree tree) -> tree.cast(parser, TypeInfos.STRING, CastMode.IMPLICIT_THROW, false)).toArray(InsnTree[]::new);
						if (Arrays.stream(strings).map(InsnTree::getConstantValue).allMatch(ConstantValue::isConstantOrDynamic)) {
							yield ldc(
								this.bootstrapConstant,
								Stream.concat(
									Stream.of(constant(AbstractConstantFactory.flags(parser, nullable))),
									Arrays.stream(strings).map(InsnTree::getConstantValue)
								)
								.toArray(ConstantValue[]::new)
							);
						}
						else {
							yield invokeStatic(
								this.nonConstant,
								ldc(AbstractConstantFactory.flags(parser, nullable)),
								newArrayWithContents(parser, type(String[].class), strings)
							);
						}
					}
				};
			}
		);
	}

	public MethodHandler.Named makeIsIn(UsageCallback callback) {
		return new MethodHandler.Named(
			this.elementType,
			"isIn",
			this.elementTypeName + ".isIn(element1 [, element2, ...])",
			callback,
			(
				ExpressionParser parser,
				InsnTree receiver,
				String name,
				GetMethodMode mode,
				InsnTree... arguments
			)
				-> {
				InsnTree tagArgument;
				boolean needsCasting;
				switch (arguments.length) {
					case 0 -> throw new ScriptParsingException("At least one argument is required", parser.input);
					case 1 -> {
						tagArgument = arguments[0].cast(parser, this.tagType, CastMode.EXPLICIT_THROW, false);
						needsCasting = tagArgument != arguments[0];
					}
					default -> {
						InsnTree[] strings = ScriptEnvironment.castArgumentsSameType(parser, "isIn", TypeInfos.STRING, CastMode.IMPLICIT_THROW, arguments);
						if (strings == null) return null;
						if (Arrays.stream(strings).map(InsnTree::getConstantValue).allMatch(ConstantValue::isConstantOrDynamic)) {
							tagArgument = ldc(
								this.bootstrapConstant,
								Stream.concat(
									Stream.of(constant(AbstractConstantFactory.flags(parser, false))),
									Arrays.stream(strings).map(InsnTree::getConstantValue)
								)
								.toArray(ConstantValue[]::new)
							);
						}
						else {
							tagArgument = invokeStatic(
								this.nonConstant,
								ldc(AbstractConstantFactory.flags(parser, false)),
								newArrayWithContents(parser, type(String[].class), strings)
							);
						}
						needsCasting = strings != arguments;
					}
				}

				return new CastResult(
					this.isIn.isStatic()
						? invokeStatic(this.isIn, receiver, tagArgument)
						: invokeInstance(receiver, this.isIn, tagArgument),
					needsCasting
				);
			}
		);
	}
}