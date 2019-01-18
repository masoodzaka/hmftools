package com.hartwig.hmftools.svanalysis.visualisation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class SvLinkFile {

    private static final String COMMENT = "#";
    private static final String DELIMITER = "\t";

    @NotNull
    public static List<SvLink> readLinks(@NotNull final String fileName) throws IOException {
        return fromLines(Files.readAllLines(new File(fileName).toPath()));
    }

    @NotNull
    private static List<SvLink> fromLines(@NotNull List<String> lines) {
        final List<SvLink> results = Lists.newArrayList();
        for (final String line : lines) {
            if (!line.startsWith(COMMENT)) {
                results.add(fromString(line));
            }
        }

        return results;

    }

    @NotNull
    private static SvLink fromString(@NotNull final String line) {
        String[] values = line.split(DELIMITER);
        return new SvLink(values[0], Long.valueOf(values[1]), values[2], Long.valueOf(values[3]));
    }

}
