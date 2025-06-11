package it.uniroma2.dicii.vcsManagement;

import it.uniroma2.dicii.issueManagement.ticket.JiraTicketsManager;
import it.uniroma2.dicii.issueManagement.ticket.TicketsManager;
import it.uniroma2.dicii.properties.PropertyNotFoundException;
import it.uniroma2.dicii.vcsManagement.commit.GitCommitManager;

import java.io.IOException;

public class CommitTicketExample {

    public static void main(String[] args) throws IOException, PropertyNotFoundException {

        TicketsManager ticketsManager = new JiraTicketsManager();
        GitCommitManager gitCommitManager = new GitCommitManager(ticketsManager);

        gitCommitManager.getCommitsWithTickets();

        ticketsManager.getTickets().forEach(System.out::println);
    }

}