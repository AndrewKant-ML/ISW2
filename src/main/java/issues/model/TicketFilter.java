package issues.model;

import lombok.Data;

import java.util.List;

@Data
public class TicketFilter {

    private List<ResolutionType> resolutions;
    private List<TicketStatus> statuses;
    private List<TicketType> types;
    private List<String> fields;

}
