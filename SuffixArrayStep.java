package com.suffixindex.model;

import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates an intermediate snapshot of the Prefix Doubling algorithm for the DSA visualizer.
 */
public class SuffixArrayStep {

    public static class TupleInfo {
        private final int suffixIndex;
        private final int rank1;
        private final int rank2;
        private final int newRank;

        public TupleInfo(int suffixIndex, int rank1, int rank2, int newRank) {
            this.suffixIndex = suffixIndex;
            this.rank1 = rank1;
            this.rank2 = rank2;
            this.newRank = newRank;
        }

        public int getSuffixIndex() {
            return suffixIndex;
        }

        public int getRank1() {
            return rank1;
        }

        public int getRank2() {
            return rank2;
        }

        public int getNewRank() {
            return newRank;
        }

        public String getRankPairString() {
            return "(" + rank1 + ", " + (rank2 >= 0 ? String.valueOf(rank2) : "-1") + ")";
        }
    }

    private final int stepNumber;
    private final int k;
    private final String description;
    private final List<TupleInfo> tuples;
    private final int[] currentSuffixArray;
    private final boolean complete;

    public SuffixArrayStep(int stepNumber, int k, String description,
                           List<TupleInfo> tuples, int[] currentSuffixArray, boolean complete) {
        this.stepNumber = stepNumber;
        this.k = k;
        this.description = description;
        this.tuples = tuples;
        this.currentSuffixArray = currentSuffixArray != null ? Arrays.copyOf(currentSuffixArray, currentSuffixArray.length) : new int[0];
        this.complete = complete;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public int getK() {
        return k;
    }

    public String getDescription() {
        return description;
    }

    public List<TupleInfo> getTuples() {
        return tuples;
    }

    public int[] getCurrentSuffixArray() {
        return currentSuffixArray;
    }

    public boolean isComplete() {
        return complete;
    }
}
