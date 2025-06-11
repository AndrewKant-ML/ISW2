package vcsManagement;

import issueManagement.model.Ticket;
import issueManagement.model.TicketFilter;
import issueManagement.model.TicketStatus;
import issueManagement.model.TicketType;
import issueManagement.ticket.JiraTicketsManager;
import issueManagement.ticket.TicketsManager;
import lombok.extern.slf4j.Slf4j;
import properties.PropertyNotFoundException;
import vcsManagement.commit.GitCommitManager;

import java.io.IOException;
import java.util.List;

@Slf4j
public class CommitTicketExample {

    /* 40ac04b
     * a788614
     * a77042d
     * c4fd34f
     */


    public static void main(String[] args) throws PropertyNotFoundException, IOException {

        String projectName = "BOOKKEEPER";  // Example: Apache HBase project
        String repoPath = "/home/cantarell/IdeaProjects/bookkeeper";

        List<Ticket> tickets = extractTicketsWithCommits(projectName, repoPath);
        tickets.stream()
                .filter(t -> t.getAssociatedCommits()==null)
                .forEach(System.out::println);

        //f();
    }

//    private static void extracted() {
//        try {
//            // Define the project and repository path
//            String projectName = "BOOKKEEPER";  // Example: Apache HBase project
//            String repoPath = "/home/cantarell/IdeaProjects/bookkeeper";
//
//            // Initialize the Git commit manager
//            GitCommitManager gitManager = new GitCommitManager();
//
//            // Get tickets from Jira
//            JiraTicketsManager ticketsManager = new JiraTicketsManager(projectName);
//
//            // Set up a filter to get only FIXED bugs
//            TicketFilter filter = new TicketFilter();
//
//            List<Ticket> tickets = extractTicketsWithCommits(projectName, repoPath, filter);
//            for (Ticket ticket : tickets) {
//                System.out.println(ticket.getKey() + " - " + ticket.getStatus());
//                if (ticket.getAssociatedCommits() != null) for (CommitInfo commit : ticket.getAssociatedCommits()) {
//                    System.out.println("  - " + commit.getCommitId().substring(0, 7) + " (" + commit.getCommitDate() + ") by " + commit.getAuthorName() + ": " + commit.getMessage().split("\n")[0]);
//                }
//                System.out.println();
//            }
//        } catch (IOException | PropertyNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    public static void f() {
        // Get modified methods for a specific commit
        String commitId = "a788614"; // Replace with actual commit hash
        String projectName = "BOOKKEEPER";  // Example: Apache HBase project
        String repoPath = "/home/cantarell/IdeaProjects/bookkeeper";

        // Initialize the Git commit manager
//            GitCommitManager gitManager = new GitCommitManager();
//            List<ModifiedMethod> methods = gitManager.getModifiedJavaMethods(commitId);
//            System.out.println("Modified methods in commit " + commitId + ":");
//            String filePath;
//            for (ModifiedMethod method : methods) {
//                filePath = method.getFilePath();
//                System.out.println(method.getMethodCode());
//                //System.out.println(" - " + method.getModificationType() + ": " + filePath.substring(0, filePath.lastIndexOf(".")) + "." + method.getMethodName());
//            }

//            Map<String, List<GitCommitManager.ModifiedMethod>> allModifications = gitManager.getAllCommitsModifiedMethods();
//            System.out.println("Total commits with Java method modifications: " + allModifications.size());
//
//            for (Map.Entry<String, List<GitCommitManager.ModifiedMethod>> entry : allModifications.entrySet()) {
//                commitId = entry.getKey();
//                List<GitCommitManager.ModifiedMethod> methods = entry.getValue();
//
//                System.out.println("Commit " + commitId.substring(0, 7) + ": " + methods.size() + " method changes");
//            }
    }

    /**
     * Extracts all project tickets and their associated commits, then populates the commits field in each Ticket entity.
     *
     * @param projectName the name of the project (e.g., "BOOKKEEPER")
     * @param repoPath    the local path to the Git repository
     * @return List of Ticket entities with populated commit information
     * @throws IOException if there's an issue accessing the repository
     */
    public static List<Ticket> extractTicketsWithCommits(String projectName, String repoPath) throws IOException, PropertyNotFoundException {
        GitCommitManager gitManager = null;
        try {
            TicketsManager ticketsManager = new JiraTicketsManager(projectName);
            gitManager = new GitCommitManager(ticketsManager);

            // Associate commits with tickets
            gitManager.getCommitsWithTickets();

            ticketsManager.setFixVersionToTickets();

            return ticketsManager.getTickets();
        } catch (Exception e) {
            log.error("Error while extracting tickets with commits: {}", e.getMessage(), e);
        } finally {
            // Ensure resources are closed properly
            gitManager.close();
        }
        return null;
    }
}