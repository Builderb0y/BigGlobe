package builderb0y.scripting.util;

import com.google.common.primitives.Primitives;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

public class VariableNameTextifier extends Textifier {

	public Int2ObjectMap<String> localVariables = new Int2ObjectOpenHashMap<>();

	public VariableNameTextifier() {
		super(Opcodes.ASM9);
		this.localVariables.defaultReturnValue("<unknown>");
		this.tab  = "\t";
		this.ltab = "\t\t";
		this.tab2 = "\t\t\t";
		this.tab3 = "\t\t\t\t";
	}

	@Override
	public void visitLdcInsn(Object value) {
		this.stringBuilder.setLength(0);
		this.stringBuilder.append(this.tab2).append("LDC ");
		if (value instanceof String string) {
			Printer.appendString(this.stringBuilder, string);
		}
		else if (value instanceof Type type) {
			this.stringBuilder.append(type.getDescriptor()).append(".class");
		}
		else {
			this.stringBuilder.append(value);
			if (value instanceof Number) {
				this.stringBuilder.append(" //").append(Primitives.unwrap(value.getClass()).getName());
			}
		}
		this.stringBuilder.append('\n');
		this.text.add(this.stringBuilder.toString());
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		this.text.add(
			new VariableLine(
				varIndex,
				this.localVariables,
				this.tab2 + OPCODES[opcode] + ' ' + varIndex + " //"
			)
		);
	}

	@Override
	public void visitIincInsn(int varIndex, int increment) {
		this.text.add(
			new VariableLine(
				varIndex,
				this.localVariables,
				this.tab2 + "IINC " + varIndex + ' ' + increment + " //"
			)
		);
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		this.localVariables.put(index, name);
		super.visitLocalVariable(name, descriptor, signature, start, end, index);
	}

	@Override
	public Textifier createTextifier() {
		return new VariableNameTextifier();
	}

	public static class VariableLine {

		public int index;
		public Int2ObjectMap<String> lookup;
		public String prefix;

		public VariableLine(int index, Int2ObjectMap<String> lookup, String prefix) {
			this.index = index;
			this.lookup = lookup;
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return this.prefix + this.lookup.get(this.index) + '\n';
		}
	}
}