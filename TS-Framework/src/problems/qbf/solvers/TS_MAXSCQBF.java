package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.MaxSCQBF;
import solutions.Solution;


public class TS_MAXSCQBF extends AbstractTS<Integer> {

    private final Integer fake = Integer.valueOf(-1);
    private final MaxSCQBF eval;

    public TS_MAXSCQBF(Integer tenure, Integer iterations, String filename) throws IOException {
        super(new MaxSCQBF(filename), tenure, iterations);
        this.eval = (MaxSCQBF) this.ObjFunction;
    }

    @Override
    public ArrayList<Integer> makeCL() {
        ArrayList<Integer> _CL = new ArrayList<>();
        for (int i = 0; i < ObjFunction.getDomainSize(); i++) 
        	_CL.add(i);
        return _CL;
    }

    @Override
    public ArrayList<Integer> makeRCL() { 
    	return new ArrayList<>(); 
    }

    @Override
    public ArrayDeque<Integer> makeTL() {
        ArrayDeque<Integer> _TS = new ArrayDeque<>(2 * tenure);
        for (int i = 0; i < 2 * tenure; i++) 
        	_TS.add(fake);
        return _TS;
    }

    @Override
    public void updateCL() {
        // sem heurística adicional por enquanto
    }

    @Override
    public Solution<Integer> createEmptySol() {
        Solution<Integer> sol = new Solution<>();
        sol.cost = 0.0;
        eval.resetCoverage();
        return sol;
    }

    @Override
    public Solution<Integer> neighborhoodMove() {
        // 0) garantir contadores de cobertura consistentes
        eval.applySolutionToCoverage(sol);

        // 0.1) se ainda não é factível, inserir UM conjunto que cubra algo descoberto e retornar
        if (!eval.isFeasible()) {
            int uncovered = -1;
            for (int k = 0; k < eval.n; k++) {
                if (eval.coverCount[k] == 0) { 
                	uncovered = k; break; 
                }
            }
            if (uncovered >= 0) {
                Integer bestCand = null;
                double bestDelta = Double.POSITIVE_INFINITY;
                for (Integer candIn : CL) {
                    if (eval.covers(candIn, uncovered)) {
                        double d = ObjFunction.evaluateInsertionCost(candIn, sol);
                        if (d < bestDelta) { bestDelta = d; bestCand = candIn; }
                    }
                }
                if (bestCand != null) {
                    TL.poll(); 
                    TL.add(fake);
                    TL.poll(); 
                    TL.add(bestCand);
                    sol.add(bestCand);
                    for (Integer k : eval.S[bestCand]) eval.coverCount[k]++;
                    CL.remove(bestCand);
                    ObjFunction.evaluate(sol);
                }
            }
            return sol;
        }

        // 1) busca local padrão (ADD, DROP, SWAP) com cobertura respeitada
        Double minDeltaCost = Double.POSITIVE_INFINITY;
        Integer bestCandIn = null, bestCandOut = null;

        updateCL();

        // 1.1) Avalia insertions
        for (Integer candIn : CL) {
            Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, sol);
            if (!TL.contains(candIn) || sol.cost + deltaCost < bestSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = candIn;
                    bestCandOut = null;
                }
            }
        }

        // 1.2) Avalia removals (somente se não quebra cobertura)
        for (Integer candOut : sol) {
            if (eval.removalBreaksCoverage(candOut)) 
            	continue;
            Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, sol);
            if (!TL.contains(candOut) || sol.cost + deltaCost < bestSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = null;
                    bestCandOut = candOut;
                }
            }
        }

        // 1.3) Avalia exchanges (somente se o swap preservar cobertura)
        for (Integer candIn : CL) {
            for (Integer candOut : sol) {
                if (!swapPreservaCobertura(candOut, candIn)) 
                	continue;
                Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                if ((!TL.contains(candIn) && !TL.contains(candOut)) || sol.cost + deltaCost < bestSol.cost) {
                    if (deltaCost < minDeltaCost) {
                        minDeltaCost = deltaCost;
                        bestCandIn = candIn;
                        bestCandOut = candOut;
                    }
                }
            }
        }

        // 2) Aplica o melhor movimento não-tabu (ou tabu com aspiração)
        TL.poll();
        if (bestCandOut != null) {
            sol.remove(bestCandOut);
            for (Integer k : eval.S[bestCandOut]) 
            	eval.coverCount[k]--;
            CL.add(bestCandOut);
            TL.add(bestCandOut);
        } else {
            TL.add(fake);
        }

        TL.poll();
        if (bestCandIn != null) {
            sol.add(bestCandIn);
            for (Integer k : eval.S[bestCandIn]) eval.coverCount[k]++;
            CL.remove(bestCandIn);
            TL.add(bestCandIn);
        } else {
            TL.add(fake);
        }

        ObjFunction.evaluate(sol);
        return sol;
    }

    private boolean swapPreservaCobertura(int out, int in) {
        for (Integer k : eval.S[out]) {
            if (eval.coverCount[k] == 1 && !eval.covers(in, k)) return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        String inst = (args.length > 0) ? args[0] : "instances/max-sc-qbf/1-max_sc_qbf-n_25-k_3.txt";
        TS_MAXSCQBF tabusearch = new TS_MAXSCQBF(20, 1000, inst);
        Solution<Integer> bestSol = tabusearch.solve();
        System.out.println("Best = " + bestSol + " | max = " + (-bestSol.cost));
        long endTime = System.currentTimeMillis();
        System.out.println("Time = " + (endTime - startTime) / 1000.0 + " s");
    }
}
