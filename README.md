
# disa-account

This is the backend service for managing accounts in Manage ISAs.

## Endpoints

### Agent Services client-details lookup

`GET /disa-account/client-details/:zref` requires an internal-auth service credential with
resource type `disa-account`, location `client-details`, action `READ`.
Z-Refs must match `^Z[0-9]{4}$`.

```json
{
  "businessName":"Test Isa Manager Ltd",
  "fcaNumber":"123456"
}
```

Application-generated errors use JSON with a `message` field:

| Status | Context | Message |
| --- | --- | --- |
| 400 | The Z-Ref fails format validation. | `Invalid Z-Ref: expected Z followed by four digits` |
| 404 | ETMP confirms that no matching client exists. | `Client not found in ETMP` |
| 500 | The matching client is missing a business name or FCA number. | `Required client details are missing` |
| 500 | The ETMP lookup fails, times out or returns an invalid response. | `Error retrieving client details from ETMP` |

Internal-auth supplies its standard authentication/permission failure responses (401/403).

### Agent preferences

`GET /disa-account/agents/preferences` and `PUT /disa-account/agents/preferences` require the
frontend user's bearer token. Both require the `Agent` affinity group and an active
`HMRC-AS-AGENT` enrolment. The session ID is obtained from the authenticated user's
`mdtpInformation` in Auth. Callers do not supply a session ID in the path or a header.

PUT accepts the following JSON, validates the Z-Ref using `^Z[0-9]{4}$`, checks access with Auth,
and returns `204 No Content`:

```json
{
  "zReference":"Z1234"
}
```

GET returns the same JSON after checking access with Auth. The first PUT for a session sets
its expiry (`agent-preferences.ttl`, four hours by default). Later PUTs replace the choice
without extending that expiry. GET will not return an expired choice, even if MongoDB has
not yet removed it. GET does not extend the expiry.

The reason is it four hours by default is due to an auth session lasting four hours maximum. The preferences will never 
expire before the auth session does.

| Status | Code | Context |
| --- | --- | --- |
| 400 | `INVALID_ZREFERENCE_FORMAT` | The PUT body is malformed or does not contain a valid Z-Ref. |
| 403 | `USER_IS_NOT_AN_AGENT` | The user lacks agent affinity or the active agent enrolment. |
| 403 | `SESSION_ID_NOT_AVAILABLE` | Auth does not provide a usable session ID. |
| 403 | `AGENT_CLIENT_RELATIONSHIP_NOT_ESTABLISHED` | Auth denied access to the DISA Z-Ref. |
| 404 | `AGENT_PREFERENCES_NOT_FOUND` | No unexpired preference exists for the session. |

Missing or invalid bearer tokens return 401. Delegated access uses Auth's `HMRC-DISA-ORG`
enrolment with identifier `ZREF` and rule `disa-auth`.

## Running the app

### Service manager
The dependent services can be started with:
```bash
sm2 --start DISA_ACCOUNT_ALL
```

### Locally

```bash
sbt run
```

You can then build and compile the app to ensure it is working with the following command:
```bash
# other useful commands
sbt clean

sbt reload

sbt compile
```

### Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

### Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source and sbt files are correctly formatted and all tests including coverage
sbt precommit

# checks all source and sbt files are correctly formatted
sbt prePrChecks

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
