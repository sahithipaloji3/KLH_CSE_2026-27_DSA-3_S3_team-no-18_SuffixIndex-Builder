package com.suffixindex.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Searches for substring occurrences using Binary Search on a precomputed Suffix Array.
 * Time Complexity: O(m * log n) where m is pattern length and n is text length.
 */
public class SuffixSearcher {

    public static class SearchOutcome {
        private final List<Integer> positions;
        private final long executionTimeNs;
        private final int comparisonCount;

        public SearchOutcome(List<Integer> positions, long executionTimeNs, int comparisonCount) {
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
     * Compares the prefix of the suffix starting at suffixStart with pattern.
     * Compares at most pattern.length() characters.
     */
    private static int comparePrefix(String text, int suffixStart, String pattern, boolean caseSensitive, int[] comparisons) {
        int n = text.length();
        int m = pattern.length();

        for (int i = 0; i < m; i++) {
            comparisons[0]++;
            if (suffixStart + i >= n) {
                // Suffix ended before pattern finished, so suffix is strictly smaller
                return -1;
            }
            char cText = text.charAt(suffixStart + i);
            char cPat = pattern.charAt(i);

            if (!caseSensitive) {
                cText = Character.toLowerCase(cText);
                cPat = Character.toLowerCase(cPat);
            }

            if (cText != cPat) {
                return Character.compare(cText, cPat);
            }
        }
        // First m characters matched exactly
        return 0;
    }

    /**
     * Searches for pattern in text using binary search over the suffix array.
     *
     * @param text The source text
     * @param pattern The pattern to search for
     * @param suffixArray The precomputed suffix array
     * @param caseSensitive Whether the search is case-sensitive
     * @return SearchOutcome containing positions, elapsed nanoseconds, and comparison count
     */
    public static SearchOutcome search(String text, String pattern, int[] suffixArray, boolean caseSensitive) {
        long startTime = System.nanoTime();

        if (text == null || pattern == null || suffixArray == null || pattern.isEmpty() || text.length() < pattern.length()) {
            long duration = System.nanoTime() - startTime;
            return new SearchOutcome(Collections.emptyList(), duration, 0);
        }

        int n = text.length();
        int[] comparisons = new int[1];

        // 1. Lower Bound Binary Search: find first index in suffixArray matching pattern prefix
        int low = 0;
        int high = n - 1;
        int firstIndex = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = comparePrefix(text, suffixArray[mid], pattern, caseSensitive, comparisons);

            if (cmp >= 0) {
                if (cmp == 0) {
                    firstIndex = mid;
                }
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        // If pattern prefix was not found at all, early exit
        if (firstIndex == -1) {
            long duration = System.nanoTime() - startTime;
            return new SearchOutcome(Collections.emptyList(), duration, comparisons[0]);
        }

        // 2. Upper Bound Binary Search: find last index in suffixArray matching pattern prefix
        low = firstIndex;
        high = n - 1;
        int lastIndex = firstIndex;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = comparePrefix(text, suffixArray[mid], pattern, caseSensitive, comparisons);

            if (cmp == 0) {
                lastIndex = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        // Collect all matching start positions
        List<Integer> positions = new ArrayList<>(lastIndex - firstIndex + 1);
        for (int i = firstIndex; i <= lastIndex; i++) {
            positions.add(suffixArray[i]);
        }

        // Sort positions ascending for sequential presentation in text
        Collections.sort(positions);

        long duration = System.nanoTime() - startTime;
        return new SearchOutcome(positions, duration, comparisons[0]);
    }

    public static List<Integer> search(String text, int[] suffixArray, String pattern) {
        return search(text, pattern, suffixArray, true).getPositions();
    }

    public static List<Integer> search(String text, String pattern, int[] suffixArray) {
        return search(text, pattern, suffixArray, true).getPositions();
    }
}
