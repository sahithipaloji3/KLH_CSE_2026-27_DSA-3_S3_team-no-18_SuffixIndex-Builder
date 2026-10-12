package com.suffixindex.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java Trie (Prefix Tree) implementation for fast autocomplete and prefix queries.
 * Time Complexity:
 *  - Insert: O(L) where L is word length
 *  - Search: O(L)
 *  - Prefix Suggestions: O(P + K) where P is prefix length and K is number of matched descendants
 */
public class Trie {

    private final TrieNode root;
    private int wordCount;
    private int nodeCount;

    public Trie() {
        this.root = new TrieNode();
        this.wordCount = 0;
        this.nodeCount = 1; // root node
    }

    /**
     * Inserts a word into the Trie.
     */
    public synchronized void insert(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            TrieNode next = current.getChildren().get(c);
            if (next == null) {
                next = new TrieNode();
                current.getChildren().put(c, next);
                nodeCount++;
            }
            current = next;
        }

        if (!current.isEndOfWord()) {
            current.setEndOfWord(true);
            current.setCompleteWord(word);
            wordCount++;
        }
        current.incrementFrequency();
    }

    /**
     * Searches for exact word match.
     */
    public boolean search(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }

        TrieNode node = findPrefixNode(word);
        return node != null && node.isEndOfWord();
    }

    /**
     * Checks if any word starts with the given prefix.
     */
    public boolean startsWith(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        return findPrefixNode(prefix) != null;
    }

    /**
     * Returns autocomplete suggestions matching prefix up to the specified limit.
     */
    public List<String> getAutocompleteSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        TrieNode prefixNode = findPrefixNode(prefix);
        if (prefixNode == null) {
            return Collections.emptyList();
        }

        List<String> results = new ArrayList<>();
        collectWords(prefixNode, results, limit);
        return results;
    }

    /**
     * Deletes a word from the Trie.
     */
    public synchronized boolean delete(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        return deleteHelper(root, word, 0);
    }

    private boolean deleteHelper(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord()) {
                return false;
            }
            current.setEndOfWord(false);
            current.setCompleteWord(null);
            wordCount--;
            return current.getChildren().isEmpty();
        }

        char c = word.charAt(index);
        TrieNode next = current.getChildren().get(c);
        if (next == null) {
            return false;
        }

        boolean shouldDeleteChild = deleteHelper(next, word, index + 1);
        if (shouldDeleteChild) {
            current.getChildren().remove(c);
            nodeCount--;
            return current.getChildren().isEmpty() && !current.isEndOfWord();
        }

        return false;
    }

    private TrieNode findPrefixNode(String prefix) {
        TrieNode current = root;
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            current = current.getChildren().get(c);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void collectWords(TrieNode node, List<String> results, int limit) {
        if (results.size() >= limit) {
            return;
        }

        if (node.isEndOfWord() && node.getCompleteWord() != null) {
            results.add(node.getCompleteWord());
        }

        for (TrieNode child : node.getChildren().values()) {
            collectWords(child, results, limit);
            if (results.size() >= limit) {
                break;
            }
        }
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void clear() {
        root.getChildren().clear();
        wordCount = 0;
        nodeCount = 1;
    }
}
