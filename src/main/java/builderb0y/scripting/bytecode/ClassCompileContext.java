package builderb0y.scripting.bytecode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
	public InheritanceContext inheritance;
	public List<ClassCompileContext> innerClasses;

	public ClassCompileContext(int access, TypeInfo info) {
		this.node = new ClassNode();
		this.info = info;
		this.inheritance = new InheritanceContext();
		this.inheritance.lookup.put(info.toAsmType(), info);
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
		this.inheritance = parent.inheritance;
		this.inheritance.lookup.put(info.toAsmType(), info);
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
		this(access, new TypeInfo(type, name, superClass, superInterfaces, null));
	}

	public ClassCompileContext(
		int access,
		ClassType type,
		String name,
		TypeInfo superClass,
		TypeInfo[] superInterfaces
	) {
		this(access, new TypeInfo(type, Type.getObjectType(name), superClass, superInterfaces, null));
	}

	public byte[] toByteArray() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {

			@Override
			public String getCommonSuperClass(String type1, String type2) {
				InheritanceContext inheritance = ClassCompileContext.this.inheritance;
				return TypeMerger.computeMostSpecificType(
					inheritance.getInheritances(
						Type.getObjectType(type1),
						Type.getObjectType(type2)
					)
				)
				.getInternalName();
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
				invokeSpecial(
					load(thisVar),
					//super constructor access doesn't actually matter for this use case.
					constructor(ACC_PUBLIC, this.info.superClass)
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
		return this.newMethod(method(access, this.info, name, returnType, paramTypes));
	}

	public MethodCompileContext newMethod(MethodInfo info) {
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