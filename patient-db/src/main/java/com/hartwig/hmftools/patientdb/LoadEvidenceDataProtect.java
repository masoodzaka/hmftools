package com.hartwig.hmftools.patientdb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class LoadEvidenceDataProtect {
    private static final Logger LOGGER = LogManager.getLogger(LoadEvidenceData.class);

    private static final String SAMPLE = "sample";

    private static final String ACTIONABILITY_TSV = "knowledgebase_dir";


    private static final String DB_USER = "db_user";
    private static final String DB_PASS = "db_pass";
    private static final String DB_URL = "db_url";

    public static void main(@NotNull final String[] args) throws ParseException, IOException, SQLException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(args, options);

        final String sampleId = cmd.getOptionValue(SAMPLE);

        final String actionabilityTsv = cmd.getOptionValue(ACTIONABILITY_TSV);

        if (Utils.anyNull(sampleId,
                actionabilityTsv,
                cmd.getOptionValue(DB_USER),
                cmd.getOptionValue(DB_PASS),
                cmd.getOptionValue(DB_URL))) {
            printUsageAndExit(options);
        }

        LOGGER.info("Connecting with database");
        DatabaseAccess dbAccess = databaseAccess(cmd);

        LOGGER.info("Reading actionability for sample {}", sampleId);

        LOGGER.info("Writing evidence items into db");
        List<EvidenceItem> combinedEvidence = Lists.newArrayList();
        dbAccess.writeClinicalEvidence(sampleId, combinedEvidence);
        LOGGER.info("Finished");
    }

    private static void printUsageAndExit(@NotNull final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("patient-db - load evidence data", options);
        System.exit(1);
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();

        options.addOption(SAMPLE, true, "Tumor sample of run");

        options.addOption(ACTIONABILITY_TSV, true, "Path towards the TSV file of the actionability TSV");

        options.addOption(DB_USER, true, "Database user name.");
        options.addOption(DB_PASS, true, "Database password.");
        options.addOption(DB_URL, true, "Database url.");

        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    @NotNull
    private static DatabaseAccess databaseAccess(@NotNull final CommandLine cmd) throws SQLException {
        final String userName = cmd.getOptionValue(DB_USER);
        final String password = cmd.getOptionValue(DB_PASS);
        final String databaseUrl = cmd.getOptionValue(DB_URL);  //e.g. mysql://localhost:port/database";
        final String jdbcUrl = "jdbc:" + databaseUrl;
        return new DatabaseAccess(userName, password, jdbcUrl);
    }
}
