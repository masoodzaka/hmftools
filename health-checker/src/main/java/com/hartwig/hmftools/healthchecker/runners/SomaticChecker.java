package com.hartwig.hmftools.healthchecker.runners;

import static com.hartwig.hmftools.common.variant.predicate.VariantFilter.passOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.context.RunContext;
import com.hartwig.hmftools.common.exception.HartwigException;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.predicate.VariantFilter;
import com.hartwig.hmftools.common.variant.predicate.VariantPredicates;
import com.hartwig.hmftools.common.variant.vcf.VCFFileLoader;
import com.hartwig.hmftools.common.variant.vcf.VCFSomaticFile;
import com.hartwig.hmftools.healthchecker.resource.ResourceWrapper;
import com.hartwig.hmftools.healthchecker.result.BaseResult;
import com.hartwig.hmftools.healthchecker.result.MultiValueResult;
import com.hartwig.hmftools.healthchecker.result.NoResult;
import com.hartwig.hmftools.healthchecker.runners.checks.HealthCheck;
import com.hartwig.hmftools.healthchecker.runners.checks.SomaticCheck;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
@ResourceWrapper(type = CheckType.SOMATIC)
public class SomaticChecker extends ErrorHandlingChecker implements HealthChecker {

    private static final Logger LOGGER = LogManager.getLogger(SomaticChecker.class);

    private static final String SOMATICS_EXTENSION = "_melted.vcf";

    private static final double AF_SD_DISTANCE = 0.16;

    public SomaticChecker() {
    }

    @NotNull
    @Override
    public CheckType checkType() {
        return CheckType.SOMATIC;
    }

    @NotNull
    @Override
    public BaseResult tryRun(@NotNull final RunContext runContext) throws IOException, HartwigException {
        if (!runContext.isSomaticRun()) {
            return new NoResult(checkType());
        }
        final VCFSomaticFile variantFile = VCFFileLoader.loadSomaticVCF(runContext.runDirectory(), SOMATICS_EXTENSION);

        if (!variantFile.sample().equals(runContext.tumorSample())) {
            LOGGER.warn("Sample name in VCF (" + variantFile.sample() + ") does not match with name (" + runContext.tumorSample()
                    + ") from run context!");
        }
        final List<SomaticVariant> variants = passOnly(variantFile.variants());

        final List<HealthCheck> checks = Lists.newArrayList();
        checks.addAll(getTypeChecks(variants, runContext.tumorSample(), VariantType.SNP));
        checks.addAll(getTypeChecks(variants, runContext.tumorSample(), VariantType.INDEL));
        checks.addAll(getAFChecks(variants, runContext.tumorSample()));

        return toMultiValueResult(checks);
    }

    @NotNull
    @Override
    public BaseResult errorRun(@NotNull final RunContext runContext) {
        if (runContext.isSomaticRun()) {
            final List<HealthCheck> checks = Lists.newArrayList();
            for (final VariantType type : Lists.newArrayList(VariantType.SNP, VariantType.INDEL)) {
                checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.COUNT_TOTAL.checkName(type.name()),
                        HealthCheckConstants.ERROR_VALUE));
                checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.DBSNP_COUNT.checkName(type.name()),
                        HealthCheckConstants.ERROR_VALUE));
                checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.PROPORTION_CHECK.checkName(type.name()),
                        HealthCheckConstants.ERROR_VALUE));
            }

            checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.AF_LOWER_SD.checkName(), HealthCheckConstants.ERROR_VALUE));
            checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.AF_MEDIAN.checkName(), HealthCheckConstants.ERROR_VALUE));
            checks.add(new HealthCheck(runContext.tumorSample(), SomaticCheck.AF_UPPER_SD.checkName(), HealthCheckConstants.ERROR_VALUE));

            return toMultiValueResult(checks);
        } else {
            return new NoResult(checkType());
        }
    }

    @NotNull
    private BaseResult toMultiValueResult(@NotNull final List<HealthCheck> checks) {
        HealthCheck.log(LOGGER, checks);
        return new MultiValueResult(checkType(), checks);
    }

    @NotNull
    private static List<HealthCheck> getTypeChecks(@NotNull final List<SomaticVariant> variants, @NotNull final String sampleId,
            @NotNull final VariantType type) {
        final List<HealthCheck> checks = new ArrayList<>();
        final List<SomaticVariant> variantsForType = VariantFilter.filter(variants, VariantPredicates.withType(type));

        final HealthCheck variantCountCheck =
                new HealthCheck(sampleId, SomaticCheck.COUNT_TOTAL.checkName(type.name()), String.valueOf(variantsForType.size()));
        checks.add(variantCountCheck);

        final List<SomaticVariant> variantsWithDBSNPAndNotCOSMIC =
                VariantFilter.filter(variantsForType, VariantPredicates.inDBSNPAndNotInCOSMIC());
        final HealthCheck dbsnpCheck = new HealthCheck(sampleId, SomaticCheck.DBSNP_COUNT.checkName(type.name()),
                String.valueOf(variantsWithDBSNPAndNotCOSMIC.size()));
        checks.add(dbsnpCheck);

        final HealthCheck proportionCheck = calculateProportion(variantsForType, sampleId, type);
        checks.add(proportionCheck);
        return checks;
    }

    @NotNull
    private static List<HealthCheck> getAFChecks(final List<SomaticVariant> variants, final String sampleId) {
        final List<HealthCheck> checks = Lists.newArrayList();
        if (variants.size() > 0) {
            List<Double> alleleFreqs = variants.stream().map(SomaticVariant::alleleFrequency).collect(Collectors.toList());
            alleleFreqs.sort(Comparator.naturalOrder());

            int lowerSDIndex = (int) Math.round(alleleFreqs.size() * AF_SD_DISTANCE);
            int medianIndex = Math.min(alleleFreqs.size() - 1, (int) Math.round(alleleFreqs.size() / 2D));
            int upperSDIndex = Math.min(alleleFreqs.size() - 1, (int) Math.round(alleleFreqs.size() * (1 - AF_SD_DISTANCE)));

            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_LOWER_SD.checkName(), String.valueOf(alleleFreqs.get(lowerSDIndex))));
            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_MEDIAN.checkName(), String.valueOf(alleleFreqs.get(medianIndex))));
            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_UPPER_SD.checkName(), String.valueOf(alleleFreqs.get(upperSDIndex))));
        } else {
            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_LOWER_SD.checkName(), "-"));
            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_MEDIAN.checkName(), "-"));
            checks.add(new HealthCheck(sampleId, SomaticCheck.AF_UPPER_SD.checkName(), "-"));
        }
        return checks;
    }

    @NotNull
    private static HealthCheck calculateProportion(@NotNull final List<SomaticVariant> variants, @NotNull final String sampleId,
            @NotNull final VariantType type) {
        final List<SomaticVariant> variantsWithCallerCount = VariantFilter.filter(variants, VariantPredicates.withType(type));
        double proportion = 0D;
        if (!variantsWithCallerCount.isEmpty() && !variants.isEmpty()) {
            proportion = (double) variantsWithCallerCount.size() / variants.size();
        }
        return new HealthCheck(sampleId, SomaticCheck.PROPORTION_CHECK.checkName(type.name()), String.valueOf(proportion));
    }
}
