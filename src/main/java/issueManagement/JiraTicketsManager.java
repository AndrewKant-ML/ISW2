package issueManagement;

import issueManagement.model.TicketFilter;
import issueManagement.model.TicketStatus;
import issueManagement.model.TicketType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JiraTicketsManager implements TicketsManager {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";

    private final String projectName;
    private final JSONUtils jsonUtils;

    public JiraTicketsManager(String projectName) {
        this.projectName = projectName;
        this.jsonUtils = new JSONUtils();
    }

    /**
     * Retrieves all tickets corresponding to the filter
     *
     * @param ticketFilter the tickets filter
     */
    public void retrieveTicketsIDs(TicketFilter ticketFilter) {
        int i = 0, total = 1;
        String baseUrl = buildUrlFromFilter(ticketFilter);
        String url;
        List<String> ticketsWithNoFixVersion = new ArrayList<>();
        // Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 100 at a time, so must do this multiple times if bugs >1000
            url = String.format(baseUrl, i, 100);
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
            for (; i < total && i < issues.length(); i++) {
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

    /**
     * Builds a URL to query the Jira REST API according to some filters
     *
     * @param ticketFilter the filter with fields
     * @return the URL with filters set
     */
    private String buildUrlFromFilter(TicketFilter ticketFilter) {
        StringBuilder url = new StringBuilder(BASE_URL + "?jql=project=\"" + projectName + "\"");

        if (ticketFilter.getStatuses() != null && !ticketFilter.getStatuses().isEmpty()) {
            url.append("AND(");
            boolean first = true;
            for (TicketStatus status : ticketFilter.getStatuses()) {
                if (!first) url.append("OR");
                url.append("\"status\"=\"").append(status.getStatus()).append("\"");
                first = false;
            }
            url.append(")");
        }

        if (ticketFilter.getTypes() != null && !ticketFilter.getTypes().isEmpty()) {
            url.append("AND(");
            boolean first = true;
            for (TicketType type : ticketFilter.getTypes()) {
                if (!first) url.append("OR");
                url.append("\"issueType\"=\"").append(type).append("\"");
                first = false;
            }
            url.append(")");
        }

        if (ticketFilter.getFields() != null && !ticketFilter.getFields().isEmpty()) {
            url.append("&\"fields=");
            StringBuilder fields = new StringBuilder();
            for (String field : ticketFilter.getFields()) {
                fields.append(field).append(",");
            }
            fields.deleteCharAt(fields.length() - 1);
            url.append(fields);
        }

        url.append("&startAt=%d&maxResults=%d");

        return url.toString();
    }
}