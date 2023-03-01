package builderb0y.scripting.bytecode;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.reflect.Proxy;
import java.util.*;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.bytecode.TypeMergerTest.A1;
import builderb0y.scripting.bytecode.TypeMergerTest.A2;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeMerger;

import static org.junit.jupiter.api.Assertions.*;

@A1
@A2
public class TypeMergerTest {

	@Test
	public void testMostSpecifics() {
		this.testMostSpecifics(Set.of(int.class), byte.class, short.class, int.class);
		this.testMostSpecifics(Set.of(void.class), byte.class, int.class, Object.class, void.class);
		this.testMostSpecifics(Set.of(Number.class, Constable.class, ConstantDesc.class, Comparable.class), int.class, Long.class);
		this.testMostSpecifics(Set.of(Number.class, Constable.class, ConstantDesc.class, Comparable.class), Integer.class, long.class);
		this.testMostSpecifics(Set.of(Enum.class), E1.class, E2.class);
		this.testMostSpecifics(Set.of(Record.class), R1.class, R2.class);
		this.testMostSpecifics(Set.of(Object.class), E1.class, R2.class);
		this.testMostSpecifics(Set.of(Annotation.class), A1.class, A2.class);
		this.testMostSpecifics(Set.of(Proxy.class, Annotation.class), TypeMergerTest.class.getDeclaredAnnotation(A1.class).getClass(), TypeMergerTest.class.getDeclaredAnnotation(A2.class).getClass());
		this.testMostSpecifics(Set.of(A1.class), A1.class, TypeMergerTest.class.getDeclaredAnnotation(A1.class).getClass());
		this.testMostSpecifics(Set.of(ConstantDesc.class, Constable.class, Serializable.class, Comparable.class), String.class, Integer.class);
		this.testMostSpecifics(Set.of(AbstractList.class, Serializable.class, Cloneable.class), LinkedList.class, ArrayList.class);
		this.testMostSpecifics(Set.of(Collection.class), List.class, Set.class);
	}

	public void testMostSpecifics(Set<Class<?>> expected, Class<?>... choices) {
		assertEquals(
			CollectionTransformer.convertCollection(expected, HashSet::new, TypeInfo::of),
			TypeMerger.computeMostSpecificTypes(TypeInfo.allOf(choices))
		);
	}

	@Test
	public void testMostSpecific() {
		this.testMostSpecific(int.class, byte.class, short.class, int.class);
		this.testMostSpecific(void.class, byte.class, int.class, Object.class, void.class);
		this.testMostSpecific(Number.class, int.class, Long.class);
		this.testMostSpecific(Number.class, Integer.class, long.class);
		this.testMostSpecific(Enum.class, E1.class, E2.class);
		this.testMostSpecific(Record.class, R1.class, R2.class);
		this.testMostSpecific(Object.class, E1.class, R2.class);
		this.testMostSpecific(I1.class, I2.class, C2.class);
		this.testMostSpecific(Annotation.class, A1.class, A2.class);
		this.testMostSpecific(Proxy.class, TypeMergerTest.class.getDeclaredAnnotation(A1.class).getClass(), TypeMergerTest.class.getDeclaredAnnotation(A2.class).getClass());
		this.testMostSpecific(A1.class, A1.class, TypeMergerTest.class.getDeclaredAnnotation(A1.class).getClass());
		this.testMostSpecific(ConstantDesc.class, String.class, Integer.class);
		this.testMostSpecific(AbstractList.class, LinkedList.class, ArrayList.class);
		this.testMostSpecific(Collection.class, List.class, Set.class);
	}

	public void testMostSpecific(Class<?> expected, Class<?>... choices) {
		assertEquals(TypeInfo.of(expected), TypeMerger.computeMostSpecificType(TypeInfo.allOf(choices)));
		if (expected != void.class) {
			assertEquals(
				Arrays.stream(choices).anyMatch(Class::isPrimitive)
				? TypeInfo.of(Cloneable.class)
				: TypeInfo.of(expected.arrayType()),
				TypeMerger.computeMostSpecificType(
					Arrays
					.stream(choices)
					.map(Class::arrayType)
					.map(TypeInfo::of)
					.toArray(TypeInfo[]::new)
				)
			);
		}
	}

	public enum E1 {}
	public enum E2 {}

	public record R1() {}
	public record R2() {}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface A1 {}
	@Retention(RetentionPolicy.RUNTIME)
	public @interface A2 {}

	public static interface I1 {}

	public static interface I2 extends I1 {}

	public static class C2 implements I1 {}

	@Test
	public void testCommonSuperTypes() {
		assertEquals(
			Set.of(
				TypeInfo.of(AbstractList.class),
				TypeInfo.of(AbstractCollection.class),
				TypeInfo.of(Object.class),
				TypeInfo.of(List.class),
				TypeInfo.of(Collection.class),
				TypeInfo.of(Iterable.class),
				TypeInfo.of(Cloneable.class),
				TypeInfo.of(Serializable.class)
			),
			TypeMerger.computeAllCommonTypes(
				TypeInfo.of(LinkedList.class),
				TypeInfo.of(ArrayList.class)
			)
		);
	}
}