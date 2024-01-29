package builderb0y.scripting.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class BoundInfoHolder {

	public InsnTree loadSelf;

	public BoundInfoHolder(InfoHolder holder, InsnTree loadSelf) {
		this.loadSelf = loadSelf;
		ReflectionData fromData = ReflectionData.forClass(holder.getClass());
		for (Field field : ReflectionData.forClass(this.getClass()).getDeclaredFields()) {
			try {
				if (field.getType() == InsnTree.class) {
					Method transformer = fromData.findDeclaredMethod(field.getName(), InsnTree.class, InsnTree.class);
					field.set(this, transformer.invoke(holder, loadSelf));
				}
			}
			catch (Throwable throwable) {
				throw AutoCodecUtil.rethrow(throwable);
			}
		}
	}
}