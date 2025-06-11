package it.uniroma2.dicii.vcsManagement.model;

import java.time.LocalDate;

public record CommitInfo(String commitId, String authorName, String authorEmail, LocalDate commitDate, String message) {

    @Override
    public String toString() {
        return String.format("%s: %s", commitId, message);
    }
}