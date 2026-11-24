package ro.whistlab.core.ai.engine.inference;

import java.util.*;

/** Internal paired-world accumulator; consumes evaluated values, never deals or random state. */
public final class BidSampleEvidence {
    public record Estimate(int bid, double exactContractProbability, double expectedPoints, int samples) {}

    private final List<Integer> allowed;
    private final boolean safety;
    private final double[] primary, points, exactTotals;
    private final double[][] squaredDifferences;
    private int count;
    private double weightTotal;
    private double squaredWeightTotal;

    public BidSampleEvidence(List<Integer> allowed, int size, boolean safety) {
        this.allowed = List.copyOf(allowed);
        if (allowed.isEmpty()) throw new IllegalArgumentException("No legal bid");
        this.safety = safety;
        primary = new double[size];
        points = new double[size];
        exactTotals = new double[size];
        squaredDifferences = new double[size][size];
    }

    public void add(double[] scores, double[] exact) {
        add(scores, exact, 1.0);
    }

    public void add(double[] scores, double[] exact, double weight) {
        if (!Double.isFinite(weight) || weight <= 0)
            throw new IllegalArgumentException("Sample weight must be finite and positive");
        for (int bid : allowed) {
            double value = safety ? exact[bid] : scores[bid];
            if (!Double.isFinite(value)
                    || !Double.isFinite(scores[bid])
                    || !Double.isFinite(exact[bid]))
                throw new IllegalArgumentException("Non-finite sampled utility");
            primary[bid] += value * weight;
            points[bid] += scores[bid] * weight;
            exactTotals[bid] += exact[bid] * weight;
        }
        for (int a : allowed)
            for (int b : allowed) {
                double d = safety ? exact[a] - exact[b] : scores[a] - scores[b];
                squaredDifferences[a][b] += d * d * weight;
            }
        count++;
        weightTotal += weight;
        squaredWeightTotal += weight * weight;
    }

    public int best() {
        int best = allowed.get(0);
        for (int bid : allowed)
            if (primary[bid] > primary[best] + 1e-12
                    || Math.abs(primary[bid] - primary[best]) < 1e-12
                            && points[bid] > points[best] + 1e-12) best = bid;
        return best;
    }

    public List<Estimate> options() {
        return Collections.unmodifiableList(
                allowed.stream()
                        .map(
                                bid ->
                                        new Estimate(
                                                bid,
                                                weightTotal == 0 ? 0.0 : exactTotals[bid] / weightTotal,
                                                weightTotal == 0 ? 0.0 : points[bid] / weightTotal,
                                                count))
                        .collect(java.util.stream.Collectors.toList()));
    }

    public boolean enough() {
        double effectiveSamples = effectiveSamples();
        if (effectiveSamples < 32) return false;
        if (effectiveSamples >= 128 || allowed.size() == 1) return true;
        int leader = best();
        for (int rival : allowed)
            if (rival != leader) {
                double gap = (primary[leader] - primary[rival]) / weightTotal;
                double variance =
                        Math.max(
                                0,
                                squaredDifferences[leader][rival] / weightTotal - gap * gap);
                if (variance > 1e-15 && gap <= 2 * Math.sqrt(variance / effectiveSamples)) return false;
            }
        return true;
    }

    public double effectiveSamples() {
        return squaredWeightTotal == 0 ? 0 : weightTotal * weightTotal / squaredWeightTotal;
    }

    public int samples() {
        return count;
    }
}
