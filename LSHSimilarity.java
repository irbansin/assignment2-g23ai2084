import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LSHSimilarity {
    private static final int T = 160; // Total hash functions
    private static final double TAU = 0.7; // Jaccard similarity threshold

    public static void main(String[] args) throws IOException {
          // File paths
          String[] docNames = {"D1.txt", "D2.txt", "D3.txt", "D4.txt"};
          Map<String, String> documents = new HashMap<>();
          Map<String, Set<String>> kgramSets = new HashMap<>();
  
          // Read documents
          for (String doc : docNames) {
              String content = new String(Files.readAllBytes(Paths.get(doc))).toLowerCase();
              documents.put(doc, content);
              kgramSets.put(doc, KGramSimilarity.generateCharacterKGrams(content, 3)); // Assuming 3-grams
          }
        // Compute optimal (r, b) values for LSH
        int[] bestRB = findBestRB(T, TAU);
        int bestR = bestRB[0];
        int bestB = bestRB[1];

        // Compute Jaccard similarities
        Map<String, Double> jaccardSimilarities = new HashMap<>();
        for (int i = 0; i < docNames.length; i++) {
            for (int j = i + 1; j < docNames.length; j++) {
                String pair = docNames[i] + "-" + docNames[j];
                double similarity = KGramSimilarity.jaccardSimilarity(kgramSets.get(docNames[i]), kgramSets.get(docNames[j]));
                jaccardSimilarities.put(pair, similarity);
            }
        }

        System.out.println("Best values for LSH: r = " + bestR + ", b = " + bestB);
        System.out.println("\nLSH Probabilities for Document Pairs:");
        for (Map.Entry<String, Double> entry : jaccardSimilarities.entrySet()) {
            double probability = computeLSHProbability(entry.getValue(), bestR, bestB);
            System.out.printf("%s: %.4f\n", entry.getKey(), probability);
        }
    }

    // Compute the best (r, b) values
    private static int[] findBestRB(int t, double tau) {
        int bestR = 1, bestB = t;
        double bestDiff = Double.MAX_VALUE;
        for (int r = 1; r <= t; r++) {
            if (t % r == 0) {
                int b = t / r;
                double fTau = 1 - Math.pow(1 - Math.pow(tau, r), b);
                double diff = Math.abs(fTau - 0.5);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestR = r;
                    bestB = b;
                }
            }
        }
        return new int[]{bestR, bestB};
    }

    // Compute LSH Probability
    private static double computeLSHProbability(double s, int r, int b) {
        return 1 - Math.pow(1 - Math.pow(s, r), b);
    }
}
