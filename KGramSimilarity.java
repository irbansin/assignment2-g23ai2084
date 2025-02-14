import java.io.*;
import java.nio.file.*;
import java.util.*;

public class KGramSimilarity {

    public static void main(String[] args) throws IOException {
        // File paths
        String[] docNames = {"D1.txt", "D2.txt", "D3.txt", "D4.txt"};
        Map<String, String> documents = new HashMap<>();
        
        // Read documents
        for (String doc : docNames) {
            documents.put(doc, new String(Files.readAllBytes(Paths.get(doc))).toLowerCase());
        }
        
        // Store k-grams
        Map<String, Set<String>> char2grams = new HashMap<>();
        Map<String, Set<String>> char3grams = new HashMap<>();
        Map<String, Set<String>> word2grams = new HashMap<>();
        
        for (String doc : docNames) {
            char2grams.put(doc, generateCharacterKGrams(documents.get(doc), 2));
            char3grams.put(doc, generateCharacterKGrams(documents.get(doc), 3));
            word2grams.put(doc, generateWordKGrams(documents.get(doc), 2));
        }
        
        // Print distinct k-gram counts
        System.out.println("How many distinct k-grams are there for each document with each type of k-gram? You should report 4 × 3 = 12\r\n" + //
                        "different number\r\n" + //
                        ":");
        for (String doc : docNames) {
            System.out.printf("%s: Char 2-grams=%d, Char 3-grams=%d, Word 2-grams=%d\n",
                    doc, char2grams.get(doc).size(), char3grams.get(doc).size(), word2grams.get(doc).size());
        }

        // Compute Jaccard similarity
        System.out.println("\nB: Compute the Jaccard similarity between all pairs of documents for each type of k-gram. You should report 3 × 6 = 18\r\n" + //
                        "different number\r\n" + //
                        "");
        for (int i = 0; i < docNames.length; i++) {
            for (int j = i + 1; j < docNames.length; j++) {
                String doc1 = docNames[i];
                String doc2 = docNames[j];
                
                double jaccardChar2 = jaccardSimilarity(char2grams.get(doc1), char2grams.get(doc2));
                double jaccardChar3 = jaccardSimilarity(char3grams.get(doc1), char3grams.get(doc2));
                double jaccardWord2 = jaccardSimilarity(word2grams.get(doc1), word2grams.get(doc2));
                
                System.out.printf("%s - %s: Char 2-gram=%.4f, Char 3-gram=%.4f, Word 2-gram=%.4f\n",
                        doc1, doc2, jaccardChar2, jaccardChar3, jaccardWord2);
            }
        }
    }

    // Generate character-based k-grams
    public static Set<String> generateCharacterKGrams(String text, int k) {
        Set<String> kgrams = new HashSet<>();
        for (int i = 0; i <= text.length() - k; i++) {
            kgrams.add(text.substring(i, i + k));
        }
        return kgrams;
    }

    // Generate word-based k-grams
    private static Set<String> generateWordKGrams(String text, int k) {
        String[] words = text.split("\\s+");
        Set<String> kgrams = new HashSet<>();
        for (int i = 0; i <= words.length - k; i++) {
            kgrams.add(String.join(" ", Arrays.copyOfRange(words, i, i + k)));
        }
        return kgrams;
    }

    // Compute Jaccard similarity
    public static double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }
}
