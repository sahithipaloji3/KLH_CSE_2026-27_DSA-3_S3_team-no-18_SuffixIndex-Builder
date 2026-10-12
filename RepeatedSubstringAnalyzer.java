package com.suffixindex.algorithm;

import java.util.*;

/**
 * Discovers the Longest Repeated Substring (LRS) and prominent repeated patterns
 * in text using the Suffix Array and LCP Array in linear time without brute-force generation.
 */
public class RepeatedSubstringAnalyzer {

    public static class LRSResult {
        private final String longestRepeatedSubstring;
        private final int length;
        private final List<Integer> positions;
        private final int occurrences;

        public LRSResult(String longestRepeatedSubstring, int length, List<Integer> positions) {
            this.longestRepeatedSubstring = longestRepeatedSubstring;
            this.length = length;
            this.positions = positions != null ? positions : Collections.emptyList();
            this.occurrences = this.positions.size();
        }

        public String getLongestRepeatedSubstring() {
            return longestRepeatedSubstring;
        }

        public int getLength() {
            return length;
        }

        public List<Integer> getPositions() {
            return positions;
        }

        public int getOccurrences() {
            return occurrences;
        }
    }

    public static class RepeatedPattern implements Comparable<RepeatedPattern> {
        private final String pattern;
        private final int length;
        private final int occurrences;
        private final List<Integer> positions;

        public RepeatedPattern(String pattern, int length, int occurrences, List<Integer> positions) {
            this.pattern = pattern;
            this.length = length;
            this.occurrences = occurrences;
            this.positions = positions;
        }

        public String getPattern() {
            return pattern;
        }

        public int getLength() {
            return length;
        }

        public int getOccurrences() {
            return occurrences;
        }

        public List<Integer> getPositions() {
            return positions;
        }

        @Override
        public int compareTo(RepeatedPattern o) {
            // Sort by occurrences descending, then length descending
            if (this.occurrences != o.occurrences) {
                return Integer.compare(o.occurrences, this.occurrences);
            }
            if (this.length != o.length) {
                return Integer.compare(o.length, this.length);
            }
            return this.pattern.compareTo(o.pattern);
        }
    }

    /**
     * Finds the Longest Repeated Substring (LRS) using the LCP array in O(n) time.
     */
    public static LRSResult findLongestRepeatedSubstring(String text, int[] suffixArray, int[] lcpArray) {
        if (text == null || text.isEmpty() || suffixArray == null || lcpArray == null || suffixArray.length <= 1) {
            return new LRSResult("", 0, Collections.emptyList());
        }

        int n = suffixArray.length;
        int maxLcp = 0;
        int maxIndex = -1;

        // 1. Find max value in LCP array
        for (int i = 1; i < n; i++) {
            if (lcpArray[i] > maxLcp) {
                maxLcp = lcpArray[i];
                maxIndex = i;
            }
        }

        if (maxIndex == -1 || maxLcp == 0) {
            return new LRSResult("", 0, Collections.emptyList());
        }

        int startPos = suffixArray[maxIndex];
        String lrs = text.substring(startPos, startPos + maxLcp);

        // 2. Find all occurrences using the suffix array contiguous range
        Set<Integer> positionSet = new TreeSet<>();
        positionSet.add(suffixArray[maxIndex]);
        positionSet.add(suffixArray[maxIndex - 1]);

        // Expand left in SA
        for (int i = maxIndex - 2; i >= 0; i--) {
            if (lcpArray[i + 1] >= maxLcp && text.startsWith(lrs, suffixArray[i])) {
                positionSet.add(suffixArray[i]);
            } else {
                break;
            }
        }

        // Expand right in SA
        for (int i = maxIndex + 1; i < n; i++) {
            if (lcpArray[i] >= maxLcp && text.startsWith(lrs, suffixArray[i])) {
                positionSet.add(suffixArray[i]);
            } else {
                break;
            }
        }

        List<Integer> positions = new ArrayList<>(positionSet);
        Collections.sort(positions);

        return new LRSResult(lrs, maxLcp, positions);
    }

    /**
     * Extracts top repeated patterns using LCP array boundaries without brute force expansion.
     *
     * @param text The input text
     * @param suffixArray Precomputed Suffix Array
     * @param lcpArray Precomputed LCP Array
     * @param maxResults Maximum number of distinct patterns to return
     * @return List of prominent RepeatedPattern items
     */
    public static List<RepeatedPattern> findRepeatedPatterns(String text, int[] suffixArray, int[] lcpArray, int maxResults) {
        if (text == null || suffixArray == null || lcpArray == null || suffixArray.length <= 1) {
            return Collections.emptyList();
        }

        int n = suffixArray.length;
        Map<String, Set<Integer>> patternMap = new HashMap<>();

        // Collect prefix repeats identified by non-zero LCPs
        for (int i = 1; i < n; i++) {
            int lcp = lcpArray[i];
            if (lcp > 0) {
                int pos1 = suffixArray[i];
                int pos2 = suffixArray[i - 1];

                // Check full LCP string
                String fullSub = text.substring(pos1, pos1 + lcp);
                if (fullSub.trim().length() >= 1) {
                    patternMap.computeIfAbsent(fullSub, k -> new TreeSet<>()).add(pos1);
                    patternMap.get(fullSub).add(pos2);
                }

                // If LCP is longer, also sample meaningful sub-prefixes (length 1, 2, 3...)
                int[] sampleLengths = {1, 2, 3, 4, 6, 8};
                for (int len : sampleLengths) {
                    if (len < lcp) {
                        String sub = text.substring(pos1, pos1 + len);
                        if (!sub.trim().isEmpty()) {
                            patternMap.computeIfAbsent(sub, k -> new TreeSet<>()).add(pos1);
                            patternMap.get(sub).add(pos2);
                        }
                    }
                }
            }
        }

        List<RepeatedPattern> list = new ArrayList<>();
        for (Map.Entry<String, Set<Integer>> entry : patternMap.entrySet()) {
            String pat = entry.getKey();
            List<Integer> positions = new ArrayList<>(entry.getValue());
            Collections.sort(positions);
            list.add(new RepeatedPattern(pat, pat.length(), positions.size(), positions));
        }

        Collections.sort(list);
        if (list.size() > maxResults) {
            return list.subList(0, maxResults);
        }
        return list;
    }
}
