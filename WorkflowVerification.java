package com.suffixindex.test;

import com.suffixindex.algorithm.SuffixSearcher;
import com.suffixindex.model.Document;
import com.suffixindex.model.DocumentMetadata;
import com.suffixindex.model.IndexStatus;
import com.suffixindex.model.SuffixIndex;
import com.suffixindex.repository.*;
import com.suffixindex.service.DocumentService;
import com.suffixindex.service.IndexService;
import com.suffixindex.service.SearchService;
import com.suffixindex.util.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Executes the complete 19-step end-to-end verification workflow from Section 35 of the specification.
 */
public class WorkflowVerification {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("   SECTION 35: 19-STEP COMPLETE LIFECYCLE WORKFLOW VERIFICATION          ");
        System.out.println("=========================================================================\n");

        try {
            // Step 1: Start application (initialize storage)
            System.out.println("[Step 1] Initializing file storage directories...");
            FileUtils.initializeDirectories();

            DocumentRepository docRepo = new DocumentRepository();
            MetadataRepository metaRepo = new MetadataRepository();
            IndexRepository idxRepo = new IndexRepository();
            SearchHistoryRepository histRepo = new SearchHistoryRepository();
            SettingsRepository setRepo = new SettingsRepository();

            DocumentService docService = new DocumentService(docRepo, metaRepo, idxRepo);
            IndexService idxService = new IndexService(docRepo, metaRepo, idxRepo);
            SearchService searchService = new SearchService(docRepo, metaRepo, idxRepo, histRepo);
            System.out.println("   ✓ Storage and services active.");

            // Step 2 & 3: Create & Save Document
            System.out.println("\n[Step 2 & 3] Creating document 'banana_demo.txt' with content 'banana'...");
            DocumentMetadata meta = docService.createDocument("banana_demo.txt", "banana");
            String docId = meta.getDocumentId();
            System.out.println("   ✓ Document created: " + docId + " (" + meta.getDocumentName() + ")");

            // Step 4 & 5: Close/reopen application & verify document exists
            System.out.println("\n[Step 4 & 5] Simulating application restart & verifying document persistence...");
            docService = new DocumentService(new DocumentRepository(), new MetadataRepository(), new IndexRepository());
            Document doc = docService.getDocument(docId);
            if (!doc.getContent().equals("banana")) {
                throw new AssertionError("Document content mismatch! Expected 'banana', got: " + doc.getContent());
            }
            System.out.println("   ✓ Document successfully reloaded after restart: '" + doc.getContent() + "'");

            // Step 6 & 7: Build suffix index & verify index file exists
            System.out.println("\n[Step 6 & 7] Building Suffix Index (Prefix Doubling + Kasai)...");
            idxService = new IndexService(new DocumentRepository(), new MetadataRepository(), new IndexRepository());
            SuffixIndex index = idxService.buildIndex(docId);
            Path indexPath = new IndexRepository().getIndexPath(docId);
            if (!Files.exists(indexPath)) {
                throw new AssertionError("Index file was not created at " + indexPath);
            }
            System.out.println("   ✓ Index file verified at " + indexPath + " (" + Files.size(indexPath) + " bytes)");

            // Step 8, 9, 10: Close, Reopen, Load index
            System.out.println("\n[Step 8, 9, 10] Simulating application restart & reloading index from disk...");
            idxService = new IndexService(new DocumentRepository(), new MetadataRepository(), new IndexRepository());
            SuffixIndex reloadedIndex = idxService.getIndex(docId);
            if (reloadedIndex == null || reloadedIndex.getSuffixCount() != 6) {
                throw new AssertionError("Reloaded index corrupt or invalid!");
            }
            System.out.println("   ✓ Index reloaded from disk with " + reloadedIndex.getSuffixCount() + " suffixes.");

            // Step 11 & 12: Search for substring & verify correct positions
            System.out.println("\n[Step 11 & 12] Searching for pattern 'ana' using Suffix Array Binary Search...");
            searchService = new SearchService(new DocumentRepository(), new MetadataRepository(), new IndexRepository(), new SearchHistoryRepository());
            var comp = searchService.executeSearch(docId, "ana", true, 20);
            List<Integer> positions = comp.getSuffixSearchResult().getPositions();
            System.out.println("   ✓ Matches found: " + positions);
            if (!positions.equals(List.of(1, 3))) {
                throw new AssertionError("Positions incorrect! Expected [1, 3], got: " + positions);
            }

            // Step 13 & 14: View Suffix Array & View LCP Array
            System.out.println("\n[Step 13 & 14] Validating Suffix Array & LCP Array values...");
            int[] sa = reloadedIndex.getSuffixArray();
            int[] lcp = reloadedIndex.getLcpArray();
            System.out.println("   Suffix Array: " + java.util.Arrays.toString(sa));
            System.out.println("   LCP Array:    " + java.util.Arrays.toString(lcp));
            if (!java.util.Arrays.equals(sa, new int[]{5, 3, 1, 0, 4, 2})) {
                throw new AssertionError("Suffix array mismatch!");
            }
            if (!java.util.Arrays.equals(lcp, new int[]{0, 1, 3, 0, 0, 2})) {
                throw new AssertionError("LCP array mismatch!");
            }
            System.out.println("   ✓ Suffix Array and LCP Array exactly match mathematical expected values.");

            // Step 15: Check search history
            System.out.println("\n[Step 15] Checking persistent search history...");
            var history = new SearchHistoryRepository().loadAll();
            System.out.println("   ✓ Total searches logged: " + history.size());
            if (history.isEmpty() || !history.get(0).getPattern().equals("ana")) {
                throw new AssertionError("Search history was not recorded correctly!");
            }

            // Step 16 & 17: Modify document & verify index becomes OUTDATED
            System.out.println("\n[Step 16 & 17] Modifying document content to 'banana is sweet'...");
            docService.updateDocument(docId, "banana_demo.txt", "banana is sweet");
            Document docUpdated = docService.getDocument(docId);
            System.out.println("   Current status: " + docUpdated.getMetadata().getIndexStatus());
            if (docUpdated.getMetadata().getIndexStatus() != IndexStatus.OUTDATED) {
                throw new AssertionError("Index status must be OUTDATED, got: " + docUpdated.getMetadata().getIndexStatus());
            }
            System.out.println("   ✓ Index successfully marked OUTDATED due to content modification.");

            // Step 18: Rebuild index
            System.out.println("\n[Step 18] Rebuilding outdated index...");
            idxService.buildIndex(docId);
            Document docRebuilt = docService.getDocument(docId);
            if (docRebuilt.getMetadata().getIndexStatus() != IndexStatus.INDEXED) {
                throw new AssertionError("Index status must be INDEXED after rebuild, got: " + docRebuilt.getMetadata().getIndexStatus());
            }
            System.out.println("   ✓ Index successfully rebuilt and verified as INDEXED.");

            // Step 19: Search again
            System.out.println("\n[Step 19] Searching again in updated text for 'sweet' and 'ana'...");
            var compUpdatedSweet = searchService.executeSearch(docId, "sweet", true, 20);
            System.out.println("   'sweet' positions: " + compUpdatedSweet.getSuffixSearchResult().getPositions());
            if (!compUpdatedSweet.getSuffixSearchResult().getPositions().equals(List.of(10))) {
                throw new AssertionError("Expected 'sweet' at position 10");
            }
            var compUpdatedAna = searchService.executeSearch(docId, "ana", true, 20);
            System.out.println("   'ana' positions: " + compUpdatedAna.getSuffixSearchResult().getPositions());
            if (!compUpdatedAna.getSuffixSearchResult().getPositions().equals(List.of(1, 3))) {
                throw new AssertionError("Expected 'ana' at positions [1, 3]");
            }
            System.out.println("   ✓ All 19 steps passed with 100% verification!");

            System.out.println("\n=========================================================================");
            System.out.println("   SUCCESS: SECTION 35 COMPLETE WORKFLOW FULLY VERIFIED                   ");
            System.out.println("=========================================================================");

        } catch (Throwable t) {
            System.err.println("\nFAILED in workflow verification: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
