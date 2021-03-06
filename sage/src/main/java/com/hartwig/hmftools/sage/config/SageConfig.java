package com.hartwig.hmftools.sage.config;

import static com.hartwig.hmftools.common.cli.Configs.defaultIntValue;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
public interface SageConfig {

    Logger LOGGER = LogManager.getLogger(SageConfig.class);

    String THREADS = "threads";
    String REFERENCE = "reference";
    String REFERENCE_BAM = "reference_bam";
    String TUMOR = "tumor";
    String TUMOR_BAM = "tumor_bam";
    String REF_GENOME = "ref_genome";
    String OUTPUT_VCF = "out";
    String MIN_MAP_QUALITY = "min_map_quality";
    String MIN_BASE_QUALITY = "min_base_quality";
    String PANEL = "panel";
    String PANEL_ONLY = "panel_only";
    String GERMLINE_ONLY = "germline";
    String HOTSPOTS = "hotspots";
    String DISABLE_MNV = "disable_mnv";

    int DEFAULT_THREADS = 2;
    int DEFAULT_MIN_MAP_QUALITY = 0;
    int DEFAULT_MIN_BASE_QUALITY = 13;

    @NotNull
    static Options createOptions() {
        final Options options = new Options();
        options.addOption(DISABLE_MNV, false, "Disables merging phased SNVs into MNVs");
        options.addOption(THREADS, true, "Number of threads [" + DEFAULT_THREADS + "]");
        options.addOption(REFERENCE, true, "Name of reference sample");
        options.addOption(REFERENCE_BAM, true, "Path to reference bam file");
        options.addOption(TUMOR, true, "Name of tumor sample");
        options.addOption(TUMOR_BAM, true, "Path to tumor bam file");
        options.addOption(REF_GENOME, true, "Path to indexed ref genome fasta file");
        options.addOption(OUTPUT_VCF, true, "Path to output vcf");
        options.addOption(MIN_MAP_QUALITY, true, "Min map quality [" + DEFAULT_MIN_MAP_QUALITY + "]");
        options.addOption(MIN_BASE_QUALITY, true, "Min base quality [" + DEFAULT_MIN_BASE_QUALITY + "]");

        options.addOption(PANEL, true, "Panel");
        options.addOption(PANEL_ONLY, false, "Only examine panel for variants");
        options.addOption(GERMLINE_ONLY, false, "Germline only mode");
        options.addOption(HOTSPOTS, true, "Hotspots");
        FilterConfig.createOptions().getOptions().forEach(options::addOption);
        QualityConfig.createOptions().getOptions().forEach(options::addOption);

        return options;
    }

    int threads();

    @NotNull
    String reference();

    @NotNull
    String referenceBam();

    @NotNull
    String refGenome();

    @NotNull
    List<String> tumor();

    @NotNull
    List<String> tumorBam();

    @NotNull
    String outputFile();

    @NotNull
    String panel();

    boolean panelOnly();

    boolean germlineOnly();

    boolean mnvDetection();

    @NotNull
    String hotspots();

    @NotNull
    FilterConfig filter();

    @NotNull
    QualityConfig qualityConfig();

    default int regionSliceSize() {
        return 500_000;
    }

    int minMapQuality();

    int minBaseQuality();

    default int maxReadDepth() {
        return 1000;
    }

    @NotNull
    static SageConfig createConfig(@NotNull final CommandLine cmd) throws ParseException {

        final int threads = defaultIntValue(cmd, THREADS, DEFAULT_THREADS);
        final String reference = cmd.getOptionValue(REFERENCE);
        final String reference_bam = cmd.getOptionValue(REFERENCE_BAM);

        final List<String> tumorList = Lists.newArrayList();
        tumorList.addAll(Arrays.asList(cmd.getOptionValue(TUMOR).split(",")));

        final List<String> tumorBamList = Lists.newArrayList();
        tumorBamList.addAll(Arrays.asList(cmd.getOptionValue(TUMOR_BAM, Strings.EMPTY).split(",")));

        if (tumorList.size() != tumorBamList.size()) {
            throw new ParseException("TODO");
        }

        return ImmutableSageConfig.builder()
                .outputFile(cmd.getOptionValue(OUTPUT_VCF))
                .threads(threads)
                .reference(reference)
                .referenceBam(reference_bam)
                .tumor(tumorList)
                .tumorBam(tumorBamList)
                .mnvDetection(!cmd.hasOption(DISABLE_MNV))
                .refGenome(cmd.getOptionValue(REF_GENOME))
                .minMapQuality(defaultIntValue(cmd, MIN_MAP_QUALITY, DEFAULT_MIN_MAP_QUALITY))
                .minBaseQuality(defaultIntValue(cmd, MIN_BASE_QUALITY, DEFAULT_MIN_BASE_QUALITY))
                .filter(FilterConfig.createConfig(cmd))
                .panel(cmd.getOptionValue(PANEL, Strings.EMPTY))
                .hotspots(cmd.getOptionValue(HOTSPOTS, Strings.EMPTY))
                .qualityConfig(QualityConfig.createConfig(cmd))
                .panelOnly(cmd.hasOption(PANEL_ONLY))
                .germlineOnly(cmd.hasOption(GERMLINE_ONLY))
                .build();
    }
}
