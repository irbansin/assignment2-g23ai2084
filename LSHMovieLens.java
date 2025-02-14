import java.io.*;
import java.util.*;

public class LSHMovieLens {
    private static final String RATINGS_FILE = "ratings.csv";
    private static final int NUM_USERS = 943;
    private static final int NUM_MOVIES = 1682;
    private static final int[][] HASH_CONFIGS = {
        {50, 5, 10},   // 50 hash functions → r=5, b=10
        {100, 5, 20},  // 100 hash functions → r=5, b=20
        {200, 5, 40},  // 200 hash functions → r=5, b=40
        {200, 10, 20}  // 200 hash functions → r=10, b=20
    };

    public static void main(String[] args) throws IOException {
        Map<Integer, Set<Integer>> userMovieMap = loadUserMovieData(RATINGS_FILE);
        Map<String, Double> exactSimilarities = computeExactJaccard(userMovieMap);
        System.out.println("Exact Jaccard Similarity (≥ 0.6): " + exactSimilarities.size());

        for (int[] config : HASH_CONFIGS) {
            int numHashes = config[0], r = config[1], b = config[2];

            System.out.println("\nUsing " + numHashes + " hash functions, r=" + r + ", b=" + b);
            List<int[]> minHashes = generateMinHashes(userMovieMap, numHashes);

            // Perform LSH
            Set<String> candidatePairs = performLSH(minHashes, b, r);
            System.out.println("Candidate Pairs Found: " + candidatePairs.size());

            // Evaluate against exact Jaccard similarity
            evaluateErrors(exactSimilarities, candidatePairs);
        }
    }

    private static Map<Integer, Set<Integer>> loadUserMovieData(String filename) throws IOException {
        Map<Integer, Set<Integer>> userMovieMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        br.readLine(); // Skip header

        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            int userId = Integer.parseInt(parts[0]);
            int movieId = Integer.parseInt(parts[1]);

            userMovieMap.computeIfAbsent(userId, k -> new HashSet<>()).add(movieId);
        }
        br.close();
        return userMovieMap;
    }

    private static Map<String, Double> computeExactJaccard(Map<Integer, Set<Integer>> userMovieMap) {
        Map<String, Double> jaccardMap = new HashMap<>();

        for (int userA : userMovieMap.keySet()) {
            for (int userB : userMovieMap.keySet()) {
                if (userA < userB) {
                    Set<Integer> moviesA = userMovieMap.get(userA);
                    Set<Integer> moviesB = userMovieMap.get(userB);

                    Set<Integer> intersection = new HashSet<>(moviesA);
                    intersection.retainAll(moviesB);

                    Set<Integer> union = new HashSet<>(moviesA);
                    union.addAll(moviesB);

                    double similarity = (double) intersection.size() / union.size();
                    if (similarity >= 0.6) {
                        jaccardMap.put(userA + "-" + userB, similarity);
                    }
                }
            }
        }
        return jaccardMap;
    }

    private static List<int[]> generateMinHashes(Map<Integer, Set<Integer>> userMovieMap, int numHashes) {
        Random random = new Random();
        int[][] hashFunctions = new int[numHashes][2];

        for (int i = 0; i < numHashes; i++) {
            hashFunctions[i][0] = random.nextInt(NUM_MOVIES);
            hashFunctions[i][1] = random.nextInt(NUM_MOVIES);
        }

        List<int[]> minHashes = new ArrayList<>();
        for (int user : userMovieMap.keySet()) {
            int[] signature = new int[numHashes];
            Arrays.fill(signature, Integer.MAX_VALUE);

            for (int i = 0; i < numHashes; i++) {
                for (int movie : userMovieMap.get(user)) {
                    int hashValue = (hashFunctions[i][0] * movie + hashFunctions[i][1]) % NUM_MOVIES;
                    signature[i] = Math.min(signature[i], hashValue);
                }
            }
            minHashes.add(signature);
        }
        return minHashes;
    }

    private static Set<String> performLSH(List<int[]> minHashes, int b, int r) {
        Map<String, List<Integer>> bandBuckets = new HashMap<>();
        Set<String> candidatePairs = new HashSet<>();

        for (int band = 0; band < b; band++) {
            bandBuckets.clear();

            for (int userIdx = 0; userIdx < minHashes.size(); userIdx++) {
                int[] signature = minHashes.get(userIdx);
                int[] bandSignature = Arrays.copyOfRange(signature, band * r, (band + 1) * r);
                String hashValue = Arrays.toString(bandSignature);

                bandBuckets.computeIfAbsent(hashValue, k -> new ArrayList<>()).add(userIdx);
            }

            for (List<Integer> users : bandBuckets.values()) {
                if (users.size() > 1) {
                    for (int i = 0; i < users.size(); i++) {
                        for (int j = i + 1; j < users.size(); j++) {
                            candidatePairs.add(users.get(i) + "-" + users.get(j));
                        }
                    }
                }
            }
        }
        return candidatePairs;
    }

    private static void evaluateErrors(Map<String, Double> exact, Set<String> candidates) {
        int falsePositives = 0;
        int falseNegatives = 0;
        int truePositives = 0;
        int totalPairs = exact.size();

        for (String pair : candidates) {
            if (!exact.containsKey(pair)) {
                falsePositives++;
            } else {
                truePositives++;
            }
        }

        for (String pair : exact.keySet()) {
            if (!candidates.contains(pair)) {
                falseNegatives++;
            }
        }

        System.out.println("False Positives: " + falsePositives);
        System.out.println("False Negatives: " + falseNegatives);
        System.out.println("True Positives: " + truePositives);
        System.out.println("Total Exact Pairs (≥ 0.6): " + totalPairs);
    }
}
