package org.grobid.core;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.utilities.GrobidProperties;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.grobid.core.engines.EngineParsers.LOGGER;

/**
 * This enum class extend the Grobid-Model class.
 *
 * Tanti, 2020
 */

public enum GrobidMedicalReportModels implements GrobidModel {
    MEDICAL_REPORT_SEGMENTER("medical-report-segmenter"),
    HEADER_MEDICAL_REPORT("header-medical-report"),
    LEFT_NOTE_MEDICAL_REPORT("left-note-medical-report"),
    NAME_MEDIC("name/medic"),
    NAME_PATIENT("name/patient"),
    FULLTEXT("fulltext"),;

    /**
     * Absolute path to the model.
     */
    private String modelPath;

    private String folderName;

    private static final ConcurrentMap<String, GrobidModel> models = new ConcurrentHashMap<>();

    GrobidMedicalReportModels(String folderName) {

        this.folderName = folderName;
        File path = GrobidProperties.getModelPath(this);
        if (!path.exists()) {
            // to be reviewed
            /*System.err.println("Warning: The file path to the "
                    + this.name() + " CRF model is invalid: "
					+ path.getAbsolutePath());*/
        }
        modelPath = path.getAbsolutePath();
    }

    public String getFolderName() {
        return folderName;
    }

    public String getModelPath() {
        return modelPath;
    }

    public String getModelName() {
        return folderName.replaceAll("/", "-");
    }

    public String getTemplateName() {
        return StringUtils.substringBefore(folderName, "/") + ".template";
    }

    @Override
    public String toString() {
        return folderName;
    }

    public static GrobidModel modelFor(final String name) {
        if (models.isEmpty()) {
            for (GrobidModel model : values())
                models.putIfAbsent(model.getFolderName(), model);
        }

        models.putIfAbsent(name.toString(/* null-check */), new GrobidModel() {
            @Override
            public String getFolderName() {
                return name;
            }

            @Override
            public String getModelPath() {
                File path = GrobidProperties.getModelPath(this);
                if (!path.exists()) {
                    LOGGER.warn("The file path to the "
                            + name + " model is invalid: "
                            + path.getAbsolutePath());
                }
                return path.getAbsolutePath();
            }

            @Override
            public String getModelName() {
                return getFolderName().replaceAll("/", "-");
            }

            @Override
            public String getTemplateName() {
                return StringUtils.substringBefore(getFolderName(), "/") + ".template";
            }
        });
        return models.get(name);
    }

    public String getName() {
        return name();
    }
}
