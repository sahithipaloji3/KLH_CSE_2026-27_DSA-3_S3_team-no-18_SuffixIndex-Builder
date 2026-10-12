package com.suffixindex.repository;

import com.suffixindex.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistence of document reports in data/reports/.
 * Pure Java File I/O.
 */
public class ReportRepository {

    private final Path reportsDirectory;

    public ReportRepository() {
        this(FileUtils.REPORTS_DIR);
    }

    public ReportRepository(Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
        try {
            Files.createDirectories(this.reportsDirectory);
        } catch (IOException e) {
            System.err.println("Warning: Could not create reports directory: " + e.getMessage());
        }
    }

    public Path saveReport(String documentId, String reportContent) throws IOException {
        String filename = "report_" + documentId + "_" + System.currentTimeMillis() + ".txt";
        Path target = reportsDirectory.resolve(filename);
        FileUtils.writeStringToFile(target, reportContent);
        return target;
    }

    public List<Path> listReportFiles() throws IOException {
        List<Path> list = new ArrayList<>();
        if (!Files.exists(reportsDirectory)) return list;
        try (var stream = Files.list(reportsDirectory)) {
            stream.filter(Files::isRegularFile).sorted((a, b) -> b.getFileName().compareTo(a.getFileName())).forEach(list::add);
        }
        return list;
    }

    public String readReport(Path reportPath) throws IOException {
        return FileUtils.readFileToString(reportPath);
    }
}
