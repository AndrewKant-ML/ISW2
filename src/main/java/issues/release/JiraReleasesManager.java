package issues.release;


import issues.JSONUtils;
import issues.model.Release;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import properties.PropertiesManager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
public class JiraReleasesManager{

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String projectName;

    private final String url;

    private final List<Release> Releases;

    private final JSONUtils jsonUtils;

    public JiraReleasesManager() {
        this.projectName = PropertiesManager.getInstance().getProperty("info.name").toUpperCase(Locale.ROOT);
        String baseUrl = PropertiesManager.getInstance().getProperty("info.jira.baseUrl");

        this.url = String.format(baseUrl + "project/%s/versions", projectName);

        this.Releases = new ArrayList<>();
        this.jsonUtils = new JSONUtils();
    }

    public List<Release> getReleases() {
        return this.Releases;
    }

    /**
     * Retrieves all the releases for the project. Ignores releases with missing dates
     */
    public void getReleasesInfo() {
        getReleasesInfo(1.0);
    }

    /**
     * Retrieves the first percentage-% of the releases for the project. Ignores releases with missing dates
     */
    public void getReleasesInfo(double percentage) {
        JSONArray Releases;
        try {
            Releases = jsonUtils.readJsonArrayFromUrl(url);
        } catch (IOException e) {
            log.error("Unable to retrieve Releases from {} : {}", url, e.getMessage());
            return;
        }

        this.Releases.clear();
        // Takes the "Releases" field from the JSON
        int ReleasesNumber = Releases.length();
        int ReleasesToBeConsidered = (int) Math.ceil(ReleasesNumber * percentage);
        String name, id;
        boolean released = false;
        for (int i = 0; i < ReleasesToBeConsidered; i++) {
            name = id = "";
            if (Releases.getJSONObject(i).has("releaseDate")) {
                if (Releases.getJSONObject(i).has("name")) name = Releases.getJSONObject(i).get("name").toString();
                if (Releases.getJSONObject(i).has("id")) id = Releases.getJSONObject(i).get("id").toString();
                if (Releases.getJSONObject(i).has("released"))
                    released = Releases.getJSONObject(i).getBoolean("released");
                this.Releases.add(new Release(id, name, LocalDate.parse(Releases.getJSONObject(i).getString("releaseDate")), released));
            } else {
                log.error("Release {} has no release date", id);
            }
        }

        // Order releases by date
        this.Releases.sort(Comparator.comparing(Release::getReleaseDate));

        for (Release availableRelease : this.Releases) {
            log.info("Available release: {}", availableRelease.getName());
        }
    }

    /**
     * Writes release info to a CSV file named "ProjectNameReleaseInfo.csv"
     */
    public void outputReleaseInfo() {
        // Name of CSV for output
        String outFileName = projectName + "ReleaseInfo.csv";
        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            // Set CSV header
            fileWriter.append("Index,Release ID,Release Name,Date");
            // Writes the CSV content using the first percentage-% of the releases
            Release Release;
            for (int i = 0; i < Releases.size(); i++) {
                fileWriter.append("\n");
                Release = Releases.get(i);
                fileWriter.append(String.valueOf(i + 1));
                fileWriter.append(",");
                fileWriter.append(Release.getId());
                fileWriter.append(",");
                fileWriter.append(Release.getName());
                fileWriter.append(",");
                fileWriter.append(Release.getReleaseDate().format(DATE_FORMATTER));
            }
        } catch (Exception e) {
            System.err.println("Error while writing on CSV: " + e.getMessage());
        }
    }
}