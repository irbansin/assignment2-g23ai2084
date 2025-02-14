import java.io.*;
import java.util.*;

public class MinHashMovieLens {
    private static final String RATINGS_FILE = "ratings.csv";
    private static final int[] NUM_HASHES = {50, 100, 200};
    private static final int NUM_USERS = 943;
    private static final int NUM_MOVIES = 1682;

    public static void main(String[] args) throws IOException {
        Map<Integer, Set<Integer>> userMovieMap = loadUserMovieData(RATINGS_FILE);
        List<int[]> minHashes = generateMinHashes(userMovieMap, NUM_HASHES[NUM_HASHES.length - 1]); // 200 hash functions max

        Map<String, Double> exactSimilarities = computeExactJaccard(userMovieMap);
        System.out.println("Exact Jaccard Similarity (>= 0.5): " + exactSimilarities.size());

        for (int numHashes : NUM_HASHES) {
            System.out.println("\nUsing " + numHashes + " hash functions:");
            Map<String, Double> approxSimilarities = computeApproximateJaccard(userMovieMap, minHashes, numHashes);
            evaluateErrors(exactSimilarities, approxSimilarities);
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
                    if (similarity >= 0.5) {
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
            hashFunctions[i][0] = random.nextInt(NUM_MOVIES); // a
            hashFunctions[i][1] = random.nextInt(NUM_MOVIES); // b
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

    private static Map<String, Double> computeApproximateJaccard(Map<Integer, Set<Integer>> userMovieMap,
                                                                 List<int[]> minHashes, int numHashes) {
        Map<String, Double> approxJaccard = new HashMap<>();
        List<int[]> truncatedSignatures = new ArrayList<>();

        for (int[] sig : minHashes) {
            truncatedSignatures.add(Arrays.copyOf(sig, numHashes));
        }

        List<Integer> users = new ArrayList<>(userMovieMap.keySet());

        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                int matches = 0;
                for (int k = 0; k < numHashes; k++) {
                    if (truncatedSignatures.get(i)[k] == truncatedSignatures.get(j)[k]) {
                        matches++;
                    }
                }
                double approxSim = (double) matches / numHashes;
                if (approxSim >= 0.5) {
                    approxJaccard.put(users.get(i) + "-" + users.get(j), approxSim);
                }
            }
        }
        return approxJaccard;
    }

    private static void evaluateErrors(Map<String, Double> exact, Map<String, Double> approx) {
        int falsePositives = 0;
        int falseNegatives = 0;
        int truePositives = 0;
        int totalPairs = exact.size();

        for (String pair : approx.keySet()) {
            if (!exact.containsKey(pair)) {
                falsePositives++;
            } else {
                truePositives++;
            }
        }

        for (String pair : exact.keySet()) {
            if (!approx.containsKey(pair)) {
                falseNegatives++;
            }
        }

        System.out.println("False Positives: " + falsePositives);
        System.out.println("False Negatives: " + falseNegatives);
        System.out.println("True Positives: " + truePositives);
        System.out.println("Total Pairs (Exact ≥ 0.5): " + totalPairs);
    }
}
