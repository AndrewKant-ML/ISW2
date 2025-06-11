package issueManagement.ticket;

import issueManagement.JSONUtils;
import issueManagement.model.*;
import issueManagement.release.JiraReleasesManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class JiraTicketsManager implements TicketsManager {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").appendOffset("+HHMM", "Z").toFormatter();
    private static final int PAGE_SIZE = 100;

    private final String projectName;
    private final JSONUtils jsonUtils;


    @Getter
    private final List<Ticket> tickets;
    private final List<Ticket> ticketsWithNoFixVersion;
    @Getter
    private final JiraReleasesManager releasesManager;

    public JiraTicketsManager(String projectName) {
        this.projectName = projectName;
        this.jsonUtils = new JSONUtils();
        this.releasesManager = new JiraReleasesManager(projectName);
        this.tickets = new ArrayList<>();
        this.ticketsWithNoFixVersion = new ArrayList<>();
    }

    /**
     * Retrieves all tickets corresponding to the filter
     *
     * @param ticketFilter the ticket's filter
     */
    public void retrieveTicketsIDs(TicketFilter ticketFilter) {
        int i = 0, j, total = 1;
        String baseUrl = buildUrlFromFilter(ticketFilter);
        String url;
        // Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 100 at a time, so must do this multiple times if bugs > 100
            url = String.format(baseUrl, i, PAGE_SIZE);
            JSONObject json;
            try {
                json = jsonUtils.readJsonFromUrl(url);
            } catch (IOException e) {
                log.error("Unable to retrieve tickets IDs: {}", e.getMessage());
                return;
            }

            // Retrieves the "issues" array
            JSONArray issues = json.getJSONArray("issues");

            // Set the total number of issues found
            if (json.getInt("total") != total) {
                total = json.getInt("total");
                log.info("Total number of issues: {}", total);
            }

            // For each retrieved issue, adds a ticket to the list
            for (j = 0; j < issues.length(); j++) {
                // Iterate through each bug
                JSONObject ticketJson = issues.getJSONObject(j);
                if (ticketJson.getJSONObject("fields").getJSONArray("fixVersions") == null || ticketJson.getJSONObject("fields").getJSONArray("fixVersions").isEmpty()) {
                    ticketsWithNoFixVersion.add(getTicketFromJson(ticketJson));
                } else tickets.add(getTicketFromJson(ticketJson));
            }
            i += j;
        } while (i < total);

        log.info("Number of valid tickets found: {}", tickets.size());
        if (!ticketsWithNoFixVersion.isEmpty()) {
            // Output tickets with no fix version
            log.warn("Warning: the following {} tickets were found with no fix versions", ticketsWithNoFixVersion.size());
            for (Ticket ticket : ticketsWithNoFixVersion)
                log.warn(ticket.getKey());
        }
    }

    /**
     * Parses a JSON to recover the ticket fields
     *
     * @param ticketJson the JSON of the ticket
     * @return a ticket object
     */
    private Ticket getTicketFromJson(JSONObject ticketJson) {
        JSONObject fields = ticketJson.getJSONObject("fields");
        LocalDate issuedDate = LocalDateTime.parse(fields.getString("created"), formatter).toLocalDate();
        LocalDate closedDate;
        if (fields.get("resolutiondate") != null && !fields.get("resolutiondate").toString().equals("null"))
            closedDate = LocalDateTime.parse(fields.getString("resolutiondate"), formatter).toLocalDate();
        else closedDate = LocalDateTime.parse(fields.getString("updated"), formatter).toLocalDate();
        TicketType issueType = TicketType.from(fields.getJSONObject("issuetype").getString("name"));
        TicketStatus status = TicketStatus.fromString(fields.getJSONObject("status").getString("name"));
        String assignee = "";
        if (fields.get("assignee") != null && !fields.get("assignee").toString().equals("null"))
            assignee = fields.getJSONObject("assignee").getString("name");
        ResolutionType resolutionType = null;
        if (fields.get("resolution") != null && !fields.get("resolution").toString().equals("null"))
            resolutionType = ResolutionType.fromResolution(fields.getJSONObject("resolution").getString("name"));
        Ticket ticket = new Ticket(ticketJson.getString("id"), ticketJson.getString("key"), issuedDate, closedDate, issueType, status, assignee);
        ticket.setResolution(resolutionType);
        Release fixVersion = getFixVersionFromTicketJson(ticketJson);
        ticket.setFixed(fixVersion);
        return ticket;
    }

    private Release getFixVersionFromTicketJson(JSONObject ticketJson) {
        JSONArray fixVersionsArray = ticketJson.getJSONObject("fields").getJSONArray("fixVersions");

        if (fixVersionsArray == null || fixVersionsArray.isEmpty()) {
            return null;
        }

        // Recupera la lista delle Release (NB: chiama releasesManager solo se necessario)
        releasesManager.getReleasesInfo(0.33);
        List<Release> releases = releasesManager.getReleases();

        // Cerco la prima fixVersion valorizzata che corrisponde a una Release
        for (int i = 0; i < fixVersionsArray.length(); i++) {
            JSONObject fixVersionObj = fixVersionsArray.getJSONObject(i);

            // A volte name è vuoto o null
            if (fixVersionObj.has("name") && !fixVersionObj.isNull("name")) {
                String fixVersionName = fixVersionObj.getString("name");

                for (Release release : releases) {
                    if (release.getName().equals(fixVersionName)) {
                        return release;
                    }
                }
            }
        }

        // Se nessuna corrisponde, torna null
        return null;
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
            url.append(" AND (");
            boolean first = true;
            for (TicketStatus status : ticketFilter.getStatuses()) {
                if (!first) url.append(" OR ");
                url.append("\"status\"=\"").append(status.getStatus()).append("\"");
                first = false;
            }
            url.append(")");
        }

        if (ticketFilter.getTypes() != null && !ticketFilter.getTypes().isEmpty()) {
            url.append(" AND (");
            boolean first = true;
            for (TicketType type : ticketFilter.getTypes()) {
                if (!first) url.append(" OR ");
                url.append("\"issueType\"=\"").append(type).append("\"");
                first = false;
            }
            url.append(")");
        }

        // ⚠️ Fissa sempre questi campi fondamentali per l'analisi
        url.append("&fields=fixVersions,created,resolutiondate,updated,issuetype,status,assignee,resolution");

        url.append("&startAt=%d&maxResults=%d");

        return url.toString();
    }

}