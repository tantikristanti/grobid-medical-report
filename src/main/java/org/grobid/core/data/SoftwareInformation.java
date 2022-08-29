package org.grobid.core.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SoftwareInformation {
    private String name;
    private String version;
    private String description;

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareInformation.class);

    private static SoftwareInformation INSTANCE = null;

    private SoftwareInformation(String name, String version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public static SoftwareInformation getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        //Default in case of issues of any nature
        INSTANCE = new SoftwareInformation("grobid-medical-report", "N/A", "Entity Recognition and Disambiguation");
        
        Properties properties = new Properties();
        try {
            properties.load(SoftwareInformation.class.getResourceAsStream("/grobid-medical-report-service.properties"));
            INSTANCE = new SoftwareInformation(properties.getProperty("name"), properties.getProperty("version"), properties.getProperty("description"));
        } catch (Exception e) {
            LOGGER.error("General error when extracting the version of this application", e);
        }

        return INSTANCE; 
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"name\": \"" + this.getName() + "\"")
                .append(", \"version\": \"" + this.getVersion() + "\"")
                .append(", \"description\": \"" + this.getDescription() + "\"")
                .append("}");
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
