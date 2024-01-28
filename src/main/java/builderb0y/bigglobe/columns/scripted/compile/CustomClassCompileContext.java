package builderb0y.bigglobe.columns.scripted.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ParameterNode;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.types.ClassColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ClassColumnValueType.ClassColumnValueField;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues.NamedValue;
import builderb0y.scripting.parsing.UserClassDefiner;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CustomClassCompileContext extends DataCompileContext {

	public final ColumnCompileContext parent;
	public final MemberKeywordHandler newHandler;

	public CustomClassCompileContext(ColumnCompileContext parent, ClassColumnValueType spec) {
		super(parent);
		this.parent = parent;
		this.mainClass = parent.mainClass.newInnerClass(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			Type.getInternalName(ScriptedColumn.class) + '$' + spec.name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.constructor = this.mainClass.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			Arrays.stream(spec.fieldsInOrder)
			.map((ClassColumnValueField field) -> new LazyVarInfo(
				field.name(),
				parent.getTypeContext(field.type()).type()
			))
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
		invokeInstance(load("this", this.mainClass.info), MethodInfo.getConstructor(Object.class)).emitBytecode(this.constructor);
		LoadInsnTree loadSelf = load(new LazyVarInfo("this", this.mainClass.info));
		List<FieldCompileContext> fieldCompileContexts = new ArrayList<>(spec.fieldsInOrder.length);
		for (ClassColumnValueField field : spec.fieldsInOrder) {
			FieldCompileContext fieldContext = this.mainClass.newField(ACC_PUBLIC, field.name(), parent.getTypeContext(field.type()).type());
			putField(
				loadSelf,
				fieldContext.info,
				load(
					field.name(),
					parent.getTypeContext(field.type()).type()
				)
			)
			.emitBytecode(this.constructor);
		}
		UserClassDefiner.addToString(this.mainClass, spec.name, fieldCompileContexts);
		this.newHandler = new MemberKeywordHandler.Named("Constructor for " + spec.name, (ExpressionParser parser, InsnTree receiver, String theStringNew, MemberKeywordMode mode) -> {
			NamedValues namedValues = NamedValues.parse(parser, null, (ExpressionParser theSameParser, String name) -> {
				if (!spec.fields.containsKey(name)) {
					throw new ScriptParsingException("Unknown field: " + name + "; valid fields are: " + spec.fields, theSameParser.input);
				}
			});
			Map<String, InsnTree> lookup = Arrays.stream(namedValues.values()).collect(Collectors.toMap(NamedValue::name, NamedValue::value));
			InsnTree[] args = new InsnTree[this.constructor.info.paramTypes.length];
			List<ParameterNode> parameters = this.constructor.node.parameters;
			for (int index = 0, size = parameters.size(); index < size; index++) {
				String name = parameters.get(index).name;
				InsnTree tree = lookup.get(name);
				if (tree == null) throw new ScriptParsingException("Must specify " + name, parser.input);
				args[index] = tree;
			}
			//todo: create synthetic permute method to preserve left-to-right evaluation order.
			return newInstance(this.constructor.info, args);
		});
		parent
		.root()
		.environment
		.addType(spec.name, this.mainClass.info)
		.addMemberKeyword(this.mainClass.info, "new", this.newHandler);
	}

	@Override
	public InsnTree loadColumn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FieldInfo flagsField(int index) {
		throw new UnsupportedOperationException();
	}
}