package com.hartwig.hmftools.common.lims;

import org.jetbrains.annotations.NotNull;

public enum LimsSampleType {
    CORE,
    WIDE,
    CPCT,
    DRUP,
    OTHER;

    @NotNull
    public static LimsSampleType fromSampleId(@NotNull String sampleId) {
        if (sampleId.startsWith("CPCT")) {
            return CPCT;
        } else if (sampleId.startsWith("WIDE")) {
            return WIDE;
        } else if (sampleId.startsWith("CORE")) {
            return CORE;
        } else if (sampleId.startsWith("DRUP")) {
            return DRUP;
        }

        return OTHER;
    }
}
