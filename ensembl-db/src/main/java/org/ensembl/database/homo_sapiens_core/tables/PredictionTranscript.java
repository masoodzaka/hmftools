/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.HomoSapiensCore_89_37;
import org.ensembl.database.homo_sapiens_core.Keys;
import org.ensembl.database.homo_sapiens_core.tables.records.PredictionTranscriptRecord;
import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PredictionTranscript extends TableImpl<PredictionTranscriptRecord> {

    private static final long serialVersionUID = 405022015;

    /**
     * The reference instance of <code>homo_sapiens_core_89_37.prediction_transcript</code>
     */
    public static final PredictionTranscript PREDICTION_TRANSCRIPT = new PredictionTranscript();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PredictionTranscriptRecord> getRecordType() {
        return PredictionTranscriptRecord.class;
    }

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.prediction_transcript_id</code>.
     */
    public final TableField<PredictionTranscriptRecord, UInteger> PREDICTION_TRANSCRIPT_ID = createField("prediction_transcript_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.seq_region_id</code>.
     */
    public final TableField<PredictionTranscriptRecord, UInteger> SEQ_REGION_ID = createField("seq_region_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.seq_region_start</code>.
     */
    public final TableField<PredictionTranscriptRecord, UInteger> SEQ_REGION_START = createField("seq_region_start", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.seq_region_end</code>.
     */
    public final TableField<PredictionTranscriptRecord, UInteger> SEQ_REGION_END = createField("seq_region_end", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.seq_region_strand</code>.
     */
    public final TableField<PredictionTranscriptRecord, Byte> SEQ_REGION_STRAND = createField("seq_region_strand", org.jooq.impl.SQLDataType.TINYINT.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.analysis_id</code>.
     */
    public final TableField<PredictionTranscriptRecord, UShort> ANALYSIS_ID = createField("analysis_id", org.jooq.impl.SQLDataType.SMALLINTUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.prediction_transcript.display_label</code>.
     */
    public final TableField<PredictionTranscriptRecord, String> DISPLAY_LABEL = createField("display_label", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * Create a <code>homo_sapiens_core_89_37.prediction_transcript</code> table reference
     */
    public PredictionTranscript() {
        this("prediction_transcript", null);
    }

    /**
     * Create an aliased <code>homo_sapiens_core_89_37.prediction_transcript</code> table reference
     */
    public PredictionTranscript(String alias) {
        this(alias, PREDICTION_TRANSCRIPT);
    }

    private PredictionTranscript(String alias, Table<PredictionTranscriptRecord> aliased) {
        this(alias, aliased, null);
    }

    private PredictionTranscript(String alias, Table<PredictionTranscriptRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return HomoSapiensCore_89_37.HOMO_SAPIENS_CORE_89_37;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<PredictionTranscriptRecord, UInteger> getIdentity() {
        return Keys.IDENTITY_PREDICTION_TRANSCRIPT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PredictionTranscriptRecord> getPrimaryKey() {
        return Keys.KEY_PREDICTION_TRANSCRIPT_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PredictionTranscriptRecord>> getKeys() {
        return Arrays.<UniqueKey<PredictionTranscriptRecord>>asList(Keys.KEY_PREDICTION_TRANSCRIPT_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PredictionTranscript as(String alias) {
        return new PredictionTranscript(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public PredictionTranscript rename(String name) {
        return new PredictionTranscript(name, null);
    }
}
