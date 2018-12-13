package com.hartwig.hmftools.patientdb.dao;

import java.util.List;

import com.google.common.collect.Iterables;
import com.hartwig.hmftools.common.actionability.EvidenceItem;

import static com.hartwig.hmftools.patientdb.Config.DB_BATCH_INSERT_SIZE;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.CLINICALEVIDENCE;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep10;

public class ClinialEvidenceDAO {

    @NotNull
    private final DSLContext context;

    ClinialEvidenceDAO(@NotNull final DSLContext context) {
        this.context = context;
    }

    void writeClinicalEvidence(@NotNull String sample, @NotNull List<EvidenceItem> evidenceItem) {
        deleteClinicalEvidenceForSample(sample);

        for (List<EvidenceItem> items : Iterables.partition(evidenceItem, DB_BATCH_INSERT_SIZE)) {
            InsertValuesStep10 inserter = context.insertInto(CLINICALEVIDENCE,
                    CLINICALEVIDENCE.SAMPLEID,
                    CLINICALEVIDENCE.EVENTTYPE,
                    CLINICALEVIDENCE.EVENTMATCH,
                    CLINICALEVIDENCE.DRUG,
                    CLINICALEVIDENCE.DRUGSTYPE,
                    CLINICALEVIDENCE.RESPONSE,
                    CLINICALEVIDENCE.CANCERTYPE,
                    CLINICALEVIDENCE.LABEL,
                    CLINICALEVIDENCE.EVIDENCELEVEL,
                    CLINICALEVIDENCE.EVIDENCESOURCE);
            items.forEach(trial -> addValues(sample, trial, inserter));
            inserter.execute();
        }

    }

    private static void addValues(@NotNull String sample, @NotNull EvidenceItem evidenceItem, @NotNull InsertValuesStep10 inserter) {
        inserter.values(sample, evidenceItem.event(), evidenceItem.scope().readableString(), evidenceItem.drug(), evidenceItem.drugsType(),
                evidenceItem.response(), evidenceItem.cancerType(), true, evidenceItem.level().readableString(), evidenceItem.source().sourceName());

    }

    void deleteClinicalEvidenceForSample(@NotNull String sample) {
        context.delete(CLINICALEVIDENCE).where(CLINICALEVIDENCE.SAMPLEID.eq(sample)).execute();
    }
}