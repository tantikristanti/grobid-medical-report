package org.grobid.core.engines;

import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.segmentation.AbstractSegmenter;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.Entity;
import org.grobid.core.data.MedicalEntity;
import org.grobid.core.data.Sentence;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentPointer;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.MedicalLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeatureFactoryMedical;
import org.grobid.core.features.FeaturesVectorMedicalNER;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.lexicon.MedicalNERLexicon;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * A parser for recognizing French medical terminologies
 * <p>
 * Tanti, 2021
 */

public class FrenchMedicalNERParser extends AbstractParser {
    private static Logger LOGGER = LoggerFactory.getLogger(FrenchMedicalNERParser.class);
    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    protected EngineMedicalParsers parsers;
    public Lexicon lexicon = Lexicon.getInstance();
    protected MedicalNERLexicon medicalNERLexicon = MedicalNERLexicon.getInstance();
    protected File tmpPath = null;

    public FrenchMedicalNERParser(EngineMedicalParsers parsers) {
        //by default, we use CRF models (e.g., with the model built on the training data of the French Quaero Corpus)
        //super(GrobidModels.FR_MEDICAL_NER_QUAERO);

        //by default, we use CRF models (e.g., with the model built on the training data of the APHP Corpus)
        super(GrobidModels.FR_MEDICAL_NER);

        // if we want to use DeLFT models
        //super(GrobidModels.FR_MEDICAL_NER_QUAERO, CntManagerFactory.getNoOpCntManager(), GrobidCRFEngine.DELFT, "BidLSTM_CRF");
        this.parsers = parsers;
        GrobidProperties.getInstance();
        tmpPath = GrobidProperties.getTempPath();
    }

    /**
     * Processing with application of the French medical terminology model
     */
    public Pair<String, Document> processing(File input, List<MedicalEntity> entities, GrobidAnalysisConfig config) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(input, config.getStartPage(), config.getEndPage());
            Document doc = parsers.getSegmentationParser().processing(documentSource, config);

            String tei = processingNer(config, doc, entities);
            return new ImmutablePair<>(tei, doc);
        } finally {
            if (documentSource != null) {
                documentSource.close(true, true, true);
            }
        }
    }

    /**
     * Medical terminology recognition after application of the segmentation model
     */
    public String processingNer(GrobidAnalysisConfig config, Document doc, List<MedicalEntity> entities) {
        try {
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            if (documentBodyParts != null) {
                Pair<String, List<LayoutToken>> featuredHeader = getNerFeatures(doc, documentBodyParts);
                String entityWithFeatures = featuredHeader.getLeft();
                List<LayoutToken> nerTokenization = featuredHeader.getRight();
                String nerLabeled = null;
                if (StringUtils.isNotBlank(entityWithFeatures)) {
                    nerLabeled = label(entityWithFeatures);
                    entities = resultExtraction(nerLabeled, nerTokenization);
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return null;
    }

    /**
     * Extract all occurrences of named entity from a simple piece of text.
     * The positions of the recognized entities are given as character offsets
     * (following Java specification of characters).
     */
    public List<MedicalEntity> extractNE(String text) throws Exception {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        List<LayoutToken> tokens = null;
        try {
            // for the analyser is English to avoid any bad surprises
            tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language(Language.EN, 1.0));
            //tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language(Language.FR, 1.0));
        } catch (Exception e) {
            LOGGER.error("Tokenization failed", e);
        }

        return extractNE(tokens);
    }


    /**
     * Extract all occurrences of named entities from a list of LayoutToken
     * coming from a document with fixed/preserved layout, e.g. PDF.
     * The positions of the recognized entities are given with coordinates in
     * the input document.
     */
    public List<MedicalEntity> extractNE(List<LayoutToken> tokens) throws Exception {
        if (tokens == null)
            return null;

        /*MedicalNERLexiconPositionsIndexes positionsIndexes = new MedicalNERLexiconPositionsIndexes(medicalNERLexicon);
        positionsIndexes.computeIndexes(tokens);*/

        List<OffsetPosition> locationsPositions = lexicon.tokenPositionsLocationNames(tokens);
        List<OffsetPosition> titlesPositions = lexicon.tokenPositionsPersonTitle(tokens);
        List<OffsetPosition> suffixesPositions = lexicon.tokenPositionsPersonSuffix(tokens);
        List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
        List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

        String featuresNERData = FeaturesVectorMedicalNER.addFeaturesNER(tokens, null, locationsPositions, titlesPositions, suffixesPositions,
            emailPositions, urlPositions);
        String result = label(featuresNERData);
        // if we use the model built on the French Quaero Corpus
        //List<MedicalEntity> entities = resultExtraction(GrobidModels.FR_MEDICAL_NER_QUAERO, result, tokens);

        // if we use the model built on the French APHP Corpus
        //List<MedicalEntity> entities = resultExtraction(GrobidModels.FR_MEDICAL_NER, result, tokens);
        List<MedicalEntity> entities = resultExtraction(result, tokens);

        return entities;
    }

    /**
     * Return the header section with features to be processed by the sequence labelling model
     */
    public Pair<String, List<LayoutToken>> getNerFeatures(Document doc, SortedSet<DocumentPiece> documentBodyParts) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        FeatureFactoryMedical featureFactoryMedical = FeatureFactoryMedical.getInstance();
        StringBuilder nerFeatures = new StringBuilder();

        // vector for features
        FeaturesVectorMedicalNER features;
        FeaturesVectorMedicalNER previousFeatures = null;

        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        List<LayoutToken> nerTokenizations = new ArrayList<LayoutToken>();

        for (DocumentPiece docPiece : documentBodyParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
                Block block = blocks.get(blockIndex);

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                String localText = block.getText();
                if (localText == null)
                    continue;
                int n = 0;
                if (blockIndex == dp1.getBlockPtr()) {
                    n = dp1.getTokenDocPos() - block.getStartToken();
                }

                List<OffsetPosition> locationPositions = lexicon.tokenPositionsLocationNames(tokens);
                List<OffsetPosition> titlesPositions = lexicon.tokenPositionsPersonTitle(tokens);
                List<OffsetPosition> suffixesPositions = lexicon.tokenPositionsPersonSuffix(tokens);
                List<OffsetPosition> emailPositions = lexicon.tokenPositionsEmailPattern(tokens);
                List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

                while (n < tokens.size()) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    nerTokenizations.add(token);

                    String text = token.getText();
                    if (text == null) {
                        n++;
                        continue;
                    }

                    text = text.replace(" ", "");
                    if (text.length() == 0) {
                        n++;
                        continue;
                    }

                    if (text.equals("\n") || text.equals("\r")) {
                        n++;
                        continue;
                    }

                    // final sanitation and filtering for the token
                    text = text.replaceAll("[ \n]", "");
                    if (TextUtilities.filterLine(text)) {
                        n++;
                        continue;
                    }

                    features = new FeaturesVectorMedicalNER();
                    features.string = text;

                    Matcher m0 = featureFactory.isPunct.matcher(text);
                    if (m0.find()) {
                        features.punctType = "PUNCT";
                    }
                    if (text.equals("(") || text.equals("[")) {
                        features.punctType = "OPENBRACKET";

                    } else if (text.equals(")") || text.equals("]")) {
                        features.punctType = "ENDBRACKET";

                    } else if (text.equals(".")) {
                        features.punctType = "DOT";

                    } else if (text.equals(",")) {
                        features.punctType = "COMMA";

                    } else if (text.equals("-")) {
                        features.punctType = "HYPHEN";

                    } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
                        features.punctType = "QUOTE";
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_first_names(text)) {
                        features.firstName = true;
                    }

                    if (featureFactory.test_last_names(text)) {
                        features.lastName = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    // check token offsets for title of person
                    if (titlesPositions != null) {
                        for (OffsetPosition thePosition : titlesPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.isPersonTitleToken = true;
                                break;
                            }
                        }
                    }

                    // check token offsets for title of person
                    if (suffixesPositions != null) {
                        for (OffsetPosition thePosition : suffixesPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.isPersonSuffixToken = true;
                                break;
                            }
                        }
                    }

                    // check token offsets for email and http address, or known location
                    if (locationPositions != null) {
                        for (OffsetPosition thePosition : locationPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.locationName = true;
                                break;
                            }
                        }
                    } else {
                        if (featureFactoryMedical.test_geography(text)) {
                            features.locationName = true;
                        }
                    }
                    if (emailPositions != null) {
                        for (OffsetPosition thePosition : emailPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.email = true;
                                break;
                            }
                        }
                    }
                    if (urlPositions != null) {
                        for (OffsetPosition thePosition : urlPositions) {
                            if (n >= thePosition.start && n <= thePosition.end) {
                                features.http = true;
                                break;
                            }
                        }
                    }

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    if (features.punctType == null)
                        features.punctType = "NOPUNCT";

                    if (featureFactory.test_country(text)) {
                        features.countryName = true;
                    }

                    if (featureFactory.test_city(text)) {
                        features.cityName = true;
                    }

                    if (featureFactoryMedical.test_anatomies(text)) {
                        features.anatomy = true;
                    }

                    if (featureFactoryMedical.test_drugs_chemicals(text)) {
                        features.chemical = true;
                    }

                    if (featureFactoryMedical.test_devices(text) || featureFactoryMedical.test_objects(text)) {
                        features.device = true;
                    }

                    if (featureFactoryMedical.test_disorders(text)) {
                        features.disorder = true;
                    }

                    if (featureFactoryMedical.test_living_beings(text)) {
                        features.livingBeing = true;
                    }

                    if (featureFactoryMedical.test_phenomena(text)) {
                        features.phenomena = true;
                    }

                    if (featureFactoryMedical.test_physiology(text)) {
                        features.physiology = true;
                    }

                    if (featureFactoryMedical.test_procedures(text)) {
                        features.procedure = true;
                    }

                    features.wordShape = TextUtilities.wordShape(text);

                    features.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(text);


                    if (previousFeatures != null)
                        nerFeatures.append(previousFeatures.printVector());
                    previousFeatures = features;

                    n++;
                }

                if (previousFeatures != null) {
                    nerFeatures.append(previousFeatures.printVector());
                    previousFeatures = null;
                }
            }
        }

        return Pair.of(nerFeatures.toString(), nerTokenizations);
    }

    /**
     * Extract results from a labelled NER data.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a medical item
     */
    public List<MedicalEntity> resultExtraction(String result, List<LayoutToken> tokenizations) {
        List<MedicalEntity> entities = new ArrayList<>();
        MedicalEntity entity = null;

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FR_MEDICAL_NER, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        entity.generalResultMapping(result, tokenizations);
        String label = null, previousLabel = null;
        boolean begin = true;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }
            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
            entity.setRawName(clusterContent);
            label = clusterLabel.getLabel();
            if (begin && label != previousLabel) {
                label = "I-" + label;
                entity.setRawType(label);
                begin = false;
            }
            previousLabel = clusterLabel.getLabel();
            entity.setBoundingBoxes(BoundingBoxCalculator.calculate(cluster.concatTokens()));
            entity.setOffsets(calculateOffsets(cluster));
            entity.setLayoutTokens(cluster.concatTokens());


            if (clusterLabel.equals(MedicalLabels.NER_ANATOMY)) {
                if (entity.getAnatomy() != null) {
                    entity.setAnatomy(entity.getAnatomy() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setAnatomy(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_DATE)) {
                if (entity.getDate() != null) {
                    entity.setDate(entity.getAnatomy() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setDate(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_DEVICE)) {
                if (entity.getDevice() != null) {
                    entity.setDevice(entity.getDevice() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setDevice(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_DOSE)) {
                if (entity.getDose() != null) {
                    entity.setDose(entity.getDose() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setDose(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_EMAIL)) {
                if (entity.getEmail() != null) {
                    entity.setEmail(entity.getEmail() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setEmail(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_FAX)) {
                if (entity.getFax() != null) {
                    entity.setFax(entity.getEmail() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setFax(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_LIVING)) {
                if (entity.getLiving() != null) {
                    entity.setLiving(entity.getLiving() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setLiving(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_LOCATION)) {
                if (entity.getLocation() != null) {
                    entity.setLocation(entity.getLocation() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setLocation(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_MEASURE)) {
                if (entity.getMeasure() != null) {
                    entity.setMeasure(entity.getMeasure() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setMeasure(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_MEDICAMENT)) {
                if (entity.getMedicament() != null) {
                    entity.setMedicament(entity.getMedicament() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setMedicament(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_OBJECT)) {
                if (entity.getObject() != null) {
                    entity.setObject(entity.getObject() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setObject(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_ORG_NAME)) {
                if (entity.getOrgname() != null) {
                    entity.setOrgname(entity.getOrgname() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setOrgname(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PATHOLOGY)) {
                if (entity.getPathology() != null) {
                    entity.setPathology(entity.getPathology() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setPathology(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PERSON_NAME)) {
                if (entity.getPersname() != null) {
                    entity.setPersname(entity.getPersname() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setPersname(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PERSON_TYPE)) {
                if (entity.getPerstype() != null) {
                    entity.setPerstype(entity.getPerstype() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setPerstype(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PHONE)) {
                if (entity.getPhone() != null) {
                    entity.setPhone(entity.getPhone() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setPhone(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PHYSIOLOGY)) {
                if (entity.getPhysiology() != null) {
                    entity.setPhysiology(entity.getPhysiology() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setPhysiology(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_PROCEDURE)) {
                if (entity.getProcedure() != null) {
                    entity.setProcedure(entity.getProcedure() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setProcedure(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_ROLE_NAME)) {
                if (entity.getRolename() != null) {
                    entity.setRolename(entity.getRolename() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setRolename(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_SUBSTANCE)) {
                if (entity.getSubstance() != null) {
                    entity.setSubstance(entity.getSubstance() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setSubstance(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_SYMPTOM)) {
                if (entity.getSymptom() != null) {
                    entity.setSymptom(entity.getSymptom() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setSymptom(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_UNIT)) {
                if (entity.getUnit() != null) {
                    entity.setUnit(entity.getUnit() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setUnit(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_TIME)) {
                if (entity.getTime() != null) {
                    entity.setTime(entity.getTime() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setTime(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_VALUE)) {
                if (entity.getValue() != null) {
                    entity.setValue(entity.getValue() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setValue(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(MedicalLabels.NER_WEB)) {
                if (entity.getWeb() != null) {
                    entity.setWeb(entity.getWeb() + "\t" + clusterNonDehypenizedContent);
                } else
                    entity.setWeb(clusterNonDehypenizedContent);
            }
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Extract the named entities from a labelled sequence of LayoutToken.
     * This version use the new Clusteror class.
     */
    public List<MedicalEntity> resultExtraction(GrobidModels model, String result, List<LayoutToken> tokenizations) {

        // convert to usual Grobid label scheme to use TaggingTokenClusteror
        result = result.replace("\tB-", "\tI-");

        List<MedicalEntity> entities = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(model, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        MedicalEntity currentEntity = null;
        String label = null, previousLabel = null;
        boolean begin = true;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            if (clusterLabel.getLabel().equals("O"))
                continue;

            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
            currentEntity = new MedicalEntity();
            currentEntity.setRawName(clusterContent);
            //currentEntity.setTypeFromString(GenericTaggerUtils.getPlainIOBLabel(clusterLabel.getLabel()));
            label = clusterLabel.getLabel();
            if (begin && label != previousLabel) {
                label = "I-" + label;
                currentEntity.setRawType(label);
                begin = false;
            }
            previousLabel = clusterLabel.getLabel();

            currentEntity.setBoundingBoxes(BoundingBoxCalculator.calculate(cluster.concatTokens()));
            currentEntity.setOffsets(calculateOffsets(cluster));
            currentEntity.setLayoutTokens(cluster.concatTokens());
            //currentEntity.setStringType(currentEntity.getRawType());
            entities.add(currentEntity);
        }

        return entities;
    }

    private OffsetPosition calculateOffsets(TaggingTokenCluster cluster) {
        final List<LabeledTokensContainer> labeledTokensContainers = cluster.getLabeledTokensContainers();
        if (CollectionUtils.isEmpty(labeledTokensContainers) || CollectionUtils.isEmpty(labeledTokensContainers.get(0).getLayoutTokens())) {
            return new OffsetPosition();
        }

        final LabeledTokensContainer labeledTokensContainer = labeledTokensContainers.get(0);
        final List<LayoutToken> layoutTokens = labeledTokensContainer.getLayoutTokens();

        int start = layoutTokens.get(0).getOffset();
        int end = start + LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens())).length();

        return new OffsetPosition(start, end);
    }


    // create training data for model-0 where the tagger comes from grobid-ner
    public StringBuilder createTrainingUsingGrobidNer(String text, StringBuilder sb, String lang) throws IOException {
        if (isEmpty(text))
            return null;

        // let's segment in paragraphs, assuming we have one per paragraph per line
        String[] paragraphs = text.split("\n");
        for (int p = 0; p < paragraphs.length; p++) {

            String theText = paragraphs[p];
            if (theText.trim().length() == 0)
                continue;

            sb.append("\t\t\t<p " + "xml:id=\"P" + p + "\">\n");

            // we process NER at paragraph level (as it is trained at this level and because
            // inter sentence features/template are used by the CFR)
            //List<Entity> entities = parsers.getNerParser().extractNE(theText); // The English grobid-ner parser
            List<Entity> entities = parsers.getNerFrParser().extractNE(theText); // The French grobid-ner parser
            //int currentEntityIndex = 0;

            // ClearParser components for sentence segmentation
            // slow down a bit at launch, but it is used only for generating more readable training
            // let's segment in sentences with ClearNLP (to be updated to the newest NLP4J !)
            // the ClearNLP library is only available in English
            List<Sentence> sentences = sentenceSegmentation(theText, "en");
            int sentenceIndex = 0;
            for (int s = 0; s < sentences.size(); s++) {
                Sentence sentence = sentences.get(s);
                int sentenceStart = sentence.getOffsetStart();
                int sentenceEnd = sentence.getOffsetEnd();

                sb.append("\t\t\t\t<sentence xml:id=\"P" + p + "E" + sentenceIndex + "\">");

                if ((entities == null) || (entities.size() == 0)) {
                    // don't forget to encode the text for XML
                    sb.append(TextUtilities.HTMLEncode(theText.substring(sentenceStart, sentenceEnd)));
                } else {
                    int index = sentenceStart;
                    // smal adjustement to avoid sentence starting with a space
                    if (theText.charAt(index) == ' ')
                        index++;
                    for (Entity entity : entities) {
                        if (entity.getOffsetEnd() < sentenceStart)
                            continue;
                        if (entity.getOffsetStart() >= sentenceEnd)
                            break;

                        int entityStart = entity.getOffsetStart();
                        int entityEnd = entity.getOffsetEnd();

                        // don't forget to encode the text for XML
                        if (index < entityStart)
                            sb.append(TextUtilities.HTMLEncode(theText.substring(index, entityStart)));
                        sb.append("<ENAMEX type=\"" + entity.getType().getName() + "\">");
                        sb.append(TextUtilities.HTMLEncode(theText.substring(entityStart, entityEnd)));
                        sb.append("</ENAMEX>");

                        index = entityEnd;

                        while (index > sentenceEnd) {
                            // bad luck, the sentence segmentation or ner failed somehow and we have an
                            // entity across 2 sentences, so we merge on the fly these 2 sentences, which is
                            // easier than it looks ;)
                            s++;
                            if (s >= sentences.size())
                                break;
                            sentence = sentences.get(s);
                            sentenceStart = sentence.getOffsetStart();
                            sentenceEnd = sentence.getOffsetEnd();
                        }
                    }

                    if (index < sentenceEnd)
                        sb.append(TextUtilities.HTMLEncode(theText.substring(index, sentenceEnd)));
                    //else if (index > sentenceEnd)
                    //System.out.println(theText.length() + " / / " + theText + "/ / " + index + " / / " + sentenceEnd);
                }

                sb.append("</sentence>\n");
                sentenceIndex++;
            }
            sb.append("\t\t\t</p>\n");
        }
        return sb;
    }

    // create training data by calling the pre-trained model
    public StringBuilder createBlankTraining(String text, StringBuilder sb) throws IOException {
        if (isEmpty(text))
            return null;

        // let's segment in paragraphs, assuming we have one per paragraph per line
        String[] paragraphs = text.split("\n");
        for (int p = 0; p < paragraphs.length; p++) {

            String theText = TextUtilities.HTMLEncode(paragraphs[p]);
            if (theText.trim().length() == 0)
                continue;

            sb.append("\t\t\t<p " + "xml:id=\"p" + p + "\">");
            sb.append(theText);
            sb.append("</p>\n");
        }
        return sb;
    }

    // create training data by calling the pre-trained model
    public StringBuilder createTraining(String text, StringBuilder sb) throws Exception {
        if (isEmpty(text))
            return null;

        // let's segment in paragraphs, assuming we have one per paragraph per line
        String[] paragraphs = text.split("\n");
        for (int p = 0; p < paragraphs.length; p++) {

            String theText = TextUtilities.HTMLEncode(paragraphs[p]);
            if (theText.trim().length() == 0)
                continue;

            sb.append("\t\t\t<p " + "xml:id=\"p" + p + "\">");
            // we process NER at paragraph level (as it is trained at this level and because
            // inter sentence features/template are used by the CRF)
            // calling the French Medical NER model

            List<MedicalEntity> medicalEntities = extractNE(theText.trim());
            int nerTextLength = 0;
            String nerTextStart = "", nerText = "", nerTextEnd = "",
                combinedText = "", prevText = "", nextText = "";
            for (MedicalEntity entity : medicalEntities) {
                if (entity.getRawType() != null) {
                    String nerType = entity.getRawType();
                    entity.setStringType(nerType);
                    if (entity.getStringType() != "OTHER") {
                        nerTextStart = "<ENAMEX type=\"" + entity.getStringType() + "\">";
                        nerText = entity.getRawName();
                        nerTextEnd = "</ENAMEX>";
                        int offsetStart = entity.getOffsetStart() + nerTextLength;
                        int offsetEnd = entity.getOffsetEnd() + nerTextLength;
                        prevText = theText.substring(0, offsetStart);
                        nextText = theText.substring(offsetEnd);
                        nerTextLength = nerTextLength + nerTextStart.length() + nerTextEnd.length();
                        combinedText = prevText + nerTextStart +
                            nerText + nerTextEnd + nextText;
                        theText = combinedText;
                    }
                }
            }
            sb.append(theText);

            sb.append("</p>\n");
        }
        return sb;
    }

    public static List<Sentence> sentenceSegmentation(String text, String language) throws FileNotFoundException {
        // this is only outputed for readability
        String dictionaryFile = "data/clearNLP/dictionary-1.3.1.zip";
        File tmpDir = new File("data/clearNLP/dictionary-1.3.1.zip");
        boolean exists = tmpDir.exists();
        AbstractTokenizer tokenizer = EngineGetter.getTokenizer(language, new FileInputStream(dictionaryFile));
        AbstractSegmenter segmenter = EngineGetter.getSegmenter(language, tokenizer);
        // convert String into InputStream
        InputStream is = new ByteArrayInputStream(text.getBytes());
        // read it with BufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<List<String>> sentences = segmenter.getSentences(br);
        List<Sentence> results = new ArrayList<Sentence>();

        if ((sentences == null) || (sentences.size() == 0)) {
            // there is some text but not in a state so that a sentence at least can be
            // identified by the sentence segmenter, so we parse it as a single sentence
            Sentence sentence = new Sentence();
            OffsetPosition pos = new OffsetPosition();
            pos.start = 0;
            pos.end = text.length();
            sentence.setOffsets(pos);
            results.add(sentence);
            return results;
        }

        // we need to realign with the original sentences, so we have to match it from the text
        // to be parsed based on the tokenization
        int offSetSentence = 0;
        //List<List<String>> trueSentences = new ArrayList<List<String>>();
        for (List<String> theSentence : sentences) {
            int next = offSetSentence;
            for (String token : theSentence) {
                next = text.indexOf(token, next);
                next = next + token.length();
            }

            Sentence sentence = new Sentence();
            OffsetPosition pos = new OffsetPosition();
            pos.start = offSetSentence;
            pos.end = next;
            sentence.setOffsets(pos);
            results.add(sentence);
            offSetSentence = next;
        }
        return results;
    }

    /**
     * Process the specified pdf and format the result as training data for all the models.
     *
     * @param inputFile  input file
     * @param pathOutput path for result
     * @param id         id
     */
    public Document createTrainingFromPDF(File inputFile,
                                          String pathOutput,
                                          int id) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        Document doc = null;
        StringBuilder result = new StringBuilder();
        String lang = Language.FR; // by default, it's French
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();
            Writer writer = null;

            // path for the output
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.french.medical.ner.tei.xml"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // in this case, we only take the body part for further process with the French medical NER model
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            if (documentBodyParts != null) {
                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // also write the raw text (it's needed for further process, for example, with DeLFT)
                StringBuffer rawtxt = new StringBuffer();
                for (LayoutToken txtline : tokenizationsBody) {
                    rawtxt.append(txtline.getText());
                }
                // path for the output
                String outPathRawtext = pathOutput + File.separator +
                    pdfFileName.replace(".pdf", ".training.french.medical.ner.rawtext.txt");
                FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");
                /*writer.write(bufferBody.toString() + "\n");*/

                // create training data by calling the grobid-ner models
                //createTrainingUsingGrobidNer(bufferBody.toString(), result, lang);

                List<OffsetPosition> locationsPositions = null;
                List<OffsetPosition> titlesPositions = null;
                List<OffsetPosition> suffixesPositions = null;
                List<OffsetPosition> emailPositions = null;
                List<OffsetPosition> urlPositions = null;

                // read the paragraph by lines
                String[] lines = bufferBody.toString().split("[\\n\\r]");
                int p = 1;
                for (String line : lines) {
                    if ((line != null) && (line.length() > 0)) {
                        // we segment the text
                        List<LayoutToken> tokensText = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(line);

                        if (tokensText == null) {
                            tokensText = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(line, new Language("en", 1.0)); // by default, we tokenize with English language
                        }

                        locationsPositions = lexicon.tokenPositionsLocationNames(tokensText);
                        titlesPositions = lexicon.tokenPositionsPersonTitle(tokensText);
                        suffixesPositions = lexicon.tokenPositionsPersonSuffix(tokensText);
                        emailPositions = lexicon.tokenPositionsEmailPattern(tokensText);
                        urlPositions = lexicon.tokenPositionsUrlPattern(tokensText);

                        String featuresNERData = FeaturesVectorMedicalNER.addFeaturesNER(tokensText, null, locationsPositions, titlesPositions, suffixesPositions,
                            emailPositions, urlPositions);
                        String labeledNerData = label(featuresNERData);
                        String bufferNer = trainingExtraction(labeledNerData, tokensText).toString();

                        if ((bufferNer != null) && (bufferNer.length() > 0)) {
                            writer.write("\t\t\t<p " + "xml:id=\"p" + p + "\">");
                            writer.write(bufferNer);
                            writer.write("</p>\n");
                            p++;
                        }
                    }
                }
                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Process the specified pdf and format the result as training data for all the models.
     *
     * @param inputFile  input file
     * @param pathOutput path for result
     * @param id         id
     */
    public Document createTrainingAnonymFromPDF(File inputFile,
                                                String pathOutput,
                                                int id) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        Document doc = null;
        StringBuilder result = new StringBuilder();
        String lang = Language.FR; // by default, it's French
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            File fileAnonymData = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".anonymized.data.txt"));

            // put the anonymized data in a list
            List<String> dataAnonym = new ArrayList<>();
            try (Stream<String> lines = Files.lines(Paths.get(fileAnonymData.getPath()))) {
                dataAnonym = lines.collect(Collectors.toList());
            }

            // treat the anonymized data first
            List<String> dataOriginal = new ArrayList<>();
            List<String> dataAnonymized = new ArrayList<>();

            for (int i = 0; i < dataAnonym.size(); i++) {
                List<String> dataAnonymSplit = Arrays.asList(dataAnonym.get(i).split("\t"));
                if (dataAnonymSplit.get(0) != null) {
                    dataOriginal.add(dataAnonymSplit.get(0));
                } else {
                    dataOriginal.add("");
                }
                if (dataAnonymSplit.get(1) != null) {
                    dataAnonymized.add(dataAnonymSplit.get(1));
                } else {
                    dataAnonymized.add("");
                }
            }

            Writer writer = null;

            // path for the output
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".anonym.training.french.medical.ner.tei.xml"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // in this case, we only take the body part for further process with the French medical NER model
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            if (documentBodyParts != null) {
                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // also write the raw text (it's needed for further process, for example, with DeLFT)
                StringBuffer rawtxt = new StringBuffer();
                for (LayoutToken txtline : tokenizationsBody) {
                    String text = txtline.getText();
                    int idx = dataOriginal.indexOf(text);
                    if (idx >= 0) {
                        text = dataAnonymized.get(idx).trim();
                    }
                    rawtxt.append(text);
                }

                // path for the output
                String outPathRawtext = pathOutput + File.separator +
                    pdfFileName.replace(".pdf", ".anonym.training.french.medical.ner.rawtext.txt");
                FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\"" + " xml:lang=\"" + lang + "\">\n");

                List<OffsetPosition> locationsPositions = null;
                List<OffsetPosition> titlesPositions = null;
                List<OffsetPosition> suffixesPositions = null;
                List<OffsetPosition> emailPositions = null;
                List<OffsetPosition> urlPositions = null;

                // read the paragraph by lines
                String[] lines = bufferBody.toString().split("[\\n\\r]");
                int p = 1;
                for (String line : lines) {
                    if ((line != null) && (line.length() > 0)) {
                        // we segment the text
                        List<LayoutToken> tokensText = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(line);

                        if (tokensText == null) {
                            tokensText = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(line, new Language("en", 1.0)); // by default, we tokenize with English language
                        }

                        locationsPositions = lexicon.tokenPositionsLocationNames(tokensText);
                        titlesPositions = lexicon.tokenPositionsPersonTitle(tokensText);
                        suffixesPositions = lexicon.tokenPositionsPersonSuffix(tokensText);
                        emailPositions = lexicon.tokenPositionsEmailPattern(tokensText);
                        urlPositions = lexicon.tokenPositionsUrlPattern(tokensText);

                        String featuresNERData = FeaturesVectorMedicalNER.addFeaturesNER(tokensText, null, locationsPositions, titlesPositions, suffixesPositions,
                            emailPositions, urlPositions);
                        String labeledNerData = label(featuresNERData);
                        String bufferNer = trainingExtractionAnonym(labeledNerData, tokensText, dataOriginal, dataAnonymized).toString();

                        if ((bufferNer != null) && (bufferNer.length() > 0)) {
                            writer.write("\t\t\t<p " + "xml:id=\"p" + p + "\">");
                            writer.write(bufferNer);
                            writer.write("</p>\n");
                            p++;
                        }
                    }
                }
                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }

    /**
     * Process the specified pdf and format the result as blank training data for the header model.
     *
     * @param inputFile  input PDF file
     * @param pathOutput path to raw monograph featured sequence
     * @param pathOutput path to TEI
     * @param id         id
     */

    public Document createBlankTrainingFromPDF(File inputFile,
                                               String pathOutput,
                                               int id) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        Document doc = null;
        StringBuilder result = new StringBuilder();
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for full-medical-text, because the file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();
            Writer writer = null;

            // path for the output
            File outputTEIFile = new File(pathOutput + File.separator + pdfFileName.replace(".pdf", ".training.blank.french.medical.ner.tei.xml"));

            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, true, true, true);
            doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String featuredData = parsers.getMedicalReportSegmenterParser().getAllLinesFeatured(doc);
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // first, call the medical-report-segmenter model to have high level segmentation
            doc = parsers.getMedicalReportSegmenterParser().processing(documentSource, GrobidAnalysisConfig.defaultInstance());

            // // in this case, we only take the body part
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(MedicalLabels.BODY);
            if (documentBodyParts != null) {
                List<LayoutToken> tokenizationsBody = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentBodyParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        tokenizationsBody.add(tokenizations.get(i));
                    }
                }

                StringBuilder bufferBody = new StringBuilder();

                // just write the text without any label
                for (LayoutToken token : tokenizationsBody) {
                    bufferBody.append(token.getText());
                }

                // also write the raw text
                StringBuffer rawtxt = new StringBuffer();
                for (LayoutToken txtline : tokenizationsBody) {
                    rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
                }
                // path for the output
                String outPathRawtext = pathOutput + File.separator +
                    pdfFileName.replace(".pdf", ".training.french.medical.ner.rawtext.txt");
                FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), "UTF-8");

                // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(outputTEIFile, false), StandardCharsets.UTF_8);
                if (id == -1) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\">\n");
                } else {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<corpus>\n\t<subcorpus>\n");
                    writer.write("\t\t<document name=\"" + pdfFileName.replace(" ", "_") + "\">\n");
                }
                String theText = bufferBody.toString();

                createBlankTraining(theText, result);
                writer.write(result + "\n");

                writer.write("\n\t\t</document>\n");
                writer.write("\t</subcorpus>\n</corpus>\n");
                writer.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }

        return doc;
    }


    /**
     * In the context of field extraction, check if a newly extracted content is not redundant
     * with the already extracted content
     */
    private boolean isDifferentContent(String existingContent, String newContent) {
        if (existingContent == null) {
            return true;
        }
        if (newContent == null) {
            return false;
        }
        String newContentSimplified = newContent.toLowerCase();
        newContentSimplified = newContentSimplified.replace(" ", "").trim();
        String existinContentSimplified = existingContent.toLowerCase();
        existinContentSimplified = existinContentSimplified.replace(" ", "").trim();
        if (newContentSimplified.equals(existinContentSimplified))
            return false;
        else
            return true;
    }

    private List<LayoutToken> getLayoutTokens(TaggingTokenCluster cluster) {
        List<LayoutToken> tokens = new ArrayList<>();

        for (LabeledTokensContainer container : cluster.getLabeledTokensContainers()) {
            tokens.addAll(container.getLayoutTokens());
        }

        return tokens;
    }

    /**
     * Extract results from a labelled medical terminology recognition in the training data format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtraction(String result, List<LayoutToken> tokenizations) {
        // this is the main buffer for the whole header
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;

        int p = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    s2 = TextUtilities.HTMLEncode(s); // lexical token
                    int p0 = p;
                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();

                        if (tokOriginal.equals(" ")
                            || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } else if (tokOriginal.equals("\n")) {
                            newLine = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                    if (p == tokenizations.size()) {
                        // either we are at the end of the header, or we might have
                        // a problematic token in tokenization for some reasons
                        if ((p - p0) > 2) {
                            // we loose the synchronicity, so we reinit p for the next token
                            p = p0;
                        }
                    }
                } else if (i == ll - 1) {
                    s1 = s; // current tag
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                }
                i++;
            }

            if (newLine) { // it's not the case for this model
                buffer.append("<lb/>");
            }

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            if (lastTag != null) {
                testClosingTag(buffer, currentTag0, lastTag0);
            }

            boolean output;

            output = writeField(buffer, s1, lastTag0, s2, "<anatomy>", "<ENAMEX type=\"anatomy\">", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<date>", "<ENAMEX type=\"date\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<device>", "<ENAMEX type=\"device\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<dose>", "<ENAMEX type=\"dose\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<ENAMEX type=\"email\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<fax>", "<ENAMEX type=\"fax\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<living>", "<ENAMEX type=\"living\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<location>", "<ENAMEX type=\"location\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<measure>", "<ENAMEX type=\"measure\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<medicament>", "<ENAMEX type=\"medicament\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<object>", "<ENAMEX type=\"object\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<orgname>", "<ENAMEX type=\"orgName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<pathology>", "<ENAMEX type=\"pathology\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<persname>", "<ENAMEX type=\"persName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<perstype>", "<ENAMEX type=\"persType\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<ENAMEX type=\"phone\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<physiology>", "<ENAMEX type=\"physiology\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<procedure>", "<ENAMEX type=\"procedure\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<rolename>", "<ENAMEX type=\"roleName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<substance>", "<ENAMEX type=\"substance\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<symptom>", "<ENAMEX type=\"symptom\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<time>", "<ENAMEX type=\"time\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<unit>", "<ENAMEX type=\"unit\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<value>", "<ENAMEX type=\"value\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ENAMEX type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>", "", addSpace);
            }

            lastTag = s1;

            if (!st.hasMoreTokens()) {
                if (lastTag != null && lastTag.length() > 0) {
                    testClosingTag(buffer, "", currentTag0);
                }
            }
        }

        return buffer;
    }

    /**
     * Extract results from a labelled medical terminology recognition in the training data format without any
     * string modification.
     *
     * @param result        result
     * @param tokenizations list of tokens
     * @return a result
     */
    public StringBuilder trainingExtractionAnonym(String result,
                                                  List<LayoutToken> tokenizations,
                                                  List<String> dataOriginal,
                                                  List<String> dataAnonymized) {
        // this is the main buffer for the whole header
        StringBuilder buffer = new StringBuilder();

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;

        int p = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    // anonymize the token
                    int idx = dataOriginal.indexOf(s);
                    if (idx >= 0) {
                        s = dataAnonymized.get(idx);
                    }

                    s2 = TextUtilities.HTMLEncode(s); // lexical token
                    int p0 = p;
                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p).t();

                        if (tokOriginal.equals(" ")
                            || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                    if (p == tokenizations.size()) {
                        // either we are at the end of the header, or we might have
                        // a problematic token in tokenization for some reasons
                        if ((p - p0) > 2) {
                            // we loose the synchronicity, so we reinit p for the next token
                            p = p0;
                        }
                    }
                } else if (i == ll - 1) {
                    s1 = s; // current tag
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                }
                i++;
            }

            if (newLine) { // it's not the case for this model
                buffer.append("<lb/>");
            }

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            if (lastTag != null) {
                testClosingTag(buffer, currentTag0, lastTag0);
            }

            boolean output;

            output = writeField(buffer, s1, lastTag0, s2, "<anatomy>", "<ENAMEX type=\"anatomy\">", addSpace);
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<date>", "<ENAMEX type=\"date\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<device>", "<ENAMEX type=\"device\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<dose>", "<ENAMEX type=\"dose\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<email>", "<ENAMEX type=\"email\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<fax>", "<ENAMEX type=\"fax\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<living>", "<ENAMEX type=\"living\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<location>", "<ENAMEX type=\"location\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<measure>", "<ENAMEX type=\"measure\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<medicament>", "<ENAMEX type=\"medicament\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<object>", "<ENAMEX type=\"object\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<orgname>", "<ENAMEX type=\"orgName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<pathology>", "<ENAMEX type=\"pathology\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<persname>", "<ENAMEX type=\"persName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<perstype>", "<ENAMEX type=\"persType\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<phone>", "<ENAMEX type=\"phone\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<physiology>", "<ENAMEX type=\"physiology\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<procedure>", "<ENAMEX type=\"procedure\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<rolename>", "<ENAMEX type=\"roleName\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<substance>", "<ENAMEX type=\"substance\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<symptom>", "<ENAMEX type=\"symptom\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<time>", "<ENAMEX type=\"time\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<unit>", "<ENAMEX type=\"unit\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<value>", "<ENAMEX type=\"value\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<web>", "<ENAMEX type=\"web\">", addSpace);
            }
            if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>", "", addSpace);
            }

            lastTag = s1;

            if (!st.hasMoreTokens()) {
                if (lastTag != null && lastTag.length() > 0) {
                    testClosingTag(buffer, "", currentTag0);
                }
            }
        }

        return buffer;
    }

    private void testClosingTag(StringBuilder buffer, String currentTag0, String lastTag0) {
        if (!currentTag0.equals(lastTag0)) {
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("");
            } else if (lastTag0.equals("<anatomy>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<device>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<dose>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<email>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<fax>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<living>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<location>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<measure>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<medicament>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<object>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<orgname>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<pathology>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<persname>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<perstype>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<phone>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<physiology>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<procedure>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<rolename>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<substance>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<symptom>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<time>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<unit>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<value>")) {
                buffer.append("</ENAMEX>");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ENAMEX>");
            }
        }
    }

    // s1: the label; s2: the text
    /*private boolean writeField(StringBuilder buffer, String s1, String lastTag0, String s2, String field, String outField, boolean addSpace) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (s1.equals(lastTag0) || (s1).equals("I-" + lastTag0)) { // if current tag is the same the last one, just concatenate the string
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2); // otherwise, add the current label with the concatenated string
            }
        }
        return result;
    }*/

    private boolean writeField(StringBuilder buffer, String s1, String lastTag0, String s2, String field, String outField, boolean addSpace) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) { // if current tag is the same the last one, just concatenate the string
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2); // otherwise, add the current label with the concatenated string
            }
        }
        return result;
    }



    @Override
    public void close() throws IOException {
        super.close();
    }

}
