package com.suffixindex.repository;

import com.suffixindex.model.BenchmarkResult;
import com.suffixindex.util.DateUtils;
import com.suffixindex.util.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistence of benchmark trials in data/benchmarks/ as .csv and .txt.
 * Pure Java File I/O.
 */
public class BenchmarkRepository {

    private final Path benchmarksDirectory;

    public BenchmarkRepository() {
        this(FileUtils.BENCHMARKS_DIR);
    }

    public BenchmarkRepository(Path benchmarksDirectory) {
        this.benchmarksDirectory = benchmarksDirectory;
        try {
            Files.createDirectories(this.benchmarksDirectory);
        } catch (IOException e) {
            System.err.println("Warning: Could not create benchmarks directory: " + e.getMessage());
        }
    }

    /**
     * Appends a BenchmarkResult row to the daily CSV file and writes a readable text summary.
     */
    public synchronized Path saveBenchmark(BenchmarkResult result) throws IOException {
        String dateStr = DateUtils.currentDateString();
        Path csvFile = benchmarksDirectory.resolve("benchmark_" + dateStr + ".csv");
        Path txtFile = benchmarksDirectory.resolve("benchmark_" + dateStr + ".txt");

        boolean isNewCsv = !Files.exists(csvFile);

        // 1. Write CSV
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (isNewCsv) {
                writer.write(BenchmarkResult.getCsvHeader());
                writer.newLine();
            }
            writer.write(result.toCsvRow());
            writer.newLine();
        }

        // 2. Write Text Log
        try (BufferedWriter writer = Files.newBufferedWriter(txtFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write("========================================================================\n");
            writer.write("BENCHMARK TRIAL RUN: " + result.getTimestamp() + "\n");
            writer.write("========================================================================\n");
            writer.write("Document:             " + result.getDocumentName() + " (" + result.getDocumentSizeChars() + " characters)\n");
            writer.write("Search Pattern:       '" + result.getSearchPattern() + "'\n");
            writer.write("Trial Iterations:     " + result.getRunCount() + "\n");
            writer.write("SA Construction Avg:  " + String.format("%.3f ms (%,d ns)", result.getSaBuildAvgMs(), result.getSaBuildAvgNs()) + "\n");
            writer.write("Kasai LCP Avg:        " + String.format("%.3f ms (%,d ns)", result.getLcpBuildAvgMs(), result.getLcpBuildAvgNs()) + "\n");
            writer.write("Naive Search Avg:     " + String.format("%.3f ms (%,d ns)", result.getNaiveSearchAvgMs(), result.getNaiveSearchAvgNs()) + "\n");
            writer.write("Suffix Binary Search: " + String.format("%.3f ms (%,d ns)", result.getSaSearchAvgMs(), result.getSaSearchAvgNs()) + "\n");
            writer.write("Matches Found:        " + result.getMatches() + "\n");
            writer.write("Empirical Speedup:    " + String.format("%.1fx faster", result.getSpeedupFactor()) + "\n\n");
        }

        return csvFile;
    }

    public List<Path> listBenchmarkFiles() throws IOException {
        List<Path> list = new ArrayList<>();
        if (!Files.exists(benchmarksDirectory)) return list;
        try (var stream = Files.list(benchmarksDirectory)) {
            stream.filter(Files::isRegularFile).forEach(list::add);
        }
        return list;
    }
}
