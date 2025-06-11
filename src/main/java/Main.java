import issues.ticket.JiraTicketsManager;
import issues.model.TicketFilter;
import issues.model.TicketStatus;
import issues.model.TicketType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class Main {

    // Change here the project name
    private static final String PROJECT_NAME = "BOOKKEEPER";

    // Change here the percentage of releases to retrieve
    private static final Double PERCENTAGE = 0.33;

    public static void main(String[] args) {
        JiraTicketsManager ticketManager = new JiraTicketsManager();
        TicketFilter ticketFilter = new TicketFilter();
        ticketFilter.setTypes(List.of(TicketType.BUG));
        ticketFilter.setStatuses(Arrays.asList(TicketStatus.CLOSED, TicketStatus.RESOLVED));
        ticketManager.retrieveTickets(ticketFilter);

        log.info("Retrieved {} tickets for project {}", ticketManager.getTickets().size(), PROJECT_NAME);

    }
}