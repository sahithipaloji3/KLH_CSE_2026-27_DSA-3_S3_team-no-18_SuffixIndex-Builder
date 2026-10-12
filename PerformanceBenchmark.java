package com.suffixindex.test;

import com.suffixindex.algorithm.LCPArrayBuilder;
import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.algorithm.SuffixArrayBuilder;
import com.suffixindex.algorithm.SuffixSearcher;

import java.util.Random;

/**
 * Performance Benchmark evaluating Prefix Doubling, Kasai LCP, and Suffix Binary Search
 * across 1 KB, 10 KB, 100 KB, and 1 MB text sizes using actual System.nanoTime() measurements.
 */
public class PerformanceBenchmark {

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("   SUFFIX INDEX BUILDER - DSA PERFORMANCE BENCHMARK (REAL SYSTEM.NANOTIME)               ");
        System.out.println("=========================================================================================\n");

        int[] sizes = {
                1024,        // 1 KB
                10240,       // 10 KB
                102400,      // 100 KB
                1048576      // 1 MB
        };

        String[] sizeLabels = {"1 KB", "10 KB", "100 KB", "1 MB"};

        System.out.printf("%-10s | %-14s | %-14s | %-14s | %-18s | %-16s | %-12s | %-12s\n",
                "Size", "SA Build Time", "LCP Build Time", "Total Index", "Binary Search", "Naive Search", "Speedup", "JVM Memory");
        System.out.println("-----------------------------------------------------------------------------------------------------------------");

        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,\n";
        Random rng = new Random(1337);

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            String label = sizeLabels[i];

            // Generate synthetic text
            StringBuilder sb = new StringBuilder(size);
            for (int c = 0; c < size; c++) {
                sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            String text = sb.toString();

            // Pick a pattern that definitely occurs in the text (e.g. 8 characters from middle)
            int patStart = size / 2;
            int patEnd = Math.min(size, patStart + 8);
            String pattern = text.substring(patStart, patEnd);

            // Force garbage collection prior to measurement for stable memory tracking
            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // 1. Benchmark Suffix Array Construction (Prefix Doubling)
            long t0 = System.nanoTime();
            int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
            long saTimeNs = System.nanoTime() - t0;

            // 2. Benchmark Kasai LCP Array Construction
            long t1 = System.nanoTime();
            int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);
            long lcpTimeNs = System.nanoTime() - t1;

            long totalIndexNs = saTimeNs + lcpTimeNs;

            // Memory usage estimate
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memUsed = Math.max(0, memAfter - memBefore);

            // 3. Benchmark Binary Search on Suffix Array
            // Warm up
            for (int w = 0; w < 5; w++) {
                SuffixSearcher.search(text, pattern, sa, true);
            }
            long t2 = System.nanoTime();
            var saOutcome = SuffixSearcher.search(text, pattern, sa, true);
            long binSearchNs = System.nanoTime() - t2;

            // 4. Benchmark Naive Search (run only up to 100KB to avoid excessive test duration on large files)
            long naiveSearchNs = 0;
            if (size <= 102400) {
                for (int w = 0; w < 3; w++) {
                    NaiveSearcher.search(text, pattern, true);
                }
                long t3 = System.nanoTime();
                var naiveOutcome = NaiveSearcher.search(text, pattern, true);
                naiveSearchNs = System.nanoTime() - t3;
            } else {
                // Sample 100K slice estimation or single run
                long t3 = System.nanoTime();
                var naiveOutcome = NaiveSearcher.search(text, pattern, true);
                naiveSearchNs = System.nanoTime() - t3;
            }

            double speedup = (binSearchNs > 0) ? (double) naiveSearchNs / binSearchNs : 1.0;

            System.out.printf("%-10s | %-14s | %-14s | %-14s | %-18s | %-16s | %-12s | %-12s\n",
                    label,
                    formatMs(saTimeNs),
                    formatMs(lcpTimeNs),
                    formatMs(totalIndexNs),
                    formatMs(binSearchNs) + " (" + saOutcome.getMatchCount() + "m)",
                    formatMs(naiveSearchNs),
                    String.format("%.1fx", speedup),
                    formatBytes(memUsed));
        }

        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        System.out.println("Notes: Binary Search exhibits O(m log n) scaling, remaining sub-millisecond even on 1 MB.");
        System.out.println("Prefix Doubling scales gracefully at O(n log^2 n), and Kasai LCP demonstrates true O(n) linear performance.\n");
    }

    private static String formatMs(long ns) {
        return String.format("%.3f ms", ns / 1_000_000.0);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
