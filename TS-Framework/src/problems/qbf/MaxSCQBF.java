// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package problems.qbf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import solutions.Solution;


public class MaxSCQBF extends QBF_Inverse {

    public ArrayList<Integer>[] S;
    public int[] coverCount;
    public int n;
    private boolean[][] coversBit;

    public MaxSCQBF(String filename) throws IOException {
        super(filename);
        this.coverCount = new int[this.n];
        this.coversBit  = new boolean[this.n][this.n];

        for (int i = 0; i < this.n; i++) {
            for (int k : this.S[i]) {
                this.coversBit[i][k] = true;
            }
        }
    }

    @Override
    protected Integer readInput(String filename) throws IOException {
        try (Reader r = new BufferedReader(new FileReader(filename))) {
            StreamTokenizer st = new StreamTokenizer(r);
            st.parseNumbers();

            // n
            st.nextToken();
            this.n = (int) st.nval;

            // aloca estruturas
            this.A = new Double[this.n][this.n];

            @SuppressWarnings("unchecked")
            ArrayList<Integer>[] _S = new ArrayList[this.n];
            this.S = _S;

            int[] sizes = new int[this.n];

            // tamanhos de cada S[i]
            for (int i = 0; i < this.n; i++) {
                st.nextToken();
                sizes[i] = (int) st.nval;
            }

            // elementos de cada S[i]
            for (int i = 0; i < n; i++) {
                S[i] = new ArrayList<>(sizes[i]);
                for (int t = 0; t < sizes[i]; t++) {
                    st.nextToken();
                    int k = (int) st.nval;
                    // Instâncias indexadas em [0, n-1]
                    if (k < 0 || k >= n) {
                        throw new IOException("Elemento de S[" + (i + 1) + "] fora de [0," + (n - 1) + "]: k=" + k);
                    }
                    S[i].add(k);
                }
            }

            // matriz A (triangular superior); abaixo da diagonal = 0.0
            for (int i = 0; i < this.n; i++) {
                for (int j = i; j < this.n; j++) {
                    st.nextToken();
                    this.A[i][j] = st.nval;
                    if (j > i) this.A[j][i] = 0.0;
                }
            }

            return this.n;
        }
    }

    public void resetCoverage() {
        Arrays.fill(this.coverCount, 0);
    }

    public void applySolutionToCoverage(Solution<Integer> sol) {
        this.resetCoverage();
        for (int i : sol) {
            for (int k : this.S[i]) {
                this.coverCount[k]++;
            }
        }
    }

    public boolean removalBreaksCoverage(int i) {
        for (int k : this.S[i]) {
            if (this.coverCount[k] == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean covers(int i, int k) {
        return this.coversBit[i][k];
    }

    public boolean isFeasible() {
        for (int k = 0; k < this.n; k++) {
            if (this.coverCount[k] <= 0) {
                return false;
            }
        }
        return true;
    }

    public int violationFromCoverage() {
        int v = 0;
        for (int k = 0; k < this.n; k++) {
            if (this.coverCount[k] == 0) v++;
        }
        return v;
    }

    public int deltaViolationInsertion(int in) {
        int dv = 0;
        for (int k : this.S[in]) {
            if (this.coverCount[k] == 0) {
                dv--;
            }
        }
        return dv;
    }

    public int deltaViolationRemoval(int out) {
        int dv = 0;
        for (int k : this.S[out]) {
            if (this.coverCount[k] == 1) {
                dv++;
            }
        }
        return dv;
    }

    public int deltaViolationExchange(int in, int out) {
        int dv = 0;

        for (int k : this.S[out]) {
            if (this.coverCount[k] == 1 && !this.covers(in, k)) {
                dv++;
            }
        }

        for (int k : this.S[in]) {
            if (this.coverCount[k] == 0) {
                dv--;
            }
        }

        return dv;
    }
}
