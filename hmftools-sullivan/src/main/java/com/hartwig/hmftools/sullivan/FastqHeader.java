package com.hartwig.hmftools.sullivan;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.samtools.fastq.FastqRecord;
import org.jetbrains.annotations.NotNull;

class FastqHeader {

    private static final String PARSE_REGEXP = ":";

    @NotNull
    private final String instrumentName;
    private final int runId;
    @NotNull
    private final String flowcellId;
    private final int flowcellLane;
    private final int tileNumber;
    private final int xCoordinate;
    private final int yCoordinate;

    public static FastqHeader parseFromFastqRecord(@NotNull FastqRecord record,
                                                   @NotNull FastqHeaderNormalizer normalizer) {
        String normalized = normalizer.apply(record.getReadHeader());

        String[] parts = normalized.split(PARSE_REGEXP);
        return new FastqHeader(parts[0], Integer.valueOf(parts[1]), parts[2], Integer.valueOf(parts[3]),
                Integer.valueOf(parts[4]), Integer.valueOf(parts[5]), Integer.valueOf(parts[6]));
    }

    @VisibleForTesting
    FastqHeader(@NotNull String instrumentName, int runId, @NotNull String flowcellId,
                       int flowcellLane, int tileNumber, int xCoordinate, int yCoordinate) {
        this.instrumentName = instrumentName;
        this.runId = runId;
        this.flowcellId = flowcellId;
        this.flowcellLane = flowcellLane;
        this.tileNumber = tileNumber;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    @NotNull
    public FastqHeaderKey key() {
        return new FastqHeaderKey(tileNumber, xCoordinate, yCoordinate);
    }

    @NotNull
    public String reference() {
         return instrumentName + PARSE_REGEXP + Integer.toString(runId) + PARSE_REGEXP +
                 flowcellId + PARSE_REGEXP + Integer.toString(flowcellLane);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FastqHeader that = (FastqHeader) o;

        if (runId != that.runId) return false;
        if (flowcellLane != that.flowcellLane) return false;
        if (tileNumber != that.tileNumber) return false;
        if (xCoordinate != that.xCoordinate) return false;
        if (yCoordinate != that.yCoordinate) return false;
        if (!instrumentName.equals(that.instrumentName)) return false;
        return flowcellId.equals(that.flowcellId);
    }

    @Override
    public int hashCode() {
        int result = instrumentName.hashCode();
        result = 31 * result + runId;
        result = 31 * result + flowcellId.hashCode();
        result = 31 * result + flowcellLane;
        result = 31 * result + tileNumber;
        result = 31 * result + xCoordinate;
        result = 31 * result + yCoordinate;
        return result;
    }
}
