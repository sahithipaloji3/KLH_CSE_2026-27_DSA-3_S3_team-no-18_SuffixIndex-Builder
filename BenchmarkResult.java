package com.suffixindex.model;

/**
 * Encapsulates empirical benchmark results for document indexing and searching.
 */
public class BenchmarkResult {
    private final String timestamp;
    private final String documentName;
    private final long documentSizeChars;
    private final String searchPattern;
    private final int runCount;

    private final long saBuildMinNs;
    private final long saBuildMaxNs;
    private final long saBuildAvgNs;

    private final long lcpBuildMinNs;
    private final long lcpBuildMaxNs;
    private final long lcpBuildAvgNs;

    private final long naiveSearchAvgNs;
    private final long saSearchAvgNs;

    private final int matches;
    private final double speedupFactor;

    public BenchmarkResult(String timestamp, String documentName, long documentSizeChars,
                           String searchPattern, int runCount,
                           long saBuildMinNs, long saBuildMaxNs, long saBuildAvgNs,
                           long lcpBuildMinNs, long lcpBuildMaxNs, long lcpBuildAvgNs,
                           long naiveSearchAvgNs, long saSearchAvgNs, int matches) {
        this.timestamp = timestamp;
        this.documentName = documentName;
        this.documentSizeChars = documentSizeChars;
        this.searchPattern = searchPattern;
        this.runCount = runCount;
        this.saBuildMinNs = saBuildMinNs;
        this.saBuildMaxNs = saBuildMaxNs;
        this.saBuildAvgNs = saBuildAvgNs;
        this.lcpBuildMinNs = lcpBuildMinNs;
        this.lcpBuildMaxNs = lcpBuildMaxNs;
        this.lcpBuildAvgNs = lcpBuildAvgNs;
        this.naiveSearchAvgNs = naiveSearchAvgNs;
        this.saSearchAvgNs = saSearchAvgNs;
        this.matches = matches;
        this.speedupFactor = (saSearchAvgNs > 0) ? (double) naiveSearchAvgNs / saSearchAvgNs : 1.0;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDocumentName() {
        return documentName;
    }

    public long getDocumentSizeChars() {
        return documentSizeChars;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public int getRunCount() {
        return runCount;
    }

    public long getSaBuildAvgNs() {
        return saBuildAvgNs;
    }

    public double getSaBuildAvgMs() {
        return saBuildAvgNs / 1_000_000.0;
    }

    public long getLcpBuildAvgNs() {
        return lcpBuildAvgNs;
    }

    public double getLcpBuildAvgMs() {
        return lcpBuildAvgNs / 1_000_000.0;
    }

    public long getNaiveSearchAvgNs() {
        return naiveSearchAvgNs;
    }

    public double getNaiveSearchAvgMs() {
        return naiveSearchAvgNs / 1_000_000.0;
    }

    public long getSaSearchAvgNs() {
        return saSearchAvgNs;
    }

    public double getSaSearchAvgMs() {
        return saSearchAvgNs / 1_000_000.0;
    }

    public int getMatches() {
        return matches;
    }

    public double getSpeedupFactor() {
        return speedupFactor;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%d,%s,%d,%.3f,%.3f,%.3f,%.3f,%d,%.1fx",
                timestamp, documentName, documentSizeChars, searchPattern, runCount,
                getSaBuildAvgMs(), getLcpBuildAvgMs(), getNaiveSearchAvgMs(), getSaSearchAvgMs(),
                matches, speedupFactor);
    }

    public static String getCsvHeader() {
        return "Timestamp,Document,SizeChars,Pattern,Runs,SABuildAvgMs,LCPBuildAvgMs,NaiveSearchAvgMs,SASearchAvgMs,Matches,Speedup";
    }
}
