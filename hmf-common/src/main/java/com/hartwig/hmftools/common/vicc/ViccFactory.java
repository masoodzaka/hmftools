package com.hartwig.hmftools.common.vicc;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public abstract class ViccFactory {
    private static final Logger LOGGER = LogManager.getLogger(ViccFactory.class);

    private ViccFactory() {
    }

    public static void extractAllFile(@NotNull String allJsonPath) throws IOException {
    }

    public static void extractBRCAFile(@NotNull String brcaJsonPath) throws IOException {
        final String csvFileName = "/data/experiments/knowledgebase_vicckb/brca.csv";
        PrintWriter writer = new PrintWriter(new File(csvFileName));
        JsonParser parser = new JsonParser();
        JsonReader reader = new JsonReader(new FileReader(brcaJsonPath));
        reader.setLenient(true);

        int index = 0;

        try {
            while (reader.hasNext()) {
                LOGGER.info(index);
                JsonObject object = parser.parse(reader).getAsJsonObject();
                if (index == 0) {
                    LOGGER.info(object.getAsJsonObject("features").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("tags").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("genes").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("source").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("dev_tags").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("gene_identifiers").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("feature_names").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("brca").keySet()); // Set of keys
                    LOGGER.info(object.getAsJsonObject("association").keySet()); // Set of keys
                    writer.append(object.getAsJsonObject("brca").keySet().toString());
                }
                StringBuilder StringToCSV = new StringBuilder();
                for (int i = 0; i < object.getAsJsonObject("brca").keySet().size(); i++) {
                    List<String> keysOfBRCAObject = new ArrayList<>(object.getAsJsonObject("brca").keySet());
                    StringToCSV.append(object.getAsJsonObject("brca").get(keysOfBRCAObject.get(i))).append(";"); // merge 1 object to string
                }
                index++;
                writer.append(StringToCSV);
                writer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }

    private static void writeToCsvFile(@NotNull String csvFormatString, @NotNull PrintWriter writer) throws IOException {
        writer.append(csvFormatString);

    }

    public static void extractCgiFile(@NotNull String cgiJsonPath) throws IOException {
    }

    public static void extractCivicFile(@NotNull String civicJsonPath) throws IOException {
    }

    public static void extractJaxFile(@NotNull String jaxJsonPath) throws IOException {
    }

    public static void extractJaxTrialsFile(@NotNull String jaxTrialsJsonPath) throws IOException {
    }

    public static void extractMolecularMatchFile(@NotNull String molecularMatchJsonPath) throws IOException {
    }

    public static void extractMolecularMatchTrailsFile(@NotNull String molecularMatchTrialsJsonPath) throws IOException {
    }

    public static void extractOncokbFile(@NotNull String oncokbJsonPath) throws IOException {
    }

    public static void extractPmkbFile(@NotNull String pmkbJsonPath) throws IOException {
    }

    public static void extractSageFile(@NotNull String sageJsonPath) throws IOException {
    }

}