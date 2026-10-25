package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public abstract class AbstractUpdaterInsnTree implements UpdateInsnTree {

	public CombinedMode mode;

	public AbstractUpdaterInsnTree(CombinedMode mode) {
		this.mode = mode;
	}

	public AbstractUpdaterInsnTree(UpdateOrder order, boolean isAssignment) {
		this.mode = CombinedMode.of(order, isAssignment);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return switch (this.mode) {
			case VOID, VOID_ASSIGN -> TypeInfos.VOID;
			case PRE, PRE_ASSIGN -> this.getPreType();
			//in some cases, it may be possible to
			//perform some operation to the right operand
			//before the value to store can even be obtained.
			//and in these cases, we cannot safely rely on the right
			//operand actually being stored directly in the lvalue.
			//example:
			//	int x = 0I
			//	float y = 0.0I
			//	int z = y :+ x
			//in this case, x must be cast to a float
			//before the addition operation can be performed.
			//as such, the value of y is not guaranteed to be equal
			//to the value of x as the conversion could be lossy.
			//thanks to Niko for finding this oversight!
			case POST, POST_ASSIGN -> this.getPostType().extendsOrImplements(this.getPreType()) ? this.getPostType() : this.getPreType();
		};
	}

	@Override
	public abstract InsnTree asStatement();

	public static enum CombinedMode {
		VOID(UpdateOrder.VOID, false), //a += b
		PRE(UpdateOrder.PRE, false), //a +: b
		POST(UpdateOrder.POST, false), //a :+ b
		VOID_ASSIGN(UpdateOrder.VOID, true), //a  = b
		PRE_ASSIGN(UpdateOrder.PRE, true), //a =: b
		POST_ASSIGN(UpdateOrder.POST, true); //a := b

		public final UpdateOrder order;
		public final boolean isAssignment;

		CombinedMode(UpdateOrder order, boolean isAssignment) {
			this.order = order;
			this.isAssignment = isAssignment;
		}

		public static CombinedMode of(UpdateOrder order, boolean isAssignment) {
			return switch (order) {
				case VOID -> isAssignment ? CombinedMode.VOID_ASSIGN : CombinedMode.VOID;
				case PRE -> isAssignment ? CombinedMode.PRE_ASSIGN : CombinedMode.PRE;
				case POST -> isAssignment ? CombinedMode.POST_ASSIGN : CombinedMode.POST;
			};
		}

		public boolean isVoid() {
			return this.order == UpdateOrder.VOID;
		}

		public CombinedMode asVoid() {
			return this.isAssignment ? VOID_ASSIGN : VOID;
		}
	}
}