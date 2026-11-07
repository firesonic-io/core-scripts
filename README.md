# FireSonic Core Scripts

Default Groovy integration scripts, reusable helpers, semantic validation rules, and Spock tests for [FireSonic](https://firesonic.io).

These files are intended for the FireSonic Community Edition and Developer SDK. **They are not a standalone application.** The FireSonic runtime and SDK supply the required APIs and dependencies.

## Repository layout

| Path | Contents |
| --- | --- |
| [`scripts/Main.groovy`](scripts/Main.groovy) | Default integration lifecycle hooks and A02/A40 workflow delegation |
| [`scripts/core/`](scripts/core/) | HL7 helpers, segment scanning, timezone normalization, patient merge and transfer workflows |
| [`scripts/script.properties`](scripts/script.properties) | Sample script properties, including `enable_debug` and `defaultTimezone` |
| [`rules/semantic/core/`](rules/semantic/core/) | Default Patient and Encounter validation rules |
| [`src/test/groovy/`](src/test/groovy/) | Spock specifications and test utilities |

## Using the scripts

1. Download the [FireSonic Developer SDK](https://firesonic.io) or install FireSonic CE.
2. Back up your current `scripts/` and `rules/` directories.
3. Copy the required files into the matching locations in your SDK or CE installation, keeping their package paths unchanged.
4. Review `scripts/script.properties` for your environment before running. Restart the platform when required for changes to take effect.

The test sources are intended for the SDK's Groovy/Spock test environment. This repository does **not** include a standalone Maven build or the FireSonic API libraries; run tests in a compatible SDK project.

## Compatibility and versions

These files were provided as the core scripts associated with **FireSonic CE 0.1.0**. Before publishing a `v0.1.0` Git tag, verify the contents against the exact CE 0.1.0 distribution. Release tags should remain immutable and represent the actual files shipped in that version.

The `main` branch can contain fixes and new scripts that have **not** been included in a published FireSonic distribution. Consult commit history or release tags before using development versions in production.

## Issues and contributions

Bug reports and small, focused pull requests are welcome. Please include the FireSonic version, affected file or semantic rule ID, a **synthetic** HL7 test message where relevant, expected behavior, and actual behavior.

## License

A license has not yet been selected for this repository. **Public visibility alone does not grant permission to redistribute or modify the code.** Add a `LICENSE` file before offering reusable licensing terms or accepting contributions under those terms.
