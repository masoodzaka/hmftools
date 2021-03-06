package com.hartwig.hmftools.linx.analysis;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import static com.hartwig.hmftools.linx.LinxConfig.REF_GENOME_HG37;
import static com.hartwig.hmftools.linx.LinxConfig.REF_GENOME_HG38;
import static com.hartwig.hmftools.linx.LinxConfig.RG_VERSION;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_END;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_START;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.refgenome.RefGenome;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.types.SvVarData;

// common utility methods for SVs

public class SvUtilities {

    public final static String CHROMOSOME_ARM_P = "P"; // short arm, and lower position
    public final static String CHROMOSOME_ARM_Q = "Q";
    public final static String CHROMOSOME_ARM_CENTROMERE = "C";

    public final static int NO_LENGTH = -1;

    public static final RefGenome refGenomeLengths()
    {
        return RG_VERSION == REF_GENOME_HG38 ? RefGenome.HG38 : RefGenome.HG19;
    }

    public static final String CHR_PREFIX = "chr";

    public static String refGenomeChromosome(final String chromosome)
    {
        if(RG_VERSION == REF_GENOME_HG38 && !chromosome.contains(CHR_PREFIX))
            return CHR_PREFIX + chromosome;
        else if(RG_VERSION == REF_GENOME_HG37)
            return stripChromosome(chromosome);
        else
            return chromosome;
    }

    public static String stripChromosome(final String chromosome)
    {
        return chromosome.startsWith(CHR_PREFIX) ? chromosome.substring(CHR_PREFIX.length()) : chromosome;
    }

    public static long getChromosomeLength(final String chromosome)
    {
        Long chrLength = refGenomeLengths().lengths().get(HumanChromosome.fromString(chromosome));
        return chrLength != null ? chrLength : 0;
    }

    public static final String getChromosomalArm(final String chromosome, final long position)
    {
        final Long centromerePos = refGenomeLengths().centromeres().get(HumanChromosome.fromString(chromosome));

        if(centromerePos == null)
            return "INVALID";

        return position < centromerePos ? CHROMOSOME_ARM_P : CHROMOSOME_ARM_Q;
    }

    public static long getChromosomalArmLength(final String chromosome, final String armType)
    {
        final RefGenome refGenome = refGenomeLengths();
        final HumanChromosome chr = HumanChromosome.fromString(chromosome);

        final Long centromerePos = refGenome.centromeres().get(chr);

        if(centromerePos == null)
            return 0;

        if(armType.equals(CHROMOSOME_ARM_P))
        {
            return centromerePos;
        }

        long chrLength = refGenome.lengths().get(chr);

        return chrLength - centromerePos;
    }

    public static boolean isShortArmChromosome(final String chromosome)
    {
        return chromosome.equals("13") || chromosome.equals("14") || chromosome.equals("15")
                || chromosome.equals("21") || chromosome.equals("22");
    }

    public static void addSvToChrBreakendMap(final SvVarData var, Map<String, List<SvBreakend>> chrBreakendMap)
    {
        for(int be = SE_START; be <= SE_END; ++be)
        {
            if(be == SE_END && var.isSglBreakend())
                continue;

            SvBreakend breakend = var.getBreakend(be);

            long position = breakend.position();

            List<SvBreakend> breakendList = chrBreakendMap.get(breakend.chromosome());

            if (breakendList == null)
            {
                breakendList = Lists.newArrayList();
                chrBreakendMap.put(breakend.chromosome(), breakendList);
            }

            // add the variant in order by ascending position
            int index = 0;
            for (; index < breakendList.size(); ++index)
            {
                final SvBreakend otherBreakend = breakendList.get(index);

                if (position < otherBreakend.position())
                    break;

                // special case of inferred placed at another SV's location to explain a CN change
                // ensure the inferred is placed such that it faces its partner breakend
                if(position == otherBreakend.position() && breakend.orientation() != otherBreakend.orientation())
                {
                    if(var.isInferredSgl() && breakend.orientation() == -1)
                        break;
                    else if(otherBreakend.getSV().isInferredSgl() &&  otherBreakend.orientation() == 1)
                        break;
                }
            }

            breakendList.add(index, breakend);
        }
    }

    public static int findCentromereBreakendIndex(final List<SvBreakend> breakendList, final String arm)
    {
        if(breakendList == null || breakendList.isEmpty())
            return -1;

        // return the last breakend list index prior to the centromere from either arm direction,
        // returning an index out of bounds if all breakends are on the other arm
        if(arm == CHROMOSOME_ARM_P)
        {
            int i = 0;
            for(; i < breakendList.size(); ++i)
            {
                if(breakendList.get(i).arm() == CHROMOSOME_ARM_Q)
                    break;
            }

            return i - 1;
        }
        else
        {
            int i = breakendList.size() - 1;
            for(; i >= 0; --i)
            {
                if(breakendList.get(i).arm() == CHROMOSOME_ARM_P)
                    break;
            }

            return i + 1 >= breakendList.size() ? -1 : i + 1;
        }
    }

    public static final String getSvTypesStr(final int[] typeCounts)
    {
        // the following map-based naming convention leads
        // to a predictable ordering of types: INV, CRS, BND, DEL and DUP
        String clusterTypeStr = "";

        for(int i = 0;i < typeCounts.length; ++i)
        {
            if(typeCounts[i] == 0)
                continue;

            if(!clusterTypeStr.isEmpty())
                clusterTypeStr += "_";

            clusterTypeStr += StructuralVariantType.values()[i] + "=" + typeCounts[i];
        }

        return clusterTypeStr;
    }

    public static String appendStr(final String dest, final String source, char delim)
    {
        return dest.isEmpty() ? source : dest + delim + source;
    }

    public static String appendStrList(final List<String> sourceList, char delim)
    {
        String combinedStr = "";
        for(String src : sourceList)
        {
            combinedStr = appendStr(combinedStr, src, ';');
        }

        return combinedStr;
    }

    public static boolean isWithin(final SvVarData variant, final String chromosome, final long position)
    {
        if(!variant.chromosome(true).equals(chromosome) || !variant.chromosome(false).equals(chromosome))
            return false;

        if(variant.position(true) > position || variant.position(false) < position)
            return false;

        return true;
    }

    public static boolean isOverlapping(final SvVarData v1, final SvVarData v2)
    {
        // tests if either variant has an end within the other variant
        if(isWithin(v2, v1.chromosome(true), v1.position(true))
        || isWithin(v2, v1.chromosome(false), v1.position(false)))
        {
            return true;
        }

        if(isWithin(v1, v2.chromosome(true), v2.position(true))
        || isWithin(v1, v2.chromosome(false), v2.position(false)))
        {
            return true;
        }

        return false;
    }

    public static long getProximity(final SvVarData var1, final SvVarData var2)
    {
        long minDistance = -1;

        for(int se1 = SE_START; se1 <= SE_END; ++se1)
        {
            SvBreakend be1 = var1.getBreakend(se1);

            if(be1 == null)
                continue;

            for(int se2 = SE_START; se2 <= SE_END; ++se2)
            {
                SvBreakend be2 = var2.getBreakend(se2);

                if(be2 == null)
                    continue;

                if(be1.chromosome().equals(be2.chromosome()))
                {
                    long distance = abs(be1.position() - be2.position());
                    if(minDistance == -1 || distance < minDistance)
                        minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    public static int calcConsistency(final List<SvVarData> svList)
    {
        int consistencyCount = 0;

        for(final SvVarData var : svList)
        {
            consistencyCount += calcConsistency(var);
        }

        return consistencyCount;
    }

    public static int calcConsistency(final SvVarData var)
    {
        int consistencyCount = 0;
        for(int be = SE_START; be <= SE_END; ++be)
        {
            if(be == SE_END && var.isSglBreakend())
                continue;

            consistencyCount += calcConsistency(var.getBreakend(be));
        }

        return consistencyCount;
    }

    public static int calcConsistency(final SvBreakend breakend)
    {
        return (breakend.arm() == CHROMOSOME_ARM_P ? 1 : -1) * breakend.orientation();
    }

    public static double MAX_COPY_NUM_DIFF = 0.5;
    public static double MAX_COPY_NUM_DIFF_PERC = 0.15;

    public static boolean copyNumbersEqual(double cn1, double cn2)
    {
        return copyNumbersEqual(cn1, cn2, MAX_COPY_NUM_DIFF, MAX_COPY_NUM_DIFF_PERC);
    }

    public static boolean copyNumbersEqual(double cn1, double cn2, double maxDiff, double maxDiffPerc)
    {
        double copyNumDiff = abs(cn2 - cn1);
        double copyNumDiffPerc = copyNumDiff / max(abs(cn1), abs(cn2));

        if (copyNumDiff > maxDiff && copyNumDiffPerc > maxDiffPerc)
            return false;

        return true;
    }

    public static String formatPloidy(double ploidy)
    {
        if(ploidy > 10)
            return String.format("%.0f", ploidy);
        else if(ploidy < 0.5)
            return String.format("%.2f", ploidy);
        else
            return String.format("%.1f", ploidy);
    }

    public static final String makeChrArmStr(final SvVarData var, boolean useStart)
    {
        return makeChrArmStr(var.chromosome(useStart), var.arm(useStart));
    }

    public static final String makeChrArmStr(final String chr, final String arm) { return chr + "_" + arm; }

}
