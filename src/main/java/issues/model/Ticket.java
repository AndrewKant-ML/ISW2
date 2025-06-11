package issues.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import vcs.model.CommitInfo;

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

    private Release injected;
    private Release fixed;

    @Setter
    private List<CommitInfo> associatedCommits;

}
