package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.jgit.JGit;
import bettergithistory.util.FileUtil;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.*;

public class Driver {
    public static void main(String[] args) throws IOException {
        // Clean the temp directory used for handling generated files
        FileUtil.cleanTempDirectory();

        // Initialize JGit object for working for repo
        JGit jgit = new JGit("../caprine");
        String fileName = "css/new-design/browser.css";

        // Get file's commit history
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
//        System.out.println("ORIGINAL HISTORY");
//        CommitHistoryUtil.printCommitHistory(commitMap);

        // Initialize a client for interacting with a GitHub repository.
        GitHubRepositoryClient gitHubRepoClient = new GitHubRepositoryClient("sindresorhus/caprine");
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithPullRequests(gitHubRepoClient);
    }
}
