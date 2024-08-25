package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

#if MC_VERSION >= MC_1_21_0
import net.minecraft.structure.StructureLiquidSettings;
#endif

public class StructureTemplateScriptEnvironment {

	public static final ConstantFactory
		TEMPLATE_FACTORY = new ConstantFactory(StructureTemplateScriptEnvironment.class, "getTemplate", String.class, StructureTemplate.class),
		PROCESSOR_FACTORY = new ConstantFactory(StructureTemplateScriptEnvironment.class, "getProcessorList", String.class, StructureProcessorList.class);

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
		.addType("StructureTemplate", StructureTemplate.class)
		.addType("StructurePlacementData", StructurePlacementData.class)

		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "mirror", String.class)
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "rotation", int.class)
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "pivotX", int.class)
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "pivotY", int.class)
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "pivotZ", int.class)
		.addMethodInvokeStatic(StructureTemplateScriptEnvironment.class, "pivotPos")
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "spawnEntities", boolean.class)
		.addFieldGetterSetterStatic(StructurePlacementData.class, StructureTemplateScriptEnvironment.class, "placeFluids", boolean.class)
		.addMethodInvokeStatic(StructureTemplateScriptEnvironment.class, "addProcessors")

		.addCastConstant(TEMPLATE_FACTORY, true)
		.addCastConstant(PROCESSOR_FACTORY, true)
	);

	public static Consumer<MutableScriptEnvironment> create(InsnTree loadWorld) {
		return (MutableScriptEnvironment environment) -> {
			environment
			.addAll(INSTANCE)
			.addFunctionMultiInvoke(loadWorld, WorldWrapper.class, "placeStructureTemplate")
			.addQualifiedFunction(
				type(StructurePlacementData.class),
				"new",
				Handlers.builder(
					WorldWrapper.class,
					"newStructurePlacementData"
				)
				.addImplicitArgument(loadWorld)
				.buildFunction()
			)
			;
		};
	}

	public static StructureTemplate getTemplate(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getTemplate(id);
	}

	public static StructureTemplate getTemplate(String id) {
		if (id == null) return null;
		Identifier identifier = IdentifierVersions.create(id);
		StructureTemplate template = BigGlobeMod.getCurrentServer().getStructureTemplateManager().getTemplate(identifier).orElse(null);
		if (template != null) return template;
		else throw new IllegalArgumentException("Template not found: " + identifier);
	}

	public static StructureProcessorList getProcessorList(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getProcessorList(id);
	}

	public static StructureProcessorList getProcessorList(String id) {
		if (id == null) return null;
		Identifier identifier = IdentifierVersions.create(id);
		StructureProcessorList template = BigGlobeMod.getRegistry(RegistryKeyVersions.processorList()).getById(identifier).value();
		if (template != null) return template;
		else throw new IllegalArgumentException("Template not found: " + identifier);
	}

	public static String mirror(StructurePlacementData data) {
		return Directions.reverseScriptMirror(data.getMirror());
	}

	public static void mirror(StructurePlacementData data, String axis) {
		data.setMirror(Directions.scriptMirror(axis));
	}

	public static int rotation(StructurePlacementData data) {
		return Directions.reverseScriptRotation(data.getRotation());
	}

	public static void rotation(StructurePlacementData data, int rotation) {
		data.setRotation(Directions.scriptRotation(rotation));
	}

	public static int pivotX(StructurePlacementData data) {
		return data.getPosition().getX();
	}

	public static int pivotY(StructurePlacementData data) {
		return data.getPosition().getY();
	}

	public static int pivotZ(StructurePlacementData data) {
		return data.getPosition().getZ();
	}

	public static void pivotX(StructurePlacementData data, int x) {
		data.setPosition(new BlockPos(x, data.getPosition().getY(), data.getPosition().getZ()));
	}

	public static void pivotY(StructurePlacementData data, int y) {
		data.setPosition(new BlockPos(data.getPosition().getX(), y, data.getPosition().getZ()));
	}

	public static void pivotZ(StructurePlacementData data, int z) {
		data.setPosition(new BlockPos(data.getPosition().getX(), data.getPosition().getY(), z));
	}

	public static void pivotPos(StructurePlacementData data, int x, int y, int z) {
		data.setPosition(new BlockPos(x, y, z));
	}

	public static boolean spawnEntities(StructurePlacementData data) {
		return !data.shouldIgnoreEntities();
	}

	public static void spawnEntities(StructurePlacementData data, boolean spawnEntities) {
		data.setIgnoreEntities(!spawnEntities);
	}

	public static boolean placeFluids(StructurePlacementData data) {
		#if MC_VERSION >= MC_1_21_0
			return data.shouldApplyWaterlogging();
		#else
			return data.shouldPlaceFluids();
		#endif
	}

	public static void placeFluids(StructurePlacementData data, boolean placeFluids) {
		#if MC_VERSION >= MC_1_21_0
			data.setLiquidSettings(placeFluids ? StructureLiquidSettings.APPLY_WATERLOGGING : StructureLiquidSettings.IGNORE_WATERLOGGING);
		#else
			data.setPlaceFluids(placeFluids);
		#endif
	}

	public static void addProcessors(StructurePlacementData data, StructureProcessorList processor) {
		data.getProcessors().addAll(processor.getList());
	}
}