# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Spring Boot application built with Maven. Production code lives under `src/main/java/com/frost/testehcache3`, starting from `TestEhcache3Application`. Configuration files belong in `src/main/resources`, with `application.yaml` as the default entry point for runtime settings. Tests live under `src/test/java/com/frost/testehcache3` and should mirror the main package structure. Maven wrapper files (`mvnw`, `mvnw.cmd`, `.mvn/`) are committed and should be preferred over a machine-local Maven install.

## Build, Test, and Development Commands
Use the wrapper so builds stay consistent across environments.

- `./mvnw spring-boot:run` or `mvnw.cmd spring-boot:run`: run the app locally.
- `./mvnw test`: execute the JUnit test suite.
- `./mvnw clean package`: compile, test, and build the runnable jar in `target/`.
- `./mvnw clean verify`: run the full verification lifecycle before opening a PR.

## Coding Style & Naming Conventions
Follow standard Java conventions with 4-space indentation and one top-level public class per file. Keep package names lowercase, class names `UpperCamelCase`, methods and fields `lowerCamelCase`, and constants `UPPER_SNAKE_CASE`. Spring configuration classes should end with `Config`, and tests should end with `Tests`. Lombok is available; use it sparingly and prefer explicit code when behavior is non-trivial. No formatter or linter is configured yet, so keep style consistent with the existing Spring Boot scaffold.

## Testing Guidelines
Tests use `spring-boot-starter-test`, which includes JUnit 5 and Spring Boot test support. Add focused unit tests where possible, and use `@SpringBootTest` only when container wiring matters. Name test classes after the target type, for example `CustomCacheManagerTests`. Every new feature or bug fix should include at least one automated test covering the main success path and one failure or edge case when practical.

## Commit & Pull Request Guidelines
Git history is not available in this workspace snapshot, so no repository-specific commit pattern could be inferred. Use short, imperative commit messages such as `Add Ehcache XML configuration`. Keep commits scoped to one concern. PRs should include a concise summary, the reason for the change, test evidence (`./mvnw test` output or equivalent), and any configuration or API impact. Include sample requests or screenshots only when behavior is user-visible.

## Configuration Notes
Do not commit secrets or environment-specific endpoints in `application.yaml`. Prefer property overrides through environment variables or profile-specific config files that are excluded from version control when needed.
