# Security

## Reporting a problem

Please don't open a public issue for a security problem. Report it privately instead: on this
repository's **Security** tab, choose **Report a vulnerability**. Only the owner sees the report.

Say what you found, how to reproduce it, and what someone could do with it.

## What counts

- Secrets in the repository or its history: keys, passwords, tokens.
- Ways to get a build signed with the test key, or a build that installs over a tester's build.
- Problems in the app, such as a way to read another app's data or the game log.

The test key that was once committed (`app/poc-test.keystore`) is already known and no longer used
(decision 0017). No need to report it.
