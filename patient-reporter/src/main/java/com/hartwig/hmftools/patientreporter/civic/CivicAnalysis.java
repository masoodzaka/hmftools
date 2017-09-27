package com.hartwig.hmftools.patientreporter.civic;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.apiclients.civic.api.CivicApiWrapper;
import com.hartwig.hmftools.apiclients.civic.data.CivicVariant;
import com.hartwig.hmftools.apiclients.diseaseontology.api.DiseaseOntologyApiWrapper;
import com.hartwig.hmftools.common.gene.GeneModel;
import com.hartwig.hmftools.common.region.hmfslicer.HmfGenomeRegion;
import com.hartwig.hmftools.patientreporter.report.data.Alteration;
import com.hartwig.hmftools.patientreporter.variants.VariantReport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CivicAnalysis {
    private static final Logger LOGGER = LogManager.getLogger(CivicAnalysis.class);

    public static List<Alteration> run(@NotNull final List<VariantReport> reportedVariants, @NotNull final GeneModel geneModel,
            @NotNull final Set<String> tumorDoids) {
        LOGGER.info(" Analysing civic associations...");
        final Set<String> tumorSubtypesDoids = getTumorSubtypesDoids(tumorDoids);
        if (tumorSubtypesDoids.isEmpty()) {
            LOGGER.warn("  Disease-ontology id set for this tumor is empty!");
        }
        return getCivicAlterations(reportedVariants, geneModel, tumorSubtypesDoids);
    }

    private static Set<String> getTumorSubtypesDoids(@NotNull final Set<String> tumorDoids) {
        LOGGER.info("  Fetching tumor subtypes...");
        final Set<String> tumorSubtypesDoids = Sets.newHashSet();
        tumorSubtypesDoids.addAll(tumorDoids);
        final DiseaseOntologyApiWrapper diseaseOntologyApi = new DiseaseOntologyApiWrapper();
        for (final String tumorDoid : tumorDoids) {
            try {
                final List<String> childrenDoid = diseaseOntologyApi.getAllChildrenDoids(tumorDoid).toList().blockingGet();
                tumorSubtypesDoids.addAll(childrenDoid);
            } catch (final Throwable throwable) {
                LOGGER.error("  Failed to get children doids for tumor doid: " + tumorDoid + ". error message: " + throwable.getMessage());
            }
        }
        diseaseOntologyApi.releaseResources();
        return tumorSubtypesDoids;
    }

    @NotNull
    private static List<Alteration> getCivicAlterations(@NotNull final List<VariantReport> reportedVariants,
            @NotNull final GeneModel geneModel, @NotNull final Set<String> tumorSubtypesDoids) {
        LOGGER.info("  Fetching civic alterations...");
        final List<Alteration> alterations = Lists.newArrayList();
        final CivicApiWrapper civicApi = new CivicApiWrapper();
        for (final VariantReport variantReport : reportedVariants) {
            for (final HmfGenomeRegion region : geneModel.hmfRegions()) {
                if (region.gene().equals(variantReport.gene())) {
                    final int entrezId = Integer.parseInt(region.entrezId());
                    try {
                        final List<CivicVariant> civicVariants = civicApi.getVariantsForGene(entrezId).toList().blockingGet();
                        final Alteration alteration = Alteration.from(variantReport, civicVariants, tumorSubtypesDoids);
                        if (alteration.getMatches().size() > 0) {
                            alterations.add(alteration);
                        }
                    } catch (final Throwable throwable) {
                        LOGGER.error("  Failed to get civic variants for variant: " + variantReport.variant().chromosomePosition()
                                + ". error message: " + throwable.getMessage());
                    }
                }
            }
        }
        civicApi.releaseResources();
        return alterations;
    }
}
