package com.hartwig.hmftools.patientreporter.variants;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.hartwig.hmftools.svannotation.VariantAnnotation;

public class StructuralVariantAnalysis {

    public static class GeneFusion {
        public String Type;
        public String Start;
        public String GeneStart;
        public String GeneContextStart;
        public String TranscriptStart;
        public String End;
        public String GeneEnd;
        public String GeneContextEnd;
        public String TranscriptEnd;
        public String VAF;
    }

    public static class GeneDisruption {
        public String GeneName;
        public String Location;
        public String GeneContext;
        public String Transcript;
        public String Partner;
        public String HGVS;
        public String Type;
        public String Orientation;
        public String VAF;
        public String TAF = "TODO";
    }

    private final List<VariantAnnotation> annotations;
    private final List<GeneFusion> fusions;
    private final List<GeneDisruption> disruptions;

    public StructuralVariantAnalysis() {
        this.annotations = Collections.emptyList();
        this.fusions = Collections.emptyList();
        this.disruptions = Collections.emptyList();
    }

    public StructuralVariantAnalysis(final List<VariantAnnotation> annotations, final List<GeneFusion> fusions,
            final List<GeneDisruption> disruptions) {
        this.annotations = annotations;
        this.fusions = fusions;
        this.disruptions = disruptions;
    }

    public List<VariantAnnotation> getAnnotations() {
        return ImmutableList.copyOf(annotations);
    }

    public List<GeneFusion> getFusions() {
        return ImmutableList.copyOf(fusions);
    }

    public List<GeneDisruption> getDisruptions() {
        return ImmutableList.copyOf(disruptions);
    }
}
