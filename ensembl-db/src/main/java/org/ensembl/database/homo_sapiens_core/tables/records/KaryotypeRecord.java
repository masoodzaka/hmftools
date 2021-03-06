/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables.records;


import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.tables.Karyotype;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;


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
public class KaryotypeRecord extends UpdatableRecordImpl<KaryotypeRecord> implements Record6<UInteger, UInteger, UInteger, UInteger, String, String> {

    private static final long serialVersionUID = 546780488;

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.karyotype_id</code>.
     */
    public void setKaryotypeId(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.karyotype_id</code>.
     */
    public UInteger getKaryotypeId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.seq_region_id</code>.
     */
    public void setSeqRegionId(UInteger value) {
        set(1, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.seq_region_id</code>.
     */
    public UInteger getSeqRegionId() {
        return (UInteger) get(1);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.seq_region_start</code>.
     */
    public void setSeqRegionStart(UInteger value) {
        set(2, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.seq_region_start</code>.
     */
    public UInteger getSeqRegionStart() {
        return (UInteger) get(2);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.seq_region_end</code>.
     */
    public void setSeqRegionEnd(UInteger value) {
        set(3, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.seq_region_end</code>.
     */
    public UInteger getSeqRegionEnd() {
        return (UInteger) get(3);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.band</code>.
     */
    public void setBand(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.band</code>.
     */
    public String getBand() {
        return (String) get(4);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.karyotype.stain</code>.
     */
    public void setStain(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.karyotype.stain</code>.
     */
    public String getStain() {
        return (String) get(5);
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
    // Record6 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<UInteger, UInteger, UInteger, UInteger, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<UInteger, UInteger, UInteger, UInteger, String, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field1() {
        return Karyotype.KARYOTYPE.KARYOTYPE_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field2() {
        return Karyotype.KARYOTYPE.SEQ_REGION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field3() {
        return Karyotype.KARYOTYPE.SEQ_REGION_START;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field4() {
        return Karyotype.KARYOTYPE.SEQ_REGION_END;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return Karyotype.KARYOTYPE.BAND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return Karyotype.KARYOTYPE.STAIN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value1() {
        return getKaryotypeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value2() {
        return getSeqRegionId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value3() {
        return getSeqRegionStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value4() {
        return getSeqRegionEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getBand();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getStain();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value1(UInteger value) {
        setKaryotypeId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value2(UInteger value) {
        setSeqRegionId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value3(UInteger value) {
        setSeqRegionStart(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value4(UInteger value) {
        setSeqRegionEnd(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value5(String value) {
        setBand(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord value6(String value) {
        setStain(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KaryotypeRecord values(UInteger value1, UInteger value2, UInteger value3, UInteger value4, String value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached KaryotypeRecord
     */
    public KaryotypeRecord() {
        super(Karyotype.KARYOTYPE);
    }

    /**
     * Create a detached, initialised KaryotypeRecord
     */
    public KaryotypeRecord(UInteger karyotypeId, UInteger seqRegionId, UInteger seqRegionStart, UInteger seqRegionEnd, String band, String stain) {
        super(Karyotype.KARYOTYPE);

        set(0, karyotypeId);
        set(1, seqRegionId);
        set(2, seqRegionStart);
        set(3, seqRegionEnd);
        set(4, band);
        set(5, stain);
    }
}
