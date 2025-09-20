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
      this.coversBit = new boolean[this.n][this.n];

      int k;
      for(int i = 0; i < this.n; ++i) {
         for(Iterator var4 = this.S[i].iterator(); var4.hasNext(); this.coversBit[i][k] = true) {
            k = (Integer)var4.next();
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
                   if (k < 0 || k >= n) //-> caso for fazer para instancias de 0 a n-1
//                   if (k < 1 || k > n)
                       throw new IOException("Elemento de S[" + (i+1) + "] fora de [1," + n + "]: k=" + k);
                   S[i].add(k); //-> -> caso for fazer para instancias de 0 a n-1
//                   S[i].add(k-1);
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
      Iterator var3 = sol.iterator();

      while(var3.hasNext()) {
         int i = (Integer)var3.next();

         int var10002;
         int k;
         for(Iterator var5 = this.S[i].iterator(); var5.hasNext(); var10002 = this.coverCount[k]++) {
            k = (Integer)var5.next();
         }
      }

   }

   public boolean removalBreaksCoverage(int i) {
      Iterator var3 = this.S[i].iterator();

      while(var3.hasNext()) {
         int k = (Integer)var3.next();
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
      for(int k = 0; k < this.n; ++k) {
         if (this.coverCount[k] <= 0) {
            return false;
         }
      }

      return true;
   }
}
