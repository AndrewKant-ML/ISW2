package issueManagement.proportion;

import issueManagement.model.Ticket;
import issueManagement.model.TicketFilter;
import issueManagement.model.Release;
import issueManagement.ticket.JiraTicketsManager;
import vcsManagement.commit.GitCommitManager;
import vcsManagement.model.CommitInfo;

import java.time.LocalDate;
import java.util.*;

public class FixVersionResolverTest {

    public static void main(String[] args) {
        try {
            String projectName = "BOOKKEEPER";
            String repoPath = "/Users/iacov/Documents/ISW2_Metrics_Proj_backup/workspace/projects/bookkeeper";

            // 1️⃣ Carica i ticket reali da JIRA
            JiraTicketsManager ticketsManager = new JiraTicketsManager(projectName);
            TicketFilter filter = new TicketFilter();
            ticketsManager.retrieveTicketsIDs(filter);
            List<Ticket> tickets = ticketsManager.getTickets();

            // 2️⃣ Ottieni i commit reali da Git
            GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);
            Map<String, List<CommitInfo>> ticketCommitsMap = gitManager.getCommitsWithTickets();

            // 3️⃣ Carica SOLO il primo 33% delle release
            System.out.println("Carico il primo 33% delle release...");
            ticketsManager.getReleasesManager().getReleasesInfo(0.33);
            List<Release> firstReleases = ticketsManager.getReleasesManager().getReleases();
            Set<String> firstReleaseNames = new HashSet<>();
            for (Release r : firstReleases) {
                firstReleaseNames.add(r.getName());
            }

            // 4️⃣ Crea la mappa releaseDates
            LinkedHashMap<String, LocalDate> releaseDates = new LinkedHashMap<>();
            for (Release release : firstReleases) {
                releaseDates.put(release.getName(), release.getReleaseDate());
            }

            // 5️⃣ Trova la data della ULTIMA release caricata (limite superiore)
            LocalDate lastReleaseDate = firstReleases.get(firstReleases.size() - 1).getReleaseDate();
            System.out.printf("Ultima release del primo 33%%: %s (%s)%n",
                    firstReleases.get(firstReleases.size() - 1).getName(), lastReleaseDate);

            // 6️⃣ Filtra i ticket che appartengono al primo 33% delle release
            List<Ticket> ticketsInFirst33 = new ArrayList<>();
            for (Ticket ticket : tickets) {
                if (ticket.getFixed() != null && firstReleaseNames.contains(ticket.getFixed().getName())) {
                    ticketsInFirst33.add(ticket);
                }
            }

            // 7️⃣ Log
            System.out.printf("✅ Ticket con FIX VERSION nel primo 33%%: %d%n", ticketsInFirst33.size());

            // 8️⃣ Ora stima FV SOLO per i ticket senza FV e che hanno latestCommitDate <= lastReleaseDate
            List<String> estimatedTickets = new ArrayList<>();

            for (Ticket ticket : tickets) {
                if (ticket.getFixed() == null) {
                    List<CommitInfo> commits = ticketCommitsMap.get(ticket.getKey());

                    if (commits == null || commits.isEmpty()) {
                        System.out.printf("Ticket %s → NO COMMIT, SKIP%n", ticket.getKey());
                        continue;
                    }

                    // Calcola latestCommitDate
                    LocalDate latestCommitDate = commits.stream()
                            .map(CommitInfo::getCommitDate)
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    if (latestCommitDate == null) {
                        System.out.printf("Ticket %s → NO COMMIT DATE, SKIP%n", ticket.getKey());
                        continue;
                    }

                    // Considero solo ticket "vecchi" (commits prima della last release del 33%)
                    if (latestCommitDate.isAfter(lastReleaseDate)) {
                        System.out.printf("Ticket %s → latestCommitDate %s > lastReleaseDate %s → SKIP%n",
                                ticket.getKey(), latestCommitDate, lastReleaseDate);
                        continue;
                    }

                    // Ora posso stimare
                    String fixVersion = FixVersionResolver.resolveFixVersion(
                            ticket.getKey(),
                            commits,
                            releaseDates,
                            false // ticketHadFixed = false perché stiamo su ticket fixed == null
                    );

                    if (fixVersion != null) {
                        estimatedTickets.add(ticket.getKey());
                        System.out.printf("[FV STIMATA] Ticket %s → FV: %s%n", ticket.getKey(), fixVersion);
                    } else {
                        System.out.printf("Ticket %s → nessuna FV stimabile%n", ticket.getKey());
                    }
                }
            }

            // 9️⃣ Stampa finale
            System.out.printf("%n=== Ticket a cui è stata STIMATA una FIX VERSION (%d): ===%n", estimatedTickets.size());
            for (String ticketKey : estimatedTickets) {
                System.out.println(ticketKey);
            }

            gitManager.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
