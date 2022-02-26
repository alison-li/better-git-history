package bettergithistory.core;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public class JiraIssueMetadata extends AbstractIssueMetadata {
    private String priority;
    private List<String> components;
    private List<String> labels;
    private int numIssueLinks;
    private int numSubTasks;
    private int numWatches;

    public JiraIssueMetadata(RevCommit commit) {
        super(commit);
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public int getNumIssueLinks() {
        return numIssueLinks;
    }

    public void setNumIssueLinks(int numIssueLinks) {
        this.numIssueLinks = numIssueLinks;
    }

    public int getNumSubTasks() {
        return numSubTasks;
    }

    public void setNumSubTasks(int numSubTasks) {
        this.numSubTasks = numSubTasks;
    }

    public int getNumWatches() {
        return numWatches;
    }

    public void setNumWatches(int numWatches) {
        this.numWatches = numWatches;
    }
}
