# Better Git History - Exploration

Scratchpad code written in development of my master's thesis.
This repository is public to help make more examples of how JGit can be used available.

## Setup

1. Create a `.env` file in the project root with the following filled in:
```env
JIRA_USER=username
JIRA_PASSWORD=password

GITHUB_AUTH_TOKEN=token
```
You will want the JIRA username and password to be the credentials you use to login to your
project's JIRA instance. You can try signing up for a JIRA account with an open-source project that uses it. 

2. Run `mvn install` to install the dependencies.
3. View the `Driver` class for some examples of how the methods are called.
   1. Create a directory called `temp` in `src/main/java/bettergithistory/`. This directory will hold all of the generated files from retrieving all versions of a file in the file's own commit history.
   2. You can also create a directory called `json` in `src/main/java/bettergithistory/`. This directory will hold the generated JSON files from retrieving GitHub/Jira information.
