package com.suffixindex.service;

import com.suffixindex.algorithm.LCPArrayBuilder;
import com.suffixindex.algorithm.NaiveSearcher;
import com.suffixindex.algorithm.SuffixArrayBuilder;
import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.model.BenchmarkResult;
import com.suffixindex.model.Document;
import com.suffixindex.repository.BenchmarkRepository;
import com.suffixindex.util.DateUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for executing multi-iteration empirical benchmarks comparing
 * Suffix Index structures against Naive text search.
 * Pure Java SE.
 */
public class BenchmarkService {

    private final BenchmarkRepository benchmarkRepository;
    private final DocumentService documentService;

    public BenchmarkService(BenchmarkRepository benchmarkRepository, DocumentService documentService) {
        this.benchmarkRepository = benchmarkRepository;
        this.documentService = documentService;
    }

    /**
     * Executes a benchmark with the given parameters and automatically persists results.
     */
    public BenchmarkResult runBenchmark(String documentId, String pattern, int iterations) throws IOException {
        if (iterations <= 0) {
            iterations = 5;
        }

        Document doc = documentService.getDocument(documentId);
        if (doc == null || doc.getContent() == null) {
            throw new IllegalArgumentException("Document content not found for ID: " + documentId);
        }

        String text = doc.getContent();
        if (pattern == null) {
            pattern = "";
        }

        long[] saBuildTimes = new long[iterations];
        long[] lcpBuildTimes = new long[iterations];
        long[] naiveSearchTimes = new long[iterations];
        long[] saSearchTimes = new long[iterations];

        int matchesCount = 0;
        int[] finalSa = null;

        // Warm-up iteration (JIT compilation warmup)
        SuffixArrayBuilder.buildSuffixArray(text);

        for (int i = 0; i < iterations; i++) {
            // 1. Suffix Array Construction
            long startSa = System.nanoTime();
            int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
            long endSa = System.nanoTime();
            saBuildTimes[i] = endSa - startSa;
            finalSa = sa;

            // 2. Kasai LCP Array Construction
            long startLcp = System.nanoTime();
            int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);
            long endLcp = System.nanoTime();
            lcpBuildTimes[i] = endLcp - startLcp;

            // 3. Naive String Search
            long startNaive = System.nanoTime();
            List<Integer> naiveMatches = NaiveSearcher.search(text, pattern);
            long endNaive = System.nanoTime();
            naiveSearchTimes[i] = endNaive - startNaive;

            // 4. Suffix Array Binary Search
            long startSaSearch = System.nanoTime();
            List<Integer> saMatches = SuffixSearcher.search(text, sa, pattern);
            long endSaSearch = System.nanoTime();
            saSearchTimes[i] = endSaSearch - startSaSearch;

            matchesCount = saMatches.size();
        }

        // Compute min, max, avg
        long saMin = Long.MAX_VALUE, saMax = Long.MIN_VALUE, saTotal = 0;
        long lcpMin = Long.MAX_VALUE, lcpMax = Long.MIN_VALUE, lcpTotal = 0;
        long naiveTotal = 0;
        long saSearchTotal = 0;

        for (int i = 0; i < iterations; i++) {
            if (saBuildTimes[i] < saMin) saMin = saBuildTimes[i];
            if (saBuildTimes[i] > saMax) saMax = saBuildTimes[i];
            saTotal += saBuildTimes[i];

            if (lcpBuildTimes[i] < lcpMin) lcpMin = lcpBuildTimes[i];
            if (lcpBuildTimes[i] > lcpMax) lcpMax = lcpBuildTimes[i];
            lcpTotal += lcpBuildTimes[i];

            naiveTotal += naiveSearchTimes[i];
            saSearchTotal += saSearchTimes[i];
        }

        long saAvg = saTotal / iterations;
        long lcpAvg = lcpTotal / iterations;
        long naiveAvg = naiveTotal / iterations;
        long saSearchAvg = saSearchTotal / iterations;

        BenchmarkResult result = new BenchmarkResult(
                DateUtils.nowFormatted(),
                doc.getName(),
                text.length(),
                pattern,
                iterations,
                saMin, saMax, saAvg,
                lcpMin, lcpMax, lcpAvg,
                naiveAvg, saSearchAvg,
                matchesCount
        );

        // Persist to CSV and TXT log
        benchmarkRepository.saveBenchmark(result);

        return result;
    }

    public List<Path> listBenchmarkFiles() throws IOException {
        return benchmarkRepository.listBenchmarkFiles();
    }
}
