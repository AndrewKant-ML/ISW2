package issues.proportion;

import issues.model.Release;
import issues.model.Ticket;
import issues.release.JiraReleasesManager;
import issues.ticket.JiraTicketsManager;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpeningVersionTest {

    public static void main(String[] args) {

            // STEP 1 - Inizializza JiraReleasesManager
            JiraReleasesManager releasesManager = new JiraReleasesManager();
            releasesManager.getReleasesInfo();

            // STEP 2 - Costruisci la mappa releaseName → releaseDate
            List<Release> releases = releasesManager.getReleases();
            Map<String, LocalDate> releaseDates = releases.stream()
                    .collect(Collectors.toMap(
                            Release::getName,
                            Release::getReleaseDate
                    ));

            // STEP 3 - Inizializza JiraTicketsManager
            JiraTicketsManager ticketsManager = new JiraTicketsManager();

            // Se vuoi puoi anche filtrare solo BUG chiusi e risolti, ma qui prendiamo tutti
            ticketsManager.retrieveTickets();

            List<Ticket> tickets = ticketsManager.getTickets();

            // STEP 4 - Per ogni ticket, calcola e stampa l'Opening Version
            System.out.println("=== Opening Versions calcolate ===");

            for (Ticket ticket : tickets) {
                // Usa la issuedDate (data di apertura)
                LocalDate issuedDate = ticket.getIssueDate();

                String ov = VersionResolver.resolveOpeningVersion(ticket.getKey(), issuedDate, releaseDates);

                System.out.printf("Ticket %s (aperto %s) → Opening Version: %s%n",
                        ticket.getKey(),
                        issuedDate,
                        (ov != null ? ov : "Nessuna"));
        }
    }
}
