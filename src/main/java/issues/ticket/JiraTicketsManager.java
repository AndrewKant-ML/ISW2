package issues.ticket;

import issues.JSONUtils;
import issues.model.*;
import issues.release.JiraReleasesManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import properties.PropertiesManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JiraTicketsManager{

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").appendOffset("+HHMM", "Z").toFormatter();
    private static final int PAGE_SIZE = 100;

    private final String projectName;
    private final String baseUrl;
    private final JSONUtils jsonUtils;

    @Getter
    private final List<Ticket> tickets;
    private final List<Ticket> ticketsWithNoFixRelease;
    @Getter
    private final JiraReleasesManager ReleasesManager;

    public JiraTicketsManager() {
        this.projectName = PropertiesManager.getInstance().getProperty("info.name");
        this.baseUrl = PropertiesManager.getInstance().getProperty("info.jira.baseUrl");
        this.jsonUtils = new JSONUtils();

        this.ReleasesManager = new JiraReleasesManager();
        ReleasesManager.getReleasesInfo();

        this.tickets = new ArrayList<>();
        this.ticketsWithNoFixRelease = new ArrayList<>();
    }

    public void clear() {
        this.tickets.clear();
        this.ticketsWithNoFixRelease.clear();
    }

    public void retrieveTickets() {
        this.retrieveTickets(new TicketFilter());
    }

    /**
     * Retrieves all tickets corresponding to the filter
     *
     * @param ticketFilter the ticket's filter
     */
    public void retrieveTickets(TicketFilter ticketFilter) {
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
                // Iterate through each ticket
                JSONObject ticketJson = issues.getJSONObject(j);
                tickets.add(getTicketFromJson(ticketJson));
            }
            i += j;
        } while (i < total);

        log.info("Number of valid tickets found: {}", tickets.size());
        if (!ticketsWithNoFixRelease.isEmpty()) {
            // Output tickets with no fix Release
            log.warn("Warning: the following {} tickets were found with no fix Releases", ticketsWithNoFixRelease.size());
            for (Ticket ticket : ticketsWithNoFixRelease)
                log.warn(ticket.getKey());
        }
    }

    public void setFixReleaseToTickets() {
        List<Release> availableReleases = ReleasesManager.getReleases();

        for (Ticket ticket : ticketsWithNoFixRelease) {
            if (ticket.getAssociatedCommits()!=null && !ticket.getAssociatedCommits().isEmpty()) {
                ticket.getAssociatedCommits().sort((c1, c2) -> c2.getCommitDate().compareTo(c1.getCommitDate()));
                LocalDate lastCommitDate = ticket.getAssociatedCommits().get(ticket.getAssociatedCommits().size()-1).getCommitDate();
                availableReleases.stream().filter(v -> v.getReleaseDate().isAfter(lastCommitDate)).findFirst().ifPresent(ticket::setFixed);
            } else
                log.warn("Ticket {} has no associated commits", ticket.getKey());
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

        Release fixRelease = getFixReleaseFromTicketJson(ticketJson);
        ticket.setFixed(fixRelease);
        if (fixRelease == null)
            ticketsWithNoFixRelease.add(ticket);

        return ticket;
    }

    private Release getFixReleaseFromTicketJson(JSONObject ticketJson) {
        JSONArray fixReleasesArray = ticketJson.getJSONObject("fields").getJSONArray("fixVersions");

        if (fixReleasesArray == null || fixReleasesArray.isEmpty()) {
            return null;
        }

        List<Release> Releases = ReleasesManager.getReleases();
        List<Release> ticketFixReleases = null;
        Release fixRelease = null;

        for (int i = 0; i < fixReleasesArray.length(); i++) {
            JSONObject fixReleaseObj = fixReleasesArray.getJSONObject(i);
            String releaseId = fixReleaseObj.getString("id");

            // Using `new ArrayList<>` to create a mutable list
            ticketFixReleases = new ArrayList<>(Releases.stream().filter(v -> v.getId().equals(releaseId)).toList());
        }

        if (ticketFixReleases != null && !ticketFixReleases.isEmpty()) {
            if (ticketFixReleases.size() > 1)
                ticketFixReleases.sort((v1, v2) -> v2.getReleaseDate().compareTo(v1.getReleaseDate()));
            fixRelease = ticketFixReleases.get(0);
        }

        return fixRelease;
    }


    /**
     * Builds a URL to query the Jira REST API according to some filters
     *
     * @param ticketFilter the filter with fields
     * @return the URL with filters set
     */
    private String buildUrlFromFilter(TicketFilter ticketFilter) {
        StringBuilder url = new StringBuilder(baseUrl + "search?jql=project=\"" + projectName + "\"");

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

        if (ticketFilter.getResolutions() != null && !ticketFilter.getResolutions().isEmpty()) {
            url.append("AND(");
            boolean first = true;
            for (ResolutionType type : ticketFilter.getResolutions()) {
                if (!first) url.append("OR");
                url.append("\"resolution\"=\"").append(type).append("\"");
                first = false;
            }
            url.append(")");
        }

        url.append("&startAt=%d&maxResults=%d");

        return url.toString();
    }

}