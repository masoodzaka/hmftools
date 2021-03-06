package com.hartwig.hmftools.common.variant.hotspot;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;

public class SAMSlicer {

    private final int minMappingQuality;
    private final Collection<GenomeRegion> regions;

    public SAMSlicer(final int minMappingQuality, @NotNull final Collection<GenomeRegion> slices) {
        this.minMappingQuality = minMappingQuality;
        this.regions = slices;
    }

    public void slice(@NotNull final SamReader samReader, @NotNull final Consumer<SAMRecord> consumer) {
        final Set<String> processed = Sets.newHashSet();
        final QueryInterval[] queryIntervals = createIntervals(regions, samReader.getFileHeader());

        try (final SAMRecordIterator iterator = samReader.queryOverlapping(queryIntervals)) {
            while (iterator.hasNext()) {
                final SAMRecord record = iterator.next();
                if (samRecordMeetsQualityRequirements(record)) {
                    if (processed.add(record.toString())) {
                        consumer.accept(record);
                    }
                }
            }
        }
    }

    @NotNull
    private static QueryInterval[] createIntervals(@NotNull final Collection<GenomeRegion> regions, @NotNull final SAMFileHeader header) {
        final List<QueryInterval> queryIntervals = Lists.newArrayList();
        for (final GenomeRegion region : regions) {
            int sequenceIndex = header.getSequenceIndex(region.chromosome());
            if (sequenceIndex > -1) {
                queryIntervals.add(new QueryInterval(sequenceIndex, (int) region.start(), (int) region.end()));
            }
        }
        return QueryInterval.optimizeIntervals(queryIntervals.toArray(new QueryInterval[queryIntervals.size()]));
    }

    private boolean samRecordMeetsQualityRequirements(@NotNull final SAMRecord record) {
        return record.getMappingQuality() >= minMappingQuality && !record.getReadUnmappedFlag() && !record.getDuplicateReadFlag() && !record
                .isSecondaryOrSupplementary();
    }
}
