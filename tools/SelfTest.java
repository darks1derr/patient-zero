import java.util.*;

/** Extra independent checks. Run after Check: java -cp out tools/SelfTest.java */
public class SelfTest {
    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static String dna(Random r, int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append("ACGT".charAt(r.nextInt(4)));
        return s.toString();
    }
    static boolean same(String a, int i, String b, int j, int k) {
        for (int t = 0; t < k; t++) if (a.charAt(i+t) != b.charAt(j+t)) return false;
        return true;
    }
    static int[] naive(String text, String p) {
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i + p.length() <= text.length(); i++)
            if (same(text, i, p, 0, p.length())) hits.add(i);
        return hits.stream().mapToInt(Integer::intValue).toArray();
    }
    static int bruteShared(String a, String b, int k) {
        int result = 0;
        for (int i = 0; i+k <= a.length(); i++) {
            for (int j = 0; j+k <= b.length(); j++) {
                if (same(a, i, b, j, k)) { result++; break; }
            }
        }
        return result;
    }
    static int bruteLcs(String a, String b) {
        int best = 0;
        for (int mask = 0; mask < (1 << a.length()); mask++) {
            int j = 0, length = 0;
            boolean valid = true;
            for (int i = 0; i < a.length(); i++) if ((mask & (1 << i)) != 0) {
                while (j < b.length() && b.charAt(j) != a.charAt(i)) j++;
                if (j == b.length()) { valid = false; break; }
                j++; length++;
            }
            if (valid) best = Math.max(best, length);
        }
        return best;
    }
    public static void main(String[] args) {
        require(Strains.editDistance("", "ACGT") == 4, "empty edit");
        require(Strains.editDistance("GATTACA", "GCATGCA") == 3, "known edit");
        require(Strains.editDistance("ACGT", "ACGT") == 0, "identity edit");
        require(Strains.editDistance("ACGT", "AGT") == 1, "delete edit");
        require(Arrays.equals(Marker.findAll("AAAA", "AA"), new int[]{0,1,2}), "overlaps");
        // These two different DNA words have the same hash under BASE=257, MOD=1e9+7.
        require(Marker.sharedWords("AAAGACGACGTA", "TTACCAGCAACC", 12) == 0, "collision rejected");
        require(Marker.sharedWords("AAAGACGACGTA", "TTACCAGCAACCAAAGACGACGTA", 12) == 1,
                "collision does not hide a real match later in the bucket");
        Random r = new Random(20261108);
        for (int round = 0; round < 1000; round++) {
            String text = dna(r, r.nextInt(35)), p = dna(r, r.nextInt(9));
            require(Arrays.equals(Marker.findAll(text, p), naive(text, p)), "random KMP");
            int[] pi = Marker.prefixFunction(p);
            for (int i = 0; i < p.length(); i++) {
                int border = 0;
                for (int k = 1; k <= i; k++) if (same(p, 0, p, i+1-k, k)) border = k;
                require(pi[i] == border, "random prefix");
            }
            String b = dna(r, r.nextInt(35)); int k = 1+r.nextInt(12);
            require(Marker.sharedWords(text, b, k) == bruteShared(text, b, k), "random rolling hash");
            String aSmall = dna(r, r.nextInt(9)), bSmall = dna(r, r.nextInt(9));
            require(Strains.lcsLength(aSmall, bSmall) == bruteLcs(aSmall, bSmall), "exhaustive LCS");
        }
        for (int round = 0; round < 300; round++) {
            int n = 1+r.nextInt(11);
            int[] x = new int[n-1], y = new int[n-1], cost = new int[n], degree = new int[n];
            for (int i = 1; i < n; i++) { x[i-1]=i; y[i-1]=r.nextInt(i); degree[i]++; degree[y[i-1]]++; }
            int[][] nb = new int[n][];
            for (int i = 0; i < n; i++) { nb[i]=new int[degree[i]]; cost[i]=r.nextInt(15); }
            int[] fill = new int[n];
            for (int i = 0; i < n-1; i++) { nb[x[i]][fill[x[i]]++]=y[i]; nb[y[i]][fill[y[i]]++]=x[i]; }
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1) Arrays.fill(cost, 1);
                long best = Long.MAX_VALUE;
                for (int mask = 0; mask < (1<<n); mask++) {
                    boolean valid = true;
                    for (int i = 0; i < n-1; i++)
                        if ((mask & (1<<x[i])) == 0 && (mask & (1<<y[i])) == 0) valid = false;
                    if (!valid) continue;
                    long sum = 0;
                    for (int i = 0; i < n; i++) if ((mask & (1<<i)) != 0) sum+=cost[i];
                    best = Math.min(best, sum);
                }
                boolean[] chosen = Outbreak.cheapestTests(n, nb, cost); long sum=0;
                for (int i = 0; i < n; i++) if (chosen[i]) sum+=cost[i];
                for (int i = 0; i < n-1; i++) require(chosen[x[i]] || chosen[y[i]], "edge covered");
                require(sum == best, "exhaustive vertex cover");
            }
            int diameter = 0;
            for (int start = 0; start < n; start++) {
                int[] dist = new int[n]; Arrays.fill(dist, -1); dist[start]=0;
                ArrayDeque<Integer> q = new ArrayDeque<>(); q.add(start);
                while (!q.isEmpty()) {
                    int v=q.remove(); diameter=Math.max(diameter, dist[v]);
                    for (int u : nb[v]) if (dist[u] < 0) { dist[u]=dist[v]+1; q.add(u); }
                }
            }
            require(Outbreak.longestChain(n, nb) == diameter, "all-pairs BFS diameter");
        }
        System.out.println("PASS: edge cases, real hash collision, 1000 string cases, 300 trees (two cost models each).");
    }
}
