package jira;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TicketManager {

    private final String projectName;
    private final JSONUtils jsonUtils;

    public TicketManager(String projectName) {
        this.projectName = projectName;
        this.jsonUtils = new JSONUtils();
    }

    public void retrieveTicketsIDs() {
        int j, i = 0, total = 1;
        List<String> ticketsWithNoFixVersion = new ArrayList<>();
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 100;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + projectName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR" + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt=" + i + "&maxResults=" + j;
            JSONObject json;
            try {
                json = jsonUtils.readJsonFromUrl(url);
            } catch (IOException e) {
                System.err.println("Unable to retrieve tickets IDs: " + e.getMessage());
                return;
            }
            JSONArray issues = json.getJSONArray("issues");
            if (json.getInt("total") != total) {
                total = json.getInt("total");
                System.out.printf("Total number of issues: %d\n", total);
            }
            for (; i < total && i < j; i++) {
                // Iterate through each bug
                JSONObject ticket = issues.getJSONObject(i % 100);
                if (ticket.getJSONObject("fields").getJSONArray("fixVersions") == null || ticket.getJSONObject("fields").getJSONArray("fixVersions").isEmpty()) {
                    System.out.println("\u001B[31m" + ticket + "\u001B[0m");
                    ticketsWithNoFixVersion.add(ticket.getString("key"));
                } else System.out.println(ticket);
            }
        } while (i < total);

        System.out.printf("%d tickets found with no fix versions\n", ticketsWithNoFixVersion.size());
        System.out.println("List of tickets found with no fix versions:");
        System.out.println(ticketsWithNoFixVersion);
    }
}
