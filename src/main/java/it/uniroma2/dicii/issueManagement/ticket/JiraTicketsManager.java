package it.uniroma2.dicii.issueManagement.ticket;

import it.uniroma2.dicii.issueManagement.JSONUtils;
import it.uniroma2.dicii.issueManagement.model.*;
import it.uniroma2.dicii.issueManagement.release.JiraVersionsManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import it.uniroma2.dicii.properties.PropertiesManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JiraTicketsManager implements TicketsManager {

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").appendOffset("+HHMM", "Z").toFormatter();
    private static final int PAGE_SIZE = 100;

    private final String projectName;
    private final String baseUrl;
    private final JSONUtils jsonUtils;

    @Getter
    private final List<Ticket> tickets;
    private final List<Ticket> ticketsWithNoFixVersion;
    @Getter
    private final JiraVersionsManager versionsManager;

    public JiraTicketsManager() {
        this.projectName = PropertiesManager.getInstance().getProperty("project.name");
        this.baseUrl = PropertiesManager.getInstance().getProperty("project.jira.baseUrl");
        this.jsonUtils = new JSONUtils();

        this.versionsManager = new JiraVersionsManager();
        versionsManager.getVersionsInfo();

        this.tickets = new ArrayList<>();
        this.ticketsWithNoFixVersion = new ArrayList<>();
    }

    @Override
    public void clear() {
        this.tickets.clear();
        this.ticketsWithNoFixVersion.clear();
    }

    @Override
    public void retrieveTickets() {
        this.retrieveTickets(new TicketFilter());
    }

    /**
     * Retrieves all tickets corresponding to the filter
     *
     * @param ticketFilter the ticket's filter
     */
    @Override
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
        if (!ticketsWithNoFixVersion.isEmpty()) {
            // Output tickets with no fix version
            log.warn("Warning: the following {} tickets were found with no fix versions", ticketsWithNoFixVersion.size());
            for (Ticket ticket : ticketsWithNoFixVersion)
                log.warn(ticket.getKey());
        }
    }

    @Override
    public void setFixVersionToTickets() {
        List<Version> availableVersions = versionsManager.getVersions();

        for (Ticket ticket : ticketsWithNoFixVersion) {
            if (ticket.getAssociatedCommits()!=null && !ticket.getAssociatedCommits().isEmpty()) {
                ticket.getAssociatedCommits().sort((c1, c2) -> c2.commitDate().compareTo(c1.commitDate()));
                LocalDate lastCommitDate = ticket.getAssociatedCommits().get(ticket.getAssociatedCommits().size()-1).commitDate();
                availableVersions.stream().filter(v -> v.getReleaseDate().isAfter(lastCommitDate)).findFirst().ifPresent(ticket::setFixed);
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

        Version fixVersion = getFixVersionFromTicketJson(ticketJson);
        ticket.setFixed(fixVersion);
        if (fixVersion == null)
            ticketsWithNoFixVersion.add(ticket);

        return ticket;
    }

    private Version getFixVersionFromTicketJson(JSONObject ticketJson) {
        JSONArray fixVersionsArray = ticketJson.getJSONObject("fields").getJSONArray("fixVersions");

        if (fixVersionsArray == null || fixVersionsArray.isEmpty()) {
            return null;
        }

        List<Version> versions = versionsManager.getVersions();
        List<Version> ticketFixVersions = null;
        Version fixVersion = null;

        for (int i = 0; i < fixVersionsArray.length(); i++) {
            JSONObject fixVersionObj = fixVersionsArray.getJSONObject(i);
            String releaseId = fixVersionObj.getString("id");

            // Using `new ArrayList<>` to create a mutable list
            ticketFixVersions = new ArrayList<>(versions.stream().filter(v -> v.getId().equals(releaseId)).toList());
        }

        if (ticketFixVersions != null && !ticketFixVersions.isEmpty()) {
            if (ticketFixVersions.size() > 1)
                ticketFixVersions.sort((v1, v2) -> v2.getReleaseDate().compareTo(v1.getReleaseDate()));
            fixVersion = ticketFixVersions.get(0);
        }

        return fixVersion;
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