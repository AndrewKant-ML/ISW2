package issueManagement.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TicketType {

    BUG("bug"),
    OTHER("other");

    @Getter
    private final String type;
}
