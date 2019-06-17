package com.hartwig.hmftools.linx.fusion;

import static com.hartwig.hmftools.linx.fusion.GenePhaseType.PHASE_NON_CODING;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData;
import com.hartwig.hmftools.linx.analysis.SvUtilities;

public class GeneRangeData
{
    public final EnsemblGeneData GeneData;
    public final String Arm;
    private List<GenePhaseRegion> mPhaseRegions;
    private List<GenePhaseRegion> mCombinedPhaseRegions;

    // maps from the DEL or DUP bucket length array index to overlap count
    private Map<Integer,Long> mDelFusionBaseCounts;
    private Map<Integer,Long> mDupFusionBaseCounts;

    private long[] mBaseOverlapCountDownstream;
    private long[] mBaseOverlapCountUpstream;

    public static final int NON_PROX_TYPE_SHORT_INV = 0;
    public static final int NON_PROX_TYPE_LONG_SAME_ARM = 1;
    public static final int NON_PROX_TYPE_REMOTE = 2;

    public GeneRangeData(final EnsemblGeneData geneData)
    {
        GeneData = geneData;
        mPhaseRegions = Lists.newArrayList();
        mCombinedPhaseRegions = Lists.newArrayList();

        Arm = SvUtilities.getChromosomalArm(geneData.Chromosome, geneData.GeneStart);

        mDelFusionBaseCounts = Maps.newHashMap();
        mDupFusionBaseCounts = Maps.newHashMap();

        mBaseOverlapCountUpstream = new long[NON_PROX_TYPE_REMOTE+1];
        mBaseOverlapCountDownstream = new long[NON_PROX_TYPE_REMOTE+1];
    }

    public final List<GenePhaseRegion> getPhaseRegions() { return mPhaseRegions; }
    public void addPhaseRegions(List<GenePhaseRegion> regions) { mPhaseRegions.addAll(regions); }

    public final List<GenePhaseRegion> getCombinedPhaseRegions() { return mCombinedPhaseRegions; }
    public void setCombinedPhaseRegions(List<GenePhaseRegion> regions) { mCombinedPhaseRegions = regions; }

    public Map<Integer,Long> getDelFusionBaseCounts() { return mDelFusionBaseCounts; }
    public Map<Integer,Long> getDupFusionBaseCounts() { return mDupFusionBaseCounts; }

    public boolean hasCodingTranscripts()
    {
        return mPhaseRegions.stream().anyMatch(x -> x.Phase != PHASE_NON_CODING);
    }

    public long getBaseOverlapCountUpstream(int type) { return mBaseOverlapCountUpstream[type]; }
    public void addBaseOverlapCountUpstream(int type, long count) { mBaseOverlapCountUpstream[type] += count; }
    public long getBaseOverlapCountDownstream(int type) { return mBaseOverlapCountDownstream[type]; }
    public void addBaseOverlapCountDownstream(int type, long count) { mBaseOverlapCountDownstream[type] += count; }
}
