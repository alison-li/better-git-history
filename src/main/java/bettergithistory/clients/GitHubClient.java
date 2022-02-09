package bettergithistory.clients;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;

public class GitHubClient {
    private static GitHub github;

    static {
        try {
            github = new GitHubBuilder()
                    .withOAuthToken("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")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
