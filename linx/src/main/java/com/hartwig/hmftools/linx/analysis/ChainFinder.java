package com.hartwig.hmftools.linx.analysis;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DEL;
import static com.hartwig.hmftools.linx.analysis.LinkFinder.getMinTemplatedInsertionLength;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.types.SvChain;
import com.hartwig.hmftools.linx.types.SvCluster;
import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* ChainFinder - forms one or more chains from the SVs in a cluster

    Set-up:
    - form all assembled links and connect into chains
    - identify high-ploidy, foldbacks and complex DUP-type SVs, since these will be prioritised during chaining
    - create a cache of all possible linked pairs
    - create a cache of available breakends, including replicating each on accoriding to the SV's ploidy

    Replication count and ploidy
    - for clusters where all SVs have the same ploidy, none of the replication logic applies
    - in reality all SVs are integer multiples of each other according to their ploidy ratio, but in practice the ploidy min/max
    values need to be used to estimate replication

    Routine:
    - apply priority rules to find the next possible link(s)
    - add the link to an existing chain or a new chain if required
    - remove the breakends & link from further consideration
    - repeat until no further links can be made

    Optimisations:
    - for large clusters with many possible pairs on a chromosomal arm, only find the closest X initially (X = MaxPossiblePairs)
    - then when these possible pairs are exhausted for a breakend, search for more from the last pair's location
    - for foldbacks don't apply this restriction

    Priority rules:
    - Max-Replicated - find the SV(s) with the highest replication count, then select the one with the fewest possible links
    - Single-Option - if a breakend has only one possible link, select this one
    - Foldbacks - look for a breakend which can link to both ends of a foldback
    - Ploidy-Match - starting with the highest ploidy SV, only link SVs of the same ploidy
    - Resolving-SV - only link a high-ploidy SV to a lower one once all ploidy-match links are exhausted
    - Shortest - after all other rules, if there is more than 1 possible link then choose the shortest

    Rule selection
    1. Single-Option
    2. Foldbacks
    3. Ploidy-Match
    4. Max-Replicated (will possibly discard)
    5. Resolving-SV (possbly not relevant after rules 2 & 3 are exhausted)
    6. Shortest

*/

public class ChainFinder
{
    private static final Logger LOGGER = LogManager.getLogger(ChainFinder.class);

    private SvCluster mCluster;
    private boolean mHasReplication;

    // chaining state
    private List<SvLinkedPair> mSkippedPairs;
    private List<SvVarData> mFoldbacks;
    private List<SvVarData> mComplexDupCandidates;
    private List<SvLinkedPair> mUniquePairs;
    private List<SvLinkedPair> mAdjacentMatchingPairs;
    private boolean mSkippedPair; // keep track of any excluded pair or SV without exiting the chaining routine

    private List<SvChain> mPartialChains;
    private int mNextChainId;
    private List<SvVarData> mUnlinkedSVs;
    private Map<SvBreakend,List<SvBreakend>> mUnlinkedBreakendMap;
    private Map<SvVarData,Integer> mSvReplicationMap; // diminishes as SVs are added to chains
    private Map<SvVarData,Integer> mSvOriginalReplicationMap;
    private Map<SvBreakend,Integer> mBreakendLastLinkIndexMap;

    private Map<SvBreakend,List<SvLinkedPair>> mSvBreakendPossibleLinks;
    private Map<String, double[][]> mChrAllelePloidies;

    private int mLinkIndex;
    private String mLinkReason;
    private boolean mIsValid;
    private boolean mLogVerbose;
    private boolean mRunValidation;
    private boolean mUseAllelePloidies;

    // current unused
    private int mMaxPossibleLinks;
    private static int DEFAULT_MAX_POSSIBLE_LINKS = 0;

    public ChainFinder()
    {
        mAdjacentMatchingPairs = Lists.newArrayList();
        mBreakendLastLinkIndexMap = Maps.newHashMap();
        mChrAllelePloidies = Maps.newHashMap();
        mComplexDupCandidates = Lists.newArrayList();
        mFoldbacks = Lists.newArrayList();
        mPartialChains = Lists.newArrayList();
        mSkippedPairs = Lists.newArrayList();
        mSvBreakendPossibleLinks = Maps.newHashMap();
        mSvOriginalReplicationMap = Maps.newHashMap();
        mSvReplicationMap = Maps.newHashMap();
        mUnlinkedSVs = Lists.newArrayList();
        mUniquePairs = Lists.newArrayList();
        mUnlinkedBreakendMap = Maps.newHashMap();

        mHasReplication = false;
        mLogVerbose = false;
        mRunValidation = false;
        mIsValid = true;
        mSkippedPair = false;
        mNextChainId = 0;
        mLinkIndex = 0;
        mMaxPossibleLinks = DEFAULT_MAX_POSSIBLE_LINKS;
        mLinkReason = "";
        mUseAllelePloidies = false;
    }

    public void initialise(SvCluster cluster)
    {
        mNextChainId = 0;
        mLinkIndex = 0;
        mCluster = cluster;
        mHasReplication = mCluster.hasReplicatedSVs();
        mIsValid = true;
        mSkippedPair = false;

        // critical that all state is cleared before the next run
        mAdjacentMatchingPairs.clear();
        mBreakendLastLinkIndexMap.clear();
        mChrAllelePloidies.clear();
        mComplexDupCandidates.clear();
        mFoldbacks.clear();
        mPartialChains.clear();
        mSkippedPairs.clear();
        mSvBreakendPossibleLinks.clear();
        mSvOriginalReplicationMap.clear();
        mSvReplicationMap.clear();
        mUnlinkedBreakendMap.clear();
        mUniquePairs.clear();
        mUnlinkedSVs.clear();

        mFoldbacks.addAll(mCluster.getFoldbacks());
    }

    public void setLogVerbose(boolean toggle)
    {
        mLogVerbose = toggle;
        setRunValidation(toggle);
    }

    public void setRunValidation(boolean toggle) { mRunValidation = toggle; }
    public void setUseAllelePloidies(boolean toggle) { mUseAllelePloidies = toggle; }
    public void setMaxPossibleLinks(int maxLinks)
    {
        if(maxLinks == 0)
            mMaxPossibleLinks = 0;
        else
            mMaxPossibleLinks = max(maxLinks, 2);
    }

    public void formClusterChains(boolean assembledLinksOnly)
    {
        List<SvVarData> svList = Lists.newArrayList(mCluster.getSVs(true));

        if(svList.size() < 2)
            return;

        mCluster.getChains().clear();

        if (mCluster.getSvCount() >= 4)
        {
            LOGGER.debug("cluster({}) starting chaining with assemblyLinks({}) svCount({} rep={})",
                    mCluster.id(), mCluster.getAssemblyLinkedPairs().size(), mCluster.getSvCount(), mCluster.getSvCount(true));
        }

        // mLogWorking = isSpecificCluster(mCluster);

        // isSpecificCluster(mCluster);

        buildChains(assembledLinksOnly);

        // report on how effective chaining was
        if(LOGGER.isDebugEnabled())
        {
            int breakendCount = (int)mUnlinkedBreakendMap.values().stream().count();

            List<SvVarData> uniqueUnlinkedSVs = Lists.newArrayList();

            for(final SvVarData var : mUnlinkedSVs)
            {
                if(!uniqueUnlinkedSVs.contains(var.getOrigSV()))
                    uniqueUnlinkedSVs.add(var.getOrigSV());
            }

            LOGGER.debug("cluster({}) chaining finished: chains({} links={}) unlinked SVs({} unique={}) breakends({} reps={}) validAllelePerc({})",
                    mCluster.id(), mPartialChains.size(), mLinkIndex, mUnlinkedSVs.size(), uniqueUnlinkedSVs.size(),
                    mUnlinkedBreakendMap.size(), breakendCount, String.format("%.2f", mCluster.getValidAllelePloidySegmentPerc()));
        }

        if(!mIsValid)
        {
            LOGGER.warn("cluster({}) chain finding failed", mCluster.id());
            return;
        }

        // add these chains to the cluster, but skip any which are identical to existing ones,
        // which can happen for clusters with replicated SVs
        mPartialChains.stream().forEach(chain -> checkAddNewChain(chain));

        for(int i = 0; i < mCluster.getChains().size(); ++i)
        {
            final SvChain chain = mCluster.getChains().get(i);

            if(LOGGER.isDebugEnabled())
            {
                LOGGER.debug("cluster({}) added chain({}) with {} linked pairs:", mCluster.id(), chain.id(), chain.getLinkCount());
                chain.logLinks();
            }

            chain.setId(i); // set after logging so can compare with logging during building
        }
    }

    private void checkAddNewChain(final SvChain newChain)
    {
        if(!mHasReplication || mCluster.getChains().isEmpty())
        {
            mCluster.addChain(newChain, false);
            return;
        }

        // any identical chains will have their replicated SVs entirely removed
        for(final SvChain chain : mCluster.getChains())
        {
            if(chain.identicalChain(newChain))
            {
                boolean allReplicatedSVs = newChain.getSvCount(false) == 0;

                LOGGER.debug("cluster({}) skipping duplicate chain({}) vs origChain({}) all replicated({})",
                        mCluster.id(), newChain.id(), chain.id(), allReplicatedSVs);

                chain.addToReplicationCount();

                // remove these replicated SVs as well as the replicated chain
                if(allReplicatedSVs)
                {
                    for (final SvVarData var : newChain.getSvList())
                    {
                        mCluster.removeReplicatedSv(var);
                    }
                }

                return;
            }
        }

        mCluster.addChain(newChain, false);
    }

    private void buildChains(boolean assembledLinksOnly)
    {
        setUnlinkedBreakends();

        // first make chains out of any assembly links
        addAssemblyLinksToChains();

        if(assembledLinksOnly)
            return;

        // isSpecificCluster(mCluster);

        setSvReplicationCounts();

        if(mUseAllelePloidies)
            determineBreakendPloidies();

        determinePossibleLinks();

        int iterationsWithoutNewLinks = 0; // protection against loops

        while (true)
        {
            mSkippedPair = false;
            int lastAddedIndex = mLinkIndex;

            List<SvLinkedPair> possiblePairs = findPossiblePairs();

            if(possiblePairs.isEmpty())
            {
                if(!mSkippedPair)
                    break;
            }
            else
            {
                processPossiblePairs(possiblePairs);
            }

            if(lastAddedIndex == mLinkIndex)
            {
                ++iterationsWithoutNewLinks;

                if (iterationsWithoutNewLinks > 5)
                {
                    LOGGER.error("cluster({}) 5 iterations without adding a new link", mCluster.id());
                    mIsValid = false;
                    break;
                }
            }
            else
            {
                iterationsWithoutNewLinks = 0;
            }

            checkProgress();
        }
    }

    private static String PP_METHOD_ONLY = "ONLY";

    private List<SvLinkedPair> findPossiblePairs()
    {
        // proceed through the link-finding methods in priority
        List<SvLinkedPair> possiblePairs = findSingleOptionPairs();

        if(!possiblePairs.isEmpty())
        {
            mLinkReason = PP_METHOD_ONLY;
            return possiblePairs;
        }

        if (mHasReplication)
        {
            possiblePairs = findDuplicationPairs();

            if (!possiblePairs.isEmpty())
            {
                mLinkReason = "FB_DUP";
                return possiblePairs;
            }
        }

        possiblePairs = findAdjacentMatchingPairs();
        if (!possiblePairs.isEmpty())
        {
            mLinkReason = "ADJAC";
            return possiblePairs;
        }

        if(mHasReplication)
        {
            possiblePairs = findPloidyMatchPairs();

            if (!possiblePairs.isEmpty())
            {
                mLinkReason = "PL_MAT";
                return possiblePairs;
            }

            possiblePairs = findMaxReplicationPairs();

            if (!possiblePairs.isEmpty())
            {
                mLinkReason = "PL_MAX";
                return possiblePairs;
            }
        }

        // try all remaining
        List<SvBreakend> breakendList = mUnlinkedBreakendMap.keySet().stream().collect(Collectors.toList());
        possiblePairs = findFewestOptionPairs(breakendList, false);

        if(!possiblePairs.isEmpty())
        {
            mLinkReason = "SHORT";
            return possiblePairs;
        }

        return possiblePairs;
    }

    private List<SvLinkedPair> findPloidyMatchPairs()
    {
        List<SvLinkedPair> possiblePairs = Lists.newArrayList();

        // find the highest pair of SVs with matching ploidy
        int maxRepCount = 1;

        for(Map.Entry<SvVarData,Integer> entry : mSvReplicationMap.entrySet())
        {
            int repCount = entry.getValue();
            SvVarData var = entry.getKey();

            if(repCount <= 1)
                continue;

            if(repCount >= maxRepCount)
            {
                List<SvLinkedPair> newPairs = Lists.newArrayList();

                // check whether this SV has any possible links with SVs of the same (remaining) rep count
                for(int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
                {
                    if(var.isNullBreakend() && be == SvVarData.SE_END)
                        continue;

                    boolean isStart = SvVarData.isStart(be);
                    SvBreakend breakend = var.getBreakend(isStart);
                    List<SvLinkedPair> svLinks = mSvBreakendPossibleLinks.get(breakend);

                    if(svLinks == null)
                        continue;

                    for(SvLinkedPair pair : svLinks)
                    {
                        if(possiblePairs.contains(pair))
                            continue;

                        if(mSkippedPairs.contains(pair))
                            continue;

                        SvVarData otherVar = pair.getOtherSV(var);
                        Integer otherRepCount = mSvReplicationMap.get(otherVar);
                        if(otherRepCount != null && otherRepCount == repCount)
                        {
                            log(LOG_LEVEL_VERBOSE, String.format("pair(%s) with matching high-rep count(%s)", pair.toString(), repCount));
                            newPairs.add(pair);
                        }
                    }
                }

                if(newPairs.isEmpty())
                    continue;

                if(repCount > maxRepCount)
                {
                    maxRepCount = repCount;
                    possiblePairs.clear();
                }

                possiblePairs.addAll(newPairs);
            }
        }

        removeSkippedPairs(possiblePairs);

        return possiblePairs;
    }

    private List<SvLinkedPair> findDuplicationPairs()
    {
        // both ends of a foldback or complex DUP connect to one end of another SV with ploidy >= 2x
        List<SvLinkedPair> possiblePairs = Lists.newArrayList();

        // are there any foldbacks where both ends should or can only connect to a single other high-ploidy breakend
        List<SvVarData> replicatingSVs = Lists.newArrayList(mFoldbacks);
        replicatingSVs.addAll(mComplexDupCandidates);

        if(replicatingSVs.isEmpty())
            return possiblePairs;

        for(SvVarData var : replicatingSVs)
        {
            int varPloidy = var.getImpliedPloidy();

            // collect up possible pairs for this foldback
            List<SvLinkedPair> varPairs = Lists.newArrayList();

            for(int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
            {
                boolean isStart = SvVarData.isStart(be);

                if(var.getFoldbackBreakend(isStart) != null)
                {
                    SvBreakend breakend = var.getBreakend(isStart);
                    List<SvLinkedPair> bePairs = mSvBreakendPossibleLinks.get(breakend);
                    if(bePairs != null)
                    {
                        varPairs.addAll(bePairs);
                    }
                }
            }

            // then check if any of these run into a breakend of higher ploidy, and if so, take the nearest pair set
            // eg for A - B - A, where A has double ploidy of B, and both ends of B connect to same end of A
            // in the logic below, the SV in question is the B variant
            List<SvVarData> otherSVs = Lists.newArrayList();
            for(SvLinkedPair pair : varPairs)
            {
                SvVarData otherSV = pair.getOtherSV(var); // the SV with equal or higher ploidy
                int otherVarPloidy = otherSV.getImpliedPloidy();

                if(varPloidy > otherVarPloidy)
                    continue;

                if(otherSVs.contains(otherSV))
                    continue;

                otherSVs.add(otherSV);

                // at this point select the nearer of the breakend pair and link them both to one end of the higher-ploidy SV
                int endsMatchedOnOtherVarStart = 0;
                int endsMatchedOnOtherVarEnd = 0;
                long linkOnOtherVarStartLength = 0;
                long linkOnOtherVarEndLength = 0;
                int maxLinkOnOtherVarStart = 0;
                int maxLinkOnOtherVarEnd = 0;
                List<SvLinkedPair> startLinks = Lists.newArrayList();
                List<SvLinkedPair> endLinks = Lists.newArrayList();

                for(SvLinkedPair otherPair : varPairs)
                {
                    int maxPairCount = getMaxUnlinkedPairCount(otherPair);

                    if(maxPairCount == 0)
                        continue;

                    if(otherPair.hasBreakend(otherSV, true) && getUnlinkedBreakendCount(otherSV.getBreakend(true)) > 1
                            && (otherPair.hasBreakend(var, true) || otherPair.hasBreakend(var, false)))
                    {
                        // link can be made from start of this var to both ends of the other SV, and record the shortest pair length
                        ++endsMatchedOnOtherVarStart;
                        maxLinkOnOtherVarStart = max(maxLinkOnOtherVarStart, maxPairCount);
                        startLinks.add(otherPair);

                        if (linkOnOtherVarStartLength == 0 || otherPair.length() < linkOnOtherVarStartLength)
                            linkOnOtherVarStartLength = otherPair.length();
                    }
                    else if(otherPair.hasBreakend(otherSV, false) && getUnlinkedBreakendCount(otherSV.getBreakend(false)) > 1
                            && (otherPair.hasBreakend(var, true) || otherPair.hasBreakend(var, false)))
                    {
                        ++endsMatchedOnOtherVarEnd;
                        maxLinkOnOtherVarEnd = max(maxLinkOnOtherVarEnd, maxPairCount);
                        endLinks.add(otherPair);

                        if (linkOnOtherVarEndLength == 0 || otherPair.length() < linkOnOtherVarEndLength)
                            linkOnOtherVarEndLength = otherPair.length();
                    }
                }

                if(endsMatchedOnOtherVarStart < 2 && endsMatchedOnOtherVarEnd < 2)
                    continue;

                log(LOG_LEVEL_VERBOSE, String.format("SV(%s) ploidy(%d) foldback dual links: start(links=%d maxLinks=%d matched=%d) start(links=%d maxLinks=%d matched=%d)",
                        var.id(), varPloidy, startLinks.size(), maxLinkOnOtherVarStart, endsMatchedOnOtherVarStart,
                        endLinks.size(), maxLinkOnOtherVarEnd, endsMatchedOnOtherVarEnd));

                // check for conflicts between this pairing and any others involving the SVs, and if found
                // go with the shortest (could change this to the highest)
                if(!possiblePairs.isEmpty())
                {
                    boolean replacePossibles = false;
                    boolean hasClashes = false;

                    for(SvLinkedPair otherPair : possiblePairs)
                    {
                        if (otherPair.hasVariant(var) || otherPair.hasVariant(otherSV))
                        {
                            hasClashes = true;
                            int shorterNewPairs = 0;

                            if (endsMatchedOnOtherVarStart == 2)
                            {
                                shorterNewPairs = (int) startLinks.stream().filter(x -> x.length() < otherPair.length()).count();
                            }

                            if (endsMatchedOnOtherVarEnd == 2)
                            {
                                shorterNewPairs += (int) endLinks.stream().filter(x -> x.length() < otherPair.length()).count();
                            }

                            if (shorterNewPairs > 0)
                            {
                                replacePossibles = true;
                                break;
                            }
                        }
                    }

                    if(replacePossibles)
                    {
                        possiblePairs.clear();
                    }
                    else if(hasClashes)
                    {
                        continue;
                    }
                }

                if(endsMatchedOnOtherVarStart == 2 && endsMatchedOnOtherVarEnd == 2)
                {
                    // prioritise potential link count followed by length
                    if(maxLinkOnOtherVarStart > maxLinkOnOtherVarEnd || linkOnOtherVarStartLength < linkOnOtherVarEndLength)
                    {
                        possiblePairs.addAll(startLinks);
                    }
                    else
                    {
                        possiblePairs.addAll(endLinks);
                    }
                }
                else if(endsMatchedOnOtherVarStart == 2)
                {
                    possiblePairs.addAll(startLinks);
                }
                else if(endsMatchedOnOtherVarEnd == 2)
                {
                    possiblePairs.addAll(endLinks);
                }
            }
        }

        if(!possiblePairs.isEmpty())
        {
            removeSkippedPairs(possiblePairs);
        }

        return possiblePairs;
    }

    private List<SvLinkedPair> findSingleOptionPairs()
    {
        List<SvLinkedPair> restrictedPairs = Lists.newArrayList();

        for(Map.Entry<SvBreakend,List<SvLinkedPair>> entry : mSvBreakendPossibleLinks.entrySet())
        {
            if(entry.getValue().size() != 1)
                continue;

            SvBreakend limitingBreakend = entry.getKey();

            // confirm that more possible pairs can't be found
            if(mMaxPossibleLinks > 0)
            {
                if(addMorePossibleLinks(limitingBreakend, true))
                    continue;
            }

            SvLinkedPair newPair = entry.getValue().get(0);

            if(mSkippedPairs.contains(newPair))
                continue;

            int minLinkCount = getMaxUnlinkedPairCount(newPair);

            // add this if it doesn't clash, and if it does then take the highest ploidy first, following by shortest
            int index = 0;
            boolean canAdd = true;
            while(index < restrictedPairs.size())
            {
                SvLinkedPair otherPair = restrictedPairs.get(index);

                if(otherPair == newPair)
                {
                    canAdd = false;
                    break;
                }

                if(!otherPair.hasLinkClash(newPair) && !otherPair.oppositeMatch(newPair))
                {
                    ++index;
                    continue;
                }

                int otherMinLinkCount = getMaxUnlinkedPairCount(otherPair);

                if(minLinkCount < otherMinLinkCount || newPair.length() < otherPair.length()) // || ploidySumNew > ploidySumOther
                {
                    restrictedPairs.remove(index);
                }
                else
                {
                    canAdd = false;
                    break;
                }
            }

            if(canAdd)
            {
                log(LOG_LEVEL_VERBOSE, String.format("single-option pair(%s) limited by breakend(%s)",
                        newPair.toString(), limitingBreakend.toString()));
                restrictedPairs.add(newPair);
            }
        }

        return restrictedPairs;
    }

    private List<SvLinkedPair> findAdjacentMatchingPairs()
    {
        if(mAdjacentMatchingPairs.isEmpty())
            return mAdjacentMatchingPairs;

        // take next one
        List<SvLinkedPair> possiblePairs = Lists.newArrayList();

        while(!mAdjacentMatchingPairs.isEmpty())
        {
            SvLinkedPair nextPair = mAdjacentMatchingPairs.get(0);

            mAdjacentMatchingPairs.remove(0);

            if(matchesExistingPair(nextPair))
            {
                continue;
            }

            // check breakends are still available
            if(getUnlinkedBreakendCount(nextPair.getFirstBreakend()) == 0 || getUnlinkedBreakendCount(nextPair.getSecondBreakend()) == 0)
            {
                continue;
            }

            possiblePairs.add(nextPair);
            return possiblePairs;
        }

        return possiblePairs;
    }

    private List<SvLinkedPair> findMaxReplicationPairs()
    {
        // look at the remaining SVs with replication at least 2 and those with the highest
        // remaining replication count and then those with fewest options
        List<SvLinkedPair> possiblePairs = Lists.newArrayList();

        // first check if there are SVs with a higher replication count, and if so favour these first
        List<SvVarData> maxRepSVs = !mSvReplicationMap.isEmpty() ? getMaxReplicationSVs() : null;

        if(maxRepSVs == null || maxRepSVs.isEmpty())
            return possiblePairs;

        List<SvBreakend> breakendList = Lists.newArrayList();

        for (final SvBreakend breakend : mUnlinkedBreakendMap.keySet())
        {
            if (maxRepSVs.contains(breakend.getSV()))
            {
                breakendList.add(breakend);
            }
        }

        if(breakendList.isEmpty())
            return possiblePairs;

        if (mLogVerbose)
        {
            for (SvVarData var : maxRepSVs)
            {
                LOGGER.debug("restricted to rep SV: {} repCount({})", var.id(), mSvReplicationMap.get(var));
            }
        }

        // next take the pairings with the least alternatives
        possiblePairs = findFewestOptionPairs(breakendList, true);

        if (possiblePairs.isEmpty())
        {
            // these high-replication SVs yielded no possible links so remove them from consideration
            for (final SvVarData var : maxRepSVs)
            {
                log(LOG_LEVEL_VERBOSE, String.format("cluster(%s) removing high-replicated SV(%s %s)",
                        mCluster.id(), var.posId(), var.type()));

                mSvReplicationMap.remove(var);
            }

            // mSkippedPair = true;
        }

        return possiblePairs;
    }

    private void removeSkippedPairs(List<SvLinkedPair> possiblePairs)
    {
        // some pairs are temporarily unavailable for use (eg those which would close a chain)
        // to to avoid continually trying to add them, keep them out of consideration until a new links is added
        if(mSkippedPairs.isEmpty())
            return;

        int index = 0;
        while(index < possiblePairs.size())
        {
            SvLinkedPair pair = possiblePairs.get(index);

            if(mSkippedPairs.contains(pair))
                possiblePairs.remove(index);
            else
                ++index;
        }
    }

    private List<SvLinkedPair> findFewestOptionPairs(List<SvBreakend> breakendList, boolean isRestricted)
    {
        // of these pairs, do some have less alternatives links which could be made than others
        // eg if high-rep SVs are A and B, and possible links have been found A-C, A-D and B-E,
        // then count how many links could be made between C, D and E to other SVs, and then select the least restrictive
        // say A-C is the only link C can make and B-D is the only link that D can make, then would want to make them both
        int minPairCount = 0;
        List<SvLinkedPair> minLinkPairs = Lists.newArrayList();

        for(SvBreakend breakend : breakendList)
        {
            List<SvLinkedPair> possiblePairs = mSvBreakendPossibleLinks.get(breakend);

            if (possiblePairs == null || possiblePairs.isEmpty())
                continue;

            if (minPairCount == 0 || possiblePairs.size() < minPairCount)
            {
                minLinkPairs.clear();
                minPairCount = possiblePairs.size();
            }

            if (possiblePairs.size() == minPairCount)
            {
                for (SvLinkedPair pair : possiblePairs)
                {
                    if (!minLinkPairs.contains(pair))
                        minLinkPairs.add(pair);
                }
            }

            if (isRestricted)
            {
                // also check this max-rep SV's pairings to test how they are restricted
                for (SvLinkedPair pair : possiblePairs)
                {
                    SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                    List<SvLinkedPair> otherPossiblePairs = mSvBreakendPossibleLinks.get(otherBreakend);

                    if (otherPossiblePairs == null || otherPossiblePairs.isEmpty())
                        continue; // logical assert

                    if (minPairCount == 0 || otherPossiblePairs.size() < minPairCount)
                    {
                        minLinkPairs.clear();
                        minPairCount = otherPossiblePairs.size();
                    }

                    if (otherPossiblePairs.size() == minPairCount)
                    {
                        for (SvLinkedPair otherPair : otherPossiblePairs)
                        {
                            if (!minLinkPairs.contains(otherPair))
                                minLinkPairs.add(otherPair);
                        }
                    }
                }
            }
        }

        removeSkippedPairs(minLinkPairs);

        return minLinkPairs;
    }

    private void processPossiblePairs(List<SvLinkedPair> possiblePairs)
    {
        // now the top candidates to link have been found, take the shortest of them and add this to a chain
        // where possible, add links multiple times according to the min replication of the breakends involved
        // after each link is added, check whether any breakend now has only one link option
        boolean linkAdded = false;

        boolean isRestrictedSet = mLinkReason == PP_METHOD_ONLY;

        while (!possiblePairs.isEmpty())
        {
            SvLinkedPair shortestPair = null;
            for (SvLinkedPair pair : possiblePairs)
            {
                log(LOG_LEVEL_VERBOSE, String.format("method(%s) possible pair: %s length(%s)",
                        mLinkReason, pair.toString(), pair.length()));

                if (shortestPair == null || pair.length() < shortestPair.length())
                {
                    shortestPair = pair;
                }
            }

            possiblePairs.remove(shortestPair);

            // log(LOG_LEVEL_VERBOSE, String.format("shortest possible pair: %s length(%s)", shortestPair.toString(), shortestPair.length()));

            int pairRepeatCount = 1;

            if(mHasReplication)
            {
                int beStartCount = getUnlinkedBreakendCount(shortestPair.getBreakend(true));
                int beEndCount = getUnlinkedBreakendCount(shortestPair.getBreakend(false));

                if(beStartCount > 1 && beEndCount > 1)
                {
                    pairRepeatCount = min(beStartCount, beEndCount);
                }

                if(pairRepeatCount > 1)
                {
                    LOGGER.debug("repeating pair({}) {} times", shortestPair.toString(), pairRepeatCount);
                }
            }

            for(int i = 0; i < pairRepeatCount; ++i)
            {
                linkAdded |= addPairToChain(shortestPair);
            }

            if(!mIsValid)
                return;

            // check whether after adding a link, some SV breakends have only a single possible link
            if(!isRestrictedSet)
            {
                List<SvLinkedPair> restrictedPairs = findSingleOptionPairs();
                if(!restrictedPairs.isEmpty())
                {
                    possiblePairs = restrictedPairs;
                    isRestrictedSet = true;
                }
            }

            if(!isRestrictedSet)
            {
                // having added a new pair, remove any other conflicting pairs
                int index = 0;
                while (index < possiblePairs.size())
                {
                    SvLinkedPair pair = possiblePairs.get(index);
                    if (pair.oppositeMatch(shortestPair))
                    {
                        possiblePairs.remove(index);
                        continue;
                    }

                    if (mHasReplication)
                    {
                        if (findUnlinkedMatchingBreakend(pair.getBreakend(true)) == null
                        || findUnlinkedMatchingBreakend(pair.getBreakend(false)) == null)
                        {
                            // replicated instances exhausted
                            possiblePairs.remove(index);
                            continue;
                        }
                    }
                    else if (pair.hasLinkClash(shortestPair))
                    {
                        possiblePairs.remove(index);
                        continue;
                    }

                    ++index;
                }
            }
        }

        if(linkAdded)
        {
            mSkippedPairs.clear(); // any skipped links can now be re-evaluated
        }
    }

    private static int SPEC_LINK_INDEX = -1;
    // private static int SPEC_LINK_INDEX = 26;

    private boolean addPairToChain(final SvLinkedPair pair)
    {
        if(mLinkIndex == SPEC_LINK_INDEX)
        {
            LOGGER.debug("specific link index({}) pair({})", mLinkIndex, pair.toString());
        }

        // attempt to add to existing chain
        boolean addedToChain = false;
        boolean[] pairToChain = {false, false};

        SvBreakend unlinkedBeFirst = null;
        SvBreakend unlinkedBeSecond = null;
        final SvLinkedPair newPair;

        if(!mHasReplication) //  && !mCluster.requiresReplication()
        {
            newPair = pair;
        }
        else
        {
            // this pair was created from the set of possibles, but needs to make use of unlinked breakends
            unlinkedBeFirst = findUnlinkedMatchingBreakend(pair.getBreakend(true));
            unlinkedBeSecond = findUnlinkedMatchingBreakend(pair.getBreakend(false));

            if(unlinkedBeFirst == null || unlinkedBeSecond == null)
            {
                // tolerate missed assembly links while a more robust approach is determined for ploidy discrepancies
                if(pair.isAssembled())
                {
                    LOGGER.warn("cluster({}) missed assembly link", mCluster.id());
                    return false;
                }

                mIsValid = false;
                LOGGER.error("new pair breakendStart({} valid={}) and breakendEnd({} valid={}) no unlinked match found",
                        pair.getBreakend(true).toString(), unlinkedBeFirst != null,
                        pair.getBreakend(false).toString(), unlinkedBeSecond != null);

                return false;
            }

            newPair = new SvLinkedPair(unlinkedBeFirst.getSV(), unlinkedBeSecond.getSV(), SvLinkedPair.LINK_TYPE_TI,
                    unlinkedBeFirst.usesStart(), unlinkedBeSecond.usesStart());

            if(pair.isAssembled())
                newPair.setIsAssembled();
        }

        boolean linkClosesChain = false;

        for(SvChain chain : mPartialChains)
        {
            // test this link against each ends to the chain
            boolean addToStart = false;
            pairToChain[0] = pairToChain[1] = false; // reset for scenario where skipped adding to both ends of chain

            for(int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
            {
                boolean isStart = SvVarData.isStart(be);
                final SvVarData chainSV = chain.getChainEndSV(isStart);

                if (chain.canAddLinkedPair(newPair, isStart, true))
                {
                    addToStart = isStart;

                    if (chainSV.equals(newPair.first(), true))
                    {
                        pairToChain[SvVarData.SE_START] = true;

                        if(chainSV != newPair.first())
                        {
                            // the correct SV was matched, but a different instance, so switch it for one matching the chain end
                            newPair.replaceFirst(chainSV);
                        }
                    }
                    else
                    {
                        pairToChain[SvVarData.SE_END] = true;

                        if (chainSV != newPair.second())
                        {
                            newPair.replaceSecond(chainSV);
                        }
                    }
                }
            }

            if(!pairToChain[SvVarData.SE_START] && !pairToChain[SvVarData.SE_END])
            {
                continue;
            }

            if(pairToChain[SvVarData.SE_START] && pairToChain[SvVarData.SE_END])
            {
                // the link can be added to both ends, which would close the chain - so search for an alternative SV on either end
                // to keep it open while still adding the link
                boolean replacementFound = false;

                if(mHasReplication)
                {
                    for (int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
                    {
                        boolean isStart = SvVarData.isStart(be);

                        SvBreakend openBreakend = chain.getOpenBreakend(isStart); // this will be one of the pair breakends

                        if(openBreakend == null)
                            continue; // eg ending on a SGL

                        List<SvBreakend> possibleBreakends = mUnlinkedBreakendMap.get(openBreakend.getOrigBreakend());
                        SvVarData chainSV = chain.getChainEndSV(isStart);

                        if (possibleBreakends == null || possibleBreakends.isEmpty())
                            continue;

                        for (SvBreakend otherBreakend : possibleBreakends)
                        {
                            if (otherBreakend.getSV() != chainSV)
                            {
                                replacementFound = true;

                                if (newPair.first() == chainSV)
                                    newPair.replaceFirst(otherBreakend.getSV());
                                else
                                    newPair.replaceSecond(otherBreakend.getSV());

                                pairToChain[be] = false;
                                addToStart = !isStart;
                                break;
                            }
                        }

                        if (replacementFound)
                            break;
                    }
                }

                if(!replacementFound)
                {
                    log(LOG_LEVEL_VERBOSE, String.format("skipping linked pair(%s) would close existing chain(%d)",
                            newPair.toString(), chain.id()));

                    if(!mSkippedPairs.contains(newPair))
                    {
                        mSkippedPair = true;
                        mSkippedPairs.add(pair);
                    }

                    linkClosesChain = true;
                    continue;
                }
            }

            chain.addLink(newPair, addToStart);
            addedToChain = true;

            LOGGER.debug("index({}) method({}) adding linked pair({} {} len={}) to existing chain({}) {}",
                    mLinkIndex, mLinkReason,
                    newPair.toString(), newPair.assemblyInferredStr(), newPair.length(), chain.id(), addToStart ? "start" : "end");
            break;
        }

        if(!addedToChain)
        {
            if(linkClosesChain)
                return false; // skip this link for now

            SvChain chain = new SvChain(mNextChainId++);
            mPartialChains.add(chain);
            chain.addLink(newPair, true);
            pairToChain[SvVarData.SE_START] = true;
            pairToChain[SvVarData.SE_END] = true;

            LOGGER.debug("index({}) method({}) adding linked pair({} {}) to new chain({})",
                    mLinkIndex, mLinkReason, newPair.toString(), newPair.assemblyInferredStr(), chain.id());
        }

        newPair.setLinkReason(mLinkReason);

        registerNewLink(newPair, pairToChain);
        ++mLinkIndex;

        if(mRunValidation)
            checkHasValidState();

        if(addedToChain)
        {
            // now see if any partial chains can be linked
            reconcileChains();
        }

        return true;
    }

    private SvBreakend findUnlinkedMatchingBreakend(final SvBreakend breakend)
    {
        // get the next available breakend (thereby reducing the replicated instances)
        final List<SvBreakend> breakendList = mUnlinkedBreakendMap.get(breakend.getOrigBreakend());

        if(breakendList == null || breakendList.isEmpty())
            return null;

        return breakendList.get(0);
    }

    private int getUnlinkedBreakendCount(final SvBreakend breakend)
    {
        List<SvBreakend> beList = mUnlinkedBreakendMap.get(breakend);
        return beList != null ? beList.size() : 0;
    }

    private int getMaxUnlinkedPairCount(final SvLinkedPair pair)
    {
        int first = getUnlinkedBreakendCount(pair.getBreakend(true));
        int second = getUnlinkedBreakendCount(pair.getBreakend(false));
        return min(first, second);
    }

    private void addAssemblyLinksToChains()
    {
        List<SvLinkedPair> assemblyLinkedPairs = mCluster.getAssemblyLinkedPairs();

        if(assemblyLinkedPairs.isEmpty())
            return;

        mLinkReason = "ASMB";

        for(SvLinkedPair pair : assemblyLinkedPairs)
        {
            addPairToChain(pair);
        }

        if(!mPartialChains.isEmpty())
        {
            LOGGER.debug("created {} partial chains from {} assembly links", mPartialChains.size(), assemblyLinkedPairs.size());
        }
    }

    private void registerNewLink(final SvLinkedPair newPair, boolean[] pairToChain)
    {
        for (int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
        {
            boolean isStart = SvVarData.isStart(be);

            final SvBreakend breakend = newPair.getBreakend(isStart);

            mUnlinkedSVs.remove(breakend.getSV());

            final SvBreakend origBreakend = breakend.getOrigBreakend();

            final List<SvBreakend> breakendList = mUnlinkedBreakendMap.get(origBreakend);

            if(breakendList == null || breakendList.isEmpty())
            {
                LOGGER.error("breakend({}) list already empty", origBreakend.toString());
                mIsValid = false;
                return;
            }

            SvVarData origSV = origBreakend.getSV();
            final SvBreakend otherOrigBreakend = origSV.getBreakend(!breakend.usesStart());

            breakendList.remove(breakend);

            boolean hasUnlinkedBreakend = true;
            if(breakendList.isEmpty())
            {
                mUnlinkedBreakendMap.remove(origBreakend);
                hasUnlinkedBreakend = false;

                // LOGGER.debug("breakend({}) has no more possible links", breakend);

                if(getUnlinkedBreakendCount(otherOrigBreakend) == 0)
                {
                    if(origSV.isFoldback())
                    {
                        // remove if no other instances of this SV remain
                        mFoldbacks.remove(origSV);
                    }
                    else if(mComplexDupCandidates.contains(origSV))
                    {
                        mComplexDupCandidates.remove(origSV);
                    }
                }
            }

            List<SvLinkedPair> possibleLinks = !mSvBreakendPossibleLinks.isEmpty() ? mSvBreakendPossibleLinks.get(origBreakend) : null;

            if(possibleLinks != null)
            {
                if (!hasUnlinkedBreakend)
                {
                    removePossibleLinks(possibleLinks, breakend);
                }
            }

            // check for an opposite pairing between these 2 SVs - need to look into other breakends' lists

            possibleLinks = otherOrigBreakend != null && !mSvBreakendPossibleLinks.isEmpty() ? mSvBreakendPossibleLinks.get(otherOrigBreakend) : null;

            if(possibleLinks != null)
            {
                final SvBreakend otherOrigBreakendAlt = newPair.first() ==  breakend.getSV() ?
                        newPair.second().getOrigSV().getBreakend(!newPair.secondLinkOnStart()) :
                        newPair.first().getOrigSV().getBreakend(!newPair.firstLinkOnStart());

                if(otherOrigBreakendAlt != null)
                {
                    for (SvLinkedPair pair : possibleLinks)
                    {
                        if (pair.hasBreakend(otherOrigBreakend) && pair.hasBreakend(otherOrigBreakendAlt))
                        {
                            possibleLinks.remove(pair);
                            break;
                        }
                    }
                }
            }

            if(mHasReplication)
            {
                // reduce replication counts for breakends which are added to a chain
                if (pairToChain[be])
                {
                    Integer replicationCount = mSvReplicationMap.get(breakend.getOrigSV());

                    if (replicationCount != null)
                    {
                        if (replicationCount <= 2)
                            mSvReplicationMap.remove(breakend.getOrigSV());
                        else
                            mSvReplicationMap.put(breakend.getOrigSV(), replicationCount - 1);
                    }
                }
            }
        }

        // track unique pairs to avoid conflicts (eg end-to-end and start-to-start)
        if(!matchesExistingPair(newPair))
        {
            mUniquePairs.add(newPair);
        }
    }

    private boolean matchesExistingPair(final SvLinkedPair pair)
    {
        for(SvLinkedPair existingPair : mUniquePairs)
        {
            if(pair.matches(existingPair))
                return true;
        }

        return false;
    }

    private boolean isOppositeMatchVsExisting(final SvLinkedPair pair)
    {
        for(SvLinkedPair existingPair : mUniquePairs)
        {
            if(pair.oppositeMatch(existingPair))
                return true;
        }

        return false;
    }

    private void removePossibleLinks(List<SvLinkedPair> possibleLinks, SvBreakend fullyLinkedBreakend)
    {
        if(possibleLinks == null || possibleLinks.isEmpty())
            return;

        final SvVarData linkedSV = fullyLinkedBreakend.getOrigSV();
        final SvBreakend origBreakend = fullyLinkedBreakend.getOrigBreakend();

        int index = 0;
        while (index < possibleLinks.size())
        {
            SvLinkedPair possibleLink = possibleLinks.get(index);
            if (possibleLink.hasBreakend(linkedSV, fullyLinkedBreakend.usesStart()))
            {
                // remove this from consideration
                possibleLinks.remove(index);

                SvBreakend otherBreakend = possibleLink.getBreakend(true) == origBreakend ?
                        possibleLink.getBreakend(false) : possibleLink.getBreakend(true);

                // and remove the pair which was cached in the other breakend's possibles list
                List<SvLinkedPair> otherPossibles = mSvBreakendPossibleLinks.get(otherBreakend);

                if(otherPossibles != null)
                {
                    for (SvLinkedPair otherPair : otherPossibles)
                    {
                        if (otherPair == possibleLink)
                        {
                            otherPossibles.remove(otherPair);

                            if (otherPossibles.isEmpty())
                            {
                                // LOGGER.debug("breakend({}) has no more possible links", otherBreakend);

                                if(!addMorePossibleLinks(otherBreakend, true))
                                {
                                    mSvBreakendPossibleLinks.remove(otherBreakend);
                                }
                            }

                            break;
                        }
                    }
                }
            }
            else
            {
                ++index;
            }
        }

        if(possibleLinks.isEmpty())
        {
            //LOGGER.debug("breakend({}) has no more possible links", origBreakend);

            if(!addMorePossibleLinks(origBreakend, true))
            {
                mSvBreakendPossibleLinks.remove(origBreakend);
            }
        }
    }

    private static int MAJOR_AP = 0;
    private static int MINOR_AP = 1;
    private static int A_FIXED_AP = 2;
    private static int B_NON_DIS_AP = 3;
    private static int CLUSTER_AP = 4;
    private static int AP_DATA_VALID = 5;
    private static int AP_IS_VALID = 1;

    // ploidy level below which a chain segment cannot cross
    private static double CLUSTER_ALLELE_PLOIDY_MIN = 0.15;

    private void determineBreakendPloidies()
    {
        if(!mHasReplication)
            return;

        final Map<String,List<SvBreakend>> chrBreakendMap = mCluster.getChrBreakendMap();

        int totalSegCount = 0;
        int totalValidSegCount = 0;

        for (final Map.Entry<String, List<SvBreakend>> entry : chrBreakendMap.entrySet())
        {
            final String chromosome = entry.getKey();
            final List<SvBreakend> breakendList = entry.getValue();
            int breakendCount = breakendList.size();

            // a multi-dim array of breakend index for this arm to A allele ploidy, B non-disrupted ploidy, and cluster ploidy
            double[][] allelePloidies = new double[breakendCount][AP_DATA_VALID +1];

            mChrAllelePloidies.put(chromosome, allelePloidies);

            boolean inSegment = false;
            SvBreakend segStartBreakend = null;
            int segStartIndex = 0;

            for (int i = 0; i < breakendList.size(); ++i)
            {
                SvBreakend breakend = breakendList.get(i);

                allelePloidies[i][MAJOR_AP] = breakend.majorAllelePloidy(false);
                allelePloidies[i][MINOR_AP] = breakend.minorAllelePloidy(false);

                if(!inSegment)
                {
                    inSegment = true;
                    segStartBreakend = breakend;
                    segStartIndex = i;
                }
                else
                {
                    SvBreakend nextBreakend = (i < breakendList.size() - 1) ? breakendList.get(i + 1) : null;
                    if(nextBreakend == null || nextBreakend.getChrPosIndex() > breakend.getChrPosIndex() + 1)
                    {
                        // a gap in the cluster so need to evaluate this contiguous section
                        inSegment = false;
                        boolean validCalcs = calculateNoneClusterSegmentPloidies(chromosome, allelePloidies, segStartIndex, i, segStartBreakend, breakend);
                        ++totalSegCount;

                        if(validCalcs)
                            ++totalValidSegCount;
                    }
                }
            }

            calculateClusterSegmentPloidies(chromosome, allelePloidies);
        }

        LOGGER.debug("cluster({}) chromosomes({}) AP totalSegments({} valid={})",
                mCluster.id(), chrBreakendMap.size(), totalSegCount, totalValidSegCount);
    }

    private boolean calculateNoneClusterSegmentPloidies(final String chromosome, double[][] allelePloidies,
            int startIndex, int endIndex, SvBreakend startBreakend, SvBreakend endBreakend)
    {
        double startMajorAP = startBreakend.majorAllelePloidy(true);
        double startMinorAP = startBreakend.minorAllelePloidy(true);
        int startMajorAPR = (int)round(startMajorAP);
        int startMinorAPR = (int)round(startMinorAP);
        boolean startMajorAPIsInt = isPloidyCloseToInteger(startMajorAP);
        boolean startMinorAPIsInt = isPloidyCloseToInteger(startMinorAP);

        // map each major and minor AP into a frequency
        Map<Integer,Integer> ploidyFrequency = new HashMap();

        int segCount = endIndex - startIndex + 1;
        for (int i = startIndex; i <= endIndex; ++i)
        {
            int majorAP = (int)round(allelePloidies[i][MAJOR_AP]);
            int minorAP = (int)round(allelePloidies[i][MINOR_AP]);
            Integer repeatCount = ploidyFrequency.get(majorAP);
            if(repeatCount == null)
                ploidyFrequency.put(majorAP, 1);
            else
                ploidyFrequency.put(majorAP, repeatCount+1);

            if(minorAP != majorAP)
            {
                repeatCount = ploidyFrequency.get(minorAP);
                if(repeatCount == null)
                    ploidyFrequency.put(minorAP, 1);
                else
                    ploidyFrequency.put(minorAP, repeatCount+1);
            }
        }

        int maxPloidyCount = 0;
        double aPloidy = 0;

        for(Map.Entry<Integer,Integer> entry : ploidyFrequency.entrySet())
        {
            if(entry.getValue() > maxPloidyCount)
            {
                maxPloidyCount = entry.getValue();
                aPloidy = entry.getKey();
            }
        }

        boolean aPloidyValid = maxPloidyCount >= segCount * 0.9;

        if(!aPloidyValid)
            return false;

        // use knowledge of A to find a minimum for non-disrupted B
        double bPloidyMin = -1;

        double startClusterPloidy = startBreakend.orientation() == 1 ? startBreakend.ploidy() : 0;

        if(startMajorAPIsInt && startMajorAPR == aPloidy)
        {
            bPloidyMin = startMinorAP - startClusterPloidy;
        }
        else if(startMinorAPIsInt && startMinorAPR == aPloidy)
        {
            bPloidyMin = startMajorAP - startClusterPloidy;
        }

        double endClusterPloidy = endBreakend.orientation() == -1 ? endBreakend.ploidy() : 0;

        for (int i = startIndex; i <= endIndex; ++i)
        {
            int majorAP = (int) round(allelePloidies[i][MAJOR_AP]);
            int minorAP = (int) round(allelePloidies[i][MINOR_AP]);

            if(majorAP == aPloidy)
            {
                if(i == endIndex)
                    minorAP -= endClusterPloidy;

                bPloidyMin = bPloidyMin == -1 ? minorAP : min(minorAP, bPloidyMin);
            }
            else if(minorAP == aPloidy)
            {
                if(i == endIndex)
                    majorAP -= endClusterPloidy;

                bPloidyMin = bPloidyMin == -1 ? majorAP : min(majorAP, bPloidyMin);
            }
        }

        // set these values int each segment
        for (int i = startIndex; i <= endIndex; ++i)
        {
            allelePloidies[i][A_FIXED_AP] = aPloidy;
            allelePloidies[i][B_NON_DIS_AP] = bPloidyMin;
            allelePloidies[i][AP_DATA_VALID] = AP_IS_VALID;
        }

        return true;
    }

    private boolean calculateClusterSegmentPloidies(final String chromosome, double[][] allelePloidies)
    {
        // first establish the non-disrupted B ploidy across all segments
        double bNonClusterPloidyMin = allelePloidies[0][B_NON_DIS_AP];
        for(int i = 1; i < allelePloidies.length; ++i)
        {
            bNonClusterPloidyMin = min(bNonClusterPloidyMin, allelePloidies[i][B_NON_DIS_AP]);
        }

        // finally set a cluster ploidy using knowledge of the other 2 ploidies
        int validSegments = 0;
        int segmentCount = allelePloidies.length;
        for(int i = 0; i < segmentCount; ++i)
        {
            if(allelePloidies[i][AP_DATA_VALID] != AP_IS_VALID)
                continue;

            ++validSegments;

            double majorAP = allelePloidies[i][MAJOR_AP];
            double minorAP = allelePloidies[i][MINOR_AP];
            double aPloidy = allelePloidies[i][A_FIXED_AP];

            allelePloidies[i][B_NON_DIS_AP] = bNonClusterPloidyMin;

            double clusterPloidy = 0;
            if(majorAP > aPloidy + 0.5)
            {
                clusterPloidy = majorAP - bNonClusterPloidyMin;
            }
            else
            {
                clusterPloidy = minorAP - bNonClusterPloidyMin;
            }

            allelePloidies[i][CLUSTER_AP] = max(clusterPloidy, 0.0);
            allelePloidies[i][AP_DATA_VALID] = AP_IS_VALID;
        }

        double validSectionPercent = validSegments / (double)segmentCount;
        mCluster.setValidAllelePloidySegmentPerc(validSectionPercent);

        return true;
    }

    private static double PLOIDY_INTEGER_PROXIMITY = 0.25;

    private boolean isPloidyCloseToInteger(double ploidy)
    {
        double remainder = abs(ploidy % 1.0);
        return remainder <= PLOIDY_INTEGER_PROXIMITY || remainder >= (1 - PLOIDY_INTEGER_PROXIMITY);
    }

    private boolean hasValidAllelePloidyData(final SvBreakend breakend, final double[][] allelePloidies)
    {
        if(allelePloidies == null)
            return false;

        if(allelePloidies.length < breakend.getClusterChrPosIndex())
            return false;

        final double[] beAllelePloidies = allelePloidies[breakend.getClusterChrPosIndex()];

        return beAllelePloidies[AP_DATA_VALID] == AP_IS_VALID;
    }

    private void determinePossibleLinks()
    {
        // form a map of each breakend to its set of all other breakends which can form a valid TI
        // need to exclude breakends which are already assigned to an assembled TI unless replication permits additional instances of it
        // add possible links to a list ordered from shortest to longest length
        // do not chain past a zero cluster allele ploidy
        // identify potential complex DUP candidates along the way
        // for the special case of foldbacks, add every possible link they can make

        List<SvBreakend> reverseFoldbackBreakends = Lists.newArrayList();

        for(SvVarData foldback : mFoldbacks)
        {
            if(foldback.isChainedFoldback())
            {
                SvBreakend breakend = foldback.getChainedFoldbackBreakend();

                if(breakend.orientation() == 1)
                    reverseFoldbackBreakends.add(breakend);
            }
            else
            {
                if(foldback.orientation(true) == 1)
                {
                    reverseFoldbackBreakends.add(foldback.getBreakend(true));
                    reverseFoldbackBreakends.add(foldback.getBreakend(false));
                }
            }
        }

        final Map<String,List<SvBreakend>> chrBreakendMap = mCluster.getChrBreakendMap();

        for (final Map.Entry<String, List<SvBreakend>> entry : chrBreakendMap.entrySet())
        {
            final String chromosome = entry.getKey();
            final List<SvBreakend> breakendList = entry.getValue();
            final double[][] allelePloidies = mChrAllelePloidies.get(chromosome);

            for (int i = 0; i < breakendList.size() -1; ++i)
            {
                final SvBreakend lowerBreakend = breakendList.get(i);

                boolean matchedPloidy = false;

                if(lowerBreakend.orientation() != -1)
                    continue;

                if(alreadyLinkedBreakend(lowerBreakend))
                    continue;

                List<SvLinkedPair> lowerPairs = mSvBreakendPossibleLinks.get(lowerBreakend);

                if(lowerPairs == null)
                {
                    lowerPairs = Lists.newArrayList();
                    mSvBreakendPossibleLinks.put(lowerBreakend, lowerPairs);
                }

                final SvVarData lowerSV = lowerBreakend.getSV();
                boolean lowerValidAP = mUseAllelePloidies && hasValidAllelePloidyData(lowerBreakend, allelePloidies);
                boolean lowerIsFoldback = lowerSV.isFoldback() && (!lowerSV.isChainedFoldback() || lowerSV.getChainedFoldbackBreakend() == lowerBreakend);

                int skippedNonAssembledIndex = -1; // the first index of a non-assembled breakend after the current one

                for (int j = i+1; j < breakendList.size(); ++j)
                {
                    final SvBreakend upperBreakend = breakendList.get(j);

                    if(skippedNonAssembledIndex == -1)
                    {
                        if(!upperBreakend.isAssembledLink())
                        {
                            // invalidate the possibility of these 2 breakends satisfying the complex DUP scenario
                            skippedNonAssembledIndex = j;
                        }
                    }

                    if(upperBreakend.orientation() != 1)
                        continue;

                    if(upperBreakend.getSV() == lowerBreakend.getSV())
                        continue;

                    if(alreadyLinkedBreakend(upperBreakend))
                        continue;

                    long distance = upperBreakend.position() - lowerBreakend.position();
                    int minTiLength = getMinTemplatedInsertionLength(lowerBreakend, upperBreakend);

                    if(distance < minTiLength)
                        continue;

                    // record the possible link
                    final SvVarData upperSV = upperBreakend.getSV();

                    SvLinkedPair newPair = new SvLinkedPair(lowerSV, upperSV, SvLinkedPair.LINK_TYPE_TI,
                            lowerBreakend.usesStart(), upperBreakend.usesStart());

                    if(getSvReplicationCount(lowerSV) == getSvReplicationCount(upperSV))
                    {
                        matchedPloidy = true;

                        // make note of any pairs formed from adjacent facing breakends
                        if(j == i + 1)
                        {
                            mAdjacentMatchingPairs.add(newPair);
                        }
                    }

                    lowerPairs.add(newPair);

                    List<SvLinkedPair> upperPairs = mSvBreakendPossibleLinks.get(upperBreakend);

                    if(upperPairs == null)
                    {
                        upperPairs = Lists.newArrayList();
                        mSvBreakendPossibleLinks.put(upperBreakend, upperPairs);

                        // create an entry at the upper breakend's start point to indicate it hasn't begun its search
                        mBreakendLastLinkIndexMap.put(upperBreakend, upperBreakend.getClusterChrPosIndex());
                    }

                    upperPairs.add(0, newPair); // add to front since always nearer than the one prior

                    if(skippedNonAssembledIndex == -1 || skippedNonAssembledIndex == j)
                    {
                        // make note of any breakends which run into a high-ploidy SV at their first opposing breakend
                        if (!lowerBreakend.getSV().isFoldback())
                        {
                            checkIsComplexDupSV(lowerBreakend, upperBreakend);
                        }

                        if (!upperBreakend.getSV().isFoldback())
                        {
                            checkIsComplexDupSV(upperBreakend, lowerBreakend);
                        }
                    }

                    if(lowerValidAP && hasValidAllelePloidyData(upperBreakend, allelePloidies))
                    {
                        double clusterAP = allelePloidies[upperBreakend.getClusterChrPosIndex()][CLUSTER_AP];

                        if(clusterAP < CLUSTER_ALLELE_PLOIDY_MIN)
                        {
                            // this lower breakend cannot match with anything further upstream
                            log(LOG_LEVEL_VERBOSE, String.format("breakends lower(%d: %s) limited at upper(%d: %s) with clusterAP(%.2f)",
                                    i, lowerBreakend.toString(), j, upperBreakend.toString(), clusterAP));

                            break;
                        }
                    }

                    if(matchedPloidy && exceedsMaxPossibleLinks(lowerPairs.size()) && !lowerIsFoldback)
                    {
                        // more possible links could be craeted, but pause adding any more for now
                        mBreakendLastLinkIndexMap.put(lowerBreakend, j);
                        break;
                    }
                }
            }
        }

        for(SvBreakend breakend : reverseFoldbackBreakends)
        {
            mBreakendLastLinkIndexMap.put(breakend, breakend.getClusterChrPosIndex());
            addMorePossibleLinks(breakend, false);
        }
    }

    private boolean exceedsMaxPossibleLinks(int linkCount)
    {
        return mMaxPossibleLinks > 0 && linkCount >= mMaxPossibleLinks;
    }

    private boolean addMorePossibleLinks(SvBreakend breakend, boolean applyMax)
    {
        Integer lastIndex = mBreakendLastLinkIndexMap.get(breakend);

        if(lastIndex == null || lastIndex < 0)
            return false;

        if(getUnlinkedBreakendCount(breakend) == 0)
        {
            mBreakendLastLinkIndexMap.remove(breakend);
            return false;
        }

        // begin from immediately after the last added index and try to add another X possible links
        final List<SvBreakend> breakendList = mCluster.getChrBreakendMap().get(breakend.chromosome());
        final double[][] allelePloidies = mChrAllelePloidies.get(breakend.chromosome());

        boolean hasValidAP = mUseAllelePloidies && hasValidAllelePloidyData(breakend, allelePloidies);

        List<SvLinkedPair> possiblePairs = mSvBreakendPossibleLinks.get(breakend);

        if(possiblePairs == null)
            return false;

        boolean traverseUp = breakend.orientation() == -1;
        int index = lastIndex;

        boolean matchedPloidy = false;
        int linksAdded = 0;
        boolean lastIndexValid = true;
        while (true)
        {
            index += traverseUp ? 1 : -1;

            if(index < 0 || index >= breakendList.size())
            {
                lastIndexValid = false;
                break;
            }

            final SvBreakend otherBreakend = breakendList.get(index);

            if(otherBreakend.orientation() == breakend.orientation())
                continue;

            if(otherBreakend.getSV() == breakend.getSV())
                continue;

            if(getUnlinkedBreakendCount(otherBreakend) == 0)
                continue;

            long distance = Math.abs(otherBreakend.position() - breakend.position());
            int minTiLength = getMinTemplatedInsertionLength(breakend, otherBreakend);

            if(distance < minTiLength)
                continue;

            List<SvLinkedPair> otherPairs = mSvBreakendPossibleLinks.get(otherBreakend);

            if(otherPairs == null)
                continue;

            // record the possible link
            SvBreakend lowerBreakend = breakend.orientation() == -1 ? breakend : otherBreakend;
            SvBreakend upperBreakend = breakend.orientation() == 1 ? breakend : otherBreakend;
            final SvVarData lowerSV = lowerBreakend.getSV();
            final SvVarData upperSV = upperBreakend.getSV();

            SvLinkedPair newPair = new SvLinkedPair(lowerSV, upperSV, SvLinkedPair.LINK_TYPE_TI,
                    lowerBreakend.usesStart(), upperBreakend.usesStart());

            // check link hasn't already been added (which can happen if added from the other breakend)
            boolean skipPair = false;

            for(SvLinkedPair existingPair : possiblePairs)
            {
                if(existingPair.matches(newPair))
                {
                    skipPair = true;
                    break;
                }
            }

            if(skipPair)
                continue;

            for(SvLinkedPair existingPair : otherPairs)
            {
                if(existingPair.matches(newPair))
                {
                    skipPair = true;
                    break;
                }
            }

            if(skipPair)
                continue;

            // check for a clash against existing pairs
            if(isOppositeMatchVsExisting(newPair))
                continue;

            ++linksAdded;
            possiblePairs.add(newPair);
            otherPairs.add(newPair);

            if(!matchedPloidy)
            {
                matchedPloidy = getSvReplicationCount(lowerSV) == getSvReplicationCount(upperSV);
            }

            if(hasValidAP && hasValidAllelePloidyData(otherBreakend, allelePloidies))
            {
                double clusterAP = allelePloidies[otherBreakend.getClusterChrPosIndex()][CLUSTER_AP];

                if(clusterAP < CLUSTER_ALLELE_PLOIDY_MIN)
                {
                    // this lower breakend cannot match with anything futher upstream
                    log(LOG_LEVEL_VERBOSE, String.format("breakend(%d: %s) limited by other(%d: %s) with clusterAP(%.2f)",
                            breakend.getClusterChrPosIndex(), breakend.toString(), index, otherBreakend.toString(), clusterAP));

                    lastIndexValid = false;
                    break;
                }
            }

            if(applyMax && matchedPloidy && exceedsMaxPossibleLinks(possiblePairs.size()))
            {
                break;
            }
        }

        if(lastIndexValid)
        {
            // make note of the last location tested for adding a new possible link
            mBreakendLastLinkIndexMap.put(breakend, index);
        }
        else
        {
            mBreakendLastLinkIndexMap.remove(breakend);
        }

        return linksAdded > 0;
    }

    private void checkIsComplexDupSV(SvBreakend lowerPloidyBreakend, SvBreakend higherPloidyBreakend)
    {
        SvVarData var = lowerPloidyBreakend.getSV();

        if(var.isNullBreakend() || var.type() == DEL)
            return;

        if(mComplexDupCandidates.contains(var))
            return;

        if(var.ploidyMin() * 2 > higherPloidyBreakend.getSV().ploidyMax())
            return;

        boolean lessThanMax = var.ploidyMax() < higherPloidyBreakend.getSV().ploidyMin();

        // check whether the other breakend satisfies the same ploidy comparison criteria
        SvBreakend otherBreakend = var.getBreakend(!lowerPloidyBreakend.usesStart());

        final List<SvBreakend> breakendList = mCluster.getChrBreakendMap().get(otherBreakend.chromosome());

        boolean traverseUp = otherBreakend.orientation() == -1;
        int index = otherBreakend.getClusterChrPosIndex();

        while(true)
        {
            index += traverseUp ? 1 : -1;

            if(index < 0 || index >= breakendList.size())
                break;

            final SvBreakend breakend = breakendList.get(index);

            if(breakend == lowerPloidyBreakend)
                break;

            if (breakend.isAssembledLink())
            {
                index += traverseUp ? 1 : -1;
                continue;
            }

            if (breakend.orientation() == otherBreakend.orientation())
                break;

            SvVarData otherSV = breakend.getSV();

            if(var.ploidyMin() * 2 <= otherSV.ploidyMax())
            {
                if(lessThanMax || var.ploidyMax() < otherSV.ploidyMin())
                {
                    LOGGER.debug("identified complex dup({} {}) vs SVs({} & {})",
                            var.posId(), var.type(), higherPloidyBreakend.getSV().id(), otherSV.id());
                    mComplexDupCandidates.add(var);
                }
            }

            break;
        }
    }

    private boolean alreadyLinkedBreakend(final SvBreakend breakend)
    {
        // assembled links have already been added to chains prior to determining remaining possible links
        // so these need to be excluded unless their replication count allows them to be used again
        return breakend.isAssembledLink() && getUnlinkedBreakendCount(breakend) == 0;
    }

    private int getSvReplicationCount(final SvVarData var)
    {
        Integer repCount = mSvOriginalReplicationMap.get(var);
        return repCount != null ? repCount.intValue() : 1;
    }

    private void setSvReplicationCounts()
    {
        if(!mHasReplication)
            return;

        for(final SvVarData var : mCluster.getSVs())
        {
            if(var.getReplicatedCount() > 0)
            {
                mSvReplicationMap.put(var, var.getReplicatedCount());
                mSvOriginalReplicationMap.put(var, var.getReplicatedCount());
            }
            else
            {
                mSvOriginalReplicationMap.put(var, 1);
            }
        }
    }

    private void setUnlinkedBreakends()
    {
        // make a cache of all unchained breakends in those of replicated SVs
        for(final SvVarData var : mCluster.getSVs(true))
        {
            for (int be = SvVarData.SE_START; be <= SvVarData.SE_END; ++be)
            {
                boolean isStart = SvVarData.isStart(be);

                if (var.isNullBreakend() && !isStart)
                    continue;

                final SvBreakend breakend = var.getBreakend(isStart);
                final SvBreakend origBreakend = breakend.getOrigBreakend();

                List<SvBreakend> breakends = mUnlinkedBreakendMap.get(origBreakend);

                if(breakends == null)
                {
                    breakends = Lists.newArrayList();
                    mUnlinkedBreakendMap.put(origBreakend, breakends);
                }

                breakends.add(breakend);
            }
        }

        mUnlinkedSVs.addAll(mCluster.getSVs(true));
    }

    private List<SvVarData> getMaxReplicationSVs()
    {
        List<SvVarData> maxRepIds = Lists.newArrayList();
        int maxRepCount = 1;

        for(Map.Entry<SvVarData,Integer> entry : mSvReplicationMap.entrySet())
        {
            int repCount = entry.getValue();

            if(repCount <= 1)
                continue;

            if(repCount > maxRepCount)
            {
                maxRepCount = repCount;
                maxRepIds.clear();
                maxRepIds.add(entry.getKey());
            }
            else if(repCount == maxRepCount)
            {
                if(!maxRepIds.contains(entry.getKey()))
                    maxRepIds.add(entry.getKey());
            }
        }

        return maxRepIds;
    }

    private void reconcileChains()
    {
        int index1 = 0;
        while(index1 < mPartialChains.size())
        {
            SvChain chain1 = mPartialChains.get(index1);

            boolean chainsMerged = false;

            for (int index2 = index1 + 1; index2 < mPartialChains.size(); ++index2)
            {
                SvChain chain2 = mPartialChains.get(index2);

                for (int be1 = SvVarData.SE_START; be1 <= SvVarData.SE_END; ++be1)
                {
                    boolean c1Start = SvVarData.isStart(be1);

                    for (int be2 = SvVarData.SE_START; be2 <= SvVarData.SE_END; ++be2)
                    {
                        boolean c2Start = SvVarData.isStart(be2);

                        if (chain1.canAddLinkedPair(chain2.getLinkedPair(c2Start), c1Start, false))
                        {
                            LOGGER.debug("merging chain({} links={}) with chain({} links={})",
                                    chain1.id(), chain1.getLinkCount(), chain2.id(), chain2.getLinkCount());

                            if(c2Start)
                            {
                                // merge chains and remove the latter
                                for (SvLinkedPair linkedPair : chain2.getLinkedPairs())
                                {
                                    chain1.addLink(linkedPair, c1Start);
                                }
                            }
                            else
                            {
                                // add in reverse
                                for (int index = chain2.getLinkedPairs().size() - 1; index >= 0; --index)
                                {
                                    SvLinkedPair linkedPair = chain2.getLinkedPairs().get(index);
                                    chain1.addLink(linkedPair, c1Start);
                                }
                            }

                            mPartialChains.remove(index2);

                            chainsMerged = true;
                            break;
                        }

                    }

                    if (chainsMerged)
                        break;
                }

                if (chainsMerged)
                    break;
            }

            if (!chainsMerged)
            {
                ++index1;
            }
        }
    }

    private static int LOG_LEVEL_ERROR = 0;
    private static int LOG_LEVEL_INFO = 1;
    private static int LOG_LEVEL_DEBUG = 2;
    private static int LOG_LEVEL_VERBOSE = 3;

    private void log(int level, final String message)
    {
        if(level >= LOG_LEVEL_VERBOSE && !mLogVerbose)
            return;

        if(level >= LOG_LEVEL_DEBUG && !LOGGER.isDebugEnabled())
            return;

        LOGGER.debug(message);
    }

    private void checkProgress()
    {
        if(!LOGGER.isDebugEnabled())
            return;

        if(!mHasReplication || mCluster.getSvCount() < 100)
            return;

        if((mLinkIndex % 100) == 0)
        {
            LOGGER.debug("cluster({}) chaining progress: SVs({}) partialChains({}) unlinked(SVs={} breakends={}) replicatedSVs({})",
                    mCluster.id(), mCluster.getSvCount(), mPartialChains.size(), mUnlinkedSVs.size(),
                    mUnlinkedBreakendMap.size(), mSvReplicationMap.size());
        }
    }

    private boolean checkHasValidState()
    {
        // first check that the remaining possible links are supported by unlinked breakends
        for(Map.Entry<SvBreakend,List<SvLinkedPair>> entry : mSvBreakendPossibleLinks.entrySet())
        {
            SvBreakend breakend = entry.getKey();

            List<SvBreakend> breakendList = mUnlinkedBreakendMap.get(breakend);

            if(breakendList == null)
            {
                LOGGER.error("cluster({}) runIndex({}): breakend({}) has {} possible pairs but no available breakends",
                        mCluster.id(), mLinkIndex, breakend.toString(), entry.getValue().size());

                mIsValid = false;
            }
        }

        return mIsValid;
    }

    private void cullPossibleLinks()
    {
        if(mMaxPossibleLinks == 0)
            return;

        int culledPairs = 0;
        for(Map.Entry<SvBreakend,List<SvLinkedPair>> entry : mSvBreakendPossibleLinks.entrySet())
        {
            SvBreakend breakend = entry.getKey();

            List<SvLinkedPair> possiblePairs = mSvBreakendPossibleLinks.get(breakend);

            if(exceedsMaxPossibleLinks(possiblePairs.size()))
                continue;

            int index = possiblePairs.size() - 1;
            while(exceedsMaxPossibleLinks(index))
            {
                SvLinkedPair pair = possiblePairs.get(index);
                SvBreakend otherBreakend = pair.getOtherBreakend(breakend);

                // only remove if the other breakend also has an excess of possible pairs and it's not in the first X entries
                List<SvLinkedPair> otherPossiblePairs = mSvBreakendPossibleLinks.get(otherBreakend);

                boolean canRemoveOther = true;
                for(int index2 = 0; index2 < mMaxPossibleLinks; ++index2)
                {
                    if(otherPossiblePairs.get(index2) == pair)
                    {
                        canRemoveOther = false;
                        break;
                    }
                }

                if(canRemoveOther)
                {
                    otherPossiblePairs.remove(pair);
                    possiblePairs.remove(possiblePairs.size() - 1);
                    ++culledPairs;
                }

                --index;
            }
        }

        LOGGER.debug("cluster({}) culled {} possible pairs", mCluster.id(), culledPairs);
    }

}