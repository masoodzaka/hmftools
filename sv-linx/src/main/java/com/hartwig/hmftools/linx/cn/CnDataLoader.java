package com.hartwig.hmftools.linx.cn;

import static java.lang.Math.min;

import static com.hartwig.hmftools.common.purple.segment.SegmentSupport.CENTROMERE;
import static com.hartwig.hmftools.common.purple.segment.SegmentSupport.NONE;
import static com.hartwig.hmftools.common.purple.segment.SegmentSupport.TELOMERE;
import static com.hartwig.hmftools.common.purple.segment.SegmentSupport.UNKNOWN;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantFactory.PON_FILTER_PON;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INF;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INS;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.SGL;
import static com.hartwig.hmftools.linx.cn.LohEvent.CN_DATA_NO_SV;
import static com.hartwig.hmftools.linx.types.SvVarData.ASSEMBLY_TYPE_EQV;
import static com.hartwig.hmftools.linx.types.SvVarData.NONE_SEGMENT_INFERRED;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_END;
import static com.hartwig.hmftools.linx.types.SvVarData.SE_START;
import static com.hartwig.hmftools.linx.types.SvVarData.isStart;
import static com.hartwig.hmftools.linx.types.SvConstants.SHORT_TI_LENGTH;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumberFile;
import com.hartwig.hmftools.common.purple.purity.FittedPurityFile;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.common.purple.segment.SegmentSupport;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantData;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CnDataLoader
{
    private final String mPurpleDataPath;

    private int mRecordId;

    private DatabaseAccess mDbAccess;
    private List<StructuralVariantData> mSvDataList;
    private List<PurpleCopyNumber> mCnRecords;
    private Map<String,List<SvCNData>> mChrCnDataMap; // map of chromosome to CN data items
    private Map<Integer,SvCNData[]> mSvIdCnDataMap; // map of SV Ids to corresponding CN data pair
    private PurityContext mPurityContext;

    private List<LohEvent> mLohEventData;
    private List<HomLossEvent> mHomLossData;
    private Map<String, TelomereCentromereCnData> mChrEndsCNMap; // telemore and centromere CN values
    private CnPloidyCalcs mCnPloidyCalcs;

    public static final double MIN_LOH_CN = 0.5;
    public static final double TOTAL_CN_LOSS = 0.5;

    public static final int P_ARM_TELOMERE_CN = 0;
    public static final int CENTROMERE_CN = 1;
    public static final int Q_ARM_TELOMERE_CN = 2;

    private static final Logger LOGGER = LogManager.getLogger(CnDataLoader.class);

    public CnDataLoader(final String purpleDataPath, DatabaseAccess dbAccess)
    {
        if(purpleDataPath.endsWith(File.separator))
            mPurpleDataPath = purpleDataPath;
        else
            mPurpleDataPath = purpleDataPath + File.separator;

        mDbAccess = dbAccess;
        mChrCnDataMap = Maps.newHashMap();
        mSvDataList = Lists.newArrayList();
        mCnRecords = Lists.newArrayList();
        mChrEndsCNMap = Maps.newHashMap();
        mSvIdCnDataMap = Maps.newHashMap();
        mPurityContext = null;
        mLohEventData = Lists.newArrayList();
        mHomLossData = Lists.newArrayList();
        mRecordId = 0;

        mCnPloidyCalcs = new CnPloidyCalcs(mChrCnDataMap, mSvIdCnDataMap, mSvDataList);
    }

    public final Map<Integer,PloidyCalcData> getSvPloidyCalcMap() { return mCnPloidyCalcs.getSvPloidyCalcMap(); }
    public final List<LohEvent> getLohData() { return mLohEventData; }
    public final List<HomLossEvent> getHomLossData() { return mHomLossData; }
    public final Map<String, TelomereCentromereCnData> getChrTeleCentroData() { return mChrEndsCNMap; }
    public final Map<String,List<SvCNData>> getChrCnDataMap() { return mChrCnDataMap; }
    public final Map<Integer,SvCNData[]> getSvIdCnDataMap() { return mSvIdCnDataMap; }
    public final PurityContext getPurityContext() { return mPurityContext; }
    public final void setPurityContext(final PurityContext context) { mPurityContext = context; }

    public void loadSampleData(final String sampleId, List<StructuralVariantData> svRecords)
    {
        mSvDataList.clear();
        mSvDataList.addAll(svRecords);

        loadCopyNumberData(sampleId);

        createChrCopyNumberMap();

        processSampleData(sampleId);
    }

    public void calculateAdjustedPloidy(final String sampleId)
    {
        mCnPloidyCalcs.calculateAdjustedPloidy(sampleId);
    }

    private void processSampleData(final String sampleId)
    {
        linkCopyNumberAndSvData(sampleId);

        findLohEvents(sampleId);

        mCnPloidyCalcs.calculateAdjustedPloidy(sampleId);
    }

    private void loadCopyNumberData(final String sampleId)
    {
        mChrCnDataMap.clear();
        mCnRecords.clear();

        if(mDbAccess == null)
        {
            try
            {
                mCnRecords = PurpleCopyNumberFile.read(PurpleCopyNumberFile.generateFilenameForReading(mPurpleDataPath, sampleId));
                mPurityContext = FittedPurityFile.read(mPurpleDataPath, sampleId);
            }
            catch(IOException e)
            {
                LOGGER.error("failed to load purity context: {}", e.toString());
                return;
            }
        }
        else
        {
            mCnRecords = mDbAccess.readCopynumbers(sampleId);
            LOGGER.debug("sample({}) retrieved {} CN entries", sampleId, mCnRecords.size());
            mPurityContext = mDbAccess.readPurityContext(sampleId);
        }

        String currentChromosome = "";
        List<SvCNData> cnDataList = null;
        for(final PurpleCopyNumber cnRecord : mCnRecords)
        {
            SvCNData cnData = new SvCNData(cnRecord, ++mRecordId);

            if(!currentChromosome.equals(cnData.Chromosome))
            {
                currentChromosome = cnData.Chromosome;
                cnDataList = Lists.newArrayList();
                mChrCnDataMap.put(cnData.Chromosome, cnDataList);
            }

            cnData.setIndex(cnDataList.size());
            cnDataList.add(cnData);
        }
    }

    private void linkCopyNumberAndSvData(final String sampleId)
    {
        mSvIdCnDataMap.clear();

        int unmatchedSVs = 0;

        for (final StructuralVariantData svData : mSvDataList)
        {
            if (svData.filter().equals(PON_FILTER_PON))
                continue;

            for(int be = SE_START; be <= SE_END; ++be)
            {
                boolean isStart = isStart(be);

                final String svChromosome = isStart ? svData.startChromosome() : svData.endChromosome();
                long svPosition = isStart ? svData.startPosition() : svData.endPosition();
                long svOrientation = isStart ? svData.startOrientation() : svData.endOrientation();

                final List<SvCNData> cnDataList = mChrCnDataMap.get(svChromosome);

                if(cnDataList == null || cnDataList.isEmpty())
                    continue;

                boolean cnDataFound = false;

                for(SvCNData cnData : cnDataList)
                {
                    // ignore orientation during matching since CN change is not a reliable determinant of SV orientation
                    long cnPosition = cnData.StartPos;

                    if(svOrientation == 1)
                        --cnPosition;

                    if (svPosition != cnPosition)
                        continue;

                    if(!svData.type().toString().equals(cnData.SegStart))
                    {
                        if((svData.type() == SGL || svData.type() == INF) && svData.filter().equals(NONE_SEGMENT_INFERRED) && cnData.matchesSegment(NONE, true))
                        {
                            // SGL inferred == NONE in CN table
                        }
                        else
                        {
                            // allow matches if the position if correct
                            LOGGER.trace("SV({} chr={} pos={} type={}) matches {} CN segment: id({})",
                                    svData.id(), svChromosome, svPosition, svData.type(), cnData.SegStart, cnData.id());
                        }
                    }

                    // a match has been found

                    // don't override with a duplicate breakend
                    boolean isDuplicateSgl = svData.type() == SGL && svData.startLinkedBy().contains(ASSEMBLY_TYPE_EQV);

                    if(cnData.getStructuralVariantData() == null || !isDuplicateSgl)
                    {
                        cnData.setStructuralVariantData(svData, isStart);
                    }

                    SvCNData[] cnDataItems = mSvIdCnDataMap.get(svData.id());

                    if(cnDataItems == null)
                    {
                        cnDataItems = new SvCNData[SE_END +1];
                        mSvIdCnDataMap.put(svData.id(), cnDataItems);
                    }

                    cnDataItems[be] = cnData;
                    cnDataFound = true;
                }

                if(!cnDataFound && svData.type() != INS)
                {
                    LOGGER.debug("SV({} chr={} pos={} type={}) {} unmatched)",
                            svData.id(), svChromosome, svPosition, svData.type(), isStart ? "start" : "end");

                    ++unmatchedSVs;
                }
            }
        }

        if(unmatchedSVs > 0)
        {
            LOGGER.warn("sample({}) has {} unmatched CN-SV segments", sampleId, unmatchedSVs);
        }
    }

    private boolean isSingleVariant(final SvCNData cnData)
    {
        final StructuralVariantData svData = cnData.getStructuralVariantData();

        if(svData == null)
            return false;

        long svPosStart = svData.startOrientation() == -1 ? cnData.StartPos : cnData.StartPos - 1;
        long svPosEnd = cnData.EndPos + 1;

        return (svData.startPosition() == svPosStart && svData.endPosition() == svPosEnd);
    }

    private boolean skipMaleChromosomes(final String chromosome)
    {
        if(chromosome.equals("X") || chromosome.equals("Y"))
        {
            if (mPurityContext != null && mPurityContext.gender().toString().startsWith("MALE"))
            {
                return true;
            }
        }

        return false;
    }

    // private static String SPECIFIC_CHR = "9";
    private static String SPECIFIC_CHR = "";
    private static int REMOTE_SV_DISTANCE = 1000000;

    private void findLohEvents(final String sampleId)
    {
        mLohEventData.clear();
        mHomLossData.clear();

        // walk through the CN records looking for any loss of hetrozygosity, defined as minor allele ploidy < 0.5
        // and when CN rises back above this level, consider the LOH section ended
        boolean isLohSection = false;
        double lohMinCN = 0;
        double lastMinCN = 0;
        double priorCN = 0; // before the LOH segment
        int lohSegments = 0;
        SvCNData lohStartCnData = null;
        int lohSectionCount = 0;
        int lohSVsMatchedCount = 0;
        boolean lohOnStartTelomere = false;
        boolean totalLoss = false;
        List<HomLossEvent> lohHomLossEvents = Lists.newArrayList();

        for(Map.Entry<String,List<SvCNData>> entry : mChrCnDataMap.entrySet())
        {
            final String chromosome = entry.getKey();

            if(skipMaleChromosomes(chromosome))
                continue;

            final List<SvCNData> cnDataList = entry.getValue();

            for(int index = 0; index < cnDataList.size(); ++index)
            {
                final SvCNData cnData = cnDataList.get(index);

                double minCN = cnData.minorAllelePloidy();

                boolean newChromosome = (index == 0);
                boolean reset = newChromosome;

                if (newChromosome && cnData.Chromosome.equals(SPECIFIC_CHR))
                {
                    LOGGER.debug("spec chr({})", SPECIFIC_CHR);
                }

                if (cnData.CopyNumber < TOTAL_CN_LOSS)
                {
                    // record homozygous loss
                    final SvCNData nextCnData = index < cnDataList.size() - 1 ? cnDataList.get(index + 1) : null;

                    StructuralVariantData svData = findSvData (cnData, 1);
                    StructuralVariantData nextSvData = nextCnData != null ? findSvData (nextCnData, -1) : null;

                    HomLossEvent homLoss = new HomLossEvent(chromosome, cnData.StartPos, cnData.EndPos,
                            cnData.SegStart, cnData.SegEnd,
                            svData != null ? svData.id() : CN_DATA_NO_SV,
                            nextSvData != null ? nextSvData.id() : CN_DATA_NO_SV);

                    LOGGER.trace("chromosome({}) HOM-loss at positions({} -> {}) segments({} - {})",
                            chromosome, homLoss.PosStart, homLoss.PosEnd, homLoss.SegStart, homLoss.SegEnd);

                    mHomLossData.add(homLoss);

                    if(isLohSection || lohOnStartTelomere)
                        lohHomLossEvents.add(homLoss);
                }

                if (isLohSection || lohOnStartTelomere)
                {
                    boolean lohRegained = (minCN >= MIN_LOH_CN);

                    // check that an SV with correct orientation exists here
                    if (lohRegained && cnData.matchesSV(true) && findSvData(cnData, -1) == null)
                    {
                        lohRegained = false;

                        LOGGER.debug("skipping LOH end segment({}) with no SV match", cnData);
                    }

                    if (lohRegained || reset)
                    {
                        // check for a short isolated TI and if found continue with the LOH
                        if (lohRegained && cnData.EndPos - cnData.StartPos <= SHORT_TI_LENGTH && index < cnDataList.size() - 1)
                        {
                            final SvCNData nextData = cnDataList.get(index + 1);
                            double nextMinCN = nextData.minorAllelePloidy();

                            if (nextData.EndPos - cnData.StartPos > REMOTE_SV_DISTANCE && nextMinCN < MIN_LOH_CN
                            && lohStartCnData != null && cnData.StartPos - lohStartCnData.StartPos > REMOTE_SV_DISTANCE)
                            {
                                LOGGER.trace("chr({}) skipping short isolated TI seg({}) length({})",
                                        chromosome, cnData, cnData.EndPos - cnData.StartPos);
                                continue;
                            }
                        }

                        if (lohOnStartTelomere || totalLoss)
                        {
                            // LOH section invalidated
                            processLOHData(chromosome, lohStartCnData, cnData, lohSegments, false, lohHomLossEvents);

                            lohOnStartTelomere = false;
                            totalLoss = false;
                        }
                        else
                        {
                            // log all relevant data for this completed section
                            lohSVsMatchedCount += processLOHData(
                                    chromosome, lohStartCnData, cnData, lohSegments, false, lohHomLossEvents);
                            ++lohSectionCount;
                        }

                        reset = true;
                    }
                    else if (cnData.matchesSegment(TELOMERE, false))
                    {
                        // rest of arm was lost so no linking SV for LOH section - but still record the event
                        processLOHData(chromosome, lohStartCnData, cnData, lohSegments, true, lohHomLossEvents);
                        reset = true;
                    }
                    else if (cnData.CopyNumber < TOTAL_CN_LOSS)
                    {
                        // other chromatid loss has occurred - this will cancel a valid LOH due to uncertainty unless due to a simple SV
                        if (cnData.matchesSegment(SegmentSupport.DEL, true) && cnData.matchesSegment(SegmentSupport.DEL, false)
                                && isSingleVariant(cnData))
                        {
                            LOGGER.trace("total CN loss matches single SV({} : {} -> {})", chromosome, cnData.StartPos, cnData.EndPos);
                        }
                        else
                        {
                            totalLoss = true;
                        }
                    }
                    else
                    {
                        ++lohSegments;
                        lohMinCN = min(lohMinCN, minCN);
                    }
                }
                else if (!reset)
                {
                    boolean lohLost = minCN < MIN_LOH_CN;

                    // check that an SV with correct orientation exists here
                    StructuralVariantData svData = findSvData(cnData, 1);

                    if (lohLost && cnData.matchesSV(true) && svData == null)
                    {
                        lohLost = false;
                    }
                    else if(svData != null && svData.type() == SGL && svData.startLinkedBy().contains(ASSEMBLY_TYPE_EQV))
                    {
                        lohLost = false;
                    }

                    if (lohLost)
                    {
                        // new LOH section identified
                        isLohSection = true;
                        lohSegments = 1;
                        lohStartCnData = cnData;
                        lohMinCN = minCN;
                        priorCN = lastMinCN;

                        if (lohOnStartTelomere)
                        {
                            LOGGER.trace("chr({}) LOH at telemore", chromosome);
                        }
                        else
                        {
                            LOGGER.trace(String.format("chr(%s) starting LOH at pos(%d) minCN(%.3f cn=%.3f baf=%.3f) priorCN(%.3f)",
                                    chromosome, cnData.StartPos, cnData.CopyNumber, cnData.ActualBaf, lohMinCN, priorCN));

                            // check for segments ending on telomere
                            if (cnData.matchesSegment(TELOMERE, false))
                            {
                                processLOHData(chromosome, lohStartCnData, lohStartCnData, lohSegments, true, null);
                                reset = true;
                            }
                        }
                    }
                }

                if (reset)
                {
                    isLohSection = false;
                    lohOnStartTelomere = false;
                    lohSegments = 0;
                    lohStartCnData = null;
                    totalLoss = false;
                    lohHomLossEvents.clear();

                    if (newChromosome && (minCN < MIN_LOH_CN))
                    {
                        lohOnStartTelomere = true;
                        lohStartCnData = cnData;
                    }
                }

                lastMinCN = minCN;
            }
        }

        LOGGER.debug("sample({}) LOH sections({}) fullMatched({})",
                sampleId, lohSectionCount, lohSVsMatchedCount);
    }

    private StructuralVariantData findSvData(final SvCNData cnData, int requiredOrient)
    {
        if (cnData.matchesSegment(UNKNOWN, true)
        || cnData.matchesSegment(TELOMERE, true)
        || cnData.matchesSegment(CENTROMERE, true))
        {
            return null;
        }

        long svPosition = requiredOrient == -1 ? cnData.StartPos : cnData.StartPos - 1;

        final StructuralVariantData svData = cnData.getStructuralVariantData();

        if (svData == null)
            return null;

        if (cnData.svLinkOnStart() && svData.startOrientation() == requiredOrient && svData.startPosition() == svPosition)
            return svData;
        else if (!cnData.svLinkOnStart() && svData.endOrientation() == requiredOrient && svData.endPosition() == svPosition)
            return svData;
        else
            return null;
    }

    private int processLOHData(final String chr, SvCNData startData, SvCNData endData,
            int segCount, boolean incomplete, List<HomLossEvent> lohHomLossEvents)
    {
        StructuralVariantData startSvData = findSvData(startData, 1);

        StructuralVariantData endSvData = null;
        long lohLength = 0;

        if(incomplete)
        {
            // ended at the telomere
            lohLength = endData.EndPos - startData.StartPos;
            endSvData = null;
        }
        else if(endData.Chromosome.equals(chr) && !startData.matchesSegment(TELOMERE, false))
        {
            lohLength = endData.StartPos - startData.StartPos;
            endSvData = findSvData(endData, -1);
        }
        else
        {
            // segment has either started and/or finished on the telomere segment
            endData = startData;
            lohLength = startData.EndPos - startData.StartPos + 1;
        }

        // exclude an LOH if one boundary is a simple non-overlapping SV and the other is another variant
        if (startData != null && endData != null && startSvData != null && endSvData != null && startSvData.id() != endSvData.id())
        {
            if(startData.matchesSegment(SegmentSupport.DEL, true) || startData.matchesSegment(SegmentSupport.DUP, true))
            {
                // check if the other segment is neighbouring to this one
                SvCNData[] cnDataItems = mSvIdCnDataMap.get(startSvData.id());

                if(cnDataItems != null && cnDataItems[SE_START].getIndex() == cnDataItems[SE_END].getIndex() - 1)
                {
                    LOGGER.debug("segs start({}) and end({}) skipped since bounded by simpleSV({})",
                            startData, endData, startSvData.id());
                    return 1;
                }
            }

            if(endData.matchesSegment(SegmentSupport.DEL, true) || endData.matchesSegment(SegmentSupport.DUP, true))
            {
                // check if the other segment is neighbouring to this one
                SvCNData[] cnDataItems = mSvIdCnDataMap.get(endSvData.id());

                if(cnDataItems != null && cnDataItems != null && cnDataItems[SE_START].getIndex() == cnDataItems[SE_END].getIndex() - 1)
                {
                    LOGGER.debug("segs start({}) and end({}) skipped since bounded by simpleSV({})",
                            startData, endData, endSvData.id());
                    return 1;
                }
            }
        }

        if(LOGGER.isDebugEnabled())
        {
            if (lohLength <= 0)
            {
                LOGGER.warn("negative length({})", lohLength);
            }

            if (startSvData != null && endSvData != null)
            {
                if (startSvData.id() == endSvData.id())
                {
                    LOGGER.trace("segs start({}) and end({}) matches singleSV({} - {})",
                            startData, endData, startSvData.id(), startSvData.type());
                }
                else
                {
                    LOGGER.trace("segs start({}) and end({}) matches pairSV({} -> {})",
                            startData, endData, startSvData.id(), endSvData.id());
                }
            }
            else
            {
                if (!incomplete && lohHomLossEvents == null)
                {
                    if (startSvData == null && startData.matchesSV(true))
                    {
                        boolean incorrectOrientation = (startData.getStructuralVariantData() != null);

                        LOGGER.debug("LOH start seg({}) no SV match: orient({})", startData, incorrectOrientation ? "wrong" : "ok");
                    }

                    if (!incomplete && endSvData == null && endData.matchesSV(true))
                    {
                        boolean incorrectOrientation = (endData.getStructuralVariantData() != null);

                        LOGGER.debug("LOH end segment({}) no SV match: orient({})", endData, incorrectOrientation ? "wrong" : "ok");
                    }
                }

                LOGGER.debug("segs start({}) and end({}) not fully matched pairSV({} -> {})",
                        startData, endData, startSvData != null ? startSvData.id() : "", endSvData != null ? endSvData.id() : "");
            }
        }

        LohEvent lohData = new LohEvent(
                chr,
                startData.StartPos, incomplete ? endData.EndPos : endData.StartPos,
                startData.SegStart, incomplete ? endData.SegEnd : endData.SegStart, segCount,
                startSvData != null ? startSvData.id() : CN_DATA_NO_SV, endSvData != null ? endSvData.id() : CN_DATA_NO_SV);

        if(lohHomLossEvents != null)
            lohData.addHomLossEvents(lohHomLossEvents);

        lohData.setCnData(startData, endData);
        mLohEventData.add(lohData);

        return (startSvData != null && endSvData != null) ? 1 : 0;
    }

    public void createChrCopyNumberMap()
    {
        mChrEndsCNMap.clear();

        for(Map.Entry<String,List<SvCNData>> entry : mChrCnDataMap.entrySet())
        {
            double telomerePArm = 0;
            double telomereQArm = 0;
            double centromerePArm = 0;
            double centromereQArm = 0;

            for(final SvCNData cnData : entry.getValue())
            {
                if(cnData.SegStart.equals(TELOMERE.toString()))
                {
                    telomerePArm = cnData.CopyNumber;
                }

                if(cnData.SegEnd.equals(CENTROMERE.toString()))
                {
                    centromerePArm = cnData.CopyNumber;
                }

                if(cnData.SegStart.equals(CENTROMERE.toString()))
                {
                    centromereQArm = cnData.CopyNumber;
                }

                if(cnData.SegEnd.equals(TELOMERE.toString()))
                {
                    telomereQArm = cnData.CopyNumber;
                }
            }

            mChrEndsCNMap.put(entry.getKey(), new TelomereCentromereCnData(telomerePArm, telomereQArm, centromerePArm, centromereQArm));
        }
    }

}
