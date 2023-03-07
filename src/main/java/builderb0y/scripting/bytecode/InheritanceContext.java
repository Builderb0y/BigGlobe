package builderb0y.scripting.bytecode;

import java.util.HashMap;
import java.util.Map;

public class InheritanceContext {

	public Map<String, TypeInfo> lookup = new HashMap<>(4);

	public TypeInfo getInheritance(String name) {
		return this.lookup.get(name);
	}
}