# Changelog

## 1.0.0

First public release.

- Age gate with parent consent: `KIDAgeGate` collects the player's age and jurisdiction, and asks a parent for consent when the player needs it.
- Adults-only age verification: `KIDVerifyAge` verifies that the player is an adult on a product that only allows adults, and `KIDSessionVerifyAge` checks it later on any product.
- Permissions and settings: `KIDSettings` lets the player manage their permissions and ask a grownup to turn on more.
- Verify age for age-threshold features: a feature with a minimum age turns on only after the player verifies their age. `KIDSettings` sends them the check, and `upgrade()` does it from your own UI.
- Error codes: every failure is a `KIDError` with a fixed set of codes, including `RATE_LIMITED` with `retryAfter`, the seconds to wait before trying again.
