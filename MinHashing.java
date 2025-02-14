import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MinHashing {
    private static final int MAX_HASH = 10000;
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // File paths
        String[] docNames = {"D1.txt", "D2.txt"};
        Map<String, String> documents = new HashMap<>();
        
        // Read documents
        for (String doc : docNames) {
            documents.put(doc, new String(Files.readAllBytes(Paths.get(doc))).toLowerCase());
        }
        
        // Store 3-grams
        Map<String, Set<String>> char3grams = new HashMap<>();
        
        for (String doc : docNames) {
            char3grams.put(doc, generateCharacterKGrams(documents.get(doc), 3));
        }
        
        // Values for t (number of hash functions)
        int[] tValues = {20, 60, 150, 300, 600};
        
        // Compute Min-Hash signatures and Jaccard estimates
        System.out.println("Min-Hash Jaccard Similarity Estimates:");
        for (int t : tValues) {
            List<Integer> sigD1 = minHashSignature(char3grams.get("D1.txt"), t);
            List<Integer> sigD2 = minHashSignature(char3grams.get("D2.txt"), t);
            double jaccardEstimate = minHashJaccard(sigD1, sigD2);
            
            System.out.printf("t = %d: Jaccard Similarity = %.4f\n", t, jaccardEstimate);
        }
    }
    
    // Generate character-based k-grams
    private static Set<String> generateCharacterKGrams(String text, int k) {
        Set<String> kgrams = new HashSet<>();
        for (int i = 0; i <= text.length() - k; i++) {
            kgrams.add(text.substring(i, i + k));
        }
        return kgrams;
    }
    
    // Create a hash function
    private static int hashFunction(String value, int seed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update((seed + value).getBytes());
        byte[] digest = md.digest();
        int hash = ((digest[0] & 0xFF) << 24) | ((digest[1] & 0xFF) << 16) | ((digest[2] & 0xFF) << 8) | (digest[3] & 0xFF);
        return Math.abs(hash) % MAX_HASH;
    }
    
    // Compute Min-Hash signature
    private static List<Integer> minHashSignature(Set<String> kgramSet, int numHashes) throws NoSuchAlgorithmException {
        List<Integer> signature = new ArrayList<>();
        for (int i = 0; i < numHashes; i++) {
            int minHash = Integer.MAX_VALUE;
            for (String kgram : kgramSet) {
                minHash = Math.min(minHash, hashFunction(kgram, i));
            }
            signature.add(minHash);
        }
        return signature;
    }
    
    // Compute Min-Hash Jaccard similarity
    private static double minHashJaccard(List<Integer> sig1, List<Integer> sig2) {
        int matches = 0;
        for (int i = 0; i < sig1.size(); i++) {
            if (sig1.get(i).equals(sig2.get(i))) {
                matches++;
            }
        }
        return (double) matches / sig1.size();
    }
}
