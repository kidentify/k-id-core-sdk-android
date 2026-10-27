# k-ID Core SDK (Android)

The k-ID SDK for Android apps. It adds an age gate with parental consent, adult age verification, and a settings menu
where players manage their permissions, as Jetpack Compose views, composable pieces, or state for your own UI. The web
SDK (`@k-id/core-react`) and the iOS SDK share its surface, names, and behavior.

## Requirements

Android 12 (API 31) or later. Kotlin 2.1 or later and Jetpack Compose. Compile against API 36.

## Install

The SDK is on Maven Central. With `mavenCentral()` in your repositories, add the library to your app module:

```kotlin
dependencies {
    implementation("com.k-id:core-ui:1.0.0")
}
```

## Test and live keys

The SDK sends the app's package name as `Kid-App-Id`, and k-ID refuses calls from a package name not registered in
Kai.

- **Test key** (`kgp_test_...`): register the package name on the test key in Kai. Emulators and CI work.
- **Live key** (`kgp_live_...`): register the package name and the SHA-256 of the certificate that signs the app you
  ship. For an app distributed through Google Play that is the app signing key's certificate (Play Console, under
  **Test and release > App integrity**); register your upload key's certificate too if you install builds signed
  with it. The SDK verifies the app with Android Key Attestation, which passes only for a build signed with a
  registered certificate, on a device with a locked bootloader and verified boot. Use a test key while developing.

## Quick start

```kotlin
import androidx.compose.runtime.Composable
import com.kid.core.JurisdictionCode
import com.kid.core.KIDDeclare
import com.kid.core.PermissionName
import com.kid.core.ui.AUTO
import com.kid.core.ui.KIDAgeGate
import com.kid.core.ui.KIDCoreProvider
import com.kid.core.ui.KIDSessionProvider

@Composable
fun Root() {
    KIDCoreProvider(
        publishableKey = "kgp_test_...",
        baseUrl = "https://kai-api.test.k-id.com/fapi/v1",
        declare = KIDDeclare(
            permissions = listOf(KIDDeclare.Permission(PermissionName.TEXT_CHAT_PUBLIC)),
            supportEmail = "help@example.com",
        ),
    ) {
        KIDSessionProvider(jurisdiction = JurisdictionCode.AUTO) {
            KIDAgeGate(pass = { AppScreen() })
        }
    }
}
```

Later, on your options screen, a standing menu lets the player manage that session's permissions:

```kotlin
KIDSettingsProvider(sessionId = sessionId) {
    KIDSettings()
}
```

(`KIDSettingsProvider` must sit inside a `KIDCoreProvider`.)

## `KIDCoreProvider` - app-level, wrap once

- `publishableKey` - required. Your k-ID publishable key; safe to ship in the app.
- `baseUrl` - required. The k-ID API URL Kai shows for your key: `https://kai-api.k-id.com/fapi/v1` for a live key,
  `https://kai-api.test.k-id.com/fapi/v1` for a test key.
- `appearance` - optional `Appearance`. See [Styling](#styling).
- `localization` - optional `KIDLocalization`: a locale, or partial copy overrides merged over the English defaults.
  See [Localization](#localization).
- `declare` - optional `KIDDeclare`, read once when the provider is first composed. See
  [Declare](#declare-test-keys).

## `KIDSessionProvider` - wrap one age gate

- `jurisdiction` - a `JurisdictionCode`: an ISO 3166-1 alpha-2 country or ISO 3166-2 subdivision code (the supported
  ones are companion members, such as `JurisdictionCode.US_CA`; `JurisdictionCode("US-CA")` also works), or
  `JurisdictionCode.AUTO` to resolve it from the client IP (falls back to the picker). Omit to show the picker.
- `age` - `AgeSignal` (`AgeSignal.DateOfBirth("YYYY-MM-DD")` or `AgeSignal.Age(n)`). Omit to show `KIDCollectAge`.
- `email` - the grownup's email for consent, or the player's own email for adult age verification. Omit to show
  `KIDAskAnAdult` / `KIDVerifyAge`.
- `challengeId` / `sessionId` - resume. The SDK keeps only its device key; it stores no session or challenge ids:
  store the ids from the callbacks below and pass them back.
- `onChallenge` - fires once a challenge's email is sent.
- `onSession` - fires once the session is established.
- `onClaim` - fires once the age check has a result, passed or pending, with a one-time token for your backend.

Age and jurisdiction freeze once the check runs; later changes are ignored with a warning.

## `KIDSettingsProvider` - wrap the settings menu

- `sessionId` - required; the session from the age gate's `onSession`.
- `challengeId` / `onChallenge` - resume an in-flight grownup ask. `onChallenge` fires once the ask's email is sent.

## Integration tiers

Three levels of control, across both surfaces: the age gate (`KIDSessionProvider`) and the settings menu
(`KIDSettingsProvider`). Mix them freely. Every view fills its container's width.

**Tier 1 - drop-in views.** `KIDAgeGate` draws the capture itself while `running`. For the other states it renders
the slot you pass, `pending`, `pass`, `prohibited`, `fail` or `error`, and nothing for one you leave out, so pass at
least `pass`. There is no reset: recover from `error` by recreating `KIDSessionProvider`, for example by wrapping it
in `key(attempt) { ... }`.

`KIDSettings()` is the all-in-one permission menu inside `KIDSettingsProvider`: player-managed permissions get an
on/off switch, guardian-managed ones are staged with a tap and asked for together in one request, and prohibited ones
are hidden.

`KIDPermission(PermissionName.VOICE_CHAT, fallback = { ... }) { ... }` renders its content when that permission is
enabled for the player, else `fallback` (default: nothing).

**Tier 2 - composable pieces.** Place these inside `KIDSessionProvider`. Each renders only during its own step, and
not at all once you supply the matching provider input. The callbacks only observe; they never take over the flow.

- `KIDSelectJurisdiction(onSelect = ...)` - the jurisdiction picker. `onSelect` gets the chosen code.
- `KIDCollectAge(methods = ..., onSubmit = ...)` - `methods` is your preferred order of `KIDAgeMethod.AGE_SLIDER` /
  `KIDAgeMethod.DATE_OF_BIRTH`, limited to what the jurisdiction approves; omit it for age slider, then date of birth.
  `onSubmit` gets the `AgeSignal`.
- `KIDAskAnAdult(onSent = ...)` - the grownup's email with a built-in send. `onSent` gets the email once it has gone
  out.
- `KIDVerifyAge(onSent = ...)` - for an adults-only product: emails the player a link to verify their age on k-ID.
  `onSent` gets the email once it has gone out.

On an established session, `KIDSessionVerifyAge(pass = { ... })` verifies the player is an adult. See
[Verifying age on a session](#verifying-age-on-a-session).

`KIDPermissionSettings()` is `KIDSettings` without its surrounding card, for your own settings screen.

**Tier 3 - headless.** Hold each value in your own state and pass it as the matching `KIDSessionProvider` input; the
views fill in anything you omit. Read and drive the flow with these composable functions, each recomposing as its
state changes:

| Web hook | Kotlin |
|---|---|
| `useKidAgeGate()` | `useKIDAgeGate()` |
| `useKidSession()` | `useKIDSession()` |
| `useKidChallenge()` | `useKIDChallenge()` |
| `useKidPermission()` | `useKIDPermission()` |
| `useKidPermissionSettings()` | `useKIDPermissionSettings()` |
| `useKidStrings()` / `useKidJurisdictions()` | `useKIDStrings()` / `useKIDJurisdictions()` |

- `useKIDAgeGate()` - `status`, `challengeId`, `challengeKind`, `sessionId`, `error`, `requirements` (including the
  jurisdiction's `digitalConsentAge`, `civilAge`, `minimumAge` and `accessAge`), and `submit()`, a suspend function
  that sends the challenge email once the check has produced a challenge and can be called again to re-send.
  `challengeKind` is `PARENTAL_CONSENT` when the email goes to a parent and `AGE_ASSURANCE` when it goes to the
  player, or null before there is a challenge.
- `useKIDSession()` - `session`, `permissions` (a name to enabled map), `sessionId`, `isLoaded`, `ageVerification`
  (`verifiedAge`, counting birthdays since, and `verifiedAt` once the player has proven an age, else null),
  `verifyAge`, and `refresh()`, a cheap re-read. The player is a verified adult when
  `ageVerification.verifiedAge >= requirements.civilAge`.
- `useKIDSession().verifyAge` - the headless form of `KIDSessionVerifyAge`, sharing its flow: `start()` checks the
  session (a no-op until the gate passes; after a fail or error it starts over with a fresh check); `status` is `PASS`
  once the session's `ageVerification.verifiedAge` meets the jurisdiction's civil age, `FAIL` when it does not, and
  `ERROR` when k-ID cannot be reached. While `step` is `EMAIL`, collect the player's email and call `submit(email)`;
  while `status` is `PENDING`, call it again to re-send.
- `useKIDPermission()` - `can(PermissionName.VOICE_CHAT)` and `get(...)`, the full `Permission` including
  `managedBy`, so you can explain a guardian-managed feature rather than just hide it.
- `useKIDChallenge()` - `challengeId`, `status` and `expiresAt`, for a custom waiting screen. `expiresAt` is only set
  when the challenge has a deadline, like Indonesia's 24-hour consent window. Once it passes, the age gate moves to
  `FAIL`; recreate the provider to ask again.
- `useKIDPermissionSettings()` - branch each permission on `managedBy`: `PLAYER` -> `change(name, enabled)`, except a
  `PLAYER` permission with a `verifiedAgeThreshold` that is off: ask for it on its own with `upgrade(listOf(name))`,
  which turns it on when the player's verified age already covers it and otherwise returns `INVALID_REQUEST` because an
  age check is needed; call it again with the player's own email (never the grownup's), `upgrade(listOf(name),
  playerEmail)`, and k-ID emails the player a link to verify their age; `GUARDIAN` -> collect a set and `upgrade(names)` (one request, emailed
  to the player's grownup); `PROHIBITED` -> hide it. One `upgrade` can't mix permissions that have a
  `verifiedAgeThreshold` with ones that don't: k-ID rejects it with `INVALID_REQUEST`. When `hasApproverEmail` is false, ask for the grownup's email and
  pass `upgrade(names, approverEmail)`. `challenge` is the in-flight ask; its `challengeKind` is `PARENTAL_CONSENT`
  while it waits on the grownup and `AGE_ASSURANCE` while it waits on the player's own age check, and null until known.
  `change` and `upgrade` return a `KIDResult` (see [Errors](#errors)).

Two steps `KIDCollectAge` and `KIDAskAnAdult` handle for you, you drive yourself here:

- Age: `requirements.approvedAgeCollectionMethods` lists the inputs the jurisdiction allows (null until a
  jurisdiction is set). Collect one and pass it as the `age` input (`date-of-birth` -> `AgeSignal.DateOfBirth`,
  `age-slider` -> `AgeSignal.Age`).
- Consent and adult verification: the `email` input alone does not send. Once a challenge exists, pass `email`, then
  call `submit()`.

A permission's name is a `PermissionName` value (`PermissionName.VOICE_CHAT`); a session's `Permission` gives it as
`permissionName`. Client-side permissions gate what a player sees. Enforce the real capability server-side.

## Styling

`Appearance(variables, elements)`. `variables` carries the same tokens as web (`colorPrimary`, `colorOnPrimary`,
`colorText`, `colorTextMuted`, `colorSurface`, `colorSurfaceMuted`, `colorBorder`, `colorDanger`, `fontFamily`,
`radius`); pass only the ones you change. `elements` overrides one element, keyed by `ElementName` (`CARD`, `TITLE`,
`LABEL`, `INPUT`, `BUTTON`, `MUTED`, `DANGER`, `ROW`, `PILL` (`PILL_ASK` / `PILL_ADDED`), `ASK_FORM`,
`CONTROL_COLUMN`, `VERIFY_AGE`, and one per view: `SELECT_JURISDICTION`, `COLLECT_AGE`, `ASK_AN_ADULT`, `SETTINGS`)
with a `KIDElementStyle`. An override beats every default.

## Localization

The views render in English. Pass a locale to translate them:

```kotlin
import com.kid.core.ui.KIDCoreProvider
import com.kid.core.ui.KIDLocalization

KIDCoreProvider(publishableKey = "kgp_test_...", baseUrl = fapiUrl, localization = KIDLocalization.jaJP) {
    ...
}
```

`getKIDLocalization(Locale.getDefault().toLanguageTag())` picks the locale for the player's language, falling back to
English. `KID_LOCALES` lists the supported locales. An Arabic locale lays the views out right to left.

To change individual strings, start from a locale and copy it with the fields you need. Keep any `{token}`
placeholders.

```kotlin
import com.kid.core.ui.KIDLocalization

val ja = KIDLocalization.jaJP
val localization = ja.copy(age = ja.age?.copy(submit = "OK"))
```

The jurisdiction picker follows the locale; `useKIDJurisdictions()` is the same translated list for a custom picker.
Permission names follow it too: `KIDSettings` shows them, and a custom settings UI reads
`useKIDStrings().permissions[name.rawValue]?.name`.

## Errors

`KIDError` carries `status`, a closed `code` (`KIDErrorCode`: `INVALID_REQUEST`, `UNAUTHORIZED`, `NOT_FOUND`,
`RATE_LIMITED`, `UNAVAILABLE`), a display-safe `message`, the server `rawCode`, and with `RATE_LIMITED`, `retryAfter`:
the whole seconds k-ID asked you to wait, when it said, or, when k-ID rate limited the client token a call needed, the
seconds left before the SDK asks for one again. The SDK never retries a rate-limited call for you. Systemic
failures throw it, and `useKIDSession().refresh()` throws nothing else apart from cancellation; user-recoverable ones
(`submit`, `change`, `upgrade`) return a `KIDResult` whose `error` is the `KIDError` instead. The `error` of
`useKIDAgeGate()`, `useKIDSession().verifyAge` and `useKIDPermissionSettings()` is the `KIDError` behind
`status == ERROR` (or a failed settings load), or null: a k-ID API error as is, and any other failure, such as a dropped
connection, as `UNAVAILABLE`. Full detail goes to Logcat under the `k-ID` tag. `submit()` returns `INVALID_REQUEST` when
called before a challenge exists or without an email.

The `error` slots of `KIDAgeGate` and `KIDSessionVerifyAge` take no arguments: read the error inside them from
`useKIDAgeGate().error` or `useKIDSession().verifyAge.error`. When the settings list fails to load,
`useKIDPermissionSettings().error` is set; call its `refresh()` to load it again.

## Google Play Age Signals

The age gate first asks Google Play Age Signals for the player's age range. When the player shares it, the range
stands in for the age step and the player never sees it. Play answers with your app's own age ranges if you set them
on the Age signals page in Play Console, or with its default ranges. It answers only for an app installed from Google
Play, or sideloaded on a device signed in as a license tester, and only where Play offers age signals. Without a range,
or when the player declines, the gate asks for their age as usual. It is asked only from an app Key Attestation has
verified (a live key), or from any app on a test key, and never when you pass `age`.

## Declare (test keys)

`declare` on `KIDCoreProvider` is a one-time integration manifest: the capabilities the app uses (which become its
permission set), plus its legal links, icon and support email. It applies only while you use a test key; a live
key skips it. The age gate sends no request until it lands; a transient
failure is retried a few times, and if declare still fails the age gate shows `error`.

```kotlin
KIDDeclare(
    permissions = listOf(KIDDeclare.Permission(PermissionName.TEXT_CHAT_PUBLIC), KIDDeclare.Permission(PermissionName.VOICE_CHAT)),
    privacyPolicyUrl = DeclareField.Set("https://example.com/privacy"),
    termsOfServiceUrl = DeclareField.Set("https://example.com/terms"),
    iconUrl = DeclareField.Set("file:///android_asset/icon.png"),
    supportEmail = "help@example.com",
)
```

`permissions` and `supportEmail` are required. Every other field is a `DeclareField`: leave it out (`DeclareField.Keep`)
to keep what k-ID already has, pass `DeclareField.Clear` to clear it, or `DeclareField.Set(value)` to set it.

- `permissions` - required. Each `name` is a `PermissionName`, and `PermissionName.allCases` lists the catalog. An
  unknown name is rejected, and the call fails naming it.
- `privacyPolicyUrl` / `termsOfServiceUrl` - optional http(s) URLs. `Clear` resets one to k-ID's default link.
- `iconUrl` - optional app icon, a PNG or JPEG up to 5MB. A public https link is sent for k-ID to fetch; an image in
  the app (an asset as `file:///android_asset/...`, an `android.resource://` or `content://` URI, or a file path) is
  read by the SDK and sent inline. `Clear` removes it.
- `supportEmail` - required email address that k-ID's emails give players and parents to contact for help. An empty
  or invalid address is rejected, and the call fails naming `supportEmail`. Getting a live key also needs your own
  privacy and terms links and an icon.

## Adult age verification (`KIDVerifyAge`)

`KIDVerifyAge` emails the player a link to verify their age on k-ID. The result comes through the SDK's status
check, the same as consent.

## Verifying age on a session

On any product, a player already through the gate can prove they are an adult (the civil age of their jurisdiction)
on their session with age assurance, for example to open an adults-only space. Like `KIDAgeGate`,
`KIDSessionVerifyAge` draws the email form itself and renders whatever you pass for each outcome, and nothing for one
you leave out:

| State | Renders |
|---|---|
| before the gate passes, and while it checks the session | nothing |
| the session is not yet a verified adult | the email form (`onSent` gets the email once the link is sent) |
| link sent, waiting for the player to verify | `pending` |
| the session is a verified adult (straight away when it already was one) | `pass`, and the session carries `ageVerification` |
| the player could not prove they are an adult | `fail` |
| k-ID could not be reached or rate limited | `error` |

Composing `KIDSessionVerifyAge` again after a fail or error tries again with a new email.

```kotlin
KIDSessionVerifyAge(
    pass = { AdultsSpace() },
    pending = { Text("Check your email to verify your age.") },
    fail = { Text("We could not verify your age.") },
)
```
