import jira.ReleasesManager;
import jira.TicketManager;

public class Main {

    // Change here the project name
    private static final String projectName = "BOOKKEEPER";

    public static void main(String[] args) {
        ReleasesManager releasesManager = new ReleasesManager(projectName);
        releasesManager.getReleasesInfo();

        TicketManager ticketManager = new TicketManager(projectName);
        ticketManager.retrieveTicketsIDs();
    }
}
