package com.hartwig.hmftools.linx.chaining;

public class ChainMetrics
{
    public int InternalTIs;
    public int InternalTICnGain;
    public int InternalShortTIs;
    public int ExternalTIs;
    public int ExternalTICnGain;
    public int ExternalShortTIs;
    public int OverlappingTIs;

    public int ChainEndsFace;
    public int ChainEndsAway;

    public ChainMetrics()
    {
        InternalTIs = 0;
        InternalTICnGain = 0;
        InternalShortTIs = 0;
        ExternalTIs = 0;
        ExternalTICnGain = 0;
        ExternalShortTIs = 0;
        OverlappingTIs = 0;
        ChainEndsFace = 0;
        ChainEndsAway = 0;
    }

    public void add(final ChainMetrics other)
    {
        InternalTIs += other.InternalTIs;
        InternalTICnGain += other.InternalTICnGain;
        InternalShortTIs += other.InternalShortTIs;
        ExternalTIs += other.ExternalTIs;
        ExternalTICnGain += other.ExternalTICnGain;
        ExternalShortTIs += other.ExternalShortTIs;
        OverlappingTIs += other.OverlappingTIs;
        ChainEndsFace += other.ChainEndsFace;
        ChainEndsAway += other.ChainEndsAway;
    }
}
