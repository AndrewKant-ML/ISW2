package issues.proportion;

import vcs.model.CommitInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class VersionResolver {

    private VersionResolver() {
        // Prevent instantiation
    }

    /**
     * Risolve la Fix Version come nome release.
     * Stampa SOLO i ticket che non avevano già FV e a cui è stata stimata.
     *
     **/
    public static String resolveFixVersion(String ticketKey,
                                           List<? extends CommitInfo> commits,
                                           Map<String, LocalDate> releaseDates,
                                           Boolean ticketHadFixed) {

        if (commits == null || commits.isEmpty()) return null;

        // Trova la data più recente del commit
        LocalDate latestCommitDate = commits.stream()
                .map(CommitInfo::getCommitDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Trova la release più vecchia con releaseDate >= latestCommitDate
        for (Map.Entry<String, LocalDate> entry : releaseDates.entrySet()) {
            if (!entry.getValue().isBefore(latestCommitDate)) {

                // Log SOLO se il ticket non aveva già FV
                if (ticketHadFixed != null && !ticketHadFixed) {
                    System.out.printf("[FV STIMATA] Ticket %s → FV: %s%n", ticketKey, entry.getKey());
                }

                return entry.getKey();
            }
        }

        // Nessuna release trovata → opzionale: se vuoi puoi loggare i falliti
        return null;
    }

    public static String resolveOpeningVersion(String ticketKey,
                                               LocalDate ticketIssuedDate,
                                               Map<String, LocalDate> releaseDates) {

        // Trova la release più recente con releaseDate <= ticketIssuedDate
        String openingVersion = null;
        LocalDate latestReleaseDate = null;

        for (Map.Entry<String, LocalDate> entry : releaseDates.entrySet()) {
            LocalDate releaseDate = entry.getValue();

            if (!releaseDate.isAfter(ticketIssuedDate)) {
                if (latestReleaseDate == null || releaseDate.isAfter(latestReleaseDate)) {
                    latestReleaseDate = releaseDate;
                    openingVersion = entry.getKey();
                }
            }
        }

        if (openingVersion != null) {
            System.out.printf("[OV CALCOLATA] Ticket %s → OV: %s%n", ticketKey, openingVersion);
        } else {
            System.out.printf("[OV NON TROVATA] Ticket %s → Nessuna release <= %s%n", ticketKey, ticketIssuedDate);
        }

        return openingVersion;
    }

}
