/**
 * Created on: 27.01.2012 19:08:23
 */
package ws.palladian.model;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A labeled sequential pattern is a pattern that maps into some label. A pattern consists of a sequence of string,
 * which might be words, part of speech tags, named entities a.s.o.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public final class LabeledSequentialPattern {
    private List<String> lhs;
    private String label;

    /**
     * 
     */
    public LabeledSequentialPattern(List<String> lhs, String label) {
        this.lhs = new ArrayList<String>();
        this.lhs.addAll(lhs);
        this.label = label;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * <p>
     * Calculates whether one {@code LabeledSequentialPattern} is contained within another one. The containment relation
     * is required to make classifications based on {@code LabeledSequentialPatterns} and to calculate a score and a
     * confidence. For further information see [1].
     * </p>
     * <p>
     * [1] ﻿Cong, G., Wang, L., Lin, C. Y., Song, Y. I., & Sun, Y. (2008). Finding question-answer pairs from online
     * forums. Proceedings of the 31st annual international ACM SIGIR conference on Research and development in
     * information retrieval (pp. 467–474). New York, NY, USA: ACM. doi:10.1145/1390334.1390415
     * </p>
     * 
     * @param otherPattern The {@code LabeledSequentialPattern} to compare to.
     * @param containmentThreshold The similarity threshold. If there are more differences between both
     *            {@code LabeledSequentialPattern}s containment is {@code false}.
     * @return {@code true} if this pattern contains {@code otherPattern} or vice versa; {@code false} otherwise.
     */
    public Boolean contains(LabeledSequentialPattern otherPattern, Integer containmentThreshold) {
        if (collectionLevenshteinDistance(otherPattern) <= containmentThreshold) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>
     * Calculates the Levenshtein distance between this {@code LabeledSequentialPattern} and the provided one. The
     * distance is calculated similar as for {@code String}s but compares elements from the patterns against each other.
     * </p>
     * 
     * @param otherPattern The {@code LabeledSequentialPattern} to compare to.
     * @return The Levenshtein distance of the two patterns.
     */
    private Integer collectionLevenshteinDistance(LabeledSequentialPattern otherPattern) {
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        Object s_i; // ith character of s
        Object t_j; // jth character of t
        int cost; // cost

        // Step 1

        n = this.lhs.size();
        m = otherPattern.lhs.size();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];

        // Step 2

        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3

        for (i = 1; i <= n; i++) {

            s_i = this.lhs.get(i - 1);

            // Step 4

            for (j = 1; j <= m; j++) {

                t_j = otherPattern.lhs.get(j - 1);

                // Step 5

                if (s_i.equals(t_j)) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6

                d[i][j] = minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);

            }

        }

        // Step 7

        return d[n][m];
    }

    /**
     * <p>
     * Provides the minimum of three integer values. Used as utility method for the Levensthein distance calculated by
     * {@link #collectionLevenshteinDistance(LabeledSequentialPattern)}.
     * </p>
     * 
     * @param a The first value to compare.
     * @param b The second value to compare.
     * @param c The third value to compare.
     * @return The minimum value from the three provided values.
     */
    private int minimum(int a, int b, int c) {
        int mi;

        mi = a;
        if (b < mi) {
            mi = b;
        }
        if (c < mi) {
            mi = c;
        }
        return mi;

    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("\nObject: " + super.toString());
        stringBuilder.append(" <" + lhs + "> --> " + label);
        return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LabeledSequentialPattern other = (LabeledSequentialPattern)obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (lhs == null) {
            if (other.lhs != null) {
                return false;
            }
        } else if (!lhs.equals(other.lhs)) {
            return false;
        }
        return true;
    }
}
