package com.suffixindex.algorithm;

import com.suffixindex.model.SuffixArrayStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds a Suffix Array for a given text using the Prefix Doubling algorithm.
 * Time Complexity: O(n log^2 n) using dual-pivot sorting of rank tuples.
 * Space Complexity: O(n) auxiliary memory.
 *
 * Supports generating step-by-step intermediate execution states for the DSA visualizer.
 */
public class SuffixArrayBuilder {

    /**
     * Internal representation of a suffix rank tuple at step k.
     */
    public static class SuffixTuple implements Comparable<SuffixTuple> {
        public int index;
        public int rank1;
        public int rank2;

        public SuffixTuple(int index, int rank1, int rank2) {
            this.index = index;
            this.rank1 = rank1;
            this.rank2 = rank2;
        }

        @Override
        public int compareTo(SuffixTuple o) {
            if (this.rank1 != o.rank1) {
                return Integer.compare(this.rank1, o.rank1);
            }
            return Integer.compare(this.rank2, o.rank2);
        }
    }

    /**
     * Builds the Suffix Array for the given string using Prefix Doubling.
     * High performance production method.
     *
     * @param text The input text (must not be null)
     * @return int array where element at index i is the starting position of the i-th lexicographical suffix
     */
    public static int[] buildSuffixArray(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Input text cannot be null");
        }

        int n = text.length();
        if (n == 0) {
            return new int[0];
        }
        if (n == 1) {
            return new int[]{0};
        }

        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = (int) text.charAt(i);
        }

        SuffixTuple[] tuples = new SuffixTuple[n];
        for (int i = 0; i < n; i++) {
            tuples[i] = new SuffixTuple(i, rank[i], (i + 1 < n) ? rank[i + 1] : -1);
        }

        for (int k = 1; k < n; k <<= 1) {
            for (int i = 0; i < n; i++) {
                tuples[i].index = i;
                tuples[i].rank1 = rank[i];
                tuples[i].rank2 = (i + k < n) ? rank[i + k] : -1;
            }

            Arrays.sort(tuples);

            int[] newRank = new int[n];
            newRank[tuples[0].index] = 0;
            int currentRank = 0;

            for (int i = 1; i < n; i++) {
                if (tuples[i].rank1 != tuples[i - 1].rank1 || tuples[i].rank2 != tuples[i - 1].rank2) {
                    currentRank++;
                }
                newRank[tuples[i].index] = currentRank;
            }

            rank = newRank;

            if (currentRank == n - 1) {
                break;
            }
        }

        int[] suffixArray = new int[n];
        for (int i = 0; i < n; i++) {
            suffixArray[i] = tuples[i].index;
        }

        return suffixArray;
    }

    /**
     * Builds the Suffix Array while capturing detailed intermediate states for each doubling step.
     * Used by the DSA Visualizer.
     *
     * @param text The input text
     * @return List of SuffixArrayStep capturing the progression from initial ranks to completion.
     */
    public static List<SuffixArrayStep> buildSuffixArrayWithSteps(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<SuffixArrayStep> steps = new ArrayList<>();
        int n = text.length();

        if (n == 1) {
            List<SuffixArrayStep.TupleInfo> single = List.of(new SuffixArrayStep.TupleInfo(0, text.charAt(0), -1, 0));
            steps.add(new SuffixArrayStep(1, 0, "Single character string: Suffix [0] = '" + text + "'", single, new int[]{0}, true));
            return steps;
        }

        // Step 0: Initial character ranks
        int[] rank = new int[n];
        List<SuffixArrayStep.TupleInfo> initTuples = new ArrayList<>(n);
        int[] currentSa = new int[n];

        for (int i = 0; i < n; i++) {
            rank[i] = (int) text.charAt(i);
            currentSa[i] = i;
            initTuples.add(new SuffixArrayStep.TupleInfo(i, rank[i], (i + 1 < n) ? text.charAt(i + 1) : -1, rank[i]));
        }

        steps.add(new SuffixArrayStep(0, 0, "Initial character ranking based on ASCII/Unicode codepoints", initTuples, currentSa, false));

        SuffixTuple[] tuples = new SuffixTuple[n];
        for (int i = 0; i < n; i++) {
            tuples[i] = new SuffixTuple(i, 0, 0);
        }

        int stepCounter = 1;
        for (int k = 1; k < n; k <<= 1) {
            for (int i = 0; i < n; i++) {
                tuples[i].index = i;
                tuples[i].rank1 = rank[i];
                tuples[i].rank2 = (i + k < n) ? rank[i + k] : -1;
            }

            // Sort suffixes based on (rank[i], rank[i + k])
            Arrays.sort(tuples);

            int[] newRank = new int[n];
            newRank[tuples[0].index] = 0;
            int currentRank = 0;

            for (int i = 1; i < n; i++) {
                if (tuples[i].rank1 != tuples[i - 1].rank1 || tuples[i].rank2 != tuples[i - 1].rank2) {
                    currentRank++;
                }
                newRank[tuples[i].index] = currentRank;
            }

            rank = newRank;

            int[] stepSa = new int[n];
            List<SuffixArrayStep.TupleInfo> stepTupleList = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                stepSa[i] = tuples[i].index;
                stepTupleList.add(new SuffixArrayStep.TupleInfo(tuples[i].index, tuples[i].rank1, tuples[i].rank2, newRank[tuples[i].index]));
            }

            boolean isDone = (currentRank == n - 1) || (k << 1 >= n);
            String desc = String.format("Phase %d (k=%d): Comparing 2-tuple ranks [rank[i], rank[i+%d]]. Distinct ranks: %d/%d",
                    stepCounter, k, k, currentRank + 1, n);

            steps.add(new SuffixArrayStep(stepCounter++, k, desc, stepTupleList, stepSa, isDone));

            if (currentRank == n - 1) {
                break;
            }
        }

        return steps;
    }
}
