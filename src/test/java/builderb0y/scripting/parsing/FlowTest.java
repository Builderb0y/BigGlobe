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
		assertFail("Not a statement", "if (yes: 1) 2");
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
			for ( byte value in list :
				sum += value
			)
			sum
			"""
		);
		assertSuccess(1 + 2 + 3 + 4 + 5,
			"""
			ArrayList list = new ( 5 ) .$ add ( 1 ) .$ add ( 2 ) .$ add ( 3 ) .$ add ( 4 ) .$ add ( 5 )
			int sum = 0
			for ( byte value in Iterable ( list ) :
				sum += value
			)
			sum
			"""
		);
		assertSuccess(1 * 2 + 3 * 4,
			"""
			HashMap map = new ( 2 ) .$ put ( 1 , 2 ) .$ put ( 3 , 4 )
			int sum = 0
			for ( byte key , byte value in map :
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
		assertFail("Switch value must be single-width int", "switch (1.0: case (1: noop))");
		assertFail("Switch value must be single-width int", "switch ('hi': case (1: noop))");
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
			for ( byte value in list :
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
	public void testRangeLoop() throws ScriptParsingException {
		boolean[] trueFalse = { true, false };
		enum Step {
			NONE,
			CONSTANT,
			VARIABLE;

			public static final Step[] VALUES = values();
		}
		final int lowerBound = 5;
		final int UpperBound = 15;
		for (boolean descending : trueFalse) {
			for (boolean lowerBoundInclusive : trueFalse) {
				for (boolean lowerBoundVariable : trueFalse) {
					for (boolean upperBoundInclusive : trueFalse) {
						for (boolean upperBoundVariable : trueFalse) {
							for (Step step : Step.VALUES) {
								StringBuilder scriptBuilder = new StringBuilder(256);
								scriptBuilder.append("ArrayList list = new()\n");
								if (lowerBoundVariable) scriptBuilder.append("int lowerBound = " + lowerBound + '\n');
								if (upperBoundVariable) scriptBuilder.append("int upperBound = " + UpperBound + '\n');
								if (step == Step.VARIABLE) scriptBuilder.append("int step = 2\n");
								scriptBuilder.append("for (int number in ");
								if (descending) scriptBuilder.append('-');
								scriptBuilder.append("range");
								scriptBuilder.append(lowerBoundInclusive ? '[' : '(');
								scriptBuilder.append(lowerBoundVariable ? "lowerBound" : "" + lowerBound);
								scriptBuilder.append(", ");
								scriptBuilder.append(upperBoundVariable ? "upperBound" : "" + UpperBound);
								scriptBuilder.append(upperBoundInclusive ? ']' : ')');
								if (step != Step.NONE) {
									scriptBuilder.append(" % ");
									scriptBuilder.append(step == Step.VARIABLE ? "step" : "2");
								}
								scriptBuilder.append(": list.add(number))\nlist");

								List<Integer> expected = new ArrayList<>(10);
								for (int number = lowerBound; upperBoundInclusive ? number <= UpperBound : number < UpperBound; number += step != Step.NONE ? 2 : 1) {
									if (number == lowerBound && !lowerBoundInclusive) continue;
									expected.add(number);
								}
								if (descending) Collections.reverse(expected);

								assertSuccess(expected, scriptBuilder.toString());
							}
						}
					}
				}
			}
		}
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
		assertFail("Variable 'x' is already defined in this scope", "for (int x in range[int x := 5, x + 5]: noop)");
		assertFail(
			"""
			Unknown variable: x
			Candidates:

			Actual form: x""",
			"for (int number in range[int x := 5, 10]: print(x))"
		);
	}
}