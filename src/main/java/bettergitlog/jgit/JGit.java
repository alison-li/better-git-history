package bettergitlog.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
     * Retrieve the full Git history for a given file, including file renames.
     * @param filePath The file to get the full history for.
     * @return The list of commits.
     */
    public List<RevCommit> getFileLog(String filePath) {
        List<RevCommit> commits = new ArrayList<>();
        try {
            List<String> allFilePaths = getAllOldFilePaths(filePath);
            RevCommit startCommit = null;
            for (String f : allFilePaths) {
                Iterable<RevCommit> log = git.log()
                        .addPath(f)
                        .setRevFilter(RevFilter.NO_MERGES)
                        .call();
                for (RevCommit commit : log) {
                    if (commits.contains(commit)) {
                        startCommit = null;
                    } else {
                        startCommit = commit;
                        commits.add(commit);
                    }
                }
                if (startCommit == null) return commits;
            }
        } catch (GitAPIException e) {
            System.out.println("Error while retrieving history for file.");
        }
        return commits;
    }

    /**
     * Retrieve all of the old paths this file had if it was renamed or moved.
     * @param filePath The initial file path to find old file paths for.
     * @return A list of all old file paths, including the current file path.
     */
    public List<String> getAllOldFilePaths(String filePath) {
        List<RevCommit> commits = new ArrayList<>();
        List<String> oldFilePaths = new ArrayList<>();
        String updatedPath = filePath;
        try {
            RevCommit startCommit = null;
            do {
                oldFilePaths.add(updatedPath);
                Iterable<RevCommit> log = git.log()
                        .addPath(updatedPath)
                        .setRevFilter(RevFilter.NO_MERGES)
                        .call();
                for (RevCommit commit : log) {
                    if (commits.contains(commit)) {
                        startCommit = null;
                    } else {
                        startCommit = commit;
                        commits.add(commit);
                    }
                }
                if (startCommit == null) return oldFilePaths;
            }
            while ((updatedPath = getRenamedPath(startCommit, filePath)) != null);
        } catch (GitAPIException | IOException e) {
            System.out.println("Error while collecting fileNames.");
        }
        return oldFilePaths;
    }

    public void getFileFromCommit(RevCommit commit, String filePath) {
        List<String> oldFilePaths = getAllOldFilePaths(filePath);
        try (RevWalk revWalk = new RevWalk(repository)) {
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                // Try all file paths associated with the given file path
                for (String path : oldFilePaths) {
                    treeWalk.setFilter(PathFilter.create(path));
                    if (treeWalk.next()) break;
                }
                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file: " + filePath);
                }
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                loader.copyTo(System.out); // TODO: What to store this in and return?
            } catch (IOException e) {
                System.out.println("Error while looking for file in commit");
            }
            revWalk.dispose();
        }
    }

    /**
     * Checks for renames in history of a certain file. Returns null, if no rename was found.
     * Source: https://stackoverflow.com/questions/11471836/how-to-git-log-follow-path-in-jgit-to-retrieve-the-full-history-includi
     * @param startCommit The first commit in the range to look at.
     * @return String or null
     * @throws IOException
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    private String getRenamedPath(RevCommit startCommit, String initialPath) throws IOException, MissingObjectException, GitAPIException {
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
