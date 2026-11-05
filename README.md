
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

Application-generated errors use JSON with a `message` field:

| Status | Context | Message |
| --- | --- | --- |
| 400 | The Z-Ref fails format validation. | `Invalid Z-Ref: expected Z followed by four digits` |
| 404 | ETMP confirms that no matching client exists. | `Client not found in ETMP` |
| 500 | The matching client is missing a business name or FCA number. | `Required client details are missing` |
| 500 | The ETMP lookup fails, times out or returns an invalid response. | `Error retrieving client details from ETMP` |

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
