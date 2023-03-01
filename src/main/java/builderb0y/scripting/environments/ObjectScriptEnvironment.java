package builderb0y.scripting.environments;

import java.lang.reflect.Method;

public class ObjectScriptEnvironment extends ClassScriptEnvironment {

	public ObjectScriptEnvironment() {
		super(Object.class);
	}

	@Override
	public boolean shouldExposeMethod(Method method) {
		return method.getName() == "toString" || method.getName() == "equals" || method.getName() == "hashCode";
	}
}