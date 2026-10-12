package com.suffixindex.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Searches for substring occurrences using a naive character-by-character sliding window.
 * Time Complexity: O(n * m) worst case.
 * Used for direct benchmarking against Suffix Array Binary Search.
 */
public class NaiveSearcher {

    public static class NaiveSearchOutcome {
        private final List<Integer> positions;
        private final long executionTimeNs;
        private final int comparisonCount;

        public NaiveSearchOutcome(List<Integer> positions, long executionTimeNs, int comparisonCount) {
            this.positions = positions;
            this.executionTimeNs = executionTimeNs;
            this.comparisonCount = comparisonCount;
        }

        public List<Integer> getPositions() {
            return positions;
        }

        public long getExecutionTimeNs() {
            return executionTimeNs;
        }

        public double getExecutionTimeMs() {
            return executionTimeNs / 1_000_000.0;
        }

        public int getComparisonCount() {
            return comparisonCount;
        }

        public int getMatchCount() {
            return positions.size();
        }
    }

    /**
     * Executes naive search scanning every text index i from 0 to n - m.
     */
    public static NaiveSearchOutcome search(String text, String pattern, boolean caseSensitive) {
        long startTime = System.nanoTime();

        if (text == null || pattern == null || pattern.isEmpty() || text.length() < pattern.length()) {
            long duration = System.nanoTime() - startTime;
            return new NaiveSearchOutcome(Collections.emptyList(), duration, 0);
        }

        int n = text.length();
        int m = pattern.length();
        List<Integer> positions = new ArrayList<>();
        int comparisons = 0;

        for (int i = 0; i <= n - m; i++) {
            boolean match = true;
            for (int j = 0; j < m; j++) {
                comparisons++;
                char cText = text.charAt(i + j);
                char cPat = pattern.charAt(j);

                if (!caseSensitive) {
                    cText = Character.toLowerCase(cText);
                    cPat = Character.toLowerCase(cPat);
                }

                if (cText != cPat) {
                    match = false;
                    break;
                }
            }
            if (match) {
                positions.add(i);
            }
        }

        long duration = System.nanoTime() - startTime;
        return new NaiveSearchOutcome(positions, duration, comparisons);
    }

    public static List<Integer> search(String text, String pattern) {
        return search(text, pattern, true).getPositions();
    }
}
