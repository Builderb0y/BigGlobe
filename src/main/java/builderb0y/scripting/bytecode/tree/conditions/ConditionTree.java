package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;

/**
represents a condition, or branch, which jumps to a new
code location when a specific condition is met or not met.
*/
public interface ConditionTree extends Opcodes {

	/**
	if both labels are non-null, jumps to ifTrue if our condition evaluates to true,
	and jumps to ifFalse if our condition evaluates to false.

	if ifTrue is non-null and ifFalse is null,
	jumps to ifTrue if our condition evaluates to true,
	but does not jump if our condition evaluates to false.
	in other words, if our condition evaluates to false,
	code will continue executing at the next available instruction after the condition.

	if ifTrue is null and ifFalse is non-null,
	jumps to ifFalse if our condition evaluates to false,
	but does not jump if our condition evaluates to true.
	in other words, if our condition evaluates to true,
	code will continue executing at the next available instruction after the condition.

	if ifTrue and ifFalse are the same label (including the case where both labels are null),
	throws {@link IllegalArgumentException}. see also: {@link #checkLabels(Label, Label)}.
	*/
	public abstract void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse);

	public static void checkLabels(@Nullable Label ifTrue, @Nullable Label ifFalse) {
		if (ifTrue == ifFalse) { //also catches the case where both labels are null.
			throw new IllegalArgumentException("ifTrue and ifFalse cannot both point to the same location.");
		}
	}

	public static int negateOpcode(int opcode) {
		return switch (opcode) {
			case IFEQ      -> IFNE;
			case IFNE      -> IFEQ;
			case IFLT      -> IFGE;
			case IFGE      -> IFLT;
			case IFGT      -> IFLE;
			case IFLE      -> IFGT;
			case IF_ICMPEQ -> IF_ICMPNE;
			case IF_ICMPNE -> IF_ICMPEQ;
			case IF_ICMPLT -> IF_ICMPGE;
			case IF_ICMPGE -> IF_ICMPLT;
			case IF_ICMPGT -> IF_ICMPLE;
			case IF_ICMPLE -> IF_ICMPGT;
			case IF_ACMPEQ -> IF_ACMPNE;
			case IF_ACMPNE -> IF_ACMPEQ;
			case IFNULL    -> IFNONNULL;
			case IFNONNULL -> IFNULL;
			default -> throw new IllegalArgumentException("Invalid jump opcode");
		};
	}

	public static int flipOpcode(int opcode) {
		return switch (opcode) {
			case IFEQ      -> IFEQ;
			case IFNE      -> IFNE;
			case IFLT      -> IFGT;
			case IFGE      -> IFLE;
			case IFGT      -> IFLT;
			case IFLE      -> IFGE;
			case IF_ICMPEQ -> IF_ICMPEQ;
			case IF_ICMPNE -> IF_ICMPNE;
			case IF_ICMPLT -> IF_ICMPGT;
			case IF_ICMPGE -> IF_ICMPLE;
			case IF_ICMPGT -> IF_ICMPLT;
			case IF_ICMPLE -> IF_ICMPGE;
			case IF_ACMPEQ -> IF_ACMPEQ;
			case IF_ACMPNE -> IF_ACMPNE;
			case IFNULL    -> IFNULL;
			case IFNONNULL -> IFNONNULL;
			default -> throw new IllegalArgumentException("Invalid jump opcode");
		};
	}
}