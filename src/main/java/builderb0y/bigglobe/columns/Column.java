package builderb0y.bigglobe.columns;

public abstract class Column {

	public int x, z;
	public int flags;

	public Column(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public void setPosUnchecked(int x, int z) {
		this.x = x;
		this.z = z;
		this.flags = 0;
	}

	public final void setPos(int x, int z) {
		if (this.x != x || this.z != z) {
			this.setPosUnchecked(x, z);
		}
	}

	public final boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	sets the specified flag, and returns true if our
	flags changed as a result of calling this method.
	*/
	public final boolean setFlag(int flag) {
		//old implementation:
		//	return this.flags != (this.flags |= flag);
		//while I do admire how compact this implementation was,
		//it unfortunately suffered from contention issues,
		//because this.flags was always written to even if it didn't change.
		//the new implementation below only writes to this.flags if the flags actually changed.
		int oldFlags = this.flags;
		int newFlags = oldFlags | flag;
		if (oldFlags != newFlags) {
			this.flags = newFlags;
			return true;
		}
		else {
			return false;
		}
	}

	public abstract Column blankCopy();
}