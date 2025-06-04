package vcsManagement;

import issueManagement.model.Ticket;
import issueManagement.model.TicketFilter;
import issueManagement.ticket.JiraTicketsManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import vcsManagement.commit.GitCommitManager;
import vcsManagement.model.CommitInfo;
import vcsManagement.model.ModifiedMethod;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CommitTicketExample {

    /* 40ac04b
     * a788614
     * a77042d
     * c4fd34f
     */


    public static void main(String[] args) {
        extracted();

        //f();
    }

    private static void extracted() {
        try {
            // Define the project and repository path
            String projectName = "BOOKKEEPER";  // Example: Apache HBase project
            String repoPath = "/home/cantarell/IdeaProjects/bookkeeper";

            // Initialize the Git commit manager
            GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);

            // Get tickets from Jira
            JiraTicketsManager ticketsManager = new JiraTicketsManager(projectName);

            // Set up a filter to get only FIXED bugs
            TicketFilter filter = new TicketFilter();

            List<Ticket> tickets = extractTicketsWithCommits(projectName, repoPath, filter);
            for (Ticket ticket : tickets) {
                System.out.println(ticket.getKey() + " - " + ticket.getStatus());
                if (ticket.getAssociatedCommits() != null)
                    for (CommitInfo commit : ticket.getAssociatedCommits()) {
                        System.out.println("  - " + commit.getCommitId().substring(0, 7) + " (" + commit.getCommitDate() + ") by " + commit.getAuthorName() + ": " + commit.getMessage().split("\n")[0]);
                    }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void f() {
        // Get modified methods for a specific commit
        String commitId = "a788614"; // Replace with actual commit hash
        try {
            String projectName = "BOOKKEEPER";  // Example: Apache HBase project
            String repoPath = "/home/cantarell/IdeaProjects/bookkeeper";

            // Initialize the Git commit manager
            GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);
            List<ModifiedMethod> methods = gitManager.getModifiedJavaMethods(commitId);
            System.out.println("Modified methods in commit " + commitId + ":");
            String filePath;
            for (ModifiedMethod method : methods) {
                filePath = method.getFilePath();
                System.out.println(method.getMethodCode());
                //System.out.println(" - " + method.getModificationType() + ": " + filePath.substring(0, filePath.lastIndexOf(".")) + "." + method.getMethodName());
            }

//            Map<String, List<GitCommitManager.ModifiedMethod>> allModifications = gitManager.getAllCommitsModifiedMethods();
//            System.out.println("Total commits with Java method modifications: " + allModifications.size());
//
//            for (Map.Entry<String, List<GitCommitManager.ModifiedMethod>> entry : allModifications.entrySet()) {
//                commitId = entry.getKey();
//                List<GitCommitManager.ModifiedMethod> methods = entry.getValue();
//
//                System.out.println("Commit " + commitId.substring(0, 7) + ": " + methods.size() + " method changes");
//            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts all project tickets and their associated commits, then populates the commits field in each Ticket entity.
     * 
     * @param projectName the name of the project (e.g., "BOOKKEEPER")
     * @param repoPath the local path to the Git repository
     * @param filter optional ticket filter to narrow down ticket selection
     * @return List of Ticket entities with populated commit information
     * @throws IOException if there's an issue accessing the repository
     */
    public static List<Ticket> extractTicketsWithCommits(String projectName, String repoPath, TicketFilter filter) throws IOException {
        // Initialize managers
        GitCommitManager gitManager = new GitCommitManager(repoPath, projectName);
        JiraTicketsManager ticketsManager = new JiraTicketsManager(projectName);
        
        try {
            // Apply filter if provided, otherwise use default filter
            if (filter == null) {
                filter = new TicketFilter();
            }
            
            // Retrieve tickets based on filter
            ticketsManager.retrieveTicketsIDs(filter);
            List<Ticket> tickets = ticketsManager.getTickets();
            
            // Associate commits with tickets
            Map<Ticket, List<CommitInfo>> ticketCommitsMap = gitManager.associateCommitsWithTickets(tickets);
            
            // Store commits in each Ticket entity
            for (Map.Entry<Ticket, List<CommitInfo>> entry : ticketCommitsMap.entrySet()) {
                Ticket ticket = entry.getKey();
                List<CommitInfo> commits = entry.getValue();
                
                // Assuming Ticket class has a setCommits method or similar
                ticket.setAssociatedCommits(commits);
            }
            
            return tickets;
        } finally {
            // Ensure resources are closed properly
            gitManager.close();
        }
    }
}