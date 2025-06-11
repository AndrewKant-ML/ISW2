package it.uniroma2.dicii.issueManagement.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class Version {

    @NonNull
    private String id;

    @NonNull
    private String name;

    @NonNull
    private LocalDate releaseDate;

    @NonNull
    private Boolean released;

}
