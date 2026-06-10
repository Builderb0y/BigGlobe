package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.scripting.wrappers.BlockWrapper;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.UsageCallback;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MinecraftScriptEnvironment {

	public static KeywordHandler.Named blockStateKeyword(UsageCallback callback) {
		return new KeywordHandler.Named(
			"BlockState",
			"BlockState(block, property1: value1, property2: value2, ...)",
			callback,
			(ExpressionParser parser, String name) -> {
			boolean nullable = parser.input.hasOperatorAfterWhitespace("?");
			if (parser.input.peekAfterWhitespace() != '(') {
				if (nullable) throw new ScriptParsingException("'BlockState?' must be followed by parentheses for nullable cast. If a nullable cast was not intended, remove the question mark.", parser.input);
				return null;
			}
			parser.beginCodeBlock();
			InsnTree state = parser.nextScript();
			if (parser.input.hasOperatorAfterWhitespace(",")) {
				//BlockState(?, b: ?)
				ConstantValue constantBlock = state.getConstantValue();
				if (constantBlock.isConstant() && constantBlock.getTypeInfo().equals(TypeInfos.STRING)) {
					//BlockState('a', b: ?)
					String blockName = (String)(constantBlock.asJavaObject());
					Identifier identifier = IdentifierVersions.create(blockName);
					if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
						Block block = BuiltInRegistries.BLOCK.getValue(identifier);
						Set<String> properties = block.getStateDefinition().getProperties().stream().map(Property::getName).collect(Collectors.toSet());
						List<ConstantValue> constantProperties = new ArrayList<>(16);
						constantProperties.add(constantBlock);
						constantProperties.add(constant(AbstractConstantFactory.flags(parser, nullable)));
						record NonConstantProperty(String name, InsnTree value) {}
						List<NonConstantProperty> nonConstantProperties = new ArrayList<>(8);
						do {
							String property = parser.input.expectIdentifierAfterWhitespace();
							if (!properties.remove(property)) {
								throw new ScriptParsingException("Duplicate or unknown property: " + property, parser.input);
							}
							parser.input.expectOperatorAfterWhitespace(":");
							InsnTree value = parser.nextScript();
							ConstantValue constantValue = value.getConstantValue();
							if (constantValue.isConstantOrDynamic()) {
								//BlockState('a', b: true)
								constantProperties.add(constant(property));
								constantProperties.add(constantValue);
							}
							else {
								//BlockState('a', b: c)
								nonConstantProperties.add(new NonConstantProperty(property, value.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW, false)));
							}
						}
						while (parser.input.hasOperatorAfterWhitespace(","));
						//System.out.println("[MinecraftScriptEnvironment]:\nConstant properties: " + constantProperties + "\nNon-constant properties: " + nonConstantProperties + "\nMissing properties: " + properties);
						if (constantProperties.size() > 1) {
							state = ldc(BOOTSTRAP_CONSTANT_STATE, constantProperties.toArray(ConstantValue.ARRAY_FACTORY));
						}
						else {
							state = BlockStateWrapper.DEFAULT_CONSTANT_FACTORY.create(parser, state, true, nullable).tree();
						}
						for (NonConstantProperty nonConstantProperty : nonConstantProperties) {
							state = invokeStatic(nullable ? BlockStateWrapper.WITH_NULLABLE : BlockStateWrapper.WITH, state, ldc(nonConstantProperty.name), nonConstantProperty.value);
						}
					}
					else {
						throw new ScriptParsingException("Unknown block: " + identifier, parser.input);
					}
				}
				else {
					//BlockState(name, b: c)
					state = invokeStatic(
						nullable ? BlockWrapper.GET_DEFAULT_STATE_NULLABLE : BlockWrapper.GET_DEFAULT_STATE,
						BlockWrapper.CONSTANT_FACTORY.create(parser, state, false, nullable).tree()
					);
					Set<String> properties = new HashSet<>(8);
					do {
						String property = parser.input.expectIdentifierAfterWhitespace();
						if (!properties.add(property)) {
							throw new ScriptParsingException("Duplicate property: " + property, parser.input);
						}
						parser.input.expectOperatorAfterWhitespace(":");
						InsnTree value = parser.nextScript().cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW, false);
						state = invokeStatic(nullable ? BlockStateWrapper.WITH_NULLABLE : BlockStateWrapper.WITH, state, ldc(property), value);
					}
					while (parser.input.hasOperatorAfterWhitespace(","));
				}
			}
			else {
				//BlockState('a[b=c]')
				state = BlockStateWrapper.CONSTANT_FACTORY.create(parser, state, false, nullable).tree();
			}
			parser.endCodeBlock();
			return state;
		}
		);
	}

	public static MethodHandler.Named tagRandom(InsnTree loadRandom, Class<?> owner, Class<?> returnType) {
		return Handlers.methodBuilder(owner, "random").resultClass(returnType).addReceiverArgument(owner).addImplicitArgument(loadRandom).buildMethod();
	}

	public static final MethodInfo BOOTSTRAP_CONSTANT_STATE = MethodInfo.getMethod(MinecraftScriptEnvironment.class, "bootstrapConstantState");

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState bootstrapConstantState(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags, Object... properties) {
		int length = properties.length;
		if ((length & 1) != 0) throw new IllegalArgumentException("properties array length must be even.");
		BlockState state = BlockStateWrapper.getDefaultState(id, flags);
		StateDefinition<Block, BlockState> manager = state.getBlock().getStateDefinition();
		for (int index = 0; index < length; index += 2) {
			Property<?> property = manager.getProperty((String)(properties[index]));
			if (property == null) throw new IllegalArgumentException("Cannot set property " + properties[index] + " as it does not exist in " + state.getBlock());
			Comparable<?> value = (Comparable<?>)(properties[index + 1]);
			if (value instanceof String string) {
				value = property.getValue(string).orElse(null);
			}
			else if (value instanceof Integer integer && property.getValueClass() == Boolean.class) {
				value = integer.intValue() != 0;
			}
			if (!property.getValueClass().isInstance(value)) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + properties[index + 1] + " on " + state.getBlock() + ", it is not an allowed value");
			}
			state = state.setValue((Property)(property), (Comparable)(value));
		}
		return state;
	}
}