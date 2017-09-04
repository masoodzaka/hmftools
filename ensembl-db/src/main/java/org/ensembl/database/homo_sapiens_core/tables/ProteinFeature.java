/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.HomoSapiensCore_89_37;
import org.ensembl.database.homo_sapiens_core.Keys;
import org.ensembl.database.homo_sapiens_core.tables.records.ProteinFeatureRecord;
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
public class ProteinFeature extends TableImpl<ProteinFeatureRecord> {

    private static final long serialVersionUID = 739648795;

    /**
     * The reference instance of <code>homo_sapiens_core_89_37.protein_feature</code>
     */
    public static final ProteinFeature PROTEIN_FEATURE = new ProteinFeature();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ProteinFeatureRecord> getRecordType() {
        return ProteinFeatureRecord.class;
    }

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.protein_feature_id</code>.
     */
    public final TableField<ProteinFeatureRecord, UInteger> PROTEIN_FEATURE_ID = createField("protein_feature_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.translation_id</code>.
     */
    public final TableField<ProteinFeatureRecord, UInteger> TRANSLATION_ID = createField("translation_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.seq_start</code>.
     */
    public final TableField<ProteinFeatureRecord, Integer> SEQ_START = createField("seq_start", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.seq_end</code>.
     */
    public final TableField<ProteinFeatureRecord, Integer> SEQ_END = createField("seq_end", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.hit_start</code>.
     */
    public final TableField<ProteinFeatureRecord, Integer> HIT_START = createField("hit_start", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.hit_end</code>.
     */
    public final TableField<ProteinFeatureRecord, Integer> HIT_END = createField("hit_end", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.hit_name</code>.
     */
    public final TableField<ProteinFeatureRecord, String> HIT_NAME = createField("hit_name", org.jooq.impl.SQLDataType.VARCHAR.length(40).nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.analysis_id</code>.
     */
    public final TableField<ProteinFeatureRecord, UShort> ANALYSIS_ID = createField("analysis_id", org.jooq.impl.SQLDataType.SMALLINTUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.score</code>.
     */
    public final TableField<ProteinFeatureRecord, Double> SCORE = createField("score", org.jooq.impl.SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.evalue</code>.
     */
    public final TableField<ProteinFeatureRecord, Double> EVALUE = createField("evalue", org.jooq.impl.SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.perc_ident</code>.
     */
    public final TableField<ProteinFeatureRecord, Double> PERC_IDENT = createField("perc_ident", org.jooq.impl.SQLDataType.FLOAT, this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.external_data</code>.
     */
    public final TableField<ProteinFeatureRecord, String> EXTERNAL_DATA = createField("external_data", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.protein_feature.hit_description</code>.
     */
    public final TableField<ProteinFeatureRecord, String> HIT_DESCRIPTION = createField("hit_description", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * Create a <code>homo_sapiens_core_89_37.protein_feature</code> table reference
     */
    public ProteinFeature() {
        this("protein_feature", null);
    }

    /**
     * Create an aliased <code>homo_sapiens_core_89_37.protein_feature</code> table reference
     */
    public ProteinFeature(String alias) {
        this(alias, PROTEIN_FEATURE);
    }

    private ProteinFeature(String alias, Table<ProteinFeatureRecord> aliased) {
        this(alias, aliased, null);
    }

    private ProteinFeature(String alias, Table<ProteinFeatureRecord> aliased, Field<?>[] parameters) {
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
    public Identity<ProteinFeatureRecord, UInteger> getIdentity() {
        return Keys.IDENTITY_PROTEIN_FEATURE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<ProteinFeatureRecord> getPrimaryKey() {
        return Keys.KEY_PROTEIN_FEATURE_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<ProteinFeatureRecord>> getKeys() {
        return Arrays.<UniqueKey<ProteinFeatureRecord>>asList(Keys.KEY_PROTEIN_FEATURE_PRIMARY, Keys.KEY_PROTEIN_FEATURE_ALN_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProteinFeature as(String alias) {
        return new ProteinFeature(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ProteinFeature rename(String name) {
        return new ProteinFeature(name, null);
    }
}
