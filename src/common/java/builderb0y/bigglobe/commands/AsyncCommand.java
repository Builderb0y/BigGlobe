package builderb0y.bigglobe.commands;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public abstract class AsyncCommand implements Runnable, Thread.UncaughtExceptionHandler {

	public final CommandSourceStack source;

	public AsyncCommand(CommandSourceStack source) {
		this.source = source;
	}

	public ScriptedColumn newScriptedColumn() {
		return BigGlobeCommands.generator(this.source).newColumn(this.source.getLevel(), 0, 0, ColumnUsage.GENERIC.normalHints());
	}

	public void start(String input) {
		Thread thread = new Thread(this, '[' + this.source.getTextName() + "]: " + input);
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(this);
		thread.start();
	}

	/**
	returns true if we can still send feedback to our {@link #source}.
	a return value of false indicates that there is
	no need for this command to continue executing.
	*/
	public boolean isValid() {
		//getServer() and isServerStopped() just return fields,
		//and are never overridden. therefore, they are thread-safe.
		if (this.source.getServer().isStopped()) {
			return false;
		}
		//getEntity() also just returns a field, and is never overridden.
		Entity entity = this.source.getEntity();
		if (entity == null) {
			return true; //server console is always valid.
		}
		//allow dead players to continue executing, as long as they haven't disconnected.
		if (entity instanceof ServerPlayer) {
			//hasDisconnected() returns a field, and is never overridden.
			//therefore, it is thread-safe.
			return !((ServerPlayer)(entity)).hasDisconnected();
		}
		else {
			//isAlive() returns a field by default, but is overridden by LivingEntity.
			//LivingEntity queries getHealth(), which queries the EntityDataManager.
			//however, the relevant methods in EntityDataManager are guarded by
			//ReadWriteLock (or more specifically, ReentrantReadWriteLock).
			//therefore, these methods are thread-safe too.
			return entity.isAlive();
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable throwable) {
		BigGlobeMod.LOGGER.error("Error running command: " + thread.getName(), throwable);
		this.source.getServer().execute(() -> {
			if (this.isValid()) {
				this.source.sendFailure(Component.literal(throwable.toString()));
			}
		});
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " started by " + this.source.getTextName();
	}
}