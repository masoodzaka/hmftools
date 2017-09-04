/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.HomoSapiensCore_89_37;
import org.ensembl.database.homo_sapiens_core.Keys;
import org.ensembl.database.homo_sapiens_core.tables.records.InterproRecord;
import org.jooq.Field;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


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
public class Interpro extends TableImpl<InterproRecord> {

    private static final long serialVersionUID = 1677914836;

    /**
     * The reference instance of <code>homo_sapiens_core_89_37.interpro</code>
     */
    public static final Interpro INTERPRO = new Interpro();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<InterproRecord> getRecordType() {
        return InterproRecord.class;
    }

    /**
     * The column <code>homo_sapiens_core_89_37.interpro.interpro_ac</code>.
     */
    public final TableField<InterproRecord, String> INTERPRO_AC = createField("interpro_ac", org.jooq.impl.SQLDataType.VARCHAR.length(40).nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.interpro.id</code>.
     */
    public final TableField<InterproRecord, String> ID = createField("id", org.jooq.impl.SQLDataType.VARCHAR.length(40).nullable(false), this, "");

    /**
     * Create a <code>homo_sapiens_core_89_37.interpro</code> table reference
     */
    public Interpro() {
        this("interpro", null);
    }

    /**
     * Create an aliased <code>homo_sapiens_core_89_37.interpro</code> table reference
     */
    public Interpro(String alias) {
        this(alias, INTERPRO);
    }

    private Interpro(String alias, Table<InterproRecord> aliased) {
        this(alias, aliased, null);
    }

    private Interpro(String alias, Table<InterproRecord> aliased, Field<?>[] parameters) {
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
    public List<UniqueKey<InterproRecord>> getKeys() {
        return Arrays.<UniqueKey<InterproRecord>>asList(Keys.KEY_INTERPRO_ACCESSION_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Interpro as(String alias) {
        return new Interpro(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Interpro rename(String name) {
        return new Interpro(name, null);
    }
}
