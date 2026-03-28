package builderb0y.bigglobe.columns.scripted.types;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.autocodec.data.StringData;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.CustomClassCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassColumnValueType implements ColumnValueType {

	public final String name;
	public final ClassColumnValueField[] fields;
	public final transient Map<String, ColumnValueType> lookup;

	public ClassColumnValueType(String name, ClassColumnValueField[] fields) {
		this.name = name;
		this.fields = fields;
		this.lookup = Arrays.stream(fields).collect(Collectors.toMap(ClassColumnValueField::name, ClassColumnValueField::type));
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		CustomClassCompileContext customClassContext = new CustomClassCompileContext(context, this);
		return new TypeContext(customClassContext.selfType(), customClassContext);
	}

	@Override
	public InsnTree createConstant(Data data, ColumnCompileContext context) {
		CustomClassCompileContext selfContext = (CustomClassCompileContext)(context.getTypeContext(this).context());
		if (data.isEmpty()) return ldc(null, selfContext.selfType());
		MapData mapData = data.tryAsMap();
		if (mapData == null) throw new ClassCastException("Not a map: " + data);
		Map<String, InsnTree> constants = mapData.value.entrySet().stream().map(
				(Map.Entry<@NotNull Data, @NotNull Data> entry) -> {
					StringData stringKey = entry.getKey().tryAsString();
					if (stringKey != null) return Map.entry(stringKey.value, entry.getValue());
					else throw new ClassCastException("Key is non-string: " + entry.getKey());
				}
			)
											.collect(
												Collectors.toMap(
													Map.Entry<String, Data>::getKey,
													(Map.Entry<String, Data> entry) -> {
														ColumnValueType type = this.lookup.get(entry.getKey());
														if (type != null) return type.createConstant(entry.getValue(), context);
														throw new IllegalArgumentException("Undeclared field specified: " + entry.getKey());
													}
												)
											);
		int length = this.fields.length;
		InsnTree[] args = new InsnTree[length];
		for (int index = 0; index < length; index++) {
			String name = this.fields[index].name;
			InsnTree constant = constants.get(name);
			if (constant == null) throw new IllegalArgumentException("Unspecified field: " + name);
			args[index] = constant;
		}
		return newInstance(selfContext.constructor.info, args);
	}

	@Override
	public void setupInternalEnvironment(MutableScriptEnvironment environment, TypeContext typeContext, DataCompileContext context, MutableDependencyView dependencies) {
		TypeInfo type = typeContext.type();
		environment.addType(this.name, type);
		for (ClassColumnValueField field : this.fields) {
			environment.addFieldGet(
				new FieldInfo(
					ACC_PUBLIC,
					type,
					field.name,
					context.root().getTypeContext(field.type).type()
				)
			);
		}
		MethodInfo constructor = new MethodInfo(
			ACC_PUBLIC,
			type,
			"<init>",
			TypeInfos.VOID,
			Arrays
				.stream(this.fields)
				.map(ClassColumnValueField::type)
				.map(context.root()::getTypeContext)
				.map(TypeContext::type)
				.toArray(TypeInfo.ARRAY_FACTORY)
		);
		environment.addQualifiedConstructor(constructor);
	}

	@Override
	public void setupExternalEnvironment(MutableScriptEnvironment environment, TypeContext typeContext, ColumnCompileContext context, ExternalEnvironmentParams params) {
		this.setupInternalEnvironment(environment, typeContext, context, params.dependencies);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + Arrays.hashCode(this.fields);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ClassColumnValueType that &&
			this.name.equals(that.name) &&
			Arrays.equals(this.fields, that.fields)
		);
	}

	@Override
	public String toString() {
		return "{ type: class, name: " + this.name + ", fields: " + Arrays.toString(this.fields) + " }";
	}

	public static record ClassColumnValueField(@IdentifierName String name, ColumnValueType type) {

	}
}