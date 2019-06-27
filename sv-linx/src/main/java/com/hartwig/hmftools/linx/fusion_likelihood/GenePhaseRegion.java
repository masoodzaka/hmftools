package com.hartwig.hmftools.linx.fusion_likelihood;

import static java.lang.Math.abs;

import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_0;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_1;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_2;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_5P_UTR;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_MAX;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.PHASE_NON_CODING;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.intAsType;
import static com.hartwig.hmftools.linx.fusion_likelihood.GenePhaseType.typeAsInt;

import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenePhaseRegion
{
    public final String GeneId;
    public GenePhaseType Phase;

    private long mStart;
    private long mEnd;

    private boolean[] mPhaseArray;
    private boolean[] mPreGenePhaseStatus;
    private int mCombinedPhase;
    private int mCombinedPreGeneStatus;

    private boolean mHasOverlaps;
    private int mTransId;

    public GenePhaseRegion(final String geneId, long start, long end, GenePhaseType phase)
    {
        GeneId = geneId;
        Phase = phase;
        mStart = start;
        mEnd = end;

        mHasOverlaps = false;
        mTransId = 0;

        mPhaseArray = new boolean[GenePhaseType.values().length];
        mPreGenePhaseStatus = new boolean[GenePhaseType.values().length];

        mPhaseArray[typeAsInt(phase)] = true;
        calcCombinedPhase();
    }

    public GenePhaseRegion(final String geneId, long start, long end, final boolean[] phaseArray, final boolean[] preGeneArray)
    {
        GeneId = geneId;
        Phase = PHASE_5P_UTR; // will not be used
        mStart = start;
        mEnd = end;
        mTransId = 0;

        mPhaseArray = new boolean[PHASE_MAX];
        mPreGenePhaseStatus = new boolean[GenePhaseType.values().length];
        addPhases(phaseArray, preGeneArray);
        calcCombinedPhase();
    }

    public static GenePhaseRegion from(final GenePhaseRegion other)
    {
        return new GenePhaseRegion(other.GeneId, other.start(), other.end(), other.getPhaseArray(), other.getPreGenePhaseStatus());
    }

    public void setPreGene(boolean toggle, GenePhaseType phase)
    {
        mPreGenePhaseStatus[typeAsInt(phase)] = toggle;
        calcCombinedPhase();
    }

    public void setHasOverlaps(boolean toggle) { mHasOverlaps = toggle; }
    public boolean hasOverlaps() { return mHasOverlaps; }

    public void setTransId(int transId) { mTransId = transId; }
    public int transId() { return mTransId; }

    public static GenePhaseType mapExonPhase(int exonPhase)
    {
        if (exonPhase == 0)
            return PHASE_0;
        else if (exonPhase == 1)
            return PHASE_1;
        else if (exonPhase == 2)
            return PHASE_2;
        else
            return PHASE_5P_UTR;
    }

    public long start() { return mStart; }
    public void setStart(long start) { mStart = start; }

    public long end() { return mEnd; }
    public void setEnd(long end) { mEnd = end; }

    public long length() { return mEnd - mStart; }

    public final boolean[] getPhaseArray() { return mPhaseArray; }
    public final boolean[] getPreGenePhaseStatus() { return mPreGenePhaseStatus; }

    public void addPhases(final boolean[] phases, final boolean[] preGeneArray)
    {
        if (phases.length != mPhaseArray.length)
            return;

        for (int i = 0; i < PHASE_MAX; ++i)
        {
            mPhaseArray[i] |= phases[i];
            mPreGenePhaseStatus[i] |= preGeneArray[i];
        }

        calcCombinedPhase();
    }

    public boolean hasPhase(GenePhaseType phase)
    {
        return mPhaseArray[typeAsInt(phase)];
    }

    public boolean hasPhaseOnly(GenePhaseType phase)
    {
        int phaseInt = typeAsInt(phase);
        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(mPhaseArray[i] && phaseInt != i)
                return false;
            if(!mPhaseArray[i] && phaseInt == i)
                return false;
        }

        return true;
    }

    public static boolean hasAnyPhaseMatch(final GenePhaseRegion regionUp, final GenePhaseRegion regionDown, boolean allowPreGeneDown)
    {
        // look for any phase match but exclude non-coding since this is handled in the direction-specific method below
        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(i == typeAsInt(PHASE_NON_CODING))
                continue;

            if(regionUp.getPhaseArray()[i] && regionDown.getPhaseArray()[i])
            {
                if((regionUp.getPreGenePhaseStatus()[i]))
                    continue;

                if((!allowPreGeneDown && regionDown.getPreGenePhaseStatus()[i]))
                    continue;

                return true;
            }
        }

        return false;
    }

    public static boolean regionsPhaseMatched(final GenePhaseRegion regionUp, final GenePhaseRegion regionDown)
    {
        if(regionUp.hasPhase(PHASE_NON_CODING) && regionDown.hasPhase(PHASE_5P_UTR))
            return true;

        if(hasAnyPhaseMatch(regionUp, regionDown, true))
            return true;

        return false;
    }


    public boolean hasAnyPhaseMatch(final boolean[] phaseArray)
    {
        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(mPhaseArray[i] && phaseArray[i])
                return true;
        }

        return false;
    }

    public boolean isAnyPreGene()
    {
        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(mPreGenePhaseStatus[i])
                return true;
        }

        return false;
    }

    public int getCombinedPhase() { return mCombinedPhase; }

    private void calcCombinedPhase()
    {
        mCombinedPhase = calcCombinedPhase(mPhaseArray);
        mCombinedPreGeneStatus = calcCombinedPhase(mPreGenePhaseStatus);
    }

    public static int calcCombinedPhase(final boolean[] phases)
    {
        if(phases.length != PHASE_MAX)
            return -1;

        int combinedPhase = 0;
        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(phases[i])
                combinedPhase += Math.pow(10, i);
        }

        return combinedPhase;
    }

    public int getCombinedPreGeneStatus() { return mCombinedPreGeneStatus; }

    public static int simpleToCombinedPhase(GenePhaseType phase)
    {
        return (int)Math.pow(10, typeAsInt(phase));
    }

    public void populateLengthCounts(long[] counts, boolean allowPreGene)
    {
        if(counts.length != PHASE_MAX)
            return;

        for(int i = 0; i < PHASE_MAX; ++i)
        {
            if(mPhaseArray[i] && allowPreGene == mPreGenePhaseStatus[i])
                counts[i] += length();
        }
    }

    public static boolean haveOverlap(final GenePhaseRegion region1, final GenePhaseRegion region2, int overlapBases)
    {
        // if adjacency is included, then regions which are a base apart are considered an overlap - eg 100-199 and 200-300
        if (region1.end() < region2.start() - overlapBases || region1.start() > region2.end() + overlapBases)
            return false;

        return true;
    }

    public static boolean hasNoOverlappingRegions(final List<GenePhaseRegion> regions)
    {
        for (int i = 0; i < regions.size(); ++i)
        {
            GenePhaseRegion region1 = regions.get(i);

            for (int j = i + 1; j < regions.size(); ++j)
            {
                GenePhaseRegion region2 = regions.get(j);

                if(haveOverlap(region1, region2, 0))
                    return false;
            }
        }

        return true;
    }

    public final String toString()
    {
        return String.format("range(%d - %d) len(%d) phases(%d) preGene(%d)",
                mStart, mEnd, length(), mCombinedPhase, mCombinedPreGeneStatus);
    }


    public static final String PD_DELIMITER = ":";

    private static String boolToStr(boolean value) { return value ? "1" : "0"; }
    private static boolean strToBool(final String value) { return value.equals("1"); }

    public String toCsv(boolean useArray)
    {
        StringJoiner output = new StringJoiner(PD_DELIMITER);
        output.add(String.valueOf(mStart));
        output.add(String.valueOf(mEnd));

        if(useArray)
        {
            for (int i = 0; i < PHASE_MAX; ++i)
            {
                output.add(boolToStr(mPhaseArray[i]));
            }

            for (int i = 0; i < PHASE_MAX; ++i)
            {
                output.add(boolToStr(mPreGenePhaseStatus[i]));
            }
        }
        else
        {
            int phase = typeAsInt(Phase);
            output.add(String.valueOf(phase));
            output.add(boolToStr(mPreGenePhaseStatus[phase]));
        }

        return output.toString();
    }

    public static GenePhaseRegion fromCsv(final String geneId, final String inputStr, boolean useArray)
    {
        String[] items = inputStr.split(PD_DELIMITER);

        int startEndItems = 2;

        if(useArray)
        {
            if (items.length != startEndItems + PHASE_MAX * 2)
                return null;
        }
        else
        {
            if(items.length != 4)
                return null;
        }

        long start = Long.parseLong(items[0]);
        long end = Long.parseLong(items[1]);

        if(useArray)
        {
            boolean[] phases = new boolean[PHASE_MAX];
            boolean[] status = new boolean[PHASE_MAX];

            for (int i = 0; i < PHASE_MAX; ++i)
            {
                phases[i] = strToBool(items[i + startEndItems]);
                status[i] = strToBool(items[i + startEndItems + PHASE_MAX]);
            }

            return new GenePhaseRegion(geneId, start, end, phases, status);
        }
        else
        {
            GenePhaseType phase = intAsType(Integer.parseInt(items[2]));
            boolean preGene = strToBool(items[3]);

            GenePhaseRegion region = new GenePhaseRegion(geneId, start, end, phase);
            region.setPreGene(preGene, phase);
            return region;
        }
    }

}