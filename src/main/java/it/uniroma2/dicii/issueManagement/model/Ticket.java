package it.uniroma2.dicii.issueManagement.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import it.uniroma2.dicii.vcsManagement.model.CommitInfo;

import java.time.LocalDate;
import java.util.List;

@Data
@RequiredArgsConstructor
public class Ticket {

    @NonNull
    private String id;

    @NonNull
    private final String key;

    @NonNull
    private final LocalDate issueDate;

    @NonNull
    private final LocalDate closedDate;

    @NonNull
    private TicketType type;

    @NonNull
    private TicketStatus status;

    @NonNull
    private String assignee;

    private ResolutionType resolution;

    private String summary;

    private Version injected;
    private Version fixed;

    @Setter
    private List<CommitInfo> associatedCommits;

}
