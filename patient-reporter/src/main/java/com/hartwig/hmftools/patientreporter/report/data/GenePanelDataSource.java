package com.hartwig.hmftools.patientreporter.report.data;

import static net.sf.dynamicreports.report.builder.DynamicReports.field;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.region.HmfGenomeRegion;
import com.hartwig.hmftools.patientreporter.SequencedReportData;

import org.jetbrains.annotations.NotNull;

import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.FieldBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

public final class GenePanelDataSource {

    public static final FieldBuilder<?> GENE_FIELD = field("gene", String.class);
    public static final FieldBuilder<?> TRANSCRIPT_FIELD = field("transcript", String.class);
    public static final FieldBuilder<?> TYPE_FIELD = field("type", String.class);

    private GenePanelDataSource() {
    }

    @NotNull
    public static JRDataSource fromSequencedReportData(@NotNull final SequencedReportData sequencedReportData) {
        final DRDataSource genePanelDataSource = new DRDataSource(GENE_FIELD.getName(), TRANSCRIPT_FIELD.getName(), TYPE_FIELD.getName());
        final List<HmfGenomeRegion> regions = Lists.newArrayList(sequencedReportData.panelGeneModel().regions());
        regions.sort(Comparator.comparing(HmfGenomeRegion::gene));

        for (final HmfGenomeRegion region : regions) {
            final String role = sequencedReportData.cosmicGeneModel().getRoleForGene(region.gene());
            genePanelDataSource.add(region.gene(), region.transcript(), role);
        }
        return genePanelDataSource;
    }

    @NotNull
    public static AbstractSimpleExpression<String> transcriptUrl() {
        return new TranscriptExpression(TRANSCRIPT_FIELD);
    }
}
