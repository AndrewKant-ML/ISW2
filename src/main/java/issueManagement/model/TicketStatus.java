package issueManagement.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TicketStatus {

    OPENED("open"),
    RESOLVED("resolved"),
    CLOSED("closed"),
    OTHER("other");

    @Getter
    private final String status;

}
