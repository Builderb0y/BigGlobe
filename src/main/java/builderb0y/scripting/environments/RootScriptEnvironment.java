package builderb0y.scripting.environments;

import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.CastingSupport.MultiCastProvider;

public class RootScriptEnvironment extends MultiScriptEnvironment {

	public MultiCastProvider castProviders;

	public RootScriptEnvironment() {
		this.castProviders = new MultiCastProvider();
		this.environments.add(new UserScriptEnvironment());
		this.environments.add(BuiltinScriptEnvironment.INSTANCE);
		this.environments.add(new MutableScriptEnvironment2());
		this.castProviders.add(CastingSupport.BUILTIN_CAST_PROVIDERS);
	}

	public RootScriptEnvironment(RootScriptEnvironment from) {
		super(from);
		this.environments.set(0, new UserScriptEnvironment(from.user()));
		this.castProviders = new MultiCastProvider(from.castProviders);
	}

	public UserScriptEnvironment user() {
		return (UserScriptEnvironment)(this.environments.get(0));
	}

	public BuiltinScriptEnvironment builtin() {
		return (BuiltinScriptEnvironment)(this.environments.get(1));
	}

	public MutableScriptEnvironment2 mutable() {
		return (MutableScriptEnvironment2)(this.environments.get(2));
	}
}