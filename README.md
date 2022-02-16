# Better Git History

This repository is a bit of a mess but is open to help make more examples of how JGit can be used available.

Emphasis on *investigative code* (a.k.a. my sandbox) because it is code I wrote to get an understanding of how to use different libraries for enriching a file's Git history, leading to my master's thesis topic.

## Setup

1. Create a `.env` file with the following filled in:
```env
JIRA_USER=username
JIRA_PASSWORD=password

GITHUB_AUTH_TOKEN=token
```
You will want the JIRA username and password to be the credentials you use to login to your
project's JIRA instance. You can try signing up for a JIRA account with an open-source project that uses it. 

2. Run `mvn install` to install the dependencies.
3. Play around with the `Driver` class.
   1. Create a directory called `temp` in the same root folder that the `Driver` class is in. This directory will hold all of the generated files from retrieving all versions of a file in the file's own commit history.