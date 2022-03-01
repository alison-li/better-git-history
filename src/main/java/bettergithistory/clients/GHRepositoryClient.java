package bettergithistory.clients;

import bettergithistory.core.AbstractIssue;
import bettergithistory.core.GHPullRequestWrapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.kohsuke.github.*;

import java.io.IOException;

/**
 * For interfacing with GitHub Java API.
 * We only care about interacting with pull requests in a given repository, so we use this class as a client for
 * extracting information from a repository.
 */
public class GHRepositoryClient implements IssueTrackingClient {
    private final GitHub github;
    private final GHRepository repo;

    public GHRepositoryClient(String repo) throws IOException {
        Dotenv dotEnv = Dotenv.load();
        String authToken = dotEnv.get("GITHUB_AUTH_TOKEN");
        this.github = new GitHubBuilder()
                .withOAuthToken(authToken) // using personal token for now
                .build();
        this.repo = github.getRepository(repo);
    }

    /**
     * Retrieve all pull requests.
     * @return An iterable of pull requests.
     */
    public PagedIterable<GHPullRequest> getAllPullRequests() {
        GHPullRequestQueryBuilder pullRequestQueryBuilder = this.repo.queryPullRequests();
        return pullRequestQueryBuilder.list();
    }

    /**
     * Retrieve a specified pull request.
     * @param id The ID of the pull request.
     * @return The pull request.
     */
    @Override
    public AbstractIssue getIssueById(String id) throws IOException {
        return new GHPullRequestWrapper(this.repo.getPullRequest(Integer.parseInt(id)));
    }
}
