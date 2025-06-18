package issues.proportion;

import issues.model.Release;
import issues.model.Ticket;
import issues.release.JiraReleasesManager;
import issues.ticket.JiraTicketsManager;
import vcs.commit.GitCommitManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixVersionTest {

    public static void main(String[] args) {
        try {
            // STEP 1 - Inizializza JiraTicketsManager
            JiraTicketsManager ticketsManager = new JiraTicketsManager();

            // STEP 2 - Inizializza GitCommitManager
            GitCommitManager commitManager = new GitCommitManager(ticketsManager);

            // STEP 3 - Recupera tickets e commit associati
            commitManager.getCommitsWithTickets();

            // STEP 4 - Recupera releases
            JiraReleasesManager releasesManager = ticketsManager.getReleasesManager();
            List<Release> releases = releasesManager.getReleases();

            // Costruisci mappa release name → release date
            Map<String, java.time.LocalDate> releaseDates = releases.stream()
                    .collect(Collectors.toMap(
                            Release::getName,
                            Release::getReleaseDate,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));

            // STEP 5 - Esegui VersionResolver solo per i ticket che NON hanno FV e che hanno commit associati
            List<Ticket> tickets = ticketsManager.getTickets();

            for (Ticket ticket : tickets) {
                // Condizione: ticket che NON ha FV e che ha commit associati
                if (ticket.getFixed() == null && ticket.getAssociatedCommits() != null && !ticket.getAssociatedCommits().isEmpty()) {

                    String estimatedFV = VersionResolver.resolveFixVersion(
                            ticket.getKey(),
                            ticket.getAssociatedCommits(),
                            releaseDates,
                            false // ticketHadFixed = false perché stiamo filtrando quelli senza FV
                    );

                    if (estimatedFV != null) {
                        // Stampa il risultato
                        System.out.printf("Ticket %s → FixVersion stimata: %s%n", ticket.getKey(), estimatedFV);
                    }
                }
            }

            // STEP 6 - Chiudi il Git repository
            commitManager.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

