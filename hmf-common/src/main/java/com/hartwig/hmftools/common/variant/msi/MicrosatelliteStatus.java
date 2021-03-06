package com.hartwig.hmftools.common.variant.msi;

import com.hartwig.hmftools.common.utils.Doubles;

import org.jetbrains.annotations.NotNull;

public enum MicrosatelliteStatus {
    MSI,
    MSS,
    UNKNOWN;

    private static final double MSI_CUTOFF = 4.0;

    @NotNull
    public static MicrosatelliteStatus fromIndelsPerMb(double microsatelliteIndelsPerMb) {
        return Doubles.greaterOrEqual(microsatelliteIndelsPerMb, MSI_CUTOFF) ? MSI : MSS;
    }
}
