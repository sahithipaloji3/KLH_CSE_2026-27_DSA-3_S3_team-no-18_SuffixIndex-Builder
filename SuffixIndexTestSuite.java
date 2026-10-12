package com.suffixindex.test;

import com.suffixindex.algorithm.*;
import com.suffixindex.model.*;
import com.suffixindex.repository.*;
import com.suffixindex.service.*;
import com.suffixindex.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Automated Unit Test Suite validating all core DSA requirements, edge cases, and file persistence.
 */
public class SuffixIndexTestSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   SUFFIX INDEX BUILDER - AUTOMATED TEST SUITE EXECUTION       ");
        System.out.println("===============================================================\n");

        runTest("Test 1: Suffix Array Construction (Prefix Doubling - 'banana')", SuffixIndexTestSuite::test1SuffixArrayBanana);
        runTest("Test 2: Substring Search via Binary Search ('banana', 'ana')", SuffixIndexTestSuite::test2SearchBananaAna);
        runTest("Test 3: Single Character Repetition ('aaaa')", SuffixIndexTestSuite::test3RepeatedCharacters);
        runTest("Test 4: Empty String & Single Character Edge Cases", SuffixIndexTestSuite::test4EdgeCases);
        runTest("Test 5: Index Persistence Roundtrip (Build -> Save -> Reload -> Search)", SuffixIndexTestSuite::test5IndexPersistence);
        runTest("Test 6: Document Persistence Roundtrip (Create -> Save -> Reload -> Verify)", SuffixIndexTestSuite::test6DocumentPersistence);
        runTest("Test 7: Outdated Index Detection (Create -> Build -> Modify -> Check Outdated)", SuffixIndexTestSuite::test7OutdatedIndexDetection);
        runTest("Test 8: Trie Autocomplete Data Structure (Insert -> Suggest -> Search)", SuffixIndexTestSuite::test8TrieAutocomplete);
        runTest("Test 9: Longest Repeated Substring (LRS) Linear Analysis via Suffix Array + LCP", SuffixIndexTestSuite::test9LongestRepeatedSubstring);
        runTest("Test 10: Multi-Document Corpus Search (Aggregate & Rank across Documents)", SuffixIndexTestSuite::test10MultiDocumentSearch);
        runTest("Test 11: Cryptographic SHA-256 Tamper Detection & Integrity Verification", SuffixIndexTestSuite::test11Sha256IntegrityVerification);
        runTest("Test 12: Benchmark Suite Multi-Iteration & CSV Logging", SuffixIndexTestSuite::test12BenchmarkExecutionAndCsvLogging);
        runTest("Test 13: Comprehensive Technical Report Generation & Export", SuffixIndexTestSuite::test13ReportGenerationAndExport);

        System.out.println("\n===============================================================");
        System.out.printf("   TEST SUMMARY: Run: %d | Passed: %d | Failed: %d\n", testsRun, testsPassed, testsFailed);
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase test) {
        testsRun++;
        System.out.printf("[%d] Running %s... ", testsRun, testName);
        try {
            test.execute();
            testsPassed++;
            System.out.println("✓ PASSED");
        } catch (Throwable t) {
            testsFailed++;
            System.out.println("✗ FAILED: " + t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    @FunctionalInterface
    interface TestCase {
        void execute() throws Exception;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Test 1 — Suffix Array:
     * Input: "banana"
     * Expected: [5, 3, 1, 0, 4, 2]
     */
    private static void test1SuffixArrayBanana() {
        String input = "banana";
        int[] expected = {5, 3, 1, 0, 4, 2};
        int[] actual = SuffixArrayBuilder.buildSuffixArray(input);

        assertTrue(Arrays.equals(expected, actual),
                "Suffix array for 'banana' must be [5, 3, 1, 0, 4, 2], got " + Arrays.toString(actual));

        // Also verify Kasai LCP for "banana":
        // Sorted suffixes:
        // 0: "a" (pos 5) -> LCP = 0
        // 1: "ana" (pos 3) -> LCP = 1
        // 2: "anana" (pos 1) -> LCP = 3
        // 3: "banana" (pos 0) -> LCP = 0
        // 4: "na" (pos 4) -> LCP = 0
        // 5: "nana" (pos 2) -> LCP = 2
        int[] expectedLcp = {0, 1, 3, 0, 0, 2};
        int[] actualLcp = LCPArrayBuilder.buildLCPArray(input, actual);
        assertTrue(Arrays.equals(expectedLcp, actualLcp),
                "LCP array for 'banana' must be [0, 1, 3, 0, 0, 2], got " + Arrays.toString(actualLcp));
    }

    /**
     * Test 2 — Search:
     * Input: "banana", Pattern: "ana"
     * Expected: [1, 3]
     */
    private static void test2SearchBananaAna() {
        String text = "banana";
        int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
        var outcome = SuffixSearcher.search(text, "ana", sa, true);

        List<Integer> expected = List.of(1, 3);
        assertEquals(expected, outcome.getPositions(), "Search for 'ana' in 'banana' must find positions [1, 3]");
        assertEquals(2, outcome.getMatchCount(), "Match count must be 2");

        // Non-existent pattern
        var missing = SuffixSearcher.search(text, "xyz", sa, true);
        assertEquals(0, missing.getMatchCount(), "Search for non-existent pattern must return 0 matches");
    }

    /**
     * Test 3 — Single character repeated:
     * Input: "aaaa"
     * Suffixes:
     * 3: "a"
     * 2: "aa"
     * 1: "aaa"
     * 0: "aaaa"
     * SA = [3, 2, 1, 0]
     * LCP = [0, 1, 2, 3]
     */
    private static void test3RepeatedCharacters() {
        String text = "aaaa";
        int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
        int[] expectedSa = {3, 2, 1, 0};
        assertTrue(Arrays.equals(expectedSa, sa),
                "Suffix array for 'aaaa' must be [3, 2, 1, 0], got " + Arrays.toString(sa));

        int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);
        int[] expectedLcp = {0, 1, 2, 3};
        assertTrue(Arrays.equals(expectedLcp, lcp),
                "LCP array for 'aaaa' must be [0, 1, 2, 3], got " + Arrays.toString(lcp));

        var outcome = SuffixSearcher.search(text, "aa", sa, true);
        assertEquals(List.of(0, 1, 2), outcome.getPositions(), "Search for 'aa' in 'aaaa' must return [0, 1, 2]");
    }

    /**
     * Test 4 — Empty string & single character handling.
     */
    private static void test4EdgeCases() {
        int[] saEmpty = SuffixArrayBuilder.buildSuffixArray("");
        assertEquals(0, saEmpty.length, "Empty string suffix array must have length 0");

        int[] lcpEmpty = LCPArrayBuilder.buildLCPArray("", saEmpty);
        assertEquals(0, lcpEmpty.length, "Empty string LCP array must have length 0");

        int[] saSingle = SuffixArrayBuilder.buildSuffixArray("x");
        assertEquals(1, saSingle.length, "Single character suffix array must have length 1");
        assertEquals(0, saSingle[0], "Single character suffix array index must be 0");

        int[] lcpSingle = LCPArrayBuilder.buildLCPArray("x", saSingle);
        assertEquals(1, lcpSingle.length, "Single character LCP array must have length 1");
        assertEquals(0, lcpSingle[0], "Single character LCP must be 0");
    }

    /**
     * Test 5 — Index persistence:
     * Build -> Save -> Reload -> Search
     */
    private static void test5IndexPersistence() throws IOException {
        Path tempDir = Paths.get("data", "scratch_test_index");
        Files.createDirectories(tempDir);

        try {
            IndexRepository repo = new IndexRepository(tempDir);
            String docId = "test_persistence_doc";
            String text = "abracadabra";

            int[] sa = SuffixArrayBuilder.buildSuffixArray(text);
            int[] lcp = LCPArrayBuilder.buildLCPArray(text, sa);

            SuffixIndex originalIndex = new SuffixIndex(docId, text.length(), FileUtils.computeContentHash(text),
                    123456L, sa, lcp);

            // Save to disk
            repo.saveIndex(originalIndex);
            assertTrue(repo.indexExists(docId), "Index file must physically exist on disk");

            // Reload from disk
            SuffixIndex reloaded = repo.loadIndex(docId);
            assertEquals(originalIndex.getDocumentId(), reloaded.getDocumentId(), "Document ID must match");
            assertEquals(originalIndex.getTextLength(), reloaded.getTextLength(), "Text length must match");
            assertEquals(originalIndex.getSuffixCount(), reloaded.getSuffixCount(), "Suffix count must match");
            assertEquals(originalIndex.getContentHash(), reloaded.getContentHash(), "Content hash must match");
            assertTrue(Arrays.equals(originalIndex.getSuffixArray(), reloaded.getSuffixArray()), "Suffix Array data must match exactly");
            assertTrue(Arrays.equals(originalIndex.getLcpArray(), reloaded.getLcpArray()), "LCP Array data must match exactly");

            // Execute search using reloaded index
            var outcome = SuffixSearcher.search(text, "abra", reloaded.getSuffixArray(), true);
            assertEquals(List.of(0, 7), outcome.getPositions(), "Search on reloaded index must return [0, 7]");

        } finally {
            cleanupDirectory(tempDir);
        }
    }

    /**
     * Test 6 — Document persistence:
     * Create -> Save -> Reload repository -> Load -> Verify
     */
    private static void test6DocumentPersistence() throws IOException {
        Path tempDocs = Paths.get("data", "scratch_test_docs");
        Path tempMeta = Paths.get("data", "scratch_test_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_indexes");

        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);

        try {
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            DocumentService service = new DocumentService(docRepo, metaRepo, idxRepo);

            // Create document
            String docName = "sample_test.txt";
            String docContent = "The quick brown fox jumps over the lazy dog.";
            DocumentMetadata created = service.createDocument(docName, docContent);

            // Restart repository instances to simulate application restart
            DocumentRepository docRepo2 = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo2 = new MetadataRepository(tempMeta);
            IndexRepository idxRepo2 = new IndexRepository(tempIdx);
            DocumentService service2 = new DocumentService(docRepo2, metaRepo2, idxRepo2);

            Document reloaded = service2.getDocument(created.getDocumentId());
            assertEquals(docContent, reloaded.getContent(), "Persisted document content must match");
            assertEquals(created.getDocumentName(), reloaded.getName(), "Persisted document name must match");
            assertEquals(docContent.length(), reloaded.length(), "Character count must match");

        } finally {
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
        }
    }

    /**
     * Test 7 — Outdated index detection:
     * Create document -> Build index -> Modify document -> Verify index marked outdated
     */
    private static void test7OutdatedIndexDetection() throws IOException {
        Path tempDocs = Paths.get("data", "scratch_test_outdated_docs");
        Path tempMeta = Paths.get("data", "scratch_test_outdated_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_outdated_idx");

        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);

        try {
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);
            IndexService idxService = new IndexService(docRepo, metaRepo, idxRepo);

            // 1. Create document
            DocumentMetadata meta = docService.createDocument("banana_doc.txt", "banana");
            assertEquals(IndexStatus.NOT_INDEXED, meta.getIndexStatus(), "Initial status must be NOT_INDEXED");

            // 2. Build index
            idxService.buildIndex(meta.getDocumentId());
            Document docAfterIndex = docService.getDocument(meta.getDocumentId());
            assertEquals(IndexStatus.INDEXED, docAfterIndex.getMetadata().getIndexStatus(), "Status must be INDEXED after build");

            // 3. Modify document
            docService.updateDocument(meta.getDocumentId(), "banana_doc.txt", "banana is sweet");
            Document docAfterModify = docService.getDocument(meta.getDocumentId());
            assertEquals(IndexStatus.OUTDATED, docAfterModify.getMetadata().getIndexStatus(),
                    "Status must be OUTDATED after content modification");

            // 4. Verify IndexService detects it as outdated
            assertTrue(idxService.isIndexOutdated(meta.getDocumentId()), "IndexService must report index as outdated");

            // 5. Rebuild index
            idxService.buildIndex(meta.getDocumentId());
            Document docAfterRebuild = docService.getDocument(meta.getDocumentId());
            assertEquals(IndexStatus.INDEXED, docAfterRebuild.getMetadata().getIndexStatus(),
                    "Status must return to INDEXED after rebuild");

        } finally {
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
        }
    }

    /**
     * Test 8 — Trie Autocomplete Data Structure:
     * Insert words, verify search, startsWith, autocomplete suggestions with limit, and deletion.
     */
    private static void test8TrieAutocomplete() {
        Trie trie = new Trie();
        trie.insert("suffix");
        trie.insert("suffixarray");
        trie.insert("substring");
        trie.insert("subsequence");
        trie.insert("prefix");

        assertEquals(5, trie.getWordCount(), "Trie word count must be 5");
        assertTrue(trie.search("suffix"), "Trie must find 'suffix'");
        assertTrue(trie.search("substring"), "Trie must find 'substring'");
        assertTrue(!trie.search("sub"), "'sub' is a prefix but not an inserted terminal word");
        assertTrue(trie.startsWith("sub"), "Trie must acknowledge 'sub' prefix exists");

        // Autocomplete suggestions for "sub"
        List<String> subSuggestions = trie.getAutocompleteSuggestions("sub", 10);
        assertTrue(subSuggestions.contains("substring"), "Suggestions for 'sub' must contain 'substring'");
        assertTrue(subSuggestions.contains("subsequence"), "Suggestions for 'sub' must contain 'subsequence'");
        assertEquals(2, subSuggestions.size(), "Suggestions for 'sub' must have exactly 2 words");

        // Autocomplete suggestions for "suff"
        List<String> suffSuggestions = trie.getAutocompleteSuggestions("suff", 1);
        assertEquals(1, suffSuggestions.size(), "Suggestions with limit 1 must return exactly 1 item");
        assertEquals("suffix", suffSuggestions.get(0), "Top suggestion for 'suff' must be 'suffix'");

        // Deletion
        assertTrue(trie.delete("suffix"), "Deleting existing word must return true");
        assertTrue(!trie.search("suffix"), "Deleted word must no longer be found");
        assertTrue(trie.search("suffixarray"), "Sibling word 'suffixarray' must remain intact");
        assertEquals(4, trie.getWordCount(), "Trie word count must decrement to 4");
    }

    /**
     * Test 9 — Longest Repeated Substring (LRS) Linear Analysis via Suffix Array + LCP:
     * Validates O(n) detection without brute force substring generation.
     */
    private static void test9LongestRepeatedSubstring() {
        // Case A: "banana" -> LRS is "ana", length 3, occurrences [1, 3]
        String textA = "banana";
        int[] saA = SuffixArrayBuilder.buildSuffixArray(textA);
        int[] lcpA = LCPArrayBuilder.buildLCPArray(textA, saA);
        RepeatedSubstringAnalyzer.LRSResult lrsA = RepeatedSubstringAnalyzer.findLongestRepeatedSubstring(textA, saA, lcpA);

        assertEquals("ana", lrsA.getLongestRepeatedSubstring(), "LRS for 'banana' must be 'ana'");
        assertEquals(3, lrsA.getLength(), "LRS length must be 3");
        assertEquals(List.of(1, 3), lrsA.getPositions(), "LRS positions in 'banana' must be [1, 3]");

        // Case B: "mississippi" -> LRS is "issi", length 4, occurrences [1, 4]
        String textB = "mississippi";
        int[] saB = SuffixArrayBuilder.buildSuffixArray(textB);
        int[] lcpB = LCPArrayBuilder.buildLCPArray(textB, saB);
        RepeatedSubstringAnalyzer.LRSResult lrsB = RepeatedSubstringAnalyzer.findLongestRepeatedSubstring(textB, saB, lcpB);

        assertEquals("issi", lrsB.getLongestRepeatedSubstring(), "LRS for 'mississippi' must be 'issi'");
        assertEquals(4, lrsB.getLength(), "LRS length must be 4");
        assertEquals(List.of(1, 4), lrsB.getPositions(), "LRS positions in 'mississippi' must be [1, 4]");

        // Case C: Prominent repeated patterns ranking
        List<RepeatedSubstringAnalyzer.RepeatedPattern> patterns =
                RepeatedSubstringAnalyzer.findRepeatedPatterns(textB, saB, lcpB, 5);
        assertTrue(!patterns.isEmpty(), "Repeated patterns must not be empty");
        assertEquals("i", patterns.get(0).getPattern(), "Most frequent repeated pattern in 'mississippi' must be 'i'");
        assertEquals(4, patterns.get(0).getOccurrences(), "Pattern 'i' must appear 4 times");
    }

    /**
     * Test 10 — Multi-Document Corpus Search:
     * Query multiple indexed documents simultaneously and verify cross-corpus aggregation and sorting.
     */
    private static void test10MultiDocumentSearch() throws IOException {
        Path tempDocs = Paths.get("data", "scratch_test_multi_docs");
        Path tempMeta = Paths.get("data", "scratch_test_multi_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_multi_idx");
        Path tempHist = Paths.get("data", "scratch_test_multi_hist", "search_history.txt");

        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);
        Files.createDirectories(tempHist.getParent());

        try {
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            SearchHistoryRepository histRepo = new SearchHistoryRepository(tempHist);

            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);
            IndexService idxService = new IndexService(docRepo, metaRepo, idxRepo);
            SearchService searchService = new SearchService(docRepo, metaRepo, idxRepo, histRepo);
            MultiDocumentSearchService multiSearch = new MultiDocumentSearchService(docService, idxService, docRepo, idxRepo, histRepo);

            // Document 1: contains "apple" twice
            DocumentMetadata doc1 = docService.createDocument("doc1.txt", "apple pie and apple cider");
            idxService.buildIndex(doc1.getDocumentId());

            // Document 2: contains "apple" once
            DocumentMetadata doc2 = docService.createDocument("doc2.txt", "an apple a day");
            idxService.buildIndex(doc2.getDocumentId());

            // Document 3: contains no "apple"
            DocumentMetadata doc3 = docService.createDocument("doc3.txt", "orange and banana juice");
            idxService.buildIndex(doc3.getDocumentId());

            // Execute Multi-Search
            MultiSearchResult result = multiSearch.searchAllDocuments("apple", true, 20, MultiSearchResult.SortOrder.BY_MATCH_COUNT);
            assertEquals(3, result.getTotalMatches(), "Total 'apple' occurrences across corpus must be 3");
            assertEquals(2, result.getMatchingDocumentsCount(), "Exactly 2 documents must contain 'apple'");

            // Verify sorting by match count descending
            List<MultiSearchResult.DocumentMatchSummary> summaries = result.getSummaries();
            assertEquals(2, summaries.size(), "Summaries must contain the 2 matching documents");
            assertEquals(doc1.getDocumentId(), summaries.get(0).getDocumentId(), "doc1 (2 matches) must be first");
            assertEquals(2, summaries.get(0).getMatchCount(), "doc1 match count must be 2");
            assertEquals(doc2.getDocumentId(), summaries.get(1).getDocumentId(), "doc2 (1 match) must be second");

        } finally {
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
            cleanupDirectory(tempHist.getParent());
        }
    }

    /**
     * Test 11 — Cryptographic SHA-256 Tamper Detection & Integrity Verification:
     * Verifies SHA-256 computation and confirms that unauthorized text modifications invalidate the index.
     */
    private static void test11Sha256IntegrityVerification() throws IOException {
        Path tempDocs = Paths.get("data", "scratch_test_sha_docs");
        Path tempMeta = Paths.get("data", "scratch_test_sha_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_sha_idx");

        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);

        try {
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);
            IndexService idxService = new IndexService(docRepo, metaRepo, idxRepo);

            String initialContent = "Data Structure Integrity Test";
            DocumentMetadata meta = docService.createDocument("secure_doc.txt", initialContent);

            String originalSha256 = FileUtils.computeSHA256(initialContent);
            assertEquals(originalSha256, meta.getSha256Hash(), "Document metadata must store valid SHA-256 hash");

            // Build index and verify integrity
            SuffixIndex index = idxService.buildIndex(meta.getDocumentId());
            assertEquals(originalSha256, index.getSha256Hash(), "Index must store document SHA-256 hash");
            assertTrue(idxService.verifyIndexIntegrity(meta.getDocumentId()), "Index integrity must verify successfully initially");

            // Simulate external disk modification (bypassing normal app update)
            docRepo.saveDocumentContent(meta.getDocumentId(), "Tampered Content Replaced!");
            assertTrue(!idxService.verifyIndexIntegrity(meta.getDocumentId()),
                    "Index integrity must FAIL cryptographically after unauthorized content modification");

        } finally {
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
        }
    }

    /**
     * Test 12 — Benchmark Suite Multi-Iteration & CSV Logging:
     * Verifies multi-iteration benchmarking, calculation of Min/Max/Avg metrics, and CSV/TXT persistence.
     */
    private static void test12BenchmarkExecutionAndCsvLogging() throws IOException {
        Path tempBench = Paths.get("data", "scratch_test_benchmarks");
        Path tempDocs = Paths.get("data", "scratch_test_bench_docs");
        Path tempMeta = Paths.get("data", "scratch_test_bench_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_bench_idx");

        Files.createDirectories(tempBench);
        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);

        try {
            BenchmarkRepository benchRepo = new BenchmarkRepository(tempBench);
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);

            DocumentMetadata meta = docService.createDocument("benchmark_sample.txt",
                    "The quick brown fox jumps over the lazy dog. The quick brown fox was very fast.");

            BenchmarkService benchService = new BenchmarkService(benchRepo, docService);
            BenchmarkResult result = benchService.runBenchmark(meta.getDocumentId(), "quick", 5);

            assertEquals(5, result.getRunCount(), "Iteration count must be 5");
            assertEquals(2, result.getMatches(), "Must find 2 matches for 'quick'");
            assertTrue(result.getSaBuildAvgNs() > 0, "SA Build average must be positive");
            assertTrue(result.getLcpBuildAvgNs() > 0, "LCP Build average must be positive");
            assertTrue(result.getSpeedupFactor() >= 0.1, "Speedup factor must be calculated");

            // Verify CSV file written to disk
            List<Path> files = benchService.listBenchmarkFiles();
            assertTrue(!files.isEmpty(), "Benchmark files must be created in benchmark directory");

            boolean csvFound = false;
            for (Path f : files) {
                if (f.getFileName().toString().endsWith(".csv")) {
                    csvFound = true;
                    String content = Files.readString(f);
                    assertTrue(content.contains("Timestamp,Document,SizeChars"), "CSV must have header");
                    assertTrue(content.contains("benchmark_sample.txt"), "CSV must contain document row");
                }
            }
            assertTrue(csvFound, "CSV benchmark file must exist on disk");

        } finally {
            cleanupDirectory(tempBench);
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
        }
    }

    /**
     * Test 13 — Comprehensive Technical Report Generation & Export:
     * Verifies report creation with all 6 required analytical sections and text export.
     */
    private static void test13ReportGenerationAndExport() throws IOException {
        Path tempReports = Paths.get("data", "scratch_test_reports");
        Path tempDocs = Paths.get("data", "scratch_test_rep_docs");
        Path tempMeta = Paths.get("data", "scratch_test_rep_meta", "documents.meta");
        Path tempIdx = Paths.get("data", "scratch_test_rep_idx");
        Path tempHist = Paths.get("data", "scratch_test_rep_hist", "search_history.txt");

        Files.createDirectories(tempReports);
        Files.createDirectories(tempDocs);
        Files.createDirectories(tempMeta.getParent());
        Files.createDirectories(tempIdx);
        Files.createDirectories(tempHist.getParent());

        try {
            ReportRepository repRepo = new ReportRepository(tempReports);
            DocumentRepository docRepo = new DocumentRepository(tempDocs);
            MetadataRepository metaRepo = new MetadataRepository(tempMeta);
            IndexRepository idxRepo = new IndexRepository(tempIdx);
            SearchHistoryRepository histRepo = new SearchHistoryRepository(tempHist);

            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);
            IndexService idxService = new IndexService(docRepo, metaRepo, idxRepo);
            AnalyticsService analyticsService = new AnalyticsService(docRepo, metaRepo, idxRepo, histRepo);
            ReportService reportService = new ReportService(repRepo, docService, idxService, analyticsService);

            String text = "banana split and banana ice cream";
            DocumentMetadata meta = docService.createDocument("banana_report.txt", text);
            idxService.buildIndex(meta.getDocumentId());

            // Generate report text
            String reportText = reportService.generateReportContent(meta.getDocumentId());
            assertTrue(reportText.contains("SUFFIX INDEX BUILDER - COMPREHENSIVE REPORT"), "Report must contain header");
            assertTrue(reportText.contains("1. DOCUMENT & INTEGRITY HEADER"), "Report must contain Section 1");
            assertTrue(reportText.contains("SHA-256 Hash:"), "Report must contain SHA-256 hash");
            assertTrue(reportText.contains("2. DOCUMENT LEXICAL METRICS"), "Report must contain Section 2");
            assertTrue(reportText.contains("3. SUFFIX INDEX & DATA STRUCTURE METRICS"), "Report must contain Section 3");
            assertTrue(reportText.contains("4. ADVANCED SUBSTRING ANALYSIS (LRS & REPEATS)"), "Report must contain Section 4");
            assertTrue(reportText.contains("Longest Repeated Substring (LRS): 'banana '"), "Report must correctly identify LRS 'banana '");
            assertTrue(reportText.contains("5. SUFFIX ARRAY & LCP SAMPLE"), "Report must contain Section 5");
            assertTrue(reportText.contains("6. CHARACTER FREQUENCY DISTRIBUTION"), "Report must contain Section 6");

            // Save report to disk
            Path savedReport = reportService.generateAndSaveReport(meta.getDocumentId());
            assertTrue(Files.exists(savedReport), "Saved report must exist on disk");
            String readBack = reportService.readReport(savedReport);
            assertEquals(reportText, readBack, "Read back report content must match generated content");

        } finally {
            cleanupDirectory(tempReports);
            cleanupDirectory(tempDocs);
            cleanupDirectory(tempMeta.getParent());
            cleanupDirectory(tempIdx);
            cleanupDirectory(tempHist.getParent());
        }
    }

    private static void cleanupDirectory(Path dir) {
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }
}
