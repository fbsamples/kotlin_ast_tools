# Kotlin AST Tools by Meta

These are various examples, tools and utilities extracted from Meta's Java to Kotlin migration effort. Some of these may be directly used, and some are supplied as examples and inspirations to encourage more automated editing of Kotlin code.

We are releasing this to provide examples of AST manipulation in Kotlin using Kotlin APIs in hope that this will encourage more people to use them.

Currently, there are three packages here:
1. `com.facebook.kotlin.asttools` containing some utilities we use to simplify work with the Kotlin compiler API
2. `com.facebook.kotlin.matching` containing a helper class to allow simple and readable refactors to a Kotlin file
3. `com.facebook.kotlin.postconversion` containing a few examples of common cleanups of newly converted Kotlin files, and a simple command line tool to run them.

## Running the tool

The example tool can be built and run with gradle:

```shell
./gradlew run <kotlin files>
```

## License

Apache License 2.0
