package builderb0y.bigglobe.scripting;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.environments.BuiltinScriptEnvironment;

public interface ScriptErrorCatcher {

	public abstract long getNextErrorTime();

	public abstract void setNextErrorTime(long time);

	public abstract @Nullable String getDebugName();

	public abstract @Nullable String getSource();

	public default void onError(Throwable throwable) {
		long time = System.currentTimeMillis();
		if (time >= this.getNextErrorTime()) {
			this.setNextErrorTime(time + 5000L);
			StringBuilder mainMessage = new StringBuilder().append("Caught exception from ").append(this.getClass().getName());
			if (this.getDebugName() != null) mainMessage.append(" (").append(this.getDebugName()).append(')');
			mainMessage.append(": ").append(throwable).append("; Check your logs for more info.");
			BuiltinScriptEnvironment.PRINTER.println(mainMessage.toString());
			if (this.getSource() != null) {
				ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
			}
			ScriptLogger.LOGGER.error("Exception was: ", throwable);
		}
	}

	public abstract class Impl implements ScriptErrorCatcher {

		public long nextErrorTime;

		@Override
		public long getNextErrorTime() {
			return this.nextErrorTime;
		}

		@Override
		public void setNextErrorTime(long time) {
			this.nextErrorTime = time;
		}
	}
}