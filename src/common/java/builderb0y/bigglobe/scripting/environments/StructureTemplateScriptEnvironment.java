package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructureTemplateScriptEnvironment {

	public static final ConstantFactory
		TEMPLATE_FACTORY = new ConstantFactory(StructureTemplateScriptEnvironment.class, "getTemplate", String.class, StructureTemplate.class),
		PROCESSOR_FACTORY = new ConstantFactory(StructureTemplateScriptEnvironment.class, "getProcessorList", String.class, StructureProcessorList.class);

	public static final MutableScriptEnvironment INSTANCE = (
		new MutableScriptEnvironment()
			.addType("StructureTemplate", StructureTemplate.class)
			.addType("StructurePlacementData", StructurePlaceSettings.class)

			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "mirror", String.class)
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "rotation", int.class)
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "pivotX", int.class)
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "pivotY", int.class)
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "pivotZ", int.class)
			.addMethodInvokeStatic(StructureTemplateScriptEnvironment.class, "pivotPos")
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "spawnEntities", boolean.class)
			.addFieldGetterSetterStatic(StructurePlaceSettings.class, StructureTemplateScriptEnvironment.class, "placeFluids", boolean.class)
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
					type(StructurePlaceSettings.class),
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

	public static StructureTemplate getTemplate(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return getTemplate(id, flags);
	}

	public static StructureTemplate getTemplate(String id, int flags) {
		if (id == null) return null;
		if ((flags & AbstractConstantFactory.CLIENT) != 0) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw new IllegalArgumentException("Structure templates are not available on the client.");
		}
		else {
			Identifier identifier = IdentifierVersions.create(id);
			if (BigGlobeMod.currentServer != null) {
				StructureTemplate template = BigGlobeMod.currentServer.getStructureManager().get(identifier).orElse(null);
				if (template != null) return template;
				else throw new IllegalArgumentException("Template not found: " + identifier);
			}
			else {
				Identifier adjusted = Identifier.fromNamespaceAndPath(identifier.getNamespace(), "structure/" + identifier.getPath() + ".nbt");
				Optional<Resource> resource = BigGlobeMod.getResourceManager().getResource(adjusted);
				if (resource.isPresent()) return null; //validation only requires that we don't throw.
				else throw new IllegalArgumentException("Template not found: " + identifier);
			}
		}
	}

	public static StructureProcessorList getProcessorList(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return getProcessorList(id, flags);
	}

	public static StructureProcessorList getProcessorList(String id, int flags) {
		if (id == null) return null;
		if ((flags & AbstractConstantFactory.CLIENT) != 0) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw new IllegalArgumentException("Processor lists are not available on the client.");
		}
		else {
			Identifier identifier = IdentifierVersions.create(id);
			StructureProcessorList template = BigGlobeMod.getRegistry(Registries.PROCESSOR_LIST).requireById(identifier).value();
			if (template != null) return template;
			else throw new IllegalArgumentException("Processor list not found: " + identifier);
		}
	}

	public static String mirror(StructurePlaceSettings data) {
		return Directions.reverseScriptMirror(data.getMirror());
	}

	public static void mirror(StructurePlaceSettings data, String axis) {
		data.setMirror(Directions.scriptMirror(axis));
	}

	public static int rotation(StructurePlaceSettings data) {
		return Directions.reverseScriptRotation(data.getRotation());
	}

	public static void rotation(StructurePlaceSettings data, int rotation) {
		data.setRotation(Directions.scriptRotation(rotation));
	}

	public static int pivotX(StructurePlaceSettings data) {
		return data.getRotationPivot().getX();
	}

	public static int pivotY(StructurePlaceSettings data) {
		return data.getRotationPivot().getY();
	}

	public static int pivotZ(StructurePlaceSettings data) {
		return data.getRotationPivot().getZ();
	}

	public static void pivotX(StructurePlaceSettings data, int x) {
		data.setRotationPivot(new BlockPos(x, data.getRotationPivot().getY(), data.getRotationPivot().getZ()));
	}

	public static void pivotY(StructurePlaceSettings data, int y) {
		data.setRotationPivot(new BlockPos(data.getRotationPivot().getX(), y, data.getRotationPivot().getZ()));
	}

	public static void pivotZ(StructurePlaceSettings data, int z) {
		data.setRotationPivot(new BlockPos(data.getRotationPivot().getX(), data.getRotationPivot().getY(), z));
	}

	public static void pivotPos(StructurePlaceSettings data, int x, int y, int z) {
		data.setRotationPivot(new BlockPos(x, y, z));
	}

	public static boolean spawnEntities(StructurePlaceSettings data) {
		return !data.isIgnoreEntities();
	}

	public static void spawnEntities(StructurePlaceSettings data, boolean spawnEntities) {
		data.setIgnoreEntities(!spawnEntities);
	}

	public static boolean placeFluids(StructurePlaceSettings data) {

		return data.shouldApplyWaterlogging();
	}

	public static void placeFluids(StructurePlaceSettings data, boolean placeFluids) {

		data.setLiquidSettings(placeFluids ? LiquidSettings.APPLY_WATERLOGGING : LiquidSettings.IGNORE_WATERLOGGING);
	}

	public static void addProcessors(StructurePlaceSettings data, StructureProcessorList processor) {
		data.getProcessors().addAll(processor.list());
	}
}