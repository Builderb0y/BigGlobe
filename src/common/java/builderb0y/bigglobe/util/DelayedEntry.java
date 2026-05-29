package builderb0y.bigglobe.util;

import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.Identifier;

import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.versions.IdentifierVersions;

@Wrapper
public class DelayedEntry {

	public final @NotNull @VerifyNotEmpty String encoding;
	public final @NotNull Identifier id;

	public DelayedEntry(@NotNull String encoding) {
		this.encoding = encoding.intern();
		if (encoding.charAt(0) == '#') {
			this.id = IdentifierVersions.create(encoding.substring(1));
		}
		else {
			this.id = IdentifierVersions.create(encoding);
		}
	}

	public DelayedEntry(@NotNull Identifier id, boolean isTag) {
		this.id = id;
		this.encoding = (
			isTag
			? '#' + id.toString()
			: id.toString()
		)
		.intern();
	}

	public String encoding() {
		return this.encoding;
	}

	public boolean isTag() {
		return this.encoding.charAt(0) == '#';
	}

	@Override
	public String toString() {
		return this.encoding;
	}
}