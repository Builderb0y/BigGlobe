package builderb0y.bigglobe.columns.scripted2;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.objectweb.asm.Type;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnCompileContext {

	public ColumnEntryRegistry registry;
	public ClassCompileContext clazz;
	public MethodCompileContext constructor;
	public Map<ColumnEntry, Object> memberContexts;
	public int flagsIndex;

	public ColumnCompileContext(ColumnEntryRegistry registry) {
		this.registry = registry;
		this.memberContexts = new Reference2ReferenceOpenHashMap<>();
		this.clazz = new ClassCompileContext(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			ClassType.CLASS,
			Type.getInternalName(ScriptedColumn.class) + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			type(ScriptedColumn.class),
			TypeInfo.ARRAY_FACTORY.empty()
		);
		{
			this.constructor = this.clazz.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				ScriptedColumn.CONSTRUCTOR_INFO.parameterVarInfos
			);
			LazyVarInfo self = new LazyVarInfo("this", this.clazz.info);
			invokeInstance(
				load(self),
				new MethodInfo(
					ACC_PUBLIC,
					type(ScriptedColumn.class),
					"<init>",
					TypeInfos.VOID,
					ScriptedColumn.CONSTRUCTOR_INFO.parameterTypeInfos
				),
				ScriptedColumn.CONSTRUCTOR_INFO.loaders
			)
			.emitBytecode(this.constructor);
		}
		{
			MethodCompileContext lookup = this.clazz.newMethod(ACC_PUBLIC | ACC_STATIC, "lookup", type(MethodHandles.Lookup.class));
			return_(invokeStatic(MethodInfo.findMethod(MethodHandles.class, "lookup", MethodHandles.Lookup.class))).emitBytecode(lookup);
			lookup.endCode();
		}
		{
			MethodCompileContext blankCopy = this.clazz.newMethod(ACC_PUBLIC, "blankCopy", type(ScriptedColumn.class));
			InsnTree loadSelf = load("this", this.clazz.info);
			return_(
				newInstance(
					this.constructor.info,
					Arrays
					.stream(ScriptedColumn.CONSTRUCTOR_INFO.parameters)
					.map(Parameter::getName)
					.map((String name) -> FieldInfo.getField(ScriptedColumn.class, name))
					.map((FieldInfo field) -> getField(loadSelf, field))
					.toArray(InsnTree[]::new)
				)
			)
			.emitBytecode(blankCopy);
			blankCopy.endCode();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getCompileContext(ColumnEntry spec) {
		return (T)(this.memberContexts.get(spec));
	}

	public void setCompileContext(ColumnEntry spec, Object value) {
		this.memberContexts.put(spec, value);
	}

	public void link(ScriptClassLoader loader) {
		MethodCompileContext clear = this.clazz.newMethod(ACC_PUBLIC, "clear", TypeInfos.VOID);
		for (int index = 0, max = this.flagsIndex >>> 5; index <= max; index++) {
			FieldCompileContext flagsField = this.clazz.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
			putField(this.loadColumn(), flagsField.info, ldc(0)).emitBytecode(clear);
		}
		clear.node.visitInsn(RETURN);
		clear.endCode();
		this.constructor.node.visitInsn(RETURN);
		this.constructor.endCode();
		loader.recursiveAddClasses(this.clazz, ColumnEntryRegistry.CLASS_DUMP_DIRECTORY, null);
	}

	public static String internalName(Identifier selfID, int uniquifier) {
		StringBuilder builder = (
			new StringBuilder(selfID.getNamespace().length() + selfID.getPath().length() + 16)
			.append(selfID.getNamespace())
			.append('_')
			.append(selfID.getPath())
		);
		for (int index = 0, length = builder.length(); index < length; index++) {
			char old = builder.charAt(index);
			if (!((old >= 'a' && old <= 'z') || (old >= '0' && old <= '9'))) {
				builder.setCharAt(index, '_');
			}
		}
		return builder.append('_').append(uniquifier).toString();
	}

	public int flagsFieldBitmask(int index) {
		//note: *because java*, this is equivalent to 1 << (index & 31).
		//this is one of the very few places where such a weird rule is actually useful.
		return 1 << index;
	}

	public int nextFlagsIndex() {
		return this.flagsIndex++;
	}

	public TypeInfo columnTypeInfo() {
		return this.clazz.info;
	}

	public InsnTree loadColumn() {
		return load("this", this.columnTypeInfo());
	}

	public FieldInfo flagsField(int index) {
		return new FieldInfo(ACC_PUBLIC, this.clazz.info, "flags_" + (index >>> 5), TypeInfos.INT);
	}
}