# Copilot Instructions

## Build, test, and lint commands

Run commands from the repository root with the Gradle wrapper:

- Build debug APK: `./gradlew :app:assembleDebug`
- Full module build + checks: `./gradlew :app:build`
- Lint (default/debug): `./gradlew :app:lint` or `./gradlew :app:lintDebug`
- Unit tests (JVM): `./gradlew :app:testDebugUnitTest`
- Instrumented tests (device/emulator required): `./gradlew :app:connectedDebugAndroidTest`

Single-test examples:

- Single unit test class: `./gradlew :app:testDebugUnitTest --tests "com.example.phantom_agent.ui.main.MainScreenViewModelTest"`
- Single unit test method: `./gradlew :app:testDebugUnitTest --tests "com.example.phantom_agent.ui.main.MainScreenViewModelTest.uiState_initiallyLoading"`
- Single instrumented test class: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.phantom_agent.ui.main.MainScreenTest`
- Single instrumented test method: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.phantom_agent.ui.main.MainScreenTest#firstItem_exists`

## High-level architecture

This is a single-module Android app (`:app`) built with Kotlin, Jetpack Compose, and Navigation 3.

- `MainActivity` is the entry point; it sets `PhantomAgentTheme` and renders `MainNavigation`.
- `Navigation.kt` owns the Navigation 3 back stack (`rememberNavBackStack`) and route entries via typed `NavKey`s.
- `NavigationKeys.kt` defines route keys as `@Serializable` `NavKey` objects (currently `Main`).
- `ui/main/MainScreenViewModel.kt` maps repository `Flow<List<String>>` into a sealed UI state (`Loading`, `Success`, `Error`) with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`.
- `ui/main/MainScreen.kt` has:
  - a screen-level composable that obtains the ViewModel and collects lifecycle-aware state
  - a data-only rendering overload (`MainScreen(data: List<String>)`) used by previews/tests
- `data/DataRepository.kt` defines the data boundary (`Flow<List<String>>`); `DefaultDataRepository` is the current in-app implementation.

## Key conventions in this repository

- Navigation is typed, not string-route based: add new destinations by introducing new `NavKey` types and wiring them in `MainNavigation`'s `entryProvider`.
- Screen state follows the sealed `UiState` pattern from ViewModel flows (`map` + `catch` + `stateIn`) instead of exposing raw repository flows directly to composables.
- `MainScreen` keeps ViewModel wiring and pure rendering separated; prefer this split for new screens so rendering can be previewed/tested without Android lifecycle/viewmodel setup.
- Repository interfaces return `Flow` values and are injected into ViewModels via constructors; keep data access behind interfaces rather than accessing data sources directly in UI code.
