package builderb0y.scripting.environments;

import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.CastingSupport.MultiCastProvider;

public class RootScriptEnvironment extends MultiScriptEnvironment {

	public static final int
		USER_INDEX    = 0,
		BUILTIN_INDEX = 1,
		MUTABLE_INDEX = 2;

	public MultiCastProvider castProviders;

	public RootScriptEnvironment() {
		this.castProviders = new MultiCastProvider();
		this.environments.add(new UserScriptEnvironment());
		this.environments.add(BuiltinScriptEnvironment.INSTANCE);
		this.environments.add(new MutableScriptEnvironment());
		this.castProviders.add(CastingSupport.BUILTIN_CAST_PROVIDERS);
	}

	public RootScriptEnvironment(RootScriptEnvironment from) {
		super(from);
		this.environments.set(USER_INDEX, new UserScriptEnvironment());
		this.environments.set(MUTABLE_INDEX, new MutableScriptEnvironment().addAll(from.mutable()));
		this.castProviders = new MultiCastProvider(from.castProviders);
	}

	public UserScriptEnvironment user() {
		return (UserScriptEnvironment)(this.environments.get(USER_INDEX));
	}

	public BuiltinScriptEnvironment builtin() {
		return (BuiltinScriptEnvironment)(this.environments.get(BUILTIN_INDEX));
	}

	public MutableScriptEnvironment mutable() {
		return (MutableScriptEnvironment)(this.environments.get(MUTABLE_INDEX));
	}

	public RootScriptEnvironment user(UserScriptEnvironment user) {
		this.environments.set(USER_INDEX, user);
		return this;
	}

	public RootScriptEnvironment builtin(BuiltinScriptEnvironment builtin) {
		this.environments.set(BUILTIN_INDEX, builtin);
		return this;
	}

	public RootScriptEnvironment mutable(MutableScriptEnvironment mutable) {
		this.environments.set(MUTABLE_INDEX, mutable);
		return this;
	}
}