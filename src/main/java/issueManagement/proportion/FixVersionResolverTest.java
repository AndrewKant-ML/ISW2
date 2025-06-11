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
            System.out.println("Totale ticket caricati: " + tickets.size());

            // 2️⃣ Ottieni i commit reali da Git
            GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);
            Map<String, List<CommitInfo>> ticketCommitsMap = gitManager.getCommitsWithTickets();

            // 3️⃣ Carica SOLO il primo 33% delle release
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

            // 6️⃣ Stima FV SOLO per i ticket senza FV e che hanno latestCommitDate <= lastReleaseDate
            Map<String, String> estimatedFixVersions = new HashMap<>();

            for (Ticket ticket : tickets) {
                if (ticket.getFixed() == null) {
                    List<CommitInfo> commits = ticketCommitsMap.get(ticket.getKey());

                    if (commits == null || commits.isEmpty()) {
                        continue;
                    }

                    // Calcola latestCommitDate
                    LocalDate latestCommitDate = commits.stream()
                            .map(CommitInfo::getCommitDate)
                            .max(LocalDate::compareTo)
                            .orElse(null);

                    if (latestCommitDate == null) {
                        continue;
                    }

                    // Considero solo ticket "vecchi" (commits prima della last release del 33%)
                    if (latestCommitDate.isAfter(lastReleaseDate)) {
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
                        estimatedFixVersions.put(ticket.getKey(), fixVersion);
                    }
                }
            }

            // 7️⃣ Stampa finale delle Fix Version stimate
            System.out.println("\n============================================");
            System.out.println("FIX VERSION STIMATE: " + estimatedFixVersions.size());
            System.out.println("============================================");

            // Raggruppa per Fix Version per migliore leggibilità
            Map<String, List<String>> groupedByFixVersion = new TreeMap<>();

            for (Map.Entry<String, String> entry : estimatedFixVersions.entrySet()) {
                groupedByFixVersion
                        .computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                        .add(entry.getKey());
            }

            for (Map.Entry<String, List<String>> entry : groupedByFixVersion.entrySet()) {
                System.out.println("\nFix Version: " + entry.getKey() + " (" + entry.getValue().size() + " ticket)");
                for (String ticketKey : entry.getValue()) {
                    System.out.println("  • " + ticketKey);
                }
            }

            gitManager.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}