package com.suffixindex.algorithm;

import java.util.Map;
import java.util.TreeMap;

/**
 * Node in the custom Trie data structure.
 * Uses a TreeMap for children so that autocomplete suggestions are naturally alphabetical.
 */
public class TrieNode {
    private final Map<Character, TrieNode> children = new TreeMap<>();
    private boolean isEndOfWord;
    private int frequency;
    private String completeWord;

    public TrieNode() {
        this.isEndOfWord = false;
        this.frequency = 0;
        this.completeWord = null;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }

    public int getFrequency() {
        return frequency;
    }

    public void incrementFrequency() {
        this.frequency++;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public String getCompleteWord() {
        return completeWord;
    }

    public void setCompleteWord(String completeWord) {
        this.completeWord = completeWord;
    }
}
