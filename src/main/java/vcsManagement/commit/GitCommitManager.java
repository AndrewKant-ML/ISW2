package vcsManagement.commit;

import issueManagement.model.Ticket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import vcsManagement.model.CommitInfo;
import vcsManagement.model.ModifiedMethod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GitCommitManager {

    private final Repository repository;
    private final Git git;
    @Getter
    private final String projectName;
    private final Pattern ticketPattern;

    /**
     * Creates a new Git Commit Manager for the specified repository path and project name
     *
     * @param repoPath The path to the Git repository
     * @param projectName The name of the Jira project (used for ticket ID pattern)
     * @throws IOException if the repository can't be accessed
     */
    public GitCommitManager(String repoPath, String projectName) throws IOException {
        this.projectName = projectName;

        // Initialize the repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

        git = new Git(repository);

        // Create a regex pattern to find ticket IDs in commit messages
        // Format is typically PROJECT-123, e.g., "HBASE-1234"
        ticketPattern = Pattern.compile(projectName + "-\\d+", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Retrieves all commits from the repository and associates them with ticket IDs
     *
     * @return Map of ticket IDs to lists of commits associated with that ticket
     */
    public Map<String, List<CommitInfo>> getCommitsWithTickets() {
        Map<String, List<CommitInfo>> ticketCommits = new HashMap<>();

        try {
            LogCommand logCommand = git.log();
            Iterable<RevCommit> commits = logCommand.call();

            for (RevCommit commit : commits) {
                String commitMessage = commit.getFullMessage();
                List<String> ticketIds = extractTicketIds(commitMessage);
                
                

                CommitInfo commitInfo = new CommitInfo(
                        commit.getName(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        LocalDate.ofInstant(
                                Instant.ofEpochSecond(commit.getCommitTime()),
                                ZoneId.systemDefault()
                        ),
                        commitMessage
                );

                // Associate the commit with each ticket ID found in the commit message
                for (String ticketId : ticketIds) {
                    ticketCommits.computeIfAbsent(ticketId, k -> new ArrayList<>())
                            .add(commitInfo);
                }
            }
        } catch (GitAPIException e) {
            log.error("Error accessing Git repository: {}", e.getMessage(), e);
        }

        return ticketCommits;
    }

    /**
     * Extracts ticket IDs from a commit message
     *
     * @param commitMessage the commit message to search
     * @return list of ticket IDs found in the commit message
     */
    private List<String> extractTicketIds(String commitMessage) {
        List<String> ticketIds = new ArrayList<>();
        Matcher matcher = ticketPattern.matcher(commitMessage);

        while (matcher.find()) {
            ticketIds.add(matcher.group());
        }

        return ticketIds;
    }

    /**
     * Associates the retrieved commits with actual Ticket objects
     *
     * @param tickets a list of tickets to associate with commits
     * @return a map of Ticket objects with their associated commits
     */
    public Map<Ticket, List<CommitInfo>> associateCommitsWithTickets(List<Ticket> tickets) {
        Map<String, List<CommitInfo>> ticketIdToCommits = getCommitsWithTickets();
        Map<Ticket, List<CommitInfo>> ticketToCommits = new HashMap<>();

        for (Ticket ticket : tickets) {
            String ticketKey = ticket.getKey();
            if (ticketIdToCommits.containsKey(ticketKey)) {
                ticketToCommits.put(ticket, ticketIdToCommits.get(ticketKey));
            }
        }

        return ticketToCommits;
    }

    /**
     * Closes the Git repository
     */
    public void close() {
        git.close();
        repository.close();
    }

    /**
     * Analyzes a commit to find all Java methods that were modified in that commit.
     *
     * @param commitId The ID of the commit to analyze
     * @return A list of modified Java methods with their file paths
     * @throws IOException If there's an error accessing the Git repository
     * @throws GitAPIException If there's an error executing Git commands
     */
    public List<ModifiedMethod> getModifiedJavaMethods(String commitId) throws IOException, GitAPIException {
        List<ModifiedMethod> modifiedMethods = new ArrayList<>();
        
        // Get the commit object
        RevCommit commit = repository.parseCommit(repository.resolve(commitId));
        
        // If it's the first commit, we don't have a parent to compare with
        if (commit.getParentCount() == 0) {
            // For first commit, get all files added
            try (Git git = new Git(repository)) {
                RevTree tree = commit.getTree();
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(".java"));
                    
                    while (treeWalk.next()) {
                        String path = treeWalk.getPathString();
                        if (path.endsWith(".java")) {
                            // Get file content
                            ObjectId objectId = treeWalk.getObjectId(0);
                            try (ObjectReader reader = repository.newObjectReader()) {
                                ObjectLoader loader = reader.open(objectId);
                                byte[] bytes = loader.getBytes();
                                String content = new String(bytes, StandardCharsets.UTF_8);
                                
                                // Extract all methods from the file
                                List<String> methods = extractJavaMethods(content);
                                for (String method : methods) {
                                    modifiedMethods.add(new ModifiedMethod(path, method, ModificationType.ADDED));
                                }
                            }
                        }
                    }
                }
            }
            return modifiedMethods;
        }
        
        // For non-first commits, compare with parent
        RevCommit parentCommit = commit.getParent(0);
        ObjectReader reader = repository.newObjectReader();
        
        // Get the diff between this commit and its parent
        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repository);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);
            
            List<DiffEntry> diffs = df.scan(parentCommit.getTree(), commit.getTree());
            
            for (DiffEntry diff : diffs) {
                // Only consider Java files
                if (!diff.getNewPath().endsWith(".java") && !diff.getOldPath().endsWith(".java")) {
                    continue;
                }
                
                // Get the edit list for this file
                EditList editList = df.toFileHeader(diff).toEditList();
                
                // Get old and new file content
                String oldContent = "";
                if (diff.getChangeType() != DiffEntry.ChangeType.ADD) {
                    oldContent = getFileContent(parentCommit, diff.getOldPath());
                }
                
                String newContent = "";
                if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
                    newContent = getFileContent(commit, diff.getNewPath());
                }
                
                // Find modified methods based on the type of change
                switch (diff.getChangeType()) {
                    case ADD:
                        // New file - all methods are added
                        List<String> addedMethods = extractJavaMethods(newContent);
                        for (String method : addedMethods) {
                            modifiedMethods.add(new ModifiedMethod(diff.getNewPath(), method, ModificationType.ADDED));
                        }
                        break;
                        
                    case DELETE:
                        // Deleted file - all methods are deleted
                        List<String> deletedMethods = extractJavaMethods(oldContent);
                        for (String method : deletedMethods) {
                            modifiedMethods.add(new ModifiedMethod(diff.getOldPath(), method, ModificationType.DELETED));
                        }
                        break;
                        
                    case MODIFY:
                    case RENAME:
                    case COPY:
                        // For modified/renamed/copied files, we need to identify which methods were changed
                        Map<String, String> oldMethods = extractJavaMethodsWithSignatures(oldContent);
                        Map<String, String> newMethods = extractJavaMethodsWithSignatures(newContent);
                        
                        // Methods in old file but not in new file were deleted
                        for (Map.Entry<String, String> entry : oldMethods.entrySet()) {
                            if (!newMethods.containsKey(entry.getKey())) {
                                modifiedMethods.add(new ModifiedMethod(
                                        diff.getOldPath(), 
                                        entry.getValue(),
                                        ModificationType.DELETED
                                ));
                            }
                        }
                        
                        // Methods in new file but not in old file were added
                        for (Map.Entry<String, String> entry : newMethods.entrySet()) {
                            if (!oldMethods.containsKey(entry.getKey())) {
                                modifiedMethods.add(new ModifiedMethod(
                                        diff.getNewPath(),
                                        entry.getValue(),
                                        ModificationType.ADDED
                                ));
                            } else if (!oldMethods.get(entry.getKey()).equals(entry.getValue())) {
                                // Method exists in both but content is different - modified
                                modifiedMethods.add(new ModifiedMethod(
                                        diff.getNewPath(),
                                        entry.getValue(),
                                        ModificationType.MODIFIED
                                ));
                            }
                        }
                        break;
                }
            }
        }
        
        return modifiedMethods;
    }

    /**
     * Get the content of a file in a specific commit
     */
    private String getFileContent(RevCommit commit, String path) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
            if (treeWalk == null) {
                return "";
            }
            
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            byte[] bytes = loader.getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Extracts method names from Java source code.
     * Note: This is a simplified implementation that may not handle all Java syntax edge cases.
     */
    private List<String> extractJavaMethods(String javaContent) {
        List<String> methods = new ArrayList<>();
        
        // Simple regex pattern to match method declarations
        // This is a simplified approach and won't handle all possible Java syntax
        Pattern methodPattern = Pattern.compile(
                "(?:public|protected|private|static|\\s) +(?:[\\w<>\\[\\]]+\\s+)+(\\w+) *\\([^)]*\\) *(\\{?|[^;])");
        
        Matcher matcher = methodPattern.matcher(javaContent);
        
        while (matcher.find()) {
            // Extract the method with its body using brace matching
            int startPos = matcher.start();
            if (startPos >= 0 && matcher.group().contains("{")) {
                int openBraces = 1;
                int pos = javaContent.indexOf('{', startPos) + 1;
                
                while (openBraces > 0 && pos < javaContent.length()) {
                    char c = javaContent.charAt(pos);
                    if (c == '{') openBraces++;
                    else if (c == '}') openBraces--;
                    pos++;
                }
                
                if (pos <= javaContent.length()) {
                    String method = javaContent.substring(startPos, pos).trim();
                    methods.add(method);
                }
            }
        }
        
        return methods;
    }

    /**
     * Extracts Java methods with their signatures as keys for comparison
     */
    private Map<String, String> extractJavaMethodsWithSignatures(String javaContent) {
        Map<String, String> methodMap = new HashMap<>();
        
        // Pattern to match method signatures
        Pattern methodPattern = Pattern.compile(
                "(?:public|protected|private|static|\\s) +(?:[\\w\\<\\>\\[\\]]+\\s+)+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
        
        Matcher matcher = methodPattern.matcher(javaContent);
        
        while (matcher.find()) {
            int startPos = matcher.start();
            String methodSignature = matcher.group().trim();
            
            // Extract method name and parameters for the key
            String methodName = methodSignature.substring(methodSignature.lastIndexOf(' ') + 1, 
                    methodSignature.indexOf('('));
            String parameters = methodSignature.substring(methodSignature.indexOf('('), 
                    methodSignature.lastIndexOf(')') + 1);
            String key = methodName + parameters;
            
            if (startPos >= 0 && methodSignature.contains("{")) {
                int openBraces = 1;
                int pos = javaContent.indexOf('{', startPos) + 1;
                
                while (openBraces > 0 && pos < javaContent.length()) {
                    char c = javaContent.charAt(pos);
                    if (c == '{') openBraces++;
                    else if (c == '}') openBraces--;
                    pos++;
                }
                
                if (pos <= javaContent.length()) {
                    String method = javaContent.substring(startPos, pos).trim();
                    methodMap.put(key, method);
                }
            }
        }
        
        return methodMap;
    }

    /**
     * Analyzes all commits to find all Java methods that were modified across the repository history
     *
     * @return A map of commit IDs to lists of modified Java methods
     * @throws IOException If there's an error accessing the Git repository
     * @throws GitAPIException If there's an error executing Git commands
     */
    public Map<String, List<ModifiedMethod>> getAllCommitsModifiedMethods() throws IOException, GitAPIException {
        Map<String, List<ModifiedMethod>> commitsWithModifiedMethods = new HashMap<>();
        
        try {
            LogCommand logCommand = git.log();
            Iterable<RevCommit> commits = logCommand.call();
            
            for (RevCommit commit : commits) {
                String commitId = commit.getName();
                List<ModifiedMethod> modifiedMethods = getModifiedJavaMethods(commitId);
                
                if (!modifiedMethods.isEmpty()) {
                    commitsWithModifiedMethods.put(commitId, modifiedMethods);
                }
            }
        } catch (GitAPIException e) {
            log.error("Error accessing Git repository: {}", e.getMessage(), e);
            throw e;
        }
        
        return commitsWithModifiedMethods;
    }

    /**
     * Type of modification to a method
     */
    public enum ModificationType {
        ADDED,
        MODIFIED,
        DELETED
    }
}