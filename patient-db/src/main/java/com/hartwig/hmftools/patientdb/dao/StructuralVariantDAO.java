package com.hartwig.hmftools.patientdb.dao;

import static com.hartwig.hmftools.patientdb.Config.BATCH_INSERT_SIZE;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANT;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANTCLUSTER;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.segment.StructuralVariantCluster;
import com.hartwig.hmftools.common.variant.structural.ImmutableStructuralVariant;
import com.hartwig.hmftools.common.variant.structural.StructuralVariant;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep14;
import org.jooq.InsertValuesStep9;
import org.jooq.Record;
import org.jooq.Result;

class StructuralVariantDAO {
    @NotNull
    private final DSLContext context;

    StructuralVariantDAO(@NotNull final DSLContext context) {
        this.context = context;
    }

    @NotNull
    List<StructuralVariant> read(@NotNull final String sample) {
        List<StructuralVariant> regions = Lists.newArrayList();

        Result<Record> result = context.select().from(STRUCTURALVARIANT).where(STRUCTURALVARIANT.SAMPLEID.eq(sample)).fetch();

        for (Record record : result) {

            StructuralVariant variant = ImmutableStructuralVariant.builder()
                    .startChromosome(record.getValue(STRUCTURALVARIANT.STARTCHROMOSOME))
                    .endChromosome(record.getValue(STRUCTURALVARIANT.ENDCHROMOSOME))
                    .startPosition(record.getValue(STRUCTURALVARIANT.STARTPOSITION))
                    .endPosition(record.getValue(STRUCTURALVARIANT.ENDPOSITION))
                    .startOrientation(record.getValue(STRUCTURALVARIANT.STARTORIENTATION))
                    .endOrientation(record.getValue(STRUCTURALVARIANT.ENDORIENTATION))
                    .startHomology(record.getValue(STRUCTURALVARIANT.STARTHOMOLOGYSEQUENCE))
                    .endHomology(record.getValue(STRUCTURALVARIANT.ENDHOMOLOGYSEQUENCE))
                    .insertSequence(record.getValue(STRUCTURALVARIANT.INSERTSEQUENCE))
                    .type(StructuralVariantType.fromAttribute(record.getValue(STRUCTURALVARIANT.TYPE)))
                    .startAF(record.getValue(STRUCTURALVARIANT.STARTAF))
                    .endAF(record.getValue(STRUCTURALVARIANT.ENDAF))
                    .build();

            regions.add(variant);
        }

        return regions;
    }

    void write(@NotNull final String sample, @NotNull final List<StructuralVariant> regions) {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        context.delete(STRUCTURALVARIANT).where(STRUCTURALVARIANT.SAMPLEID.eq(sample)).execute();

        for (List<StructuralVariant> batch : Iterables.partition(regions, BATCH_INSERT_SIZE)) {
            InsertValuesStep14 inserter = context.insertInto(STRUCTURALVARIANT,
                    STRUCTURALVARIANT.SAMPLEID,
                    STRUCTURALVARIANT.STARTCHROMOSOME,
                    STRUCTURALVARIANT.ENDCHROMOSOME,
                    STRUCTURALVARIANT.STARTPOSITION,
                    STRUCTURALVARIANT.ENDPOSITION,
                    STRUCTURALVARIANT.STARTORIENTATION,
                    STRUCTURALVARIANT.ENDORIENTATION,
                    STRUCTURALVARIANT.STARTHOMOLOGYSEQUENCE,
                    STRUCTURALVARIANT.ENDHOMOLOGYSEQUENCE,
                    STRUCTURALVARIANT.INSERTSEQUENCE,
                    STRUCTURALVARIANT.TYPE,
                    STRUCTURALVARIANT.STARTAF,
                    STRUCTURALVARIANT.ENDAF,
                    STRUCTURALVARIANT.MODIFIED);
            batch.forEach(x -> addRecord(timestamp, inserter, sample, x));
            inserter.execute();
        }
    }

    void writeClusters(@NotNull final String sample, @NotNull final Collection<StructuralVariantCluster> clusters) {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        context.delete(STRUCTURALVARIANTCLUSTER).where(STRUCTURALVARIANTCLUSTER.SAMPLEID.eq(sample)).execute();

        for (List<StructuralVariantCluster> batch : Iterables.partition(clusters, BATCH_INSERT_SIZE)) {
            InsertValuesStep9 inserter = context.insertInto(STRUCTURALVARIANTCLUSTER,
                    STRUCTURALVARIANTCLUSTER.SAMPLEID,
                    STRUCTURALVARIANTCLUSTER.CHROMOSOME,
                    STRUCTURALVARIANTCLUSTER.START,
                    STRUCTURALVARIANTCLUSTER.END,
                    STRUCTURALVARIANTCLUSTER.FIRSTVARIANT,
                    STRUCTURALVARIANTCLUSTER.FINALVARIANT,
                    STRUCTURALVARIANTCLUSTER.COUNT,
                    STRUCTURALVARIANTCLUSTER.TYPE,
                    STRUCTURALVARIANTCLUSTER.MODIFIED);

            batch.forEach(x -> addRecord(timestamp, inserter, sample, x));
            inserter.execute();
        }

    }

    private void addRecord(Timestamp timestamp, InsertValuesStep9 inserter, String sample, StructuralVariantCluster cluster) {
        inserter.values(sample,
                cluster.chromosome(),
                cluster.start(),
                cluster.end(),
                cluster.firstVariantPosition(),
                cluster.finalVariantPosition(),
                cluster.variants().size(),
                cluster.type(),
                timestamp);
    }

    private void addRecord(Timestamp timestamp, InsertValuesStep14 inserter, String sample, StructuralVariant region) {
        inserter.values(sample,
                region.startChromosome(),
                region.endChromosome(),
                region.startPosition(),
                region.endPosition(),
                region.startOrientation(),
                region.endOrientation(),
                region.startHomology(),
                region.endHomology(),
                region.insertSequence(),
                region.type(),
                region.startAF(),
                region.endAF(),
                timestamp);
    }
}