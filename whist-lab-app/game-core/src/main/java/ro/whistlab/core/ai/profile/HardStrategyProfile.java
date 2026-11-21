package ro.whistlab.core.ai.profile;

/** Offline comparison profiles; production CURRENT adds a late burn only after public impossibility. */
public enum HardStrategyProfile {
    CURRENT(0.0),
    PRUDENT_BID_04(0.04),
    PRUDENT_BID(0.08),
    PRUDENT_BID_12(0.12),
    SCORE_FIRST(0.0),
    BURN_IF_DOOMED(0.0);

    private final double bidProbabilityGap;

    HardStrategyProfile(double bidProbabilityGap) { this.bidProbabilityGap=bidProbabilityGap; }

    public double bidProbabilityGap() { return bidProbabilityGap; }
}
