package builderb0y.bigglobe.config;

import org.jetbrains.annotations.NotNull;

import builderb0y.autocodec.fixers.DataFixContext;
import builderb0y.autocodec.fixers.DataFixException;
import builderb0y.autocodec.fixers.VersionedFixer;

public class BigGlobeConfigFixer extends VersionedFixer<BigGlobeConfig> {

	public static final BigGlobeConfigFixer INSTANCE = new BigGlobeConfigFixer("BigGlobeConfigFixer.INSTANCE");

	public BigGlobeConfigFixer(String name) {
		super(name, "Config Version", BigGlobeConfig.CONFIG_VERSION);
	}

	@Override
	public <T_Encoded> int getDefaultVersion(@NotNull DataFixContext<T_Encoded> rootContext) throws DataFixException {
		return 0;
	}

	@Override
	@SuppressWarnings("fallthrough")
	public @NotNull <T_Encoded> DataFixContext<T_Encoded> fixData(@NotNull DataFixContext<T_Encoded> context, int version) throws DataFixException {
		switch (version) {
			default:
				throw new DataFixException(() -> "Unknown config version: " + version);
			case 0:
			case 1:
				this.resetLodRendering(context);
			case 2:
		}
		return context;
	}

	public <T_Encoded> void resetLodRendering(DataFixContext<T_Encoded> context) throws DataFixException {
		context.removeMember("LOD Rendering");
	}
}