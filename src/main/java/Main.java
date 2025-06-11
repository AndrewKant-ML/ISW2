import issueManagement.ticket.JiraTicketsManager;
import issueManagement.model.TicketFilter;
import issueManagement.model.TicketStatus;
import issueManagement.model.TicketType;

import java.util.Arrays;
import java.util.List;

public class Main {

    // Change here the project name
    private static final String projectName = "BOOKKEEPER";

    // Change here the percentage of releases to retrieve
    private static final Double percentage = 0.33;

    public static void main(String[] args) {
        JiraTicketsManager ticketManager = new JiraTicketsManager(projectName);
        TicketFilter ticketFilter = new TicketFilter();
        ticketFilter.setTypes(List.of(TicketType.BUG));
        ticketFilter.setStatuses(Arrays.asList(TicketStatus.CLOSED, TicketStatus.RESOLVED));
        ticketManager.retrieveTicketsIDs(ticketFilter);
    }
}
