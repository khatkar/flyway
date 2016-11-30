# Flyway MongoDB Sample Project
## About
This is a sample project depending on the Flyway MongoDB schema migrator PoC. At the top level of
the repository, enter the following command to build and run the sample project.

```bash
user:~/.../flyway$ mvn exec:java -Dexec.mainClass=org.flywaydb.sample.mongodb.Main -P-InstallableDBTest -P-CommercialDBTest -pl flyway-sample-mongodb
```

## JavaScript migrations
Mongo migrations defined in JavaScript file are now supported. JS file should contain database run commands
as defined [here](https://docs.mongodb.com/v3.2/reference/method/db.runCommand/) . Single line `//` and
multi-line `/*..*/` comments can be added as well.
