package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class FlowTest extends TestCommon {

	@Test
	public void testIf() throws ScriptParsingException {
		assertFail("Unreachable statement", "if (yes: return(0)) else (return(1)) return(2)");
		assertSuccess(0, "return ( if ( yes : return ( 0 ) ) else ( return ( 1 ) ) )");
		assertSuccess(1, "if ( yes : noop ) ,, 1");
		assertSuccess(1, "if ( yes : return ( 1 ) ) ,, 2");
		assertSuccess(1, "if ( yes : return ( 1 ) ) ,, return ( 2 )");
		assertFail("Not a statement: ConstantInsnTree of type int (constant: 1 of type int)", "if (yes: 1) 2");
		assertFail("Not a statement", "if (yes: 1) else (2) 3");
		assertSuccess(1,
			"""
			int local = 0
			local = local + 1
			if ( local == 1 :
				return ( 1 )
			)
			else (
				return ( 2 )
			)
			"""
		);
		assertSuccess(1,
			"""
			return(
				if ( yes : return ( 1 ) )
				else ( 2 )
			)
			"""
		);
		assertSuccess(3,
			"""
			int result = 0
			if ( false : result = 1 )
			else result = 2 result = 3
			result
			"""
		);
		assertSuccess(3,
			"""
			int result = 0
			if ( false : result = 1 )
			else result = 2 ,, result = 3
			result
			"""
		);
	}

	@Test
	public void testLoops() throws ScriptParsingException {
		assertSuccess(10,
			"""
			int sum = 0
			int counter = 1
			while ( counter <= 4 :
				sum = sum + counter
				counter = counter + 1
			)
			sum
			"""
		);
		assertSuccess(10,
			"""
			int sum = 0
			int counter = 1
			while ( ( int tmp = counter * counter tmp < 25 ) :
				int tmp = sum + counter
				sum = tmp
				tmp = counter + 1
				counter = tmp
			)
			int tmp = sum
			tmp
			"""
		);
		assertSuccess(1,
			"""
			int counter = 0
			int loop = 0
			do while ( loop < 0 :
				++ counter
			)
			counter
			"""
		);
		assertSuccess(1,
			"""
			int counter = 0
			int loop = 0
			do until ( loop == 0 :
				++ counter
			)
			counter
			"""
		);
		assertSuccess(5,
			"""
			int sum = 0
			repeat ( 5 :
				++ sum
			)
			sum
			"""
		);
		assertSuccess(125,
			"""
			int sum = 0
			repeat ( 5 :
				repeat ( 5 :
					repeat ( 5 :
						sum = sum + 1
					)
				)
			)
			sum
			"""
		);
		assertSuccess(15,
			"""
			int sum = 0
			for ( int tmp = 1 , tmp <= 5 , ++ tmp :
				sum += tmp
			)
			sum
			"""
		);
		assertSuccess(15,
			"""
			int sum = 0
			for ( int forwards = 0 int backwards = 5 , forwards <= 5 , ++ forwards -- backwards :
				sum += backwards
			)
			sum
			"""
		);
	}

	@Test
	void testEnhancedLoops() throws ScriptParsingException {
		assertSuccess(1 + 2 + 3 + 4 + 5,
			"""
			ArrayList list = new ( 5 ) .$ add ( 1 ) .$ add ( 2 ) .$ add ( 3 ) .$ add ( 4 ) .$ add ( 5 )
			int sum = 0
			for ( int value in list :
				sum += value
			)
			sum
			"""
		);
		assertSuccess(1 + 2 + 3 + 4 + 5,
			"""
			ArrayList list = new ( 5 ) .$ add ( 1 ) .$ add ( 2 ) .$ add ( 3 ) .$ add ( 4 ) .$ add ( 5 )
			int sum = 0
			for ( int value in Iterable ( list ) :
				sum += value
			)
			sum
			"""
		);
		assertSuccess(1 * 2 + 3 * 4,
			"""
			HashMap map = new ( 2 ) .$ put ( 1 , 2 ) .$ put ( 3 , 4 )
			int sum = 0
			for ( int key , int value in map :
				sum += key * value
			)
			sum
			"""
		);
		assertSuccess(1 * 2 + 3 * 4,
			"""
			HashMap map = new ( 2 ) .$ put ( 1 , 2 ) .$ put ( 3 , 4 )
			int sum = 0
			for ( int * ( key , value ) in map :
				sum += key * value
			)
			sum
			"""
		);
		assertSuccess(List.of(4, 5),
			"""
			ArrayList firstList = new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
			ArrayList secondList = new()
			Iterator iterator = firstList.iterator()
			for (int element in iterator:
				if (element == 3: break())
			)
			for (int element in iterator:
				secondList.add(element)
			)
			secondList
			"""
		);
		assertSuccess(
			true,
			"""
			ArrayList list = new().$add('a').$add('b').$add('c')
			int sum = 0
			String concat = ''
			for (int index, String string in list:
				sum += index
				concat = '$concat$string'
			)
			sum == 3 && concat == 'abc'
			"""
		);
		assertSuccess(
			true,
			"""
			LinkedList list = new().$add('a').$add('b').$add('c')
			int sum = 0
			String concat = ''
			for (int index, String string in list:
				sum += index
				concat = '$concat$string'
			)
			sum == 3 && concat == 'abc'
			"""
		);
		assertSuccess(125,
			"""
			int sum = 0
			for ( int * ( x , y , z ) in range [ -2 , 2 ] :
				++ sum
			)
			sum
			"""
		);
	}

	@Test
	public void testSwitch() throws ScriptParsingException {
		assertSuccess(11,
			"""
			switch ( 1 :
				case ( 0 : 10 )
				case ( 1 : 11 )
				case ( 2 : 12 )
				default ( -1 )
			)
			"""
		);
		assertSuccess(-1,
			"""
			switch ( 3 :
				case ( 0 : 10 )
				case ( 1 : 11 )
				case ( 2 : 12 )
				default ( -1 )
			)
			"""
		);
		assertSuccess(10,
			"""
			switch ( 2 :
				case ( 0 : -1 )
				case ( 1 , 2 , 3 : 10 )
				case ( 4 , 5 : -2 )
				default ( -3 )
			)
			"""
		);
		assertFail("Switch must have at least one case", "switch (0: ) 1");
		assertFail("Switch must have at least one case", "switch (0: default (1))");
		assertFail("Switch value must be enum or single-width int", "switch (1.0: case (1: noop))");
		assertFail("Switch value must be enum or single-width int", "switch ('hi': case (1: noop))");
		assertSuccess(1,
			"""
			switch ( int value = 5 value :
				case ( 0 : 0 )
				case ( 1 : 1 )
				default ( value & 1 )
			)
			"""
		);
		assertFail("Not a statement",
			"""
			switch (int value = 5,, value:
				case (0: 0)
				case (1: 1)
				default (value & 1)
			)
			value
			"""
		);
	}

	@Test
	public void testCompound() throws ScriptParsingException {
		assertSuccess(1,
			"""
			int counter = 0
			while ( yes :
				if ( counter == 10 : return ( 1 ) )
				counter = counter + 1
			) ,,
			-1
			"""
		);
		assertSuccess(10,
			"""
			int low = 0
			int high = 100
			while ( yes :
				int mid = ( low + high ) >> 1
				int square = mid ^ 2
				if ( square > 100 : high = mid )
				else if ( square < 100 : low = mid )
				else return ( mid )
			) ,,
			-1
			"""
		);
		assertSuccess(true,
			"""
			List list = ArrayList . new ( 5 )
			list .$ add ( 1 ) .$ add ( 2 ) .$ add ( 3 ) .$ add ( 4 ) .$ add ( 5 )
			for ( int value in list :
				if ( value == 3 : return ( true ) )
			)
			return ( false )
			"""
		);
		assertSuccess(5,
			"""
			int result = 0
			block (
				++ result
				if ( result < 5 : continue ( ) )
			)
			result
			"""
		);
		assertSuccess(5,
			"""
			int result = 0
			while ( true :
				++ result
				if ( result >= 5 : break ( ) )
			)
			result
			"""
		);
		assertSuccess(2 + 4 + 6 + 8 + 10,
			"""
			int sum = 0
			for ( int x = 0 , x <= 10 , ++ x :
				if ( x & 1 != 0 : continue ( ) )
				sum += x
			)
			sum
			"""
		);
	}

	@Test
	public void testNamedLoops() throws ScriptParsingException {
		assertSuccess(List.of(1, 2, 3, 4, 5),
			"""
			List list = ArrayList . new ( )
			for outer ( int outer = 1 , outer <= 5 , ++ outer :
				for ( int inner = 1 , inner <= 5 , ++ inner :
					if ( outer == 2 : break ( outer ) )
					list . add ( inner )
				)
			)
			return ( list )
			"""
		);
		assertSuccess(List.of(1, 2, 3, 4, 5),
			"""
			List list = ArrayList . new ( )
			int outer = 1
			while outer ( outer <= 5 :
				for ( int inner = 1 , inner <= 5 , ++ inner :
					if ( outer == 2 : break ( outer ) )
					list . add ( inner )
				)
				++ outer
			)
			return ( list )
			"""
		);
		assertSuccess(List.of(1, 2, 3, 4, 5),
			"""
			List list = ArrayList . new ( )
			int outer = 1
			block outer (
				for ( int inner = 1 , inner <= 5 , ++ inner :
					if ( outer == 2 : break ( outer ) )
					list . add ( inner )
				)
				++ outer
				continue ( )
			)
			return ( list )
			"""
		);
	}

	@Test
	public void testRangeLoop() {
		runTestWithTimeLimit(10_000L, () -> {
			boolean[] trueFalse = { true, false };
			enum Step {
				NONE,
				CONSTANT,
				VARIABLE;

				public static final Step[] VALUES = values();
			}
			final int lowerBound = 5;
			final int UpperBound = 15;
			for (boolean isFloat : trueFalse) {
				for (boolean isLong : trueFalse) {
					String type = isFloat ? (isLong ? "double" : "float") : (isLong ? "long" : "int");
					char suffix = isLong ? 'L' : 'I';
					for (boolean descending : trueFalse) {
						for (boolean lowerBoundInclusive : trueFalse) {
							for (boolean lowerBoundVariable : trueFalse) {
								for (boolean upperBoundInclusive : trueFalse) {
									for (boolean upperBoundVariable : trueFalse) {
										for (Step step : Step.VALUES) {
											StringBuilder scriptBuilder = new StringBuilder(256);
											scriptBuilder.append("ArrayList list = new()\n");
											if (lowerBoundVariable) scriptBuilder.append(type).append(" lowerBound = ").append(lowerBound).append(suffix).append('\n');
											if (upperBoundVariable) scriptBuilder.append(type).append(" upperBound = ").append(UpperBound).append(suffix).append('\n');
											if (step == Step.VARIABLE) scriptBuilder.append(type).append(" step = 2").append(suffix).append('\n');
											scriptBuilder.append("for (").append(type).append(" number in ");
											if (descending) scriptBuilder.append('-');
											scriptBuilder.append("range");
											scriptBuilder.append(lowerBoundInclusive ? '[' : '(');
											scriptBuilder.append(lowerBoundVariable ? "lowerBound" : "" + lowerBound + suffix);
											scriptBuilder.append(", ");
											scriptBuilder.append(upperBoundVariable ? "upperBound" : "" + UpperBound + suffix);
											scriptBuilder.append(upperBoundInclusive ? ']' : ')');
											if (step != Step.NONE) {
												scriptBuilder.append(" % ");
												scriptBuilder.append(step == Step.VARIABLE ? "step" : "2" + suffix);
											}
											scriptBuilder.append(": list.add(number))\nlist");

											List<Number> expected = new ArrayList<>(11);
											for (int number = lowerBound; upperBoundInclusive ? number <= UpperBound : number < UpperBound; number += step != Step.NONE ? 2 : 1) {
												if (number == lowerBound && !lowerBoundInclusive) continue;
												expected.add(
													isFloat
													? (isLong ? ((Number)(Double.valueOf(number))) : ((Number)(Float.valueOf(number))))
													: (isLong ? ((Number)(Long.valueOf(number))) : ((Number)(Integer.valueOf(number))))
												);
											}
											if (descending) Collections.reverse(expected);

											assertSuccess(expected, scriptBuilder.toString());

											//make sure loop doesn't loop infinitely.
											scriptBuilder.setLength(0);
											if (lowerBoundVariable) scriptBuilder.append(type).append(" lowerBound = ").append(lowerBound).append(suffix).append('\n');
											if (upperBoundVariable) scriptBuilder.append(type).append(" upperBound = ").append(UpperBound).append(suffix).append('\n');
											if (step == Step.VARIABLE) scriptBuilder.append(type).append(" step = 2").append(suffix).append('\n');
											scriptBuilder.append("for (").append(type).append(" number in ");
											if (descending) scriptBuilder.append('-');
											scriptBuilder.append("range");
											scriptBuilder.append(lowerBoundInclusive ? '[' : '(');
											scriptBuilder.append(lowerBoundVariable ? "lowerBound" : "" + lowerBound + suffix);
											scriptBuilder.append(", ");
											scriptBuilder.append(upperBoundVariable ? "upperBound" : "" + UpperBound + suffix);
											scriptBuilder.append(upperBoundInclusive ? ']' : ')');
											if (step != Step.NONE) {
												scriptBuilder.append(" % ");
												scriptBuilder.append(step == Step.VARIABLE ? "step" : "2" + suffix);
											}
											scriptBuilder.append(": continue())\ntrue");

											evaluate(scriptBuilder.toString());
										}
									}
								}
							}
						}
					}
				}
			}
		});
	}

	@Test
	public void testRangeLoopScopes() throws ScriptParsingException {
		assertSuccess(List.of(1, 2, 3, 4),
			"""
			ArrayList list = new(4)
			int a = 1
			int b = 2
			int c = 3
			int d = 4
			for (int x in range[a, b]: list.add(x))
			for (int x in range[c, d]: list.add(x))
			list
			"""
		);
		assertFail("Variable 'x' has not been assigned to yet.", "for (int x in range[int x := 5, x + 5]: noop)");
		assertFail(
			"""
			Unknown variable: x
			Candidates:""",
			"for (int number in range[int x := 5, 10]: print(x))"
		);
	}

	@Test
	public void testInfiniteLoops() {
		TestCommon.runTestWithTimeLimit(10000L, () -> {
			assertSuccess(1 + 2 + 3 + 4 + 5,
				"""
				ArrayList list = new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
				int sum = 0
				for (int number in list:
					sum += number
					continue()
				)
				sum
				"""
			);
			assertSuccess(1 + 2 + 3 + 4 + 5,
				"""
				LinkedList list = new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
				int sum = 0
				for (int number in list:
					sum += number
					continue()
				)
				sum
				"""
			);
			assertSuccess(1 + 2 + 3 + 4 + 5,
				"""
				Iterable list = ArrayList.new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
				int sum = 0
				for (int number in list:
					sum += number
					continue()
				)
				sum
				"""
			);
			assertSuccess(1 + 2 + 3 + 4 + 5,
				"""
				Iterator list = ArrayList.new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i).iterator()
				int sum = 0
				for (int number in list:
					sum += number
					continue()
				)
				sum
				"""
			);
			assertSuccess(0 + 1 + 2 + 3 + 4,
				"""
				ArrayList list = new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
				int sum = 0
				for (int index, int number in list:
					sum += index
					continue()
				)
				sum
				"""
			);
			assertSuccess(0 + 1 + 2 + 3 + 4,
				"""
				LinkedList list = new().$add(1i).$add(2i).$add(3i).$add(4i).$add(5i)
				int sum = 0
				for (int index, int number in list:
					sum += index
					continue()
				)
				sum
				"""
			);
			assertSuccess(1 + 2 + 3,
				"""
				HashMap map = new().$put('a', 1i).$put('b', 2i).$put('c', 3i)
				int sum = 0
				for (String key, int value in map:
					sum += value
					continue()
				)
				sum
				"""
			);
			assertSuccess(1 + 2 + 3 + 4 + 5,
				"""
				int sum = 0
				for (int value = 1, value <= 5, ++value:
					sum += value
					continue()
				)
				sum
				"""
			);
		});
	}

	@Test
	public void testMultiLoops() throws ScriptParsingException {
		assertSuccess(true,
			"""
			class Point(int x int y)
			ArrayList expected = new(4)
				.$add(Point.new(1, 1))
				.$add(Point.new(1, 2))
				.$add(Point.new(2, 1))
				.$add(Point.new(2, 2))
			ArrayList actual = new(4)
			for (int x, int y in range[1, 2]:
				actual.add(Point.new(x, y))
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			class Point(int x int y)
			ArrayList expected = new()
			ArrayList actual = new()
			for (int x in range[0, 5]:
				for (int y in range[0, 5]:
					if (y & 1 == 0: continue())
					expected.add(Point.new(x, y))
				)
			)
			for (int x, int y in range[0, 5]:
				if (y & 1 == 0: continue())
				actual.add(Point.new(x, y))
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			class Point(int x int y)
			ArrayList expected = new()
			ArrayList actual = new()
			for outer (int x in range[0, 4):
				for inner (int y in range[0, 4):
					if (x == 2 && y == 2: break(outer))
					expected.add(Point.new(x, y))
				)
			)
			for (int x, int y in range[0, 4):
				if (x == 2 && y == 2: break())
				actual.add(Point.new(x, y))
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			class Point(int a int b int c int d)
			ArrayList expected = new()
			ArrayList actual = new()
			block outer (
				for (int a in range[0, 5):
					for (int b in range[0, 5):
						for (int c in range[0, 5):
							for (int d in range[0, 5):
								if (c & 1 != 0 || d & 1 != 0: continue())
								if (a == 2 && b == 2 && c == 2 && d == 2: break(outer))
								expected.add(Point.new(a, b, c, d))
							)
						)
					)
				)
			)
			block outer (
				for (int a, int b in range[0, 5):
					for (int c, int d in range[0, 5):
						if (c & 1 != 0 || d & 1 != 0: continue())
						if (a == 2 && b == 2 && c == 2 && d == 2: break(outer))
						actual.add(Point.new(a, b, c, d))
					)
				)
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			class Point(int a int b int c int d)
			ArrayList expected = new()
			ArrayList actual = new()
			for (int a in range[0, 5):
				for (int b in range[0, 5):
					for middle (int c in range[0, 5):
						for (int d in range[0, 5):
							if (c & 1 != 0 || d & 1 != 0: continue())
							if (c == 2 && d == 2: break(middle))
							expected.add(Point.new(a, b, c, d))
						)
					)
				)
			)
			for (int a, int b in range[0, 5):
				for (int c, int d in range[0, 5):
					if (c & 1 != 0 || d & 1 != 0: continue())
					if (c == 2 && d == 2: break())
					actual.add(Point.new(a, b, c, d))
				)
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			class Point(int a int b int c int d)
			ArrayList expected = new()
			ArrayList actual = new()
			for (int a in range[0, 5):
				for middle (int b in range[0, 5):
					for (int c in range[0, 5):
						for (int d in range[0, 5):
							if (c == 2 && d == 2: continue(middle))
							expected.add(Point.new(a, b, c, d))
						)
					)
				)
			)
			for outer (int a, int b in range[0, 5):
				for (int c, int d in range[0, 5):
					if (c == 2 && d == 2: continue(outer))
					actual.add(Point.new(a, b, c, d))
				)
			)
			expected == actual
			"""
		);
		assertSuccess(true,
			"""
			HashMap original = new()
			HashMap copy = new()
			for (int i in range[0, 5):
				ArrayList list = new()
				for (int j in range[0, 5): list.add(j))
				original.put(i, list)
				copy.put(i, ArrayList.new())
			)
			for (Integer key, List value in original, Integer element in value:
				copy.get(key).as(ArrayList).add(element)
			)
			original == copy
			"""
		);
		assertFail(
			"Variable 'x' has not been assigned to yet.",
			"for (int x in range[-x, x]: noop) 0"
		);
	}
}