package bettergitlog.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * For interfacing with JGit.
 */
public class JGit {
    private final Repository repository;
    private final Git git;

    public JGit(String gitPath) throws RepositoryNotFoundException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            this.repository = builder.setGitDir(new File(gitPath, ".git"))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .setMustExist(true)
                    .build();
        } catch (IOException e) {
            throw new RepositoryNotFoundException(gitPath, e);
        }
        this.git = new Git(repository);
    }

    /**
     * Retrieve the full Git history for a given file, including file renames/old paths.
     * @param filePath The file to get the full history for.
     * @return A map with the commit mapped to the file path in that commit.
     *          The entries in the map are ordered from most recent commit to oldest commit.
     */
    public Map<RevCommit, String> getFileCommitHistory(String filePath) {
        Map<RevCommit, String> commitMap = new LinkedHashMap<>();
        String updatedPath = filePath;
        try {
            RevCommit startCommit = null;
            do {
                Iterable<RevCommit> log = git.log()
                        .addPath(updatedPath)
                        .setRevFilter(RevFilter.NO_MERGES)
                        .call();
                for (RevCommit commit : log) {
                    if (commitMap.containsKey(commit)) {
                        startCommit = null;
                    } else {
                        startCommit = commit;
                        commitMap.put(commit, updatedPath);
                    }
                }
                if (startCommit == null) return commitMap;
            }
            while ((updatedPath = getRenamedPath(startCommit, filePath)) != null);
        } catch (GitAPIException | IOException e) {
            System.out.println("Error while collecting fileNames.");
        }
        return commitMap;
    }

    /**
     * Takes a file's commit history and generates what the file looked like for each commit.
     * The resulting files are in the "out" directory with the name being "ver#.java" where # is the version of the
     * file and 1 represents the oldest version of the file (i.e. first commit in the file's commit history).
     * @param commitMap A file's commit history.
     * @throws IOException From calling a class method for writing files.
     */
    public void generateFilesFromFileCommitHistory(Map<RevCommit, String> commitMap) throws IOException {
        // Counter starts from the size of the commit history because commitMap is ordered from most recent commit
        // to oldest.
        int count = commitMap.size() - 1;
        for (Map.Entry<RevCommit, String> entry : commitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            String filePath = entry.getValue();
            this.getFileFromCommit(commit, filePath, String.format("out/ver%d.java", count));
            count--;
        }
    }

    /**
     * Get the given filepath in the commit.
     * @param commit The commit to retrieve the file from.
     * @param filePath The file to retrieve.
     * @param newFilePath The file path of the newly created file that will store the contents of the retrieved file.
     * @return The retrieved file in the commit.
     */
    public File getFileFromCommit(RevCommit commit, String filePath, String newFilePath) throws IOException {
        File file = new File(newFilePath);
        if (file.createNewFile()) {
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commit.getTree())) {
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                loader.copyTo(new FileOutputStream(file, false));
            } catch (IOException e) {
                System.out.println("Error while looking for file in commit");
            }
        } else {
            throw new IllegalArgumentException("New file path already exists. Please try another file path to store the result in.");
        }
        return file;
    }

    /**
     * Checks for renames in history of a certain file. Returns null, if no rename was found.
     * Source: https://stackoverflow.com/questions/11471836/how-to-git-log-follow-path-in-jgit-to-retrieve-the-full-history-includi
     * @param startCommit The first commit in the range to look at.
     * @return String or null
     */
    private String getRenamedPath(RevCommit startCommit, String initialPath) throws IOException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log()
                .add(startCommit)
                .setRevFilter(RevFilter.NO_MERGES)
                .call();
        for (RevCommit commit : allCommitsLater) {
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(commit.getTree());
            treeWalk.addTree(startCommit.getTree());
            treeWalk.setRecursive(true);
            RenameDetector renameDetector = new RenameDetector(repository);
            renameDetector.addAll(DiffEntry.scan(treeWalk));
            List<DiffEntry> files = renameDetector.compute();
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME ||
                        diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) &&
                        diffEntry.getNewPath().contains(initialPath)) {
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }
}
