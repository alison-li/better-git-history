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
     */
    public Map<RevCommit, String> getFileLog(String filePath) {
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
            TreeWalk tw = new TreeWalk(repository);
            tw.addTree(commit.getTree());
            tw.addTree(startCommit.getTree());
            tw.setRecursive(true);
            RenameDetector rd = new RenameDetector(repository);
            rd.addAll(DiffEntry.scan(tw));
            List<DiffEntry> files = rd.compute();
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
