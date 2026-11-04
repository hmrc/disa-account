
# disa-account

This is the backend service for managing accounts in Manage ISAs.

## Endpoints

### Agent Services client-details lookup

`GET /disa-account/client-details/:zref` requires an internal-auth service credential with
resource type `disa-account`, location `client-details`, action `READ`.
Z-Refs must match `^Z[0-9]{4}$`.

```json
{"businessName":"Test Isa Manager Ltd","fcaNumber":"123456"}
```

Missing, null or blank required fields produce 500. Upstream 404 produces 404; other failures,
malformed responses and timeouts produce 500.
Internal-auth supplies its standard authentication/permission failure responses (401/403).

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
