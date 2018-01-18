package com.hartwig.hmftools.patientdb.dao;

import static com.hartwig.hmftools.patientdb.Config.BATCH_INSERT_SIZE;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANT;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANTBREAKEND;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANTDISRUPTION;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.STRUCTURALVARIANTFUSION;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Iterables;
import com.hartwig.hmftools.common.variant.structural.EnrichedStructuralVariant;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep21;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.types.UInteger;

class StructuralVariantDAO {
    @NotNull
    private final DSLContext context;

    StructuralVariantDAO(@NotNull final DSLContext context) {
        this.context = context;
    }

    void write(@NotNull final String sample, @NotNull final List<EnrichedStructuralVariant> variants) {
        Timestamp timestamp = new Timestamp(new Date().getTime());

        final Result<Record1<UInteger>> breakendsToDelete = context.select(STRUCTURALVARIANTBREAKEND.ID)
                .from(STRUCTURALVARIANTBREAKEND)
                .innerJoin(STRUCTURALVARIANT)
                .on(STRUCTURALVARIANT.ID.eq(STRUCTURALVARIANTBREAKEND.STRUCTURALVARIANTID))
                .where(STRUCTURALVARIANT.SAMPLEID.eq(sample))
                .fetch();

        // NERA: delete annotations
        context.delete(STRUCTURALVARIANTDISRUPTION).where(STRUCTURALVARIANTDISRUPTION.BREAKENDID.in(breakendsToDelete)).execute();
        context.delete(STRUCTURALVARIANTFUSION).where(STRUCTURALVARIANTFUSION.FIVEPRIMEBREAKENDID.in(breakendsToDelete)).execute();
        context.delete(STRUCTURALVARIANTBREAKEND).where(STRUCTURALVARIANTBREAKEND.ID.in(breakendsToDelete)).execute();

        // NERA: delete structural variants
        context.delete(STRUCTURALVARIANT).where(STRUCTURALVARIANT.SAMPLEID.eq(sample)).execute();

        for (List<EnrichedStructuralVariant> batch : Iterables.partition(variants, BATCH_INSERT_SIZE)) {
            InsertValuesStep21 inserter = context.insertInto(STRUCTURALVARIANT,
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
                    STRUCTURALVARIANT.ADJUSTEDSTARTAF,
                    STRUCTURALVARIANT.ADJUSTEDSTARTCOPYNUMBER,
                    STRUCTURALVARIANT.ADJUSTEDSTARTCOPYNUMBERCHANGE,
                    STRUCTURALVARIANT.ENDAF,
                    STRUCTURALVARIANT.ADJUSTEDENDAF,
                    STRUCTURALVARIANT.ADJUSTEDENDCOPYNUMBER,
                    STRUCTURALVARIANT.ADJUSTEDENDCOPYNUMBERCHANGE,
                    STRUCTURALVARIANT.PLOIDY,
                    STRUCTURALVARIANT.MODIFIED);
            batch.forEach(entry -> addRecord(timestamp, inserter, sample, entry));
            inserter.execute();
        }
    }

    private static void addRecord(@NotNull Timestamp timestamp, @NotNull InsertValuesStep21 inserter, @NotNull String sample,
            @NotNull EnrichedStructuralVariant variant) {
        //noinspection unchecked
        inserter.values(sample,
                variant.start().chromosome(),
                variant.end().chromosome(),
                variant.start().position(),
                variant.end().position(),
                variant.start().orientation(),
                variant.end().orientation(),
                variant.start().homology(),
                variant.end().homology(),
                variant.insertSequence(),
                variant.type(),
                variant.start().alleleFrequency(),
                variant.start().adjustedAlleleFrequency(),
                variant.start().adjustedCopyNumber(),
                variant.start().adjustedCopyNumberChange(),
                variant.end().alleleFrequency(),
                variant.end().adjustedAlleleFrequency(),
                variant.end().adjustedCopyNumber(),
                variant.end().adjustedCopyNumberChange(),
                variant.ploidy(),
                timestamp);
    }
}