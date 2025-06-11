package it.uniroma2.dicii.vcsManagement.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CommitInfo {
    private final String commitId;
    private final String authorName;
    private final String authorEmail;
    private final LocalDate commitDate;
    private final String message;
}