/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables.records;


import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.tables.Ditag;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;
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
public class DitagRecord extends UpdatableRecordImpl<DitagRecord> implements Record5<UInteger, String, String, UShort, String> {

    private static final long serialVersionUID = 558037344;

    /**
     * Setter for <code>homo_sapiens_core_89_37.ditag.ditag_id</code>.
     */
    public void setDitagId(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.ditag.ditag_id</code>.
     */
    public UInteger getDitagId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.ditag.name</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.ditag.name</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.ditag.type</code>.
     */
    public void setType(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.ditag.type</code>.
     */
    public String getType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.ditag.tag_count</code>.
     */
    public void setTagCount(UShort value) {
        set(3, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.ditag.tag_count</code>.
     */
    public UShort getTagCount() {
        return (UShort) get(3);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.ditag.sequence</code>.
     */
    public void setSequence(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.ditag.sequence</code>.
     */
    public String getSequence() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row5<UInteger, String, String, UShort, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row5<UInteger, String, String, UShort, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field1() {
        return Ditag.DITAG.DITAG_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return Ditag.DITAG.NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return Ditag.DITAG.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UShort> field4() {
        return Ditag.DITAG.TAG_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return Ditag.DITAG.SEQUENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value1() {
        return getDitagId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UShort value4() {
        return getTagCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getSequence();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord value1(UInteger value) {
        setDitagId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord value2(String value) {
        setName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord value3(String value) {
        setType(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord value4(UShort value) {
        setTagCount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord value5(String value) {
        setSequence(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DitagRecord values(UInteger value1, String value2, String value3, UShort value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DitagRecord
     */
    public DitagRecord() {
        super(Ditag.DITAG);
    }

    /**
     * Create a detached, initialised DitagRecord
     */
    public DitagRecord(UInteger ditagId, String name, String type, UShort tagCount, String sequence) {
        super(Ditag.DITAG);

        set(0, ditagId);
        set(1, name);
        set(2, type);
        set(3, tagCount);
        set(4, sequence);
    }
}
