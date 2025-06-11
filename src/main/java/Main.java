import it.uniroma2.dicii.issueManagement.ticket.JiraTicketsManager;
import it.uniroma2.dicii.issueManagement.model.TicketFilter;
import it.uniroma2.dicii.issueManagement.model.TicketStatus;
import it.uniroma2.dicii.issueManagement.model.TicketType;
import it.uniroma2.dicii.properties.PropertyNotFoundException;

import java.util.Arrays;
import java.util.List;

public class Main {

    // Change here the project name
    private static final String projectName = "BOOKKEEPER";

    // Change here the percentage of releases to retrieve
    private static final Double percentage = 0.33;

    public static void main(String[] args) throws PropertyNotFoundException {
        JiraTicketsManager ticketManager = new JiraTicketsManager();
        TicketFilter ticketFilter = new TicketFilter();
        ticketFilter.setTypes(List.of(TicketType.BUG));
        ticketFilter.setStatuses(Arrays.asList(TicketStatus.CLOSED, TicketStatus.RESOLVED));
        ticketManager.retrieveTickets(ticketFilter);
    }
}
