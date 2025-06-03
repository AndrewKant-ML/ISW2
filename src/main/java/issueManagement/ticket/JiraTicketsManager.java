package issueManagement.ticket;

import issueManagement.JSONUtils;
import issueManagement.model.*;
import issueManagement.release.JiraReleasesManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JiraTicketsManager implements TicketsManager {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";

    private final String projectName;
    private final JSONUtils jsonUtils;
    private final JiraReleasesManager releasesManager;

    private final List<Ticket> tickets;

    public JiraTicketsManager(String projectName) {
        this.projectName = projectName;
        this.jsonUtils = new JSONUtils();
        this.releasesManager = new JiraReleasesManager(projectName);
        this.tickets = new ArrayList<>();
    }

    /**
     * Retrieves all tickets corresponding to the filter
     *
     * @param ticketFilter the tickets filter
     */
    public void retrieveTicketsIDs(TicketFilter ticketFilter) {
        // Retrieves the releases for the project
        releasesManager.getReleasesInfo(0.33);
        List<Release> releases = releasesManager.getReleases();

        int i = 0, total = 1;
        String baseUrl = buildUrlFromFilter(ticketFilter);
        String url;
        List<String> ticketsWithNoFixVersion = new ArrayList<>();
        // Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 100 at a time, so must do this multiple times if bugs > 100
            url = String.format(baseUrl, i, 100);
            JSONObject json;
            try {
                json = jsonUtils.readJsonFromUrl(url);
            } catch (IOException e) {
                System.err.println("Unable to retrieve tickets IDs: " + e.getMessage());
                return;
            }

            // Retrieves the "issues" array
            JSONArray issues = json.getJSONArray("issues");

            // Set the total number of issues found
            if (json.getInt("total") != total) {
                total = json.getInt("total");
                System.out.printf("Total number of issues: %d\n", total);
            }

            // For each retrieved issue, adds a ticket to the list
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
     * Parses a JSON to recover the ticket fields
     *
     * @param ticketJson the JSON of the ticket
     * @return a ticket object
     */
    private Ticket getTicketFromJson(JSONObject ticketJson) {
        LocalDate issuedDate = Instant.parse(ticketJson.getString("created")).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate closedDate = Instant.parse(ticketJson.getString("created")).atZone(ZoneOffset.UTC).toLocalDate();
        TicketType issueType = TicketType.valueOf(ticketJson.getJSONObject("fields").getJSONObject("issueType").getString("name").toLowerCase(Locale.ROOT));
        TicketStatus status = TicketStatus.valueOf(ticketJson.getJSONObject("fields").getJSONObject("status").getString("name").toLowerCase(Locale.ROOT));
        String assignee = ticketJson.getJSONObject("fields").getJSONObject("assignee").getString("name");
        ResolutionType resolutionType = ResolutionType.valueOf(ticketJson.getJSONObject("fields").getJSONObject("resolution").getString("name").toLowerCase(Locale.ROOT));
        Ticket ticket = new Ticket(ticketJson.getString("id"), ticketJson.getString("key"), issuedDate, closedDate, issueType, status, assignee);
        ticket.setResolution(resolutionType);
        return ticket;
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