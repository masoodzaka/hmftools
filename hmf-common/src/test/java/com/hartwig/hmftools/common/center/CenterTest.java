package com.hartwig.hmftools.common.center;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Resources;
import com.hartwig.hmftools.common.exception.EmptyFileException;

import org.junit.Test;

public class CenterTest {

    private static final String BASE_RESOURCE_DIR = Resources.getResource("center").getPath();
    private static final String TEST_FILE = BASE_RESOURCE_DIR + File.separator + "centers.csv";

    @Test
    public void canReadCPCTRecipients() throws IOException, EmptyFileException {
        final CenterModel centerModel = Center.readFromCSV(TEST_FILE);
        assertEquals("my@email.com; my2@email.com", centerModel.getCpctRecipients("01"));
    }

    @Test
    public void canReadPIs() throws IOException, EmptyFileException {
        final CenterModel centerModel = Center.readFromCSV(TEST_FILE);
        final CenterData center = centerModel.centerPerId("01");
        assertNotNull(center);
        assertEquals("Someone", CenterModel.getPI("CPCT02010001", center));
        assertEquals("Someone Else", CenterModel.getPI("DRUP01010001", center));

        //MIVO center with '*' for drup pi & recipients
        final CenterData center2 = centerModel.centerPerId("02");
        assertNotNull(center2);
        assertEquals("Someone 2", CenterModel.getPI("CPCT02010001", center2));
        assertEquals("Someone 2", CenterModel.getPI("DRUP01010001", center2));
    }

    @Test
    public void canReadDRUPRecipients() throws IOException, EmptyFileException {
        final CenterModel centerModel = Center.readFromCSV(TEST_FILE);
        assertEquals("my3@email.com", centerModel.getDrupRecipients("01"));
        //MIVO: drup recipient field with '*'
        assertEquals("my@email.com; my2@email.com", centerModel.getDrupRecipients("02"));
    }

    @Test
    public void canReadAddress() throws IOException, EmptyFileException {
        final CenterModel centerModel = Center.readFromCSV(TEST_FILE);
        final CenterData center = centerModel.centerPerId("01");
        assertNotNull(center);
        assertEquals("Address-AVL", center.addressName());
        assertEquals("1000 AB", center.addressZip());
        assertEquals("AMSTERDAM", center.addressCity());
    }
}
