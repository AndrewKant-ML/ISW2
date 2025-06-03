package issueManagement.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@RequiredArgsConstructor
public class Ticket {

    @NonNull
    private final String key;

    @NonNull
    private final LocalDate issueDate;

    @NonNull
    private TicketType type;

    @NonNull
    private TicketStatus status;

    @NonNull
    private List<String> authors;

    private ResolutionType resolution;

    private Release injected;
    private Release fixed;

}
