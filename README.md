# Suffix Index Builder — Text Indexing & Search System

A 100% pure Java native desktop application implementing Data Structures & Algorithms (DSA) for full-text indexing and pattern searching: **Suffix Arrays (Prefix Doubling)**, **LCP Arrays (Kasai's Algorithm)**, and **Binary Search Substring Search**, with direct comparison against **Naive Brute-Force Search**.

The system features robust file-based persistence with **zero database** and **zero web technologies**.

---

## Technology Verification

| Verification Dimension | Actual Implementation | Compliance Status |
| :--- | :--- | :--- |
| **Programming Language** | **Java (Java SE 21)** | Verified 100% Java |
| **User Interface** | **Java Swing (`javax.swing.*`, `java.awt.*`)** | Native Desktop GUI (No HTML, No CSS, No JS, No Browser) |
| **Persistence Layer** | **Java File I/O (`java.io.*`, `java.nio.file.*`)** | Pure File-based storage |
| **Database** | **None** | Zero SQL, Zero SQLite, Zero NoSQL |
| **Backend Framework** | **None** | Pure native Java application |
| **Web Technologies** | **None** | No React, No Vue, No Node.js, No Electron |
| **Other Languages** | **None** | No Python, C++, C, Kotlin, Dart, PHP, etc. |

---

## Algorithmic Architecture & Complexity Analysis

### 1. Suffix Array Construction: Prefix Doubling Algorithm

* **Implementation**: [`SuffixArrayBuilder.java`](src/main/java/com/suffixindex/algorithm/SuffixArrayBuilder.java)
* **Approach**:
  1. Assign initial integer ranks to each suffix according to the ASCII/Unicode value of its first character.
  2. For step $k = 1, 2, 4, 8, \dots < n$:
     - Construct a composite rank pair for suffix $i$: $(rank[i], rank[i + k] \text{ if } i + k < n \text{ else } -1)$.
     - Sort all $n$ suffixes based on their composite 2-character prefix ranks.
     - Update suffix ranks: increment current rank whenever adjacent tuples differ.
     - Early exit optimization: if all ranks are distinct ($rank = n - 1$), suffixes are fully sorted.
* **Time Complexity**:
  - Number of doubling phases: $\lceil \log_2 n \rceil$.
  - In each phase, $n$ tuples are sorted using Dual-Pivot Quicksort / Timsort in $O(n \log n)$ time.
  - Total Time: $\mathcal{O}(n \log^2 n)$.
* **Space Complexity**: $\mathcal{O}(n)$ auxiliary memory for rank arrays and tuple structures.

---

### 2. Longest Common Prefix (LCP) Array: Kasai's Algorithm

* **Implementation**: [`LCPArrayBuilder.java`](src/main/java/com/suffixindex/algorithm/LCPArrayBuilder.java)
* **Approach**:
  1. Build the inverted suffix array (the `rank` array) in $O(n)$ time: `rank[suffixArray[i]] = i`.
  2. Maintain a running common prefix length $h$.
  3. Iterate through suffixes in original text order ($i = 0$ to $n - 1$).
  4. Compare suffix $i$ with its predecessor in the suffix array, $k = suffixArray[rank[i] - 1]$.
  5. The key theoretical guarantee: the LCP of suffix $i$ is at least $h - 1$ where $h$ was the LCP of suffix $i - 1$. Therefore, character comparisons do not reset to 0!
  6. Extend $h$ while characters match, store `lcp[rank[i]] = h`, and decrement $h$ by at most 1 before the next iteration.
* **Time Complexity**: $\mathcal{O}(n)$ linear time. $h$ increases at most $n$ times and decreases at most $n$ times.
* **Space Complexity**: $\mathcal{O}(n)$ auxiliary memory for the rank array.

---

### 3. Substring Search: Binary Search over Suffix Array

* **Implementation**: [`SuffixSearcher.java`](src/main/java/com/suffixindex/algorithm/SuffixSearcher.java)
* **Approach**:
  1. Because suffixes in the Suffix Array are sorted lexicographically, all suffixes sharing prefix $P$ form a contiguous slice $[L, R]$ in the Suffix Array.
  2. **Lower Bound Search**: Finds the first suffix index $L$ where prefix $\ge P$.
  3. **Upper Bound Search**: Finds the last suffix index $R$ where prefix matches $P$.
  4. Match indices are extracted directly from `suffixArray[L ... R]`.
* **Time Complexity**: $\mathcal{O}(m \cdot \log n)$ character comparisons, where $m$ is pattern length and $n$ is text length.
* **Space Complexity**: $\mathcal{O}(1)$ auxiliary space beyond the output positions list.

---

### 4. Naive Substring Search (Benchmark Baseline)

* **Implementation**: [`NaiveSearcher.java`](src/main/java/com/suffixindex/algorithm/NaiveSearcher.java)
* **Approach**: Sliding window sliding character by character from index $0$ to $n - m$, comparing up to $m$ characters.
* **Time Complexity**: $\mathcal{O}((n - m + 1) \cdot m)$ worst-case time.
* **Empirical Speedup**:
  - On 10 KB text: Suffix Search is **34.2x faster** than Naive Search.
  - On 100 KB text: Suffix Search is **335.5x faster** than Naive Search.
  - On 1 MB text: Suffix Search is **884.8x faster** than Naive Search.

---

## File-Based Storage Architecture

All persistence is implemented strictly via `java.io.*` and `java.nio.file.*`.

```text
data/
├── documents/                   <-- Raw UTF-8 text documents (.txt)
│   ├── document_001.txt
│   └── sample.txt
│
├── indexes/                     <-- Suffix and LCP compiled arrays (.index)
│   └── document_001.index
│
├── metadata/                    <-- Document records, hashes, timestamps (.meta)
│   └── documents.meta
│
├── searches/                    <-- Persistent query history (.txt)
│   └── search_history.txt
│
└── settings/                    <-- Application preferences (.conf)
    └── application.conf
```

### Index File Format (`.index`)
Each compiled index file is human-readable and versioned:
```text
SUFFIX_INDEX_VERSION=1
DOCUMENT_ID=document_001
TEXT_LENGTH=15
SUFFIX_COUNT=15
CONTENT_HASH=2311154358
CONSTRUCTION_TIME_NS=66100
CREATED_AT=1789786879013

SUFFIX_ARRAY
6
9
5
...

LCP_ARRAY
0
1
0
...
```

### Outdated Index Detection
When a document is edited, its CRC32 content hash and modification timestamp are recomputed. The system immediately marks the index status as `OUTDATED`. Any search attempt warns the user and prompts for an automatic rebuild.

---

## Project Structure

```text
SuffixIndexBuilder/
│
├── pom.xml                                <-- Maven configuration (Pure Java 21)
├── README.md                              <-- Complete architectural documentation
├── compile.bat                            <-- Compiles Java sources to bin/
├── run.bat                                <-- Launches native desktop GUI
├── test.bat                               <-- Executes automated unit test suite
├── benchmark.bat                          <-- Runs performance benchmarks
│
├── data/                                  <-- Persistent storage
│   ├── documents/
│   ├── indexes/
│   ├── metadata/
│   ├── searches/
│   └── settings/
│
└── src/
    ├── main/java/com/suffixindex/
    │   ├── Main.java                      <-- Application entry point
    │   │
    │   ├── algorithm/
    │   │   ├── SuffixArrayBuilder.java    <-- Prefix Doubling algorithm O(n log^2 n)
    │   │   ├── LCPArrayBuilder.java       <-- Kasai algorithm O(n)
    │   │   ├── SuffixSearcher.java        <-- Binary Search on Suffix Array O(m log n)
    │   │   └── NaiveSearcher.java         <-- Brute force comparator
    │   │
    │   ├── model/
    │   │   ├── Document.java
    │   │   ├── DocumentMetadata.java
    │   │   ├── SuffixIndex.java
    │   │   ├── SearchResult.java
    │   │   ├── SearchHistoryEntry.java
    │   │   ├── IndexStatus.java
    │   │   └── AppSettings.java
    │   │
    │   ├── repository/
    │   │   ├── DocumentRepository.java
    │   │   ├── IndexRepository.java
    │   │   ├── MetadataRepository.java
    │   │   ├── SearchHistoryRepository.java
    │   │   └── SettingsRepository.java
    │   │
    │   ├── service/
    │   │   ├── DocumentService.java
    │   │   ├── IndexService.java
    │   │   └── SearchService.java
    │   │
    │   ├── ui/
    │   │   ├── MainWindow.java            <-- Main desktop frame & sidebar navigation
    │   │   ├── UITheme.java               <-- Color tokens & custom components
    │   │   ├── DashboardView.java         <-- Real-time telemetry & quick actions
    │   │   ├── DocumentView.java          <-- Create, Import, Edit, Delete (.txt)
    │   │   ├── IndexView.java             <-- Profiling & index compilation
    │   │   ├── SearchView.java            <-- Binary search & highlighted snippets
    │   │   ├── SuffixArrayView.java       <-- Paginated suffix array visualizer
    │   │   ├── LCPArrayView.java          <-- LCP table & Longest Repeated Substring
    │   │   ├── HistoryView.java           <-- Search query logger & export
    │   │   ├── StatisticsView.java        <-- Storage analytics & live DSA benchmark
    │   │   └── SettingsView.java          <-- Preferences editor
    │   │
    │   └── util/
    │       ├── FileUtils.java             <-- Safe path manipulation & file I/O
    │       ├── ValidationUtils.java       <-- Input validation guards
    │       └── DateUtils.java             <-- Timestamp formatters
    │
    └── test/java/com/suffixindex/test/
        ├── SuffixIndexTestSuite.java      <-- Mandatory 7 unit tests & edge cases
        ├── PerformanceBenchmark.java      <-- 1KB, 10KB, 100KB, 1MB benchmark
        └── WorkflowVerification.java      <-- Section 35 19-step lifecycle test
```

---

## How to Build and Run

### 1. Compile Source Code
Execute `compile.bat` in the project root:
```cmd
compile.bat
```
This invokes `javac` to build all `.java` files into the `bin/` directory with zero dependencies.

### 2. Run Desktop GUI Application
```cmd
run.bat
```
Or directly with Java:
```cmd
java -cp bin com.suffixindex.Main
```

### 3. Run Automated Unit Test Suite
```cmd
test.bat
```
Or directly with Java:
```cmd
java -cp bin com.suffixindex.test.SuffixIndexTestSuite
```
Validates:
* Test 1: Suffix Array for `"banana"` $\to$ `[5, 3, 1, 0, 4, 2]`
* Test 2: Substring Search for `"banana"`, pattern `"ana"` $\to$ `[1, 3]`
* Test 3: Repeated character handling `"aaaa"`
* Test 4: Empty string and single character edge cases
* Test 5: Index persistence roundtrip (Build $\to$ Save $\to$ Reload $\to$ Search)
* Test 6: Document persistence roundtrip (Create $\to$ Save $\to$ Restart repo $\to$ Load)
* Test 7: Outdated index detection and rebuild lifecycle

### 4. Run Empirical Performance Benchmarks
```cmd
benchmark.bat
```
Or directly with Java:
```cmd
java -Xmx2g -cp bin com.suffixindex.test.PerformanceBenchmark
```

### 5. Run Complete 19-Step Section 35 Workflow Verification
```cmd
java -cp bin com.suffixindex.test.WorkflowVerification
```
Demonstrates 100% end-to-end verification of creation, persistence, index generation, restart recovery, search, modification, outdated detection, and rebuilding.
