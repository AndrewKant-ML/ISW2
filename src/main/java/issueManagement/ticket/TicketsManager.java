package issueManagement.ticket;

import issueManagement.model.TicketFilter;

public interface TicketsManager {

    void retrieveTicketsIDs(TicketFilter ticketFilter);

}
