package problems.qbf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;

import solutions.Solution;


public class MaxSCQBF extends QBF_Inverse {

    /** S[i] = elementos cobertos por i. */
    public ArrayList<Integer>[] S;

    /** coverCount[k] = quantas vezes k está coberto na solução atual. */
    public int[] coverCount;

    /** número de variáveis/elementos. */
    public int n;

    /** mapa rápido de cobertura: coversBit[i][k] == true se i cobre k. */
    private boolean[][] coversBit;

    public MaxSCQBF(String filename) throws IOException {
        super(filename);
        this.coverCount = new int[n];
        this.coversBit  = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int k : S[i]) coversBit[i][k] = true;
        }
    }

    @Override
    protected Integer readInput(String filename) throws IOException {
        try (Reader r = new BufferedReader(new FileReader(filename))) {
            StreamTokenizer st = new StreamTokenizer(r);
            st.parseNumbers();

            st.nextToken();
            n = (int) st.nval;

            A = new Double[n][n];
            @SuppressWarnings("unchecked")
            ArrayList<Integer>[] _S = new ArrayList[n];
            S = _S;

            int[] sizes = new int[n+1];

            for (int i = 0; i < n; i++) {
                st.nextToken();
                sizes[i] = (int) st.nval;
            }

            for (int i = 0; i < n; i++) {
                S[i] = new ArrayList<>(sizes[i]);
                for (int t = 0; t < sizes[i]; t++) {
                    st.nextToken();
                    int k = (int) st.nval;
                    /*if (k < 0 || k >= n) -> caso for fazer para instancias de 0 a n-1*/
                    if (k < 1 || k > n)
                        throw new IOException("Elemento de S[" + (i+1) + "] fora de [1," + n + "]: k=" + k);
                    /*S[i].add(k); -> -> caso for fazer para instancias de 0 a n-1*/
                    S[i].add(k-1);
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    st.nextToken();
                    A[i][j] = st.nval;
                    if (j > i) A[j][i] = 0.0;
                }
            }
        }
        return n;
    }

    public void resetCoverage() { Arrays.fill(coverCount, 0); }

    public void applySolutionToCoverage(Solution<Integer> sol) {
        resetCoverage();
        for (int i : sol) for (int k : S[i]) coverCount[k]++;
    }

    public boolean removalBreaksCoverage(int i) {
        for (int k : S[i]) if (coverCount[k] == 1) return true;
        return false;
    }

    public boolean covers(int i, int k) { return coversBit[i][k]; }

    public boolean isFeasible() {
        for (int k = 0; k < n; k++) if (coverCount[k] <= 0) return false;
        return true;
    }
}
