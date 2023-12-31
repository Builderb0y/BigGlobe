package builderb0y.scripting.util;

import org.jetbrains.annotations.NotNull;

/**
the double value should not be used or relied on.
the fields/record components of this class are subject to change in the future.
always treat StringSimilarity as a black box which
can be compared to other StringSimilarity black boxes.
*/
public record StringSimilarity(double value) implements Comparable<StringSimilarity> {

	public static final StringSimilarity NO_MATCH = new StringSimilarity(0.0D);

	public static StringSimilarity compare(String query, String base) {
		int queryLength = query.length();
		int baseLength = base.length();
		double similarity = 0.0D;
		for (int offset = -queryLength; ++offset < baseLength;) {
			int minQuery = Math.max(0, -offset);
			int maxQuery = Math.min(queryLength, baseLength - offset);
			int run = 0;
			for (int queryIndex = minQuery; queryIndex < maxQuery; queryIndex++) {
				if (
					Character.toLowerCase(query.charAt(queryIndex))
					==
					Character.toLowerCase(base.charAt(queryIndex + offset))
				) {
					run++;
				}
				else {
					similarity += run * run;
					run = 0;
				}
			}
			similarity += run * run;
		}
		return similarity == 0.0D ? NO_MATCH : new StringSimilarity(similarity / (queryLength * baseLength));
	}

	@Override
	public int compareTo(@NotNull StringSimilarity that) {
		return Double.compare(this.value, that.value);
	}
}