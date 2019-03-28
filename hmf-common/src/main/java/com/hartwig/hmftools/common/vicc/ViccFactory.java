package com.hartwig.hmftools.common.vicc;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public abstract class ViccFactory {
    private static final Logger LOGGER = LogManager.getLogger(ViccFactory.class);

    private ViccFactory() {
    }

    private static StringBuilder readObjectBRCA(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //BRCA object
        StringBuilder stringToCSVBRCA = new StringBuilder();
        for (int i = 0; i < object.getAsJsonObject("brca").keySet().size(); i++) {
            List<String> keysOfBRCAObject = new ArrayList<>(object.getAsJsonObject("brca").keySet());
            stringToCSVBRCA.append(object.getAsJsonObject("brca").get(keysOfBRCAObject.get(i))).append(";"); // brca data
        }
        headerCSV.append(object.getAsJsonObject("brca").keySet()).append(";"); // header brca
        return stringToCSVBRCA;
    }

    private static StringBuilder readObjectCGI(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //CGI object
        StringBuilder stringToCSVCGI = new StringBuilder();
        return stringToCSVCGI;
    }

    private static StringBuilder readObjectCIVIC(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //CIVIC object
        StringBuilder stringToCSVCIVIC = new StringBuilder();
        return stringToCSVCIVIC;
    }

    private static StringBuilder readObjectJax(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //Jax object
        StringBuilder stringToCSVJax = new StringBuilder();
        StringBuilder stringUrl = new StringBuilder();
        StringBuilder stringId = new StringBuilder();
        StringBuilder stringPubmedId = new StringBuilder();
        StringBuilder stringTitle = new StringBuilder();
        int indexValue;
        if (object.getAsJsonObject("jax") != null) {
            List<String> keysOfJax = new ArrayList<>(object.getAsJsonObject("jax").keySet());

            LOGGER.info(keysOfJax);
            for (int j = 0; j < keysOfJax.size(); j++) {
                if (keysOfJax.get(j).equals("molecularProfile")) {
                    JsonObject jaxObject = object.getAsJsonObject("jax").get(keysOfJax.get(j)).getAsJsonObject();
                    List<String> keysOfMolecularProfile = new ArrayList<>(jaxObject.keySet());
                    for (int x = 0; x < jaxObject.keySet().size(); x++) {
                        stringToCSVJax.append(jaxObject.get(keysOfMolecularProfile.get(x))).append(";");
                    }
                    indexValue = keysOfJax.indexOf("molecularProfile");
                    keysOfJax.remove(indexValue);
                    keysOfJax.add(indexValue, String.join(";", keysOfMolecularProfile));
                } else if (keysOfJax.get(j).equals("therapy")) {
                    JsonObject jaxObject = object.getAsJsonObject("jax").get(keysOfJax.get(j)).getAsJsonObject();
                    List<String> keysOfTherapy = new ArrayList<>(jaxObject.keySet());
                    for (int x = 0; x < jaxObject.keySet().size(); x++) {
                        stringToCSVJax.append(jaxObject.get(keysOfTherapy.get(x))).append(";");
                    }
                    indexValue = keysOfJax.indexOf("therapy");
                    keysOfJax.remove(indexValue);
                    keysOfJax.add(indexValue, String.join(";", keysOfTherapy));
                } else if (keysOfJax.get(j).equals("indication")) {
                    JsonObject jaxObject = object.getAsJsonObject("jax").get(keysOfJax.get(j)).getAsJsonObject();
                    List<String> keysOfIndication = new ArrayList<>(jaxObject.keySet());
                    for (int x = 0; x < jaxObject.keySet().size(); x++) {
                        stringToCSVJax.append(jaxObject.get(keysOfIndication.get(x))).append(";");
                    }
                    indexValue = keysOfJax.indexOf("indication");
                    keysOfJax.remove(indexValue);
                    keysOfJax.add(indexValue, String.join(";", keysOfIndication));
                } else if (keysOfJax.get(j).equals("references")) {
                    JsonArray jaxArray = object.getAsJsonObject("jax").get(keysOfJax.get(j)).getAsJsonArray();
                    indexValue = keysOfJax.indexOf("references");

                    for (int x = 0; x < jaxArray.size(); x++) {
                        JsonObject objectRefereces = (JsonObject) jaxArray.get(x);
                        Set<String> set = objectRefereces.keySet();
                        keysOfJax.remove(indexValue);
                        keysOfJax.add(indexValue, String.join(";", set));

                        for (int v = 0; v < objectRefereces.keySet().size(); v++) {
                            List<String> keysRefereces = new ArrayList<>(objectRefereces.keySet());
                            if (keysRefereces.get(v).equals("url")) {
                                JsonElement url = objectRefereces.get(keysRefereces.get(v));
                                stringUrl.append(url).append(",");
                            } else if (keysRefereces.get(v).equals("id")) {
                                JsonElement url = objectRefereces.get(keysRefereces.get(v));
                                stringId.append(url).append(",");
                            } else if (keysRefereces.get(v).equals("pubMedId")) {
                                JsonElement url = objectRefereces.get(keysRefereces.get(v));
                                stringPubmedId.append(url).append(",");
                            } else if (keysRefereces.get(v).equals("title")) {
                                JsonElement url = objectRefereces.get(keysRefereces.get(v));
                                stringTitle.append(url).append(",");
                            }

                        }
                    }
                    headerCSV.append(String.join(";", keysOfJax)).append(";");
                    stringToCSVJax.append(stringUrl)
                            .append(";")
                            .append(stringId)
                            .append(";")
                            .append(stringPubmedId)
                            .append(";")
                            .append(stringTitle)
                            .append(";");
                } else {
                    stringToCSVJax.append(object.getAsJsonObject("jax").get(keysOfJax.get(j))).append(";");
                }
            }

        } else {
            headerCSV.append("responseType;approvalStatus;profileName;id;id;therapyName;evidenceType;source;id;name;efficacyEvidence;url;id;pubMedId;title;id;");
            stringToCSVJax.append(";;;;;;;;;;;;;;;;");
        }
        return stringToCSVJax;
    }

    private static StringBuilder readObjectJaxTrials(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //Jax_trails object
        StringBuilder stringToCSVJaxTrials = new StringBuilder();
        return stringToCSVJaxTrials;
    }

    private static StringBuilder readObjectMolecularMatch(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //MolecularMatch object
        StringBuilder stringToCSVMolecularMatch = new StringBuilder();
        return stringToCSVMolecularMatch;
    }

    private static StringBuilder readObjectMolecularMatchTrials(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //MolecularMatchTrials object
        StringBuilder stringToCSVMolecularMatchTrials = new StringBuilder();
        return stringToCSVMolecularMatchTrials;
    }

    private static StringBuilder readObjectOncokb(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //ONCOKB object
        StringBuilder stringToCSVOncoKb = new StringBuilder();
        return stringToCSVOncoKb;
    }

    private static StringBuilder readObjectPmkb(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //PMKB object
        StringBuilder stringToCSVPmkb = new StringBuilder();
        List<String> keysOfPmkb;
        String header = "";
        int indexValue;
        StringBuilder stringId = new StringBuilder();
        StringBuilder stringName = new StringBuilder();
        if (object.getAsJsonObject("pmkb") != null) {
            keysOfPmkb = new ArrayList<>(object.getAsJsonObject("pmkb").keySet());
            for (int j = 0; j < keysOfPmkb.size(); j++) {
                if (keysOfPmkb.get(j).equals("tumor")) {
                    JsonObject pmkbObject = object.getAsJsonObject("pmkb").get(keysOfPmkb.get(j)).getAsJsonObject();
                    List<String> keysOfVariant = new ArrayList<>(pmkbObject.keySet());
                    for (int x = 0; x < pmkbObject.keySet().size(); x++) {
                        stringToCSVPmkb.append(pmkbObject.get(keysOfVariant.get(x))).append(";");
                    }
                    headerCSV.append(String.join(";", pmkbObject.keySet())).append(";");
                } else if (keysOfPmkb.get(j).equals("tissues")) {
                    JsonArray arrayTissue = object.getAsJsonObject("pmkb").get(keysOfPmkb.get(j)).getAsJsonArray();
                    for (int x = 0; x < arrayTissue.size(); x++) {
                        JsonObject objectTissue = (JsonObject) arrayTissue.get(x);
                        Set<String> set = objectTissue.keySet();
                        header = String.join(";", set);
                        for (int v = 0; v < objectTissue.keySet().size(); v++) {
                            List<String> keysTissue = new ArrayList<>(objectTissue.keySet());
                            if (keysTissue.get(v).equals("id")) {
                                JsonElement idTissue = objectTissue.get(keysTissue.get(v));
                                stringId.append(idTissue).append(",");
                            } else if (keysTissue.get(v).equals("name")) {
                                JsonElement nameTissue = objectTissue.get(keysTissue.get(v));
                                stringName.append(nameTissue).append(",");
                            }
                        }
                    }
                    headerCSV.append(String.join(";", header)).append(";");
                    stringToCSVPmkb.append(stringId).append(";").append(stringName).append(";");
                } else if (keysOfPmkb.get(j).equals("variant")) {
                    JsonObject pmkbObject = object.getAsJsonObject("pmkb").get(keysOfPmkb.get(j)).getAsJsonObject();
                    List<String> keysOfVariant = new ArrayList<>(pmkbObject.keySet());
                    for (int x = 0; x < pmkbObject.keySet().size(); x++) {
                        if (keysOfVariant.get(x).equals("gene")) {
                            JsonElement elementGene = object.getAsJsonObject("pmkb").get("variant");
                            List<String> keysGene = new ArrayList<>(elementGene.getAsJsonObject().get("gene").getAsJsonObject().keySet());

                            indexValue = keysOfVariant.indexOf("gene");
                            keysOfVariant.remove(indexValue);
                            keysOfVariant.add(indexValue, String.join(";", keysGene));

                            for (int d = 0; d < keysGene.size(); d++) {
                                stringToCSVPmkb.append(elementGene.getAsJsonObject() // association data
                                        .get("gene").getAsJsonObject().get(keysGene.get(d))).append(";");
                            }
                        } else {
                            stringToCSVPmkb.append(pmkbObject.get(keysOfVariant.get(x))).append(";");
                        }
                    }
                    headerCSV.append(String.join(";", keysOfVariant)).append(";");
                }
            }
        } else {
            stringToCSVPmkb.append(";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
            headerCSV.append("id")
                    .append(";")
                    .append("name")
                    .append(";")
                    .append("id")
                    .append(";")
                    .append("name")
                    .append(";")
                    .append("amino_acid_change")
                    .append(";")
                    .append("germline")
                    .append(";")
                    .append("partner_gene")
                    .append(";")
                    .append("codons")
                    .append(";")
                    .append("description")
                    .append(";")
                    .append("exons")
                    .append(";")
                    .append("notes")
                    .append(";")
                    .append("cosmic")
                    .append(";")
                    .append("effect")
                    .append(";")
                    .append("cnv_type")
                    .append(";")
                    .append("id")
                    .append(";")
                    .append("cytoband")
                    .append(";")
                    .append("variant_type")
                    .append(";")
                    .append("dna_change")
                    .append(";")
                    .append("coordinates")
                    .append(";")
                    .append("chromosome_based_cnv")
                    .append(";")
                    .append("description")
                    .append(";")
                    .append("created_at")
                    .append(";")
                    .append("updated_at")
                    .append(";")
                    .append("active_ind")
                    .append(";")
                    .append("external_id")
                    .append(";")
                    .append("id")
                    .append(";")
                    .append("name")
                    .append(";")
                    .append("transcript")
                    .append(";")
                    .append("description_type")
                    .append(";")
                    .append("chromosome")
                    .append(";")
                    .append("name")
                    .append(";");
        }

        return stringToCSVPmkb;
    }

    private static StringBuilder readObjectSage(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //SAGE object
        StringBuilder stringToCSVSage = new StringBuilder();
        List<String> keysOfSage;

        if (object.getAsJsonObject("sage") != null) {
            keysOfSage = new ArrayList<>(object.getAsJsonObject("sage").keySet());

            for (int j = 0; j < keysOfSage.size(); j++) {
                stringToCSVSage.append(object.getAsJsonObject("sage").get(keysOfSage.get(j))).append(";");
            }
            Set<String> set = object.getAsJsonObject("sage").keySet();
            headerCSV.append(String.join(";", set));
        } else {
            stringToCSVSage.append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";");
            headerCSV.append("entrez_id")
                    .append(";")
                    .append("clinical_manifestation")
                    .append(";")
                    .append("publication_url")
                    .append(";")
                    .append("germline_or_somatic")
                    .append(";")
                    .append("evidence_label")
                    .append(";")
                    .append("drug_labels")
                    .append(";")
                    .append("response_type")
                    .append(";")
                    .append("gene")
                    .append(";");
        }
        return stringToCSVSage;
    }

    private static StringBuilder readObjectSource(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //Source object
        StringBuilder stringToCSVSource = new StringBuilder();
        stringToCSVSource.append(object.getAsJsonPrimitive("source")).append(";"); // source data
        headerCSV.append("source").append(";"); // header source
        return stringToCSVSource;
    }

    private static StringBuilder readObjectGenes(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //Genes object
        StringBuilder stringToCSVGenes = new StringBuilder();
        JsonArray arrayGenes = object.getAsJsonArray("genes");
        String genes = arrayGenes.toString();
        genes = genes.substring(1, genes.length() - 1);
        stringToCSVGenes.append(genes).append(";"); // genes data
        headerCSV.append("genes").append(";"); // header genes
        return stringToCSVGenes;
    }

    private static StringBuilder readObjectTags(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //Tags object
        StringBuilder stringToCSVTags = new StringBuilder();
        JsonArray arrayTags = object.getAsJsonArray("tags");
        String tags = arrayTags.toString();
        tags = tags.substring(1, tags.length() - 1);
        stringToCSVTags.append(tags).append(";"); // tags data
        headerCSV.append("tags").append(";"); // header tags
        return stringToCSVTags;
    }

    private static StringBuilder readObjectDevTags(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //dev_tags
        StringBuilder stringToCSVDevTags = new StringBuilder();
        JsonArray arrayDevTags = object.getAsJsonArray("dev_tags");
        String devTags = arrayDevTags.toString();
        devTags = devTags.substring(1, devTags.length() - 1);
        stringToCSVDevTags.append(devTags).append(";"); // dev tags data
        headerCSV.append("dev_tags").append(";"); // header tags data
        return stringToCSVDevTags;
    }

    private static StringBuilder readObjectGeneIdentifiers(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //gene_identifiers object
        StringBuilder stringToCSVGeneIdentifiers = new StringBuilder();
        StringBuilder stringSymbol = new StringBuilder();
        StringBuilder stringEntrezId = new StringBuilder();
        StringBuilder stringEnsembleGeneId = new StringBuilder();
        String header = "";
        JsonArray arrayGeneIdentifiers = object.getAsJsonArray("gene_identifiers");
        if (arrayGeneIdentifiers.size() == 0) {
            stringToCSVGeneIdentifiers.append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";");
            headerCSV.append("symbol").append(";").append("entrez_id").append(";").append("ensembl_gene_id").append(";");
        } else {
            for (int j = 0; j < arrayGeneIdentifiers.size(); j++) {
                JsonObject objectGeneIdentiefiers = (JsonObject) arrayGeneIdentifiers.get(j);
                for (int i = 0; i < objectGeneIdentiefiers.keySet().size(); i++) {
                    List<String> keysOfGeneIdentifiersObject = new ArrayList<>(objectGeneIdentiefiers.keySet());
                    if (keysOfGeneIdentifiersObject.get(i).equals("symbol")) {
                        JsonElement symbol = objectGeneIdentiefiers.get(keysOfGeneIdentifiersObject.get(i));
                        stringSymbol.append(symbol).append(",");
                    } else if (keysOfGeneIdentifiersObject.get(i).equals("entrez_id")) {
                        JsonElement entrezId = objectGeneIdentiefiers.get(keysOfGeneIdentifiersObject.get(i));
                        stringEntrezId.append(entrezId).append(",");
                    } else if (keysOfGeneIdentifiersObject.get(i).equals("ensembl_gene_id")) {
                        JsonElement ensemblGeneID = objectGeneIdentiefiers.get(keysOfGeneIdentifiersObject.get(i));
                        stringEnsembleGeneId.append(ensemblGeneID).append(",");
                    }
                }
                Set<String> set = objectGeneIdentiefiers.keySet();
                header = String.join(";", set);

            }
            headerCSV.append(header).append(";"); // header gene identifiers
            stringToCSVGeneIdentifiers.append(stringSymbol)
                    .append(";")
                    .append(stringEntrezId)
                    .append(";")
                    .append(stringEnsembleGeneId)
                    .append(";");
        }
        return stringToCSVGeneIdentifiers;
    }

    private static StringBuilder readObjectAssociation(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //association object
        int indexValue;
        List<String> headerEvidence = Lists.newArrayList();
        List<String> headerPhenotype = Lists.newArrayList();
        List<String> keysOfAssocationObject = Lists.newArrayList();

        StringBuilder stringToCSVAssociation = new StringBuilder();
        for (int i = 0; i < object.getAsJsonObject("association").keySet().size(); i++) {
            keysOfAssocationObject = new ArrayList<>(object.getAsJsonObject("association").keySet());

            if (keysOfAssocationObject.get(i).equals("description")) {
                stringToCSVAssociation.append(object.getAsJsonObject("association").get(keysOfAssocationObject.get(i)))
                        .append(";"); // association data
            } else if (keysOfAssocationObject.get(i).equals("evidence")) {
                JsonElement elementEvidence = object.getAsJsonObject("association").get("evidence");
                JsonArray arrayEvidence = elementEvidence.getAsJsonArray();
                JsonObject objectEvidence = (JsonObject) arrayEvidence.iterator().next();

                for (int a = 0; a < objectEvidence.keySet().size(); a++) {
                    List<String> keysOfEvidenceObject = new ArrayList<>(objectEvidence.keySet());
                    if (keysOfEvidenceObject.get(a).equals("evidenceType")) {
                        indexValue = keysOfEvidenceObject.indexOf("evidenceType");
                        keysOfEvidenceObject.remove(indexValue);
                        keysOfEvidenceObject.add(indexValue,
                                String.join(",", objectEvidence.get("evidenceType").getAsJsonObject().keySet()));
                        headerEvidence = keysOfEvidenceObject;
                        for (int b = 0; b < objectEvidence.get("evidenceType").getAsJsonObject().keySet().size(); b++) {
                            List<String> keysOfEvidenceTypeObject =
                                    new ArrayList<>(objectEvidence.get("evidenceType").getAsJsonObject().keySet());
                            stringToCSVAssociation.append(objectEvidence.get("evidenceType")
                                    .getAsJsonObject()
                                    .get(keysOfEvidenceTypeObject.get(b))).append(";"); // association data
                        }
                    } else {
                        stringToCSVAssociation.append(objectEvidence.get(keysOfEvidenceObject.get(a))).append(";"); // association data
                    }
                }
            } else if (keysOfAssocationObject.get(i).equals("environmentalContexts")) {
                stringToCSVAssociation.append(object.getAsJsonObject("association").get("environmentalContexts"))
                        .append(";"); // association data
            } else if (keysOfAssocationObject.get(i).equals("evidence_label")) {
                stringToCSVAssociation.append(object.getAsJsonObject("association").get(keysOfAssocationObject.get(i)))
                        .append(";"); // association data
            } else if (keysOfAssocationObject.get(i).equals("phenotype")) {
                JsonElement elementPhenotype = object.getAsJsonObject("association").get("phenotype");
                for (int a = 0; a < elementPhenotype.getAsJsonObject().keySet().size(); a++) {
                    List<String> keysOfPhenotypeObject = new ArrayList<>(elementPhenotype.getAsJsonObject().keySet());
                    if (keysOfPhenotypeObject.get(a).equals("type")) {

                        List<String> keysOfPhenotypeTypeObject =
                                new ArrayList<>(elementPhenotype.getAsJsonObject().get("type").getAsJsonObject().keySet());

                        indexValue = keysOfPhenotypeObject.indexOf("type");
                        keysOfPhenotypeObject.remove(indexValue);
                        keysOfPhenotypeObject.add(indexValue, String.join(",", keysOfPhenotypeTypeObject));
                        headerPhenotype = keysOfPhenotypeObject;

                        for (int c = 0; c < keysOfPhenotypeObject.size(); c++) {
                            stringToCSVAssociation.append(elementPhenotype.getAsJsonObject() // association data
                                    .get("type").getAsJsonObject().get(keysOfPhenotypeTypeObject.get(c))).append(";");
                        }
                    } else {
                        stringToCSVAssociation.append(elementPhenotype.getAsJsonObject().get(keysOfPhenotypeObject.get(a)))
                                .append(";"); // association data
                    }
                }
            } else if (keysOfAssocationObject.get(i).equals("oncogenic")) {
                stringToCSVAssociation.append(object.getAsJsonObject("association").get(keysOfAssocationObject.get(i)))
                        .append(";"); // association data
            }
            keysOfAssocationObject.set(keysOfAssocationObject.indexOf("evidence"), String.join(",", headerEvidence));
            keysOfAssocationObject.set(keysOfAssocationObject.indexOf("phenotype"), String.join(",", headerPhenotype));
        }
        headerCSV.append(keysOfAssocationObject).append(";"); // header features
        return stringToCSVAssociation;
    }

    private static StringBuilder readObjectFeaturesNames(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //feature_names object
        StringBuilder stringToCSVFeaturesNames = new StringBuilder();
        stringToCSVFeaturesNames.append(object.getAsJsonPrimitive("feature_names")).append(";"); // features names data
        headerCSV.append("feature_names").append(";"); // header features names
        return stringToCSVFeaturesNames;
    }

    private static StringBuilder readObjectFeatures(@NotNull JsonObject object, @NotNull StringBuilder headerCSV) {
        //features
        StringBuilder stringToCSVFeatures = new StringBuilder();
        List<String> b = Lists.newArrayList();
        List<String> keysOfSequenceOntologyObjectMerged;
        JsonArray arrayFeatures = object.getAsJsonArray("features");
        if (arrayFeatures.size() == 0) {
            stringToCSVFeatures.append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";")
                    .append(Strings.EMPTY)
                    .append(";");

            headerCSV.append("provenance_rule")
                    .append(";")
                    .append("end")
                    .append(";")
                    .append("description")
                    .append(";")
                    .append("links")
                    .append(";")
                    .append("hierarchy")
                    .append(";")
                    .append("soid")
                    .append(";")
                    .append("parent_soid")
                    .append(";")
                    .append("name")
                    .append(";")
                    .append("parent_name")
                    .append(";")
                    .append("provenance")
                    .append(";")
                    .append("start")
                    .append(";")
                    .append("synonyms")
                    .append(";")
                    .append("biomarker_type")
                    .append(";")
                    .append("referenceName")
                    .append(";")
                    .append("alt")
                    .append(";")
                    .append("ref")
                    .append(";")
                    .append("chromosome")
                    .append(";")
                    .append("name")
                    .append(";");
        } else {
            JsonObject objectFeatures = (JsonObject) arrayFeatures.iterator().next();
            int indexValue;
            List<String> keysOfFeaturesObject = new ArrayList<>(objectFeatures.keySet());

            for (int i = 0; i < objectFeatures.keySet().size(); i++) {

                if (keysOfFeaturesObject.get(i).equals("sequence_ontology")) {
                    List<String> keysOfSequenceOntologyObject =
                            new ArrayList<>(objectFeatures.getAsJsonObject("sequence_ontology").keySet());
                    indexValue = keysOfFeaturesObject.indexOf("sequence_ontology");
                    keysOfFeaturesObject.set(indexValue, String.join(";", keysOfSequenceOntologyObject));

                    for (int e = 0; e < objectFeatures.getAsJsonObject("sequence_ontology").keySet().size(); e++) {
                        b.add(objectFeatures.get("sequence_ontology")
                                .getAsJsonObject()
                                .get(keysOfSequenceOntologyObject.get(e))
                                .toString());

                    }
                } else {
                    b.add(objectFeatures.get(keysOfFeaturesObject.get(i)).toString());
                }

            }

            keysOfSequenceOntologyObjectMerged = keysOfFeaturesObject;
            if (!keysOfFeaturesObject.get(1).equals("entrez_id")) {
                keysOfSequenceOntologyObjectMerged.add(1, "entrez_id");
                b.add(1, Strings.EMPTY);
            }
            if (!keysOfFeaturesObject.get(3).equals("name")) {
                keysOfSequenceOntologyObjectMerged.add(3, "name");
                b.add(3, Strings.EMPTY);
            }
            stringToCSVFeatures.append(b.toString().replace(",", ";"));

            String header = String.join(";", keysOfSequenceOntologyObjectMerged);
            headerCSV.append(header).append(";"); // header features
        }

        return stringToCSVFeatures;
    }

    public static void extractAllFile(@NotNull String allJsonPath) throws IOException {
        final String csvFileName = "/Users/liekeschoenmaker/hmf/tmp/all.csv";
        PrintWriter writer = new PrintWriter(new File(csvFileName));
        JsonParser parser = new JsonParser();
        JsonReader reader = new JsonReader(new FileReader(allJsonPath));
        reader.setLenient(true);
        int index = 1;
        while (reader.peek() != JsonToken.END_DOCUMENT && index < 1000) {
            LOGGER.info(index);
            JsonObject object = parser.parse(reader).getAsJsonObject();

            StringBuilder stringToCSVAll = new StringBuilder();
            StringBuilder headerCSV = new StringBuilder();
            headerCSV.append("index").append(";");
            StringBuilder StringToCSVSource = readObjectSource(object, headerCSV);

            //            StringBuilder StringToCSVGenes = readObjectGenes(object, headerCSV);
            //            StringBuilder StringToCSVTags = readObjectTags(object, headerCSV);
            //            StringBuilder StringToCSVDevTags = readObjectDevTags(object, headerCSV);
            //            StringBuilder StringToCSVGeneIdentifiers = readObjectGeneIdentifiers(object, headerCSV);
            //            StringBuilder StringToCSVFeatures = readObjectFeatures(object, headerCSV);
            //  StringBuilder StringToCSVSage = readObjectSage(object, headerCSV);
            // StringBuilder StringToCSVPmkb = readObjectPmkb(object, headerCSV);
            StringBuilder StringToCSVJax = readObjectJax(object, headerCSV);

            stringToCSVAll.append(index).append(";");
            stringToCSVAll.append(StringToCSVSource);
            //            stringToCSVAll.append(StringToCSVGenes);
            //            stringToCSVAll.append(StringToCSVTags);
            //            stringToCSVAll.append(StringToCSVDevTags);
            //            stringToCSVAll.append(StringToCSVGeneIdentifiers);
            stringToCSVAll.append(StringToCSVJax);

            writer.append(headerCSV);
            writer.append("\n");
            writer.append(stringToCSVAll);
            writer.append("\n");

            index++;

        }
        reader.close();
        writer.close();
    }

    public static void extractBRCAFile(@NotNull String brcaJsonPath) throws IOException {
        //        final String csvFileName = "/Users/liekeschoenmaker/hmf/tmp/brca.csv";
        //        PrintWriter writer = new PrintWriter(new File(csvFileName));
        //        JsonParser parser = new JsonParser();
        //        JsonReader reader = new JsonReader(new FileReader(brcaJsonPath));
        //        reader.setLenient(true);
        //
        //        while (reader.peek() != JsonToken.END_DOCUMENT) {
        //            JsonObject object = parser.parse(reader).getAsJsonObject();
        //
        //            StringBuilder stringToCSVAll = new StringBuilder();
        //            StringBuilder headerCSV = new StringBuilder();
        //
        //            StringBuilder StringToCSVBRCA = readObjectBRCA(object, headerCSV);
        //            StringBuilder StringToCSVGenes = readObjectGenes(object, headerCSV);
        //            StringBuilder StringToCSVTags = readObjectTags(object, headerCSV);
        //            StringBuilder StringToCSVDevTags = readObjectDevTags(object, headerCSV);
        //            StringBuilder StringToCSVSource = readObjectSource(object, headerCSV);
        //            StringBuilder StringToCSVGeneIdentifiers = readObjectGeneIdentifiers(object, headerCSV);
        //            StringBuilder StringToCSVAssociation = readObjectAssociation(object, headerCSV);
        //            StringBuilder StringToCSVFeaturesNames = readObjectFeaturesNames(object, headerCSV);
        //            StringBuilder StringToCSVFeatures = readObjectFeatures(object, headerCSV);
        //
        //            stringToCSVAll.append(StringToCSVBRCA)
        //                    .append(StringToCSVGenes)
        //                    .append(StringToCSVTags)
        //                    .append(StringToCSVDevTags)
        //                    .append(StringToCSVSource)
        //                    .append(StringToCSVGeneIdentifiers)
        //                    .append(StringToCSVAssociation)
        //                    .append(StringToCSVFeaturesNames)
        //                    .append(StringToCSVFeatures);
        //
        //            writer.append(headerCSV);
        //            writer.append("\n");
        //            writer.append(stringToCSVAll);
        //            writer.append("\n");
        //        }
        //        reader.close();
        //        writer.close();
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
