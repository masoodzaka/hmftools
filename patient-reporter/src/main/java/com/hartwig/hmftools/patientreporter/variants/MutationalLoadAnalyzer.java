package com.hartwig.hmftools.patientreporter.variants;

import java.util.List;

import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.EnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariant;

import org.jetbrains.annotations.NotNull;

public final class MutationalLoadAnalyzer {

    private MutationalLoadAnalyzer() {
    }

    static int analyzeVariants(@NotNull final List<EnrichedSomaticVariant> variants) {
        int mutationalLoadSize = 0;
        for (final SomaticVariant variant : variants) {
            if (mutationalLoadCheck(variant)) {
                mutationalLoadSize++;
            }
        }
        return mutationalLoadSize;
    }

    private static boolean mutationalLoadCheck(@NotNull final SomaticVariant variant) {
        // KODU: Patient reporting should already filter on passed only.
        assert !variant.isFiltered();
        return variant.worstCodingEffect() == CodingEffect.MISSENSE;
    }
}
