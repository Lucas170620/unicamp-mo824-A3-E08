package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import solutions.Solution;

public class TS_MAXSCQBF_Probabilistic extends TS_MAXSCQBF {

     // Taxa de amostragem para movimentos de inserção
    private final double sampleRate;

    //Taxa de amostragem para movimentos de remoção/troca
    public TS_MAXSCQBF_Probabilistic(
            Integer tenure,
            Integer iterations,
            String filename,
            SearchMode mode,
            double sampleRate
    ) throws IOException {
        super(tenure, iterations, filename, mode);
        this.sampleRate =  sampleRate;
    }

    private <T> List<T> createSample(List<T> source, double rate) {
        if (rate >= 1.0) {
            return new ArrayList<>(source);
        }
        if (rate <= 0.0 || source.isEmpty()) {
            return new ArrayList<>();
        }
        int sampleSize = (int) Math.ceil(source.size() * rate);
        ArrayList<T> temp = new ArrayList<>(source);
        Collections.shuffle(temp, rng);
        return temp.subList(0, Math.min(sampleSize, temp.size()));
    }

    @Override
    public Solution<Integer> neighborhoodMove() {
        return super.neighborhoodMove();
    }

    @Override
    protected Solution<Integer> neighborhoodMoveBest() {
        eval.applySolutionToCoverage(sol);
        if (!eval.isFeasible()) {
            return super.neighborhoodMoveBest();
        }

        List<Integer> clSample = createSample(this.CL, this.sampleRate);

        Double minDelta = Double.POSITIVE_INFINITY;
        Integer bestIn = null, bestOut = null;

        updateCL();

        // Busca por melhor movimento de INSERÇÃO na amostra
        for (Integer candIn : clSample) {
            double d = ObjFunction.evaluateInsertionCost(candIn, sol);
            boolean isAdmissible = (!TL.contains(candIn)) || (sol.cost + d < bestSol.cost);
            if (isAdmissible && d < minDelta) {
                minDelta = d;
                bestIn = candIn;
                bestOut = null;
            }
        }

        // Busca por melhor movimento de REMOÇÃO na amostra
        for (Integer candOut : sol) {
            if (eval.removalBreaksCoverage(candOut)) continue;
            double d = ObjFunction.evaluateRemovalCost(candOut, sol);
            boolean isAdmissible = (!TL.contains(candOut)) || (sol.cost + d < bestSol.cost);
            if (isAdmissible && d < minDelta) {
                minDelta = d;
                bestIn = null;
                bestOut = candOut;
            }
        }

        //  Busca por melhor movimento de TROCA entre as amostras
        for (Integer candIn : clSample) {
            for (Integer candOut : sol) {
                if (!swapPreservaCobertura(candOut, candIn)) continue;
                double d = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                boolean isAdmissible = (!TL.contains(candIn) && !TL.contains(candOut)) || (sol.cost + d < bestSol.cost);
                if (isAdmissible && d < minDelta) {
                    minDelta = d;
                    bestIn = candIn;
                    bestOut = candOut;
                }
            }
        }

        aplicarMovimento(bestOut, bestIn);
        return sol;
    }

    //Sobrescreve a busca first-improving para opera sobre uma amostra da vizinhança
    @Override
    protected Solution<Integer> neighborhoodMoveFirst() {
        eval.applySolutionToCoverage(sol);
        if (!eval.isFeasible()) {
            return super.neighborhoodMoveFirst();
        }
        List<Integer> clSample = createSample(this.CL, this.sampleRate);
        Double bestFallbackDelta = Double.POSITIVE_INFINITY;
        Integer bestFallbackIn = null, bestFallbackOut = null;

        updateCL();

        for (Integer candIn : clSample) {
            double delta = ObjFunction.evaluateInsertionCost(candIn, sol);
            boolean isAdmissible = (!TL.contains(candIn)) || (sol.cost + delta < bestSol.cost);
            if (isAdmissible && delta < bestFallbackDelta) {
                bestFallbackDelta = delta;
                bestFallbackIn = candIn;
                bestFallbackOut = null;
            }
            if (isAdmissible && delta < -1e-12) {
                aplicarMovimento(null, candIn);
                return sol;
            }
        }

        for (Integer candOut : sol) {
            if (eval.removalBreaksCoverage(candOut)) continue;
            double delta = ObjFunction.evaluateRemovalCost(candOut, sol);
            boolean isAdmissible = (!TL.contains(candOut)) || (sol.cost + delta < bestSol.cost);
            if (isAdmissible && delta < bestFallbackDelta) {
                bestFallbackDelta = delta;
                bestFallbackIn = null;
                bestFallbackOut = candOut;
            }
            if (isAdmissible && delta < -1e-12) {
                aplicarMovimento(candOut, null);
                return sol;
            }
        }

        for (Integer candIn : clSample) {
            for (Integer candOut : sol) {
                if (!swapPreservaCobertura(candOut, candIn)) continue;
                double delta = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                boolean isAdmissible = (!TL.contains(candIn) && !TL.contains(candOut)) || (sol.cost + delta < bestSol.cost);
                if (isAdmissible && delta < bestFallbackDelta) {
                    bestFallbackDelta = delta;
                    bestFallbackIn = candIn;
                    bestFallbackOut = candOut;
                }
                if (isAdmissible && delta < -1e-12) {
                    aplicarMovimento(candOut, candIn);
                    return sol;
                }
            }
        }
        aplicarMovimento(bestFallbackOut, bestFallbackIn);
        return sol;
    }
}