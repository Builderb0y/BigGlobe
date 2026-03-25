package builderb0y.bigglobe.columns.scripted2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstructorInfo {

	public final Class<?> clazz;
	public final TypeInfo typeInfo;
	public final MethodInfo methodInfo;
	public final Parameter[] parameters;
	public final Class<?>[] parameterClasses;
	public final TypeInfo[] parameterTypeInfos;
	public final LazyVarInfo[] parameterVarInfos;
	public final LoadInsnTree[] loaders;

	public ConstructorInfo(Class<?> clazz) {
		this.clazz = clazz;
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		if (constructors.length != 1) {
			throw new IllegalArgumentException(clazz + " has more than one constructor!");
		}
		this.typeInfo = type(clazz);
		this.methodInfo = MethodInfo.forConstructor(constructors[0]);
		this.parameters = constructors[0].getParameters();
		this.parameterClasses = Arrays.stream(this.parameters).map(Parameter::getType).toArray(Class<?>[]::new);
		this.parameterTypeInfos = Arrays.stream(this.parameterClasses).map(InsnTrees::type).toArray(TypeInfo[]::new);
		this.parameterVarInfos = Arrays.stream(this.parameters).map((Parameter parameter) -> new LazyVarInfo(parameter.getName(), type(parameter.getType()))).toArray(LazyVarInfo[]::new);
		this.loaders = Arrays.stream(this.parameterVarInfos).map(InsnTrees::load).toArray(LoadInsnTree[]::new);
	}
}