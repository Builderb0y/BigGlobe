package builderb0y.scripting.bytecode.tree.instructions.update2;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class UpdateInsnTrees {

	public static interface VoidUpdateInsnTree extends UpdateInsnTree {

		@Override
		public default TypeInfo getTypeInfo() {
			return TypeInfos.VOID;
		}
	}

	public static interface PreUpdateInsnTree extends UpdateInsnTree {

		@Override
		public default TypeInfo getTypeInfo() {
			return this.getPreType();
		}

		@Override
		public abstract InsnTree asStatement();
	}

	public interface PostUpdateInsnTree extends UpdateInsnTree {

		@Override
		public default TypeInfo getTypeInfo() {
			return this.getPostType();
		}

		@Override
		public abstract InsnTree asStatement();
	}
}