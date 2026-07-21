# Copilot Instructions

This project is a production-quality Android music player for local files only. Use Kotlin, Gradle Kotlin DSL, Jetpack Compose, AndroidX Media3, Room, Coroutines, Flow, MVVM, Repository Pattern, and Clean Architecture.

- Keep UI separate from business logic; UI must never query MediaStore directly.
- Prefer small, buildable, reviewable feature increments.
- Maintain `ARCHITECTURE.md`, `TODO.md`, and `CHANGELOG.md` with meaningful changes.
- Use open-source dependencies only and keep the dependency set small.
- Add tests for domain logic, repository behavior, and business rules where practical.
- Avoid Android Studio specific setup; everything should be editable and buildable from VS Code and Gradle.
