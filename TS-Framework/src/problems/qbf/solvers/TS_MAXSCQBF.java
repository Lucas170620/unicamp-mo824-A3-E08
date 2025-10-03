package problems.qbf.solvers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.MaxSCQBF;
import solutions.Solution;

public class TS_MAXSCQBF extends AbstractTS<Integer> {

    public enum SearchMode { FIRST_IMPROVING, BEST_IMPROVING }

    private final Integer fake = Integer.valueOf(-1);
    protected final MaxSCQBF eval;
    private final SearchMode mode;

    protected boolean useSO = false;
    protected double lambda = 100.0;
    protected double lambdaMin = 1e-6;
    protected double lambdaMax = 1e9;
    protected double etaUp = 0.10;
    protected double etaDown = 0.05;
    protected int winViable = 25;
    protected int winInfeasible = 25;
    protected int stayViable = 0;
    protected int stayInfeasible = 0;

    public TS_MAXSCQBF(Integer tenure, Integer iterations, String filename) throws IOException {
        this(tenure, iterations, filename, SearchMode.FIRST_IMPROVING);
    }

    public TS_MAXSCQBF(Integer tenure, Integer iterations, String filename, SearchMode mode) throws IOException {
        super(new MaxSCQBF(filename), tenure, iterations);
        this.eval = (MaxSCQBF) this.ObjFunction;
        this.mode = mode;
    }

    public void setSO(boolean on) { this.useSO = on; }
    public void setSOParams(double lambda, double lambdaMin, double lambdaMax,
                            double etaUp, double etaDown, int winViable, int winInfeasible) {
        this.lambda = lambda;
        this.lambdaMin = lambdaMin;
        this.lambdaMax = lambdaMax;
        this.etaUp = etaUp;
        this.etaDown = etaDown;
        this.winViable = winViable;
        this.winInfeasible = winInfeasible;
    }

    @Override
    public ArrayList<Integer> makeCL() {
        ArrayList<Integer> _CL = new ArrayList<>();
        for (int i = 0; i < ObjFunction.getDomainSize(); i++) _CL.add(i);
        return _CL;
    }

    @Override
    public ArrayList<Integer> makeRCL() { return new ArrayList<>(); }

    @Override
    public ArrayDeque<Integer> makeTL() {
        ArrayDeque<Integer> _TS = new ArrayDeque<>(2 * tenure);
        for (int i = 0; i < 2 * tenure; i++) _TS.add(fake);
        return _TS;
    }

    @Override
    public void updateCL() {
    }

    @Override
    public Solution<Integer> createEmptySol() {
        Solution<Integer> s = new Solution<>();
        s.cost = 0.0;
        eval.resetCoverage();
        return s;
    }

    @Override
    public Solution<Integer> neighborhoodMove() {
        return (mode == SearchMode.BEST_IMPROVING) ? neighborhoodMoveBest() : neighborhoodMoveFirst();
    }


    protected Solution<Integer> neighborhoodMoveFirst() {
        eval.applySolutionToCoverage(sol);

        if (!useSO && !eval.isFeasible()) {
            int uncovered = -1;
            for (int k = 0; k < eval.n; k++) if (eval.coverCount[k] == 0) { uncovered = k; break; }
            if (uncovered >= 0) {
                Integer bestCand = null; double bestDelta = Double.POSITIVE_INFINITY;
                for (Integer candIn : CL) {
                    if (!eval.covers(candIn, uncovered)) continue;
                    double d = ObjFunction.evaluateInsertionCost(candIn, sol);
                    if (d < bestDelta) { bestDelta = d; bestCand = candIn; }
                }
                if (bestCand != null) {
                    TL.poll(); TL.add(fake);
                    TL.poll(); TL.add(bestCand);
                    sol.add(bestCand);
                    for (Integer k : eval.S[bestCand]) eval.coverCount[k]++;
                    CL.remove(bestCand);
                    ObjFunction.evaluate(sol);
                    postMoveUpdate();
                }
            }
            return sol;
        }

        final int vNow = eval.violationFromCoverage();
        double bestFallbackScore = Double.POSITIVE_INFINITY;
        Integer bestFallbackIn = null, bestFallbackOut = null;

        updateCL();

        for (Integer candIn : CL) {
            double dCost = ObjFunction.evaluateInsertionCost(candIn, sol);
            int dV = useSO ? eval.deltaViolationInsertion(candIn) : 0;
            double score = useSO ? (dCost + lambda * dV) : dCost;

            boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
            boolean admiss = (!TL.contains(candIn)) || aspiration;

            if (admiss && score < bestFallbackScore) {
                bestFallbackScore = score; bestFallbackIn = candIn; bestFallbackOut = null;
            }
            if (admiss && score < -1e-12) {
                aplicarMovimento(null, candIn);
                postMoveUpdate();
                return sol;
            }
        }

        for (Integer candOut : sol) {
            if (!useSO && eval.removalBreaksCoverage(candOut)) continue;
            
            double dCost = ObjFunction.evaluateRemovalCost(candOut, sol);
            int dV = useSO ? eval.deltaViolationRemoval(candOut) : 0;
            double score = useSO ? (dCost + lambda * dV) : dCost;

            boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
            boolean admiss = (!TL.contains(candOut)) || aspiration;

            if (admiss && score < bestFallbackScore) {
                bestFallbackScore = score; bestFallbackIn = null; bestFallbackOut = candOut;
            }
            if (admiss && score < -1e-12) {
                aplicarMovimento(candOut, null);
                postMoveUpdate();
                return sol;
            }
        }

        for (Integer candIn : CL) {
            for (Integer candOut : sol) {
                if (!useSO && !swapPreservaCobertura(candOut, candIn)) continue;

                double dCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                int dV = useSO ? eval.deltaViolationExchange(candIn, candOut) : 0;
                double score = useSO ? (dCost + lambda * dV) : dCost;

                boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
                boolean admiss = (!TL.contains(candIn) && !TL.contains(candOut)) || aspiration;

                if (admiss && score < bestFallbackScore) {
                    bestFallbackScore = score; bestFallbackIn = candIn; bestFallbackOut = candOut;
                }
                if (admiss && score < -1e-12) {
                    aplicarMovimento(candOut, candIn);
                    postMoveUpdate();
                    return sol;
                }
            }
        }

        aplicarMovimento(bestFallbackOut, bestFallbackIn);
        postMoveUpdate();
        return sol;
    }

    protected Solution<Integer> neighborhoodMoveBest() {
        eval.applySolutionToCoverage(sol);

        if (!useSO && !eval.isFeasible()) {
            int uncovered = -1;
            for (int k = 0; k < eval.n; k++) if (eval.coverCount[k] == 0) { uncovered = k; break; }
            if (uncovered >= 0) {
                Integer bestCand = null; double bestDelta = Double.POSITIVE_INFINITY;
                for (Integer candIn : CL) {
                    if (!eval.covers(candIn, uncovered)) continue;
                    double d = ObjFunction.evaluateInsertionCost(candIn, sol);
                    if (d < bestDelta) { bestDelta = d; bestCand = candIn; }
                }
                if (bestCand != null) {
                    TL.poll(); TL.add(fake);
                    TL.poll(); TL.add(bestCand);
                    sol.add(bestCand);
                    for (Integer k : eval.S[bestCand]) eval.coverCount[k]++;
                    CL.remove(bestCand);
                    ObjFunction.evaluate(sol);
                    postMoveUpdate();
                }
            }
            return sol;
        }

        final int vNow = eval.violationFromCoverage();
        double minScore = Double.POSITIVE_INFINITY;
        Integer bestIn = null, bestOut = null;

        updateCL();

        for (Integer candIn : CL) {
            double dCost = ObjFunction.evaluateInsertionCost(candIn, sol);
            int dV = useSO ? eval.deltaViolationInsertion(candIn) : 0;
            double score = useSO ? (dCost + lambda * dV) : dCost;

            boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
            boolean admiss = (!TL.contains(candIn)) || aspiration;

            if (admiss && score < minScore) {
                minScore = score; bestIn = candIn; bestOut = null;
            }
        }

        for (Integer candOut : sol) {
            if (!useSO && eval.removalBreaksCoverage(candOut)) continue;

            double dCost = ObjFunction.evaluateRemovalCost(candOut, sol);
            int dV = useSO ? eval.deltaViolationRemoval(candOut) : 0;
            double score = useSO ? (dCost + lambda * dV) : dCost;

            boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
            boolean admiss = (!TL.contains(candOut)) || aspiration;

            if (admiss && score < minScore) {
                minScore = score; bestIn = null; bestOut = candOut;
            }
        }

        for (Integer candIn : CL) {
            for (Integer candOut : sol) {
                if (!useSO && !swapPreservaCobertura(candOut, candIn)) continue;

                double dCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                int dV = useSO ? eval.deltaViolationExchange(candIn, candOut) : 0;
                double score = useSO ? (dCost + lambda * dV) : dCost;

                boolean aspiration = (vNow + dV == 0) && (sol.cost + dCost < bestSol.cost);
                boolean admiss = (!TL.contains(candIn) && !TL.contains(candOut)) || aspiration;

                if (admiss && score < minScore) {
                    minScore = score; bestIn = candIn; bestOut = candOut;
                }
            }
        }

        aplicarMovimento(bestOut, bestIn);
        postMoveUpdate();
        return sol;
    }

    protected void aplicarMovimento(Integer out, Integer in) {
        TL.poll();
        if (out != null) {
            sol.remove(out);
            for (Integer k : eval.S[out]) eval.coverCount[k]--;
            CL.add(out);
            TL.add(out);
        } else TL.add(fake);

        TL.poll();
        if (in != null) {
            sol.add(in);
            for (Integer k : eval.S[in]) eval.coverCount[k]++;
            CL.remove(in);
            TL.add(in);
        } else TL.add(fake);

        ObjFunction.evaluate(sol);
    }

    protected boolean swapPreservaCobertura(int out, int in) {
        for (Integer k : eval.S[out]) if (eval.coverCount[k] == 1 && !eval.covers(in, k)) return false;
        return true;
    }

    protected void postMoveUpdate() {
        if (eval.isFeasible() && sol.cost < bestSol.cost) {
            bestSol = new Solution<>(sol);
        }
        if (useSO) {
            if (eval.isFeasible()) {
                stayViable++; stayInfeasible = 0;
                if (stayViable >= winViable) {
                    lambda = Math.max(lambdaMin, lambda * (1.0 - etaDown));
                    stayViable = 0;
                }
            } else {
                stayInfeasible++; stayViable = 0;
                if (stayInfeasible >= winInfeasible) {
                    lambda = Math.min(lambdaMax, lambda * (1.0 + etaUp));
                    stayInfeasible = 0;
                }
            }
        }
    }

    public Solution<Integer> solveWithTimeLimit(long timeLimitMillis, int maxIterations) {
        CL = makeCL(); RCL = makeRCL(); TL = makeTL();
        sol = createEmptySol(); ObjFunction.evaluate(sol);
        bestSol = new Solution<>(sol);

        long start = System.currentTimeMillis();
        int it = 0;
        while (it < maxIterations && (System.currentTimeMillis() - start) < timeLimitMillis) {
            neighborhoodMove();
            it++;
        }
        return bestSol;
    }

    private static class Config {
        String name; SearchMode mode; int tenure; double sample_rate = 0.1;
        Config(String n, SearchMode m, int t) { name = n; mode = m; tenure = t; }
        Config(String n, SearchMode m, int t, double sr) { name = n; mode = m; tenure = t; sample_rate = sr; }
    }

    public static void main(String[] args) throws Exception {
        final String instancesDir   = (args.length > 0) ? args[0] : "instances/max-sc-qbf";
        final String outCsv         = (args.length > 1) ? args[1] : "results/ts_results7.csv";
        final long timeLimitSeconds = (args.length > 2) ? Long.parseLong(args[2]) : 1800L;
        final int maxIterations     = (args.length > 3) ? Integer.parseInt(args[3]) : 1_000_0;
        final int T1                = (args.length > 4) ? Integer.parseInt(args[4]) : 20;
        final int T2                = (args.length > 5) ? Integer.parseInt(args[5]) : (2 * T1);

        final long timeLimitMillis = timeLimitSeconds * 1000L;
        Config[] configs = new Config[] {
            new Config("PADRAO",        SearchMode.FIRST_IMPROVING, T1),
            new Config("PADRAO+BEST",   SearchMode.BEST_IMPROVING,  T1),
            new Config("PADRAO+TENURE", SearchMode.FIRST_IMPROVING, T2),
            new Config("PROBABILISTIC(SR=0.2)", SearchMode.FIRST_IMPROVING, T1, 0.2),
            new Config("PROBABILISTIC(SR=0.6)", SearchMode.FIRST_IMPROVING, T1, 0.6)
        };

        File dir = new File(instancesDir);
        if (!dir.isDirectory()) {
            System.err.println("Diretório de instâncias inválido: " + instancesDir);
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("Nenhuma instância .txt encontrada em: " + instancesDir);
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));

        Pattern pat = Pattern.compile("n_(\\d+).*k_(\\d+)");
        File out = new File(outCsv);
        if (out.getParentFile() != null) out.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)))) {
            pw.println("config,file,n,k,tenure,search_mode,time_limit_s,max_iterations,timed_out,max_value,size,feasible,time_s,elements");

            for (File f : files) {
                String path = f.getPath();
                Matcher m = pat.matcher(f.getName());
                String nStr = "", kStr = "";
                if (m.find()) { nStr = m.group(1); kStr = m.group(2); }
                TS_MAXSCQBF ts;
                for (Config cfg : configs) {
                    long t0 = System.currentTimeMillis();
                    if (cfg.name.contains("PROBABILISTIC")){
                        ts = new TS_MAXSCQBF_Probabilistic(
                                cfg.tenure,
                                maxIterations,
                                path,
                                cfg.mode,
                                cfg.sample_rate
                        );
                    }
                    else {
//                        System.out.printf(Locale.US,
//                                "Set SO!"
//                        );
                        ts = new TS_MAXSCQBF(cfg.tenure, maxIterations, path, cfg.mode);
//                        ts.setSO(true);
//                        ts.setSOParams(10.0, 1e-4, 1e6, 0.10, 0.05, 25, 25);
                    }
                    Solution<Integer> best = ts.solveWithTimeLimit(timeLimitMillis, maxIterations);
                    long t1 = System.currentTimeMillis();

                    double timeSec = (t1 - t0) / 1000.0;
                    boolean timedOut = (t1 - t0) >= timeLimitMillis - 1;

                    MaxSCQBF eval = new MaxSCQBF(path);
                    eval.applySolutionToCoverage(best);
                    boolean feasible = eval.isFeasible();
                    double maxVal = -best.cost;

                    StringBuilder els = new StringBuilder();
                    for (int i = 0; i < best.size(); i++) {
                        if (i > 0) els.append(' ');
                        els.append(best.get(i));
                    }

                    pw.printf(Locale.US,
                        "%s,%s,%s,%s,%d,%s,%.0f,%d,%s,%.6f,%d,%s,%.3f,\"%s\"%n",
                        cfg.name, f.getName(), nStr, kStr, cfg.tenure, cfg.mode.name(),
                        (double) timeLimitSeconds, maxIterations, timedOut ? "true" : "false",
                        maxVal, best.size(), feasible ? "true" : "false",
                        timeSec, els.toString()
                    );

                    System.out.printf(Locale.US,
                        "[%s] %s -> max=%.6f, size=%d, feasible=%s, time=%.3fs, timed_out=%s%n",
                        cfg.name, f.getName(), maxVal, best.size(), feasible, timeSec, timedOut
                    );
                }
            }
        }

        System.out.println("Resultados (TS) salvos em: " + out.getPath());
    }
}
