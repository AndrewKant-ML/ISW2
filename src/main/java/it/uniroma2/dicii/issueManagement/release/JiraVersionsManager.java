package it.uniroma2.dicii.issueManagement.release;

import it.uniroma2.dicii.issueManagement.JSONUtils;
import it.uniroma2.dicii.issueManagement.model.Version;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import it.uniroma2.dicii.properties.PropertiesManager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
public class JiraVersionsManager implements VersionsManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String projectName;

    private final String url;

    private final List<Version> versions;

    private final JSONUtils jsonUtils;

    public JiraVersionsManager() {
        this.projectName = PropertiesManager.getInstance().getProperty("project.name").toUpperCase(Locale.ROOT);
        String baseUrl = PropertiesManager.getInstance().getProperty("project.jira.baseUrl");

        this.url = String.format(baseUrl + "project/%s/versions", projectName);

        this.versions = new ArrayList<>();
        this.jsonUtils = new JSONUtils();
    }

    @Override
    public List<Version> getVersions() {
        return this.versions;
    }

    /**
     * Retrieves all the releases for the project. Ignores releases with missing dates
     */
    @Override
    public void getVersionsInfo() {
        getVersionsInfo(1.0);
    }

    /**
     * Retrieves the first percentage-% of the releases for the project. Ignores releases with missing dates
     */
    @Override
    public void getVersionsInfo(double percentage) {
        JSONArray versions;
        try {
            versions = jsonUtils.readJsonArrayFromUrl(url);
        } catch (IOException e) {
            log.error("Unable to retrieve versions from {} : {}", url, e.getMessage());
            return;
        }

        this.versions.clear();
        // Takes the "versions" field from the JSON
        int versionsNumber = versions.length();
        int versionsToBeConsidered = (int) Math.ceil(versionsNumber * percentage);
        String name, id;
        boolean released = false;
        for (int i = 0; i < versionsToBeConsidered; i++) {
            name = id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name")) name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id")) id = versions.getJSONObject(i).get("id").toString();
                if (versions.getJSONObject(i).has("released"))
                    released = versions.getJSONObject(i).getBoolean("released");
                this.versions.add(new Version(id, name, LocalDate.parse(versions.getJSONObject(i).getString("releaseDate")), released));
            } else {
                log.error("Version {} has no release date", id);
            }
        }

        // Order releases by date
        this.versions.sort(Comparator.comparing(Version::getReleaseDate));

        for (Version availableVersion : this.versions) {
            log.info("Available release: {}", availableVersion.getName());
        }
    }

    /**
     * Writes release info to a CSV file named "ProjectNameVersionInfo.csv"
     */
    public void outputReleaseInfo() {
        // Name of CSV for output
        String outFileName = projectName + "VersionInfo.csv";
        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            // Set CSV header
            fileWriter.append("Index,Version ID,Version Name,Date");
            // Writes the CSV content using the first percentage-% of the releases
            Version version;
            for (int i = 0; i < versions.size(); i++) {
                fileWriter.append("\n");
                version = versions.get(i);
                fileWriter.append(String.valueOf(i + 1));
                fileWriter.append(",");
                fileWriter.append(version.getId());
                fileWriter.append(",");
                fileWriter.append(version.getName());
                fileWriter.append(",");
                fileWriter.append(version.getReleaseDate().format(DATE_FORMATTER));
            }
        } catch (Exception e) {
            System.err.println("Error while writing on CSV: " + e.getMessage());
        }
    }
}
