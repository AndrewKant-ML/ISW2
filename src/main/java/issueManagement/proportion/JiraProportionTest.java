package issueManagement.proportion;

import issueManagement.model.Release;
import issueManagement.model.Ticket;
import issueManagement.model.TicketFilter;
import issueManagement.release.JiraReleasesManager;
import issueManagement.ticket.JiraTicketsManager;
import vcsManagement.commit.GitCommitManager;
import vcsManagement.model.CommitInfo;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JiraProportionTest {

    public static void main(String[] args) {

        try {
            String projectName = "BOOKKEEPER";
            String repoPath = "/Users/iacov/Documents/ISW2_Metrics_Proj_backup/workspace/projects/bookkeeper";

            // 1️⃣ Recupera release vere da ReleaseManager
            JiraReleasesManager releaseManager = new JiraReleasesManager(projectName);
            releaseManager.getReleasesInfo(1.0); // carica tutte le release
            List<Release> releases = releaseManager.getReleases();

            // 2️⃣ Recupera ticket veri da JIRA
            JiraTicketsManager ticketsManager = new JiraTicketsManager(projectName);
            TicketFilter filter = new TicketFilter(); // puoi settare i parametri se vuoi filtrare
            ticketsManager.retrieveTicketsIDs(filter);
            List<Ticket> tickets = ticketsManager.getTickets();

            // 3️⃣ Recupera commits da Git
            GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);
            Map<String, List<CommitInfo>> ticketCommitsMap = gitManager.getCommitsWithTickets();

            // 4️⃣ Costruisci mappa releaseDates (LinkedHashMap per ordine)
            LinkedHashMap<String, LocalDate> releaseDates = new LinkedHashMap<>();
            for (Release r : releases) {
                releaseDates.put(r.getName(), r.getReleaseDate());
            }

            // 5️⃣ Applica JiraProportion completa
            List<Ticket> estimatedTickets = JiraProportion.applyProportion(tickets, releases, ticketCommitsMap, releaseDates);

            // 6️⃣ (Facoltativo) Stampa i ticket che ora hanno IV stimata
            System.out.println("\n=== Ticket con IV FINALE ===");
            for (Ticket ticket : estimatedTickets) {
                if (ticket.getInjected() != null) {
                    System.out.printf("Ticket %s → IV finale: %s%n", ticket.getKey(), ticket.getInjected().getName());
                }
            }

            // 7️⃣ Chiudi Git
            gitManager.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
