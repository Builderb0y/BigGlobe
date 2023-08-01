package builderb0y.scripting.bytecode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassCompileContext {

	public static final ObjectArrayFactory<String> STRING_ARRAY_FACTORY = new ObjectArrayFactory<>(String.class);

	public ClassNode node;
	public TypeInfo info;
	public Map<String, TypeInfo> definedClasses;
	public List<ClassCompileContext> innerClasses;

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
		this.node.accept(new TraceClassVisitor(new PrintWriter(writer)));
		return writer.toString();
	}

	public void addNoArgConstructor(int access) {
		this.newMethod(access, "<init>", TypeInfos.VOID).scopes.withScope(method -> {
			VarInfo thisVar = method.addThis();
			return_(
				invokeInstance(
					load(thisVar),
					//super constructor access doesn't actually matter for this use case.
					new MethodInfo(ACC_PUBLIC, this.info.superClass, "<init>", TypeInfos.VOID)
				)
			)
			.emitBytecode(method);
		});
	}

	public void addToString(String toString) {
		this.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING).scopes.withScope(method -> {
			method.addThis();
			return_(ldc(toString)).emitBytecode(method);
		});
	}

	public MethodCompileContext newMethod(int access, String name, TypeInfo returnType, TypeInfo... paramTypes) {
		access |= this.node.access & ACC_INTERFACE;
		return this.newMethod(new MethodInfo(access, this.info, name, returnType, paramTypes));
	}

	public MethodCompileContext newMethod(MethodInfo info) {
		if (!info.owner.equals(this.info)) {
			throw new IllegalArgumentException("Attempt to add method from a different class: Expected " + this.info + ", got " + info.owner);
		}
		MethodNode method = new MethodNode(info.access(), info.name, info.getDescriptor(), null, null);
		this.node.methods.add(method);
		return new MethodCompileContext(this, method, info);
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