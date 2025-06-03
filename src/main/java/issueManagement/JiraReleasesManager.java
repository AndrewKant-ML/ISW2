package issueManagement;

import issueManagement.model.Release;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JiraReleasesManager implements ReleasesManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Getter
    private final String projectName;
    @Getter
    private final String url;
    @Getter
    private final List<Release> releases;

    private final JSONUtils jsonUtils;

    public JiraReleasesManager(String projectName) {
        this.projectName = projectName;
        this.url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;
        this.releases = new ArrayList<>();
        this.jsonUtils = new JSONUtils();
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
    public void getReleasesInfo(Double percentage) {
        JSONObject json;
        try {
            json = jsonUtils.readJsonFromUrl(url);
        } catch (IOException e) {
            System.err.println("Unable to read release info from " + url + ": " + e.getMessage());
            return;
        }

        this.releases.clear();
        // Takes the "versions" field from the JSON
        JSONArray versions = json.getJSONArray("versions");
        int versionsNumber = versions.length();
        int versionsToBeConsidered = (int) Math.ceil(versionsNumber * percentage);
        String name, id;
        for (int i = 0; i < versionsToBeConsidered; i++) {
            name = id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name")) name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id")) id = versions.getJSONObject(i).get("id").toString();
                releases.add(new Release(id, name, LocalDate.parse(versions.getJSONObject(i).getString("releaseDate"))));
            } else {
                System.err.println("Version " + versions.getJSONObject(i).getString("name") + " has no release date");
            }
        }

        // Order releases by date
        releases.sort(Comparator.comparing(Release::getReleaseDate));
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
            Release release;
            for (int i = 0; i < releases.size(); i++) {
                fileWriter.append("\n");
                release = releases.get(i);
                fileWriter.append(String.valueOf(i + 1));
                fileWriter.append(",");
                fileWriter.append(release.getId());
                fileWriter.append(",");
                fileWriter.append(release.getName());
                fileWriter.append(",");
                fileWriter.append(release.getReleaseDate().format(DATE_FORMATTER));
            }
        } catch (Exception e) {
            System.err.println("Error while writing on CSV: " + e.getMessage());
        }
    }
}
