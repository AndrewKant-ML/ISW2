package vcs;


import issues.ticket.JiraTicketsManager;
import vcs.commit.GitCommitManager;

import java.io.IOException;

public class CommitTicketExample {

    public static void main(String[] args) throws IOException {

        JiraTicketsManager ticketsManager = new JiraTicketsManager();
        GitCommitManager gitCommitManager = new GitCommitManager(ticketsManager);

        gitCommitManager.getCommitsWithTickets();

        ticketsManager.getTickets().forEach(System.out::println);
    }

}