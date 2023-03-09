package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class StructurePieceTypeWrapper {

	public static final TypeInfo TYPE = type(StructurePieceType.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructurePieceTypeWrapper.class, "of", String.class, StructurePieceType.class);

	public static StructurePieceType of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static StructurePieceType of(String id) {
		StructurePieceType type = Registry.STRUCTURE_PIECE.get(new Identifier(id));
		if (type != null) return type;
		else throw new IllegalArgumentException("Unknown structure piece type: " + id);
	}
}