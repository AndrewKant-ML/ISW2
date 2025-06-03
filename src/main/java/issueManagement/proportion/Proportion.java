package issueManagement.proportion;

import issueManagement.model.Release;
import issueManagement.model.Ticket;

import java.util.List;

public interface Proportion {

    Release getInjectedVersionByProportion(List<Ticket> tickets, Ticket ticket);
}
