package builderb0y.scripting.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class NumberParser {

	public static final BigInteger
		MIN_RADIX = BigInteger.valueOf(2),
		MAX_RADIX = BigInteger.valueOf(16),
		MAX_PRECISION = BigInteger.valueOf(999999999L); //as specified in BigDecimal.pow().
	public static final MathContext
		DIVIDE_CONTEXT = new MathContext(17, RoundingMode.HALF_EVEN);

	public static BigDecimal parse(ExpressionReader input) throws ScriptParsingException {
		BigInteger firstPart = BigInteger.ZERO;
		while (input.canRead()) {
			char c = input.peek();
			if (c == '_') {
				input.onCharRead(c);
			}
			else if (c >= '0' && c <= '9') {
				input.onCharRead(c);
				firstPart = firstPart.multiply(BigInteger.TEN).add(BigInteger.valueOf(c - '0'));
			}
			else if (c == 'x' || c == 'X') {
				input.onCharRead(c);
				if (firstPart.compareTo(MIN_RADIX) >= 0 && firstPart.compareTo(MAX_RADIX) <= 0) {
					return parseWithRadix(input, firstPart.intValue());
				}
				else {
					StringBuilder message = new StringBuilder("Invalid radix: ").append(firstPart).append(" (must be between 2 and 16)");
					if (firstPart.signum() == 0) {
						message.append("; if you meant to specify a number in hexadecimal, the correct prefix is '16x', not '0x'");
					}
					throw new ScriptParsingException(message.toString(), input);
				}
			}
			else if (c == '.') {
				input.onCharRead(c);
				return parseWithRadixPoint(input, firstPart, 10);
			}
			else if (c == 'p' || c == 'P') {
				input.onCharRead(c);
				return finishPrecision(input, new BigDecimal(firstPart), 10);
			}
			else {
				break;
			}
		}
		return new BigDecimal(firstPart);
	}

	public static BigDecimal parseWithRadix(ExpressionReader input, int radix) throws ScriptParsingException {
		BigInteger intPart = BigInteger.ZERO;
		while (input.canRead()) {
			char c = input.peek();
			int digit;
			if (c == '_') {
				input.onCharRead(c);
			}
			else if ((digit = Character.digit(c, radix)) >= 0) {
				input.onCharRead(c);
				intPart = intPart.multiply(BigInteger.valueOf(radix)).add(BigInteger.valueOf(digit));
			}
			else if (c == '.') {
				input.onCharRead(c);
				return parseWithRadixPoint(input, intPart, radix);
			}
			else if (c == 'p' || c == 'P') {
				input.onCharRead(c);
				return finishPrecision(input, new BigDecimal(intPart), radix);
			}
			else {
				break;
			}
		}
		return new BigDecimal(intPart);
	}

	public static BigDecimal parseWithRadixPoint(ExpressionReader input, BigInteger intPart, int radix) throws ScriptParsingException {
		int fractionalDigits = 0;
		while (input.canRead()) {
			char c = input.peek();
			int digit;
			if (c == '_') {
				input.onCharRead(c);
			}
			else if (c == '.') {
				throw new ScriptParsingException("Multiple radix points", input);
			}
			else if ((digit = Character.digit(c, radix)) >= 0) {
				input.onCharRead(c);
				intPart = intPart.multiply(BigInteger.valueOf(radix)).add(BigInteger.valueOf(digit));
				fractionalDigits++;
			}
			else {
				break;
			}
		}
		if (fractionalDigits == 0) {
			throw new ScriptParsingException("Expected fractional part of number", input);
		}
		BigInteger divide = BigInteger.valueOf(radix).pow(fractionalDigits);
		BigDecimal result = new BigDecimal(intPart).divide(new BigDecimal(divide), DIVIDE_CONTEXT);
		char c = input.peek();
		if (c == 'p' || c == 'P') {
			input.onCharRead(c);
			result = finishPrecision(input, result, radix);
		}
		if (result.scale() <= 0) {
			result = result.setScale(1, RoundingMode.UNNECESSARY);
		}
		return result;
	}

	public static BigInteger parsePrecision(ExpressionReader input) throws ScriptParsingException {
		boolean negative = input.has('-');
		BigInteger precision = BigInteger.ZERO;
		while (input.canRead()) {
			char c = input.peek();
			if (c == '_') {
				input.onCharRead(c);
			}
			else if (c >= '0' && c <= '9') {
				input.onCharRead(c);
				precision = precision.multiply(BigInteger.TEN).add(BigInteger.valueOf(c - '0'));
			}
			else if (c == 'x' || c == 'X') {
				input.onCharRead(c);
				if (precision.compareTo(MIN_RADIX) >= 0 && precision.compareTo(MAX_RADIX) <= 0) {
					return parsePrecisionWithRadix(input, precision.intValue());
				}
				else {
					StringBuilder message = new StringBuilder("Invalid radix: ").append(precision).append(" (must be between 2 and 16)");
					if (precision.signum() == 0) {
						message.append("; if you meant to specify a number in hexadecimal, the correct prefix is '16x', not '0x'");
					}
					throw new ScriptParsingException(message.toString(), input);
				}
			}
			else if (c == '.') {
				input.onCharRead(c);
				throw new ScriptParsingException("Cannot have radix point in precision specifier", input);
			}
			else if (c == 'p' || c == 'P') {
				input.onCharRead(c);
				throw new ScriptParsingException("Cannot have nested precision specifiers", input);
			}
			else {
				break;
			}
		}
		return negative ? precision.negate() : precision;
	}

	public static BigInteger parsePrecisionWithRadix(ExpressionReader input, int radix) throws ScriptParsingException {
		BigInteger precision = BigInteger.ZERO;
		while (input.canRead()) {
			char c = input.peek();
			int digit;
			if (c == '_') {
				input.onCharRead(c);
			}
			else if ((digit = Character.digit(c, radix)) >= 0) {
				input.onCharRead(c);
				precision = precision.multiply(BigInteger.valueOf(radix)).add(BigInteger.valueOf(digit));
			}
			else if (c == '.') {
				input.onCharRead(c);
				throw new ScriptParsingException("Cannot have radix point in precision specifier", input);
			}
			else if (c == 'p' || c == 'P') {
				input.onCharRead(c);
				throw new ScriptParsingException("Cannot have nested precision specifiers", input);
			}
			else {
				break;
			}
		}
		return precision;
	}

	public static BigDecimal finishPrecision(ExpressionReader input, BigDecimal firstPart, int radix) throws ScriptParsingException {
		BigInteger precision = parsePrecision(input);
		if (precision.signum() > 0) {
			if (precision.compareTo(MAX_PRECISION) <= 0) {
				return firstPart.multiply(new BigDecimal(BigInteger.valueOf(radix).pow(precision.intValue())));
			}
			else {
				throw new ScriptParsingException("Precision too large", input);
			}
		}
		else if (precision.signum() < 0) {
			if (precision.compareTo(MAX_PRECISION.negate()) >= 0) {
				return firstPart.divide(new BigDecimal(BigInteger.valueOf(radix).pow(-precision.intValue())), DIVIDE_CONTEXT);
			}
			else {
				throw new ScriptParsingException("Precision too small", input);
			}
		}
		else {
			return firstPart;
		}
	}
}