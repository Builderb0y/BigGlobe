package builderb0y.scripting.bytecode;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.UncheckedReflection;

public class InheritanceContext {

	public Map<Type, TypeInfo> lookup = new HashMap<>(32);

	public TypeInfo[] getInheritances(Type... names) {
		return CollectionTransformer.convertArray(names, TypeInfo.ARRAY_FACTORY, this::getInheritance);
	}

	public TypeInfo getInheritance(Type name) {
		TypeInfo cached = this.lookup.get(name);
		if (cached == null) {
			cached = this.computeInheritance(name);
			TypeInfo newCached = this.lookup.putIfAbsent(name, cached);
			if (newCached != null) cached = newCached;
		}
		return cached;
	}

	/**
	computes and returns the inheritance data for the provided Type.
	the default implementation of this method will
	load the class represented by the given Type,
	and then reflectively fetch the inheritance data from there.
	subclasses can override this method to not load classes when not applicable.
	for example, if the class is not yet done being defined yet.
	subclasses can also put "to be defined" classes in our {@link #lookup}
	so that the information about that class is returned by
	{@link #getInheritance(Type)} before this method is called.
	this method throws {@link TypeNotPresentException} when the provided Type
	does not correspond to a class that this InheritanceContext knows about.
	*/
	public TypeInfo computeInheritance(Type name) throws TypeNotPresentException {
		return TypeInfo.of(UncheckedReflection.findClass(name.getClassName()));
	}
}