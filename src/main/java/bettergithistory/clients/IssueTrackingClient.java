package bettergithistory.clients;

import bettergithistory.core.AbstractIssue;

public interface IssueTrackingClient {

    public AbstractIssue getIssueById(String id) throws Exception;
}
