package builderb0y.scripting.optimization;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.*;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantJumpOptimizer implements MethodOptimizer {

	public static final ConstantJumpOptimizer INSTANCE = new ConstantJumpOptimizer();

	@Override
	public boolean optimize(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode maybeLoadConstant = method.instructions.getFirst(); maybeLoadConstant != null;) {
			AbstractInsnNode next = maybeLoadConstant.getNext();
			ConstantValue constant = getConstant(maybeLoadConstant);
			if (constant != null) {
				AbstractInsnNode maybeJump = getJumpTarget(next);
				Boolean willJump = switch (maybeJump.getOpcode()) {
					case IFEQ -> constant.asInt() == 0;
					case IFNE -> constant.asInt() != 0;
					case IFLE -> constant.asInt() <= 0;
					case IFGE -> constant.asInt() >= 0;
					case IFLT -> constant.asInt() <  0;
					case IFGT -> constant.asInt() >  0;
					case IFNULL -> constant.asJavaObject() == null;
					case IFNONNULL -> constant.asJavaObject() != null;
					default -> null;
				};
				if (willJump != null) {
					if (willJump) {
						method.instructions.set(maybeLoadConstant, new JumpInsnNode(GOTO, ((JumpInsnNode)(maybeJump)).label));
					}
					else {
						LabelNode target = labelNode();
						method.instructions.insert(maybeJump, target);
						method.instructions.set(maybeLoadConstant, new JumpInsnNode(GOTO, target));
					}
					changed = true;
				}
			}
			maybeLoadConstant = next;
		}
		return changed;
	}

	public static @Nullable ConstantValue getConstant(AbstractInsnNode node) {
		return switch (node.getOpcode()) {
			case ICONST_M1 -> constant(-1);
			case ICONST_0 -> constant(0);
			case ICONST_1 -> constant(1);
			case ICONST_2 -> constant(2);
			case ICONST_3 -> constant(3);
			case ICONST_4 -> constant(4);
			case ICONST_5 -> constant(5);
			case BIPUSH, SIPUSH -> constant(((IntInsnNode)(node)).operand);
			case LDC -> ((LdcInsnNode)(node)).cst instanceof Integer i ? constant(i) : null;
			case ACONST_NULL -> constant(null, TypeInfos.OBJECT);
			default -> null;
		};
	}

	public static AbstractInsnNode getJumpTarget(AbstractInsnNode maybeLabel) {
		Set<AbstractInsnNode> starts = Collections.newSetFromMap(new IdentityHashMap<>(4));
		while (true) {
			if (!starts.add(maybeLabel)) {
				throw new IllegalStateException("Detected empty infinite loop in script");
			}
			if (maybeLabel instanceof LabelNode || maybeLabel instanceof LineNumberNode) {
				maybeLabel = maybeLabel.getNext();
			}
			else if (maybeLabel.getOpcode() == GOTO) {
				maybeLabel = ((JumpInsnNode)(maybeLabel)).label;
			}
			else {
				return maybeLabel;
			}
		}
	}
}