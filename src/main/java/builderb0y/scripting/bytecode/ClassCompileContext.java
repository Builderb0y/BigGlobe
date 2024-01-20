package builderb0y.scripting.bytecode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;
import builderb0y.scripting.util.VariableNameTextifier;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassCompileContext {

	public static final ObjectArrayFactory<String> STRING_ARRAY_FACTORY = new ObjectArrayFactory<>(String.class);

	public ClassNode node;
	public TypeInfo info;
	public Map<String, TypeInfo> definedClasses;
	public List<ClassCompileContext> innerClasses;
	public int memberUniquifier;
	public List<Object> constants = new ArrayList<>();

	public ClassCompileContext(int access, TypeInfo info) {
		this.node = new ClassNode();
		this.info = info;
		this.definedClasses = new HashMap<>(2);
		this.definedClasses.put(info.getInternalName(), info);
		this.node.visit(
			V17,
			access,
			info.getInternalName(),
			null,
			info.superClass.getInternalName(),
			CollectionTransformer.convertArray(info.superInterfaces, STRING_ARRAY_FACTORY, TypeInfo::getInternalName)
		);
		this.node.visitSource("Script", null);
		this.innerClasses = new ArrayList<>(2);
	}

	public ClassCompileContext(ClassCompileContext parent, int access, TypeInfo info) {
		this.node = new ClassNode();
		this.info = info;
		this.definedClasses = parent.definedClasses;
		this.definedClasses.put(info.getInternalName(), info);
		this.node.visit(
			V17,
			access,
			info.getInternalName(),
			null,
			info.superClass.getInternalName(),
			CollectionTransformer.convertArray(info.superInterfaces, STRING_ARRAY_FACTORY, TypeInfo::getInternalName)
		);
		this.node.visitSource(info.getSimpleName(), null);
		this.innerClasses = new ArrayList<>(0);
	}

	public ClassCompileContext(
		int access,
		ClassType type,
		Type name,
		TypeInfo superClass,
		TypeInfo[] superInterfaces
	) {
		this(access, new TypeInfo(type, name, superClass, superInterfaces, null, false));
	}

	public ClassCompileContext(
		int access,
		ClassType type,
		String name,
		TypeInfo superClass,
		TypeInfo[] superInterfaces
	) {
		this(access, new TypeInfo(type, Type.getObjectType(name), superClass, superInterfaces, null, false));
	}

	public ConstantValue newConstant(Object value, TypeInfo type) {
		int which = this.constants.size();
		this.constants.add(value);
		return ConstantValue.dynamic(
			type,
			ScriptClassLoader.GET_CONSTANT,
			ConstantValue.of(which)
		);
	}

	public byte[] toByteArray() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {

			@Override
			public String getCommonSuperClass(String type1, String type2) {
				TypeInfo info1 = ClassCompileContext.this.definedClasses.get(type1);
				TypeInfo info2 = ClassCompileContext.this.definedClasses.get(type2);
				if (info1 == null) info1 = TypeInfo.parseInternalName(type1, 0, type1.length());
				if (info2 == null) info2 = TypeInfo.parseInternalName(type2, 0, type2.length());
				return TypeMerger.computeMostSpecificType(info1, info2).getInternalName();
			}
		};
		this.node.accept(writer);
		return writer.toByteArray();
	}

	public String dump() {
		StringWriter writer = new StringWriter(8192);
		this.node.accept(new TraceClassVisitor(null, new VariableNameTextifier(), new PrintWriter(writer)));
		return writer.toString();
	}

	public void addNoArgConstructor(int access) {
		MethodCompileContext constructor = this.newMethod(access, "<init>", TypeInfos.VOID);
		LazyVarInfo self = new LazyVarInfo("this", constructor.clazz.info);
		return_(
			invokeInstance(
				load(self),
				//super constructor access doesn't actually matter for this use case.
				new MethodInfo(ACC_PUBLIC, this.info.superClass, "<init>", TypeInfos.VOID)
			)
		)
		.emitBytecode(constructor);
		constructor.endCode();
	}

	public void addToString(String string) {
		MethodCompileContext toString = this.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING);
		return_(ldc(string)).emitBytecode(toString);
	}

	public MethodCompileContext newMethod(int access, String name, TypeInfo returnType, LazyVarInfo... parameters) {
		access |= this.node.access & ACC_INTERFACE;
		MethodInfo info = new MethodInfo(access, this.info, name, returnType, CollectionTransformer.convertArray(parameters, TypeInfo[]::new, LazyVarInfo::type));
		MethodNode method = new MethodNode(info.access(), info.name, info.getDescriptor(), null, null);
		this.node.methods.add(method);
		return new MethodCompileContext(this, method, info, CollectionTransformer.convertArray(parameters, String[]::new, LazyVarInfo::name));
	}

	public FieldCompileContext newField(FieldInfo info) {
		return this.newField(info.access, info.name, info.type);
	}

	public FieldCompileContext newField(int access, String name, TypeInfo type) {
		FieldCompileContext field = new FieldCompileContext(this, access, name, type);
		this.node.fields.add(field.node);
		return field;
	}

	public String innerClassName(String simpleName) {
		return this.info.getInternalName() + '$' + simpleName + '_' + this.innerClasses.size();
	}

	public ClassCompileContext newInnerClass(int access, String name, TypeInfo superClass, TypeInfo[] superInterfaces) {
		return this.newInnerClass(access, TypeInfo.makeClass(Type.getObjectType(name), superClass, superInterfaces));
	}

	public ClassCompileContext newInnerClass(int access, TypeInfo type) {
		ClassCompileContext inner = new ClassCompileContext(this, access, type);
		this.innerClasses.add(inner);
		this.node.visitInnerClass(type.getInternalName(), this.info.getInternalName(), type.getSimpleName(), access);
		inner.node.visitOuterClass(this.info.getInternalName(), null, null);
		return inner;
	}
}