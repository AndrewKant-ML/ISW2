package issues.proportion;

import issues.model.Ticket;

import java.util.List;
import java.util.stream.Collectors;

public class Proportion {

    private Proportion() {
        // Prevent instantiation
    }

    /**
     * Restituisce i ticket "completi", cioè quelli che hanno:
     * - almeno un commit associato (per calcolare IV)
     * - una FixVersion già presente
     */
    public static List<Ticket> getCompleteTickets(List<Ticket> tickets) {
        return tickets.stream()
                .filter(ticket -> ticket.getFixed() != null
                        && ticket.getAssociatedCommits() != null
                        && !ticket.getAssociatedCommits().isEmpty())
                .collect(Collectors.toList());
    }

}
