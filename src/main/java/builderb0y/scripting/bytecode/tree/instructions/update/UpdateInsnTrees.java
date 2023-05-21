package builderb0y.scripting.bytecode.tree.instructions.update;

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

		/**
		for pre-updaters, the TypeInfo is that of the thing we are assigning the value to.
		for example, if we are updating a local variable, then the
		TypeInfo of this updater is the same as that of the variable.
		it is NOT the same as the value being stored in that variable,
		as that could be a subtype of the variable itself.
		*/
		@Override
		public abstract TypeInfo getTypeInfo();

		@Override
		public abstract InsnTree asStatement();
	}

	public interface PostUpdateInsnTree extends UpdateInsnTree {

		/**
		for post-updaters, the TypeInfo is that of the value being stored somewhere.
		for example, if we are updating a local variable, then the TypeInfo of
		this updater is the same as the value we are storing in that variable.
		it is NOT the same as the type of the variable itself,
		as that could be a supertype of the value.
		*/
		@Override
		public abstract TypeInfo getTypeInfo();

		@Override
		public abstract InsnTree asStatement();
	}
}