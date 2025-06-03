package issueManagement;

import issueManagement.model.TicketFilter;

public interface TicketsManager {

    void retrieveTicketsIDs(TicketFilter ticketFilter);

}
