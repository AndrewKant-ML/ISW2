package jira;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class ReleasesManager {

    private final String projectName;
    private final String url;
    private final HashMap<LocalDateTime, String> releaseNames;
    private final HashMap<LocalDateTime, String> releaseID;
    private final ArrayList<LocalDateTime> releases;

    private final JSONUtils jsonUtils;

    public ReleasesManager(String projectName) {
        this.projectName = projectName;
        this.url = "https://issues.apache.org/jira/rest/api/2/project/" + projectName;
        this.releases = new ArrayList<>();
        this.releaseNames = new HashMap<>();
        this.releaseID = new HashMap<>();

        jsonUtils = new JSONUtils();
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
        int i;
        JSONObject json;
        try {
            json = jsonUtils.readJsonFromUrl(url);
        } catch (IOException e) {
            System.err.println("Unable to read release info from " + url + ": " + e.getMessage());
            return;
        }

        // Takes the "versions" field from the JSON
        JSONArray versions = json.getJSONArray("versions");
        String name, id;
        for (i = 0; i < versions.length(); i++) {
            name = id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name")) name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id")) id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(), name, id);
            } else {
                System.err.println("Version " + versions.getJSONObject(i).getString("name") + " has no release date");
            }
        }

        // Order releases by date
        releases.sort(LocalDateTime::compareTo);
        // Name of CSV for output
        String outFileName = projectName + "VersionInfo.csv";
        try (FileWriter fileWriter = new FileWriter(outFileName)) {
            // Set CSV header
            fileWriter.append("Index,Version ID,Version Name,Date\n");
            // Writes the CSV content using the first percentage-% of the releases
            int numVersions = releases.size();
            for (i = 0; i < Math.ceil(numVersions * percentage); i++) {
                fileWriter.append(String.valueOf(i + 1));
                fileWriter.append(",");
                fileWriter.append(releaseID.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releaseNames.get(releases.get(i)));
                fileWriter.append(",");
                fileWriter.append(releases.get(i).toString());
                fileWriter.append("\n");
            }
        } catch (Exception e) {
            System.err.println("Error while writing on CSV: " + e.getMessage());
        }
    }

    private void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime)) releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
    }
}
