import java.util.*;

/**
 * Missions 3 and 4. Find a word in a long text.
 */
public class Marker {

    /**
     * Mission 3a. The prefix function of p. pi[i] = the length of the longest proper prefix of p[0..i] that is
     * also a suffix of p[0..i] ("proper" means shorter than p[0..i] itself).
     * Example: p = "ACGAC" gives [0, 0, 0, 1, 2]. The last number is the "Border length" line.
     */
    public static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1; i < p.length(); i++) {
            int j = pi[i - 1];
            while (j > 0 && p.charAt(i) != p.charAt(j)) j = pi[j - 1];
            if (p.charAt(i) == p.charAt(j)) j++;
            pi[i] = j;
        }
        return pi;
    }

    /**
     * Mission 3b. KMP. All start positions (counting from 0, in increasing order) where p occurs in text.
     * Overlapping occurrences count: "ACG" occurs in "ACGACG" at 0 and 3, and "AA" in "AAA" at 0 and 1.
     * Use the prefix function: the text pointer never goes back. The number of positions is "Marker hits",
     * the first one is "First hit" (-1 when there is none).
     */
    public static int[] findAll(String text, String p) {
        // An empty word occurs at every boundary, including after the last letter.
        if (p.isEmpty()) {
            int[] boundaries = new int[text.length() + 1];
            for (int i = 0; i < boundaries.length; i++) boundaries[i] = i;
            return boundaries;
        }
        int[] pi = prefixFunction(p);
        List<Integer> hits = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != p.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == p.charAt(j)) j++;
            if (j == p.length()) {
                hits.add(i - p.length() + 1);
                j = pi[j - 1]; // Retain a border so overlapping matches are found.
            }
        }
        int[] result = new int[hits.size()];
        for (int i = 0; i < result.length; i++) result[i] = hits.get(i);
        return result;
    }

    /**
     * Mission 4. Rabin-Karp. The number of positions i in a such that the word of k letters starting at i
     * (a[i..i+k-1]) also occurs somewhere in b. Count positions, not different words. This is the "Shared words" line.
     * The idea: give every window of k letters a number (a rolling hash, updated in O(1) when the window moves by one),
     * look the number up among the windows of b, and when it is found COMPARE THE LETTERS, because two different words
     * can have the same hash.
     * Example: a = "ACGTAC", b = "TTACGT", k = 4: the words of a are ACGT, CGTA, GTAC; only ACGT is in b, so the answer is 1.
     */
    public static int sharedWords(String a, String b, int k) {
        if (k <= 0) throw new IllegalArgumentException("Window length must be positive");
        if (a.length() < k || b.length() < k) return 0;
        long power = 1;
        for (int i = 1; i < k; i++) power = power * BASE % MOD;

        Map<Long, List<Integer>> windows = new HashMap<>();
        long hash = firstHash(b, k);
        for (int j = 0; j <= b.length() - k; j++) {
            if (j > 0) hash = slide(hash, b.charAt(j - 1), b.charAt(j + k - 1), power);
            windows.computeIfAbsent(hash, key -> new ArrayList<>()).add(j);
        }

        int count = 0;
        hash = firstHash(a, k);
        for (int i = 0; i <= a.length() - k; i++) {
            if (i > 0) hash = slide(hash, a.charAt(i - 1), a.charAt(i + k - 1), power);
            List<Integer> candidates = windows.get(hash);
            if (candidates == null) continue;
            for (int j : candidates) {
                if (sameWord(a, i, b, j, k)) {
                    count++; // Count this position in A once, even if B has many copies.
                    break;
                }
            }
        }
        return count;
    }

    private static final long BASE = 257;
    private static final long MOD = 1_000_000_007L;

    private static long firstHash(String s, int k) {
        long h = 0;
        for (int i = 0; i < k; i++) h = (h * BASE + s.charAt(i)) % MOD;
        return h;
    }

    private static long slide(long hash, char outgoing, char incoming, long power) {
        long withoutFirst = (hash - outgoing * power % MOD + MOD) % MOD;
        return (withoutFirst * BASE + incoming) % MOD;
    }

    private static boolean sameWord(String a, int i, String b, int j, int k) {
        for (int t = 0; t < k; t++) {
            if (a.charAt(i + t) != b.charAt(j + t)) return false;
        }
        return true;
    }
}
