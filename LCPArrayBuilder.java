package com.suffixindex.algorithm;

/**
 * Builds the Longest Common Prefix (LCP) Array using Kasai's Algorithm.
 * Time Complexity: O(n) linear time.
 * Space Complexity: O(n) auxiliary memory for the rank array.
 */
public class LCPArrayBuilder {

    /**
     * Builds the LCP array given the text and its precomputed Suffix Array.
     *
     * @param text The source text (must not be null)
     * @param suffixArray The sorted suffix array of the text
     * @return int array where lcp[i] is the length of the longest common prefix between
     *         the suffix at suffixArray[i] and suffixArray[i - 1]. lcp[0] is always 0.
     */
    public static int[] buildLCPArray(String text, int[] suffixArray) {
        if (text == null || suffixArray == null) {
            throw new IllegalArgumentException("Text and SuffixArray cannot be null");
        }

        int n = text.length();
        if (n == 0) {
            return new int[0];
        }
        if (n == 1) {
            return new int[]{0};
        }

        // Compute rank array: rank[suffixArray[i]] = i
        // rank[i] gives the position of suffix starting at i in the suffix array
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[suffixArray[i]] = i;
        }

        int[] lcp = new int[n];
        int h = 0; // Length of current longest common prefix

        for (int i = 0; i < n; i++) {
            if (rank[i] > 0) {
                // k is the start position of the suffix that directly precedes suffix i in Suffix Array
                int k = suffixArray[rank[i] - 1];

                // Extend common prefix match
                while (i + h < n && k + h < n && text.charAt(i + h) == text.charAt(k + h)) {
                    h++;
                }

                lcp[rank[i]] = h;

                // For the next suffix (i + 1), the LCP is at least h - 1
                if (h > 0) {
                    h--;
                }
            } else {
                lcp[0] = 0; // First suffix has no predecessor
            }
        }

        return lcp;
    }
}
