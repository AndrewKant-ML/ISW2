package issueManagement.ticket;

import issueManagement.model.Ticket;
import issueManagement.model.TicketFilter;

import java.util.List;

public interface TicketsManager {

    /**
     * Clears all ticket lists.
     */
    void clear();

    /**
     * Retrieves all project tickets
     */
    void retrieveTickets();

    /**
     * Retrieves all project tickets corresponding to the filter
     *
     * @param ticketFilter the ticket's filter
     */
    void retrieveTickets(TicketFilter ticketFilter);

    /**
     * Sets the fixed version to all tickets using the commits associated with each ticket
     */
    void setFixVersionToTickets();

    /**
     * Returns the retrieved project tickets
     * @return the retrieved project tickets
     */
    List<Ticket> getTickets();

}
