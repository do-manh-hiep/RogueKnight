# Logic audit — 28/11/2026

- **Repository:** do-manh-hiep/RogueKnight
- **Branch:** refactor/architecture-cleanup-7119
- **Audited source head before this pass:** 6256062899ddb931a4abd2b114a17e341957f68a
- **TD baseline:** RogueKnight TD v0.1 (unchanged)
- **Scope:** logic source/tests and direct core/Main integration relevant to battle lifecycle and DEC-12.
- **Method:** static source review at the audited head, mapped to TD requirement register and available test suites. Test execution status is reported separately.

> **Terminology note:** The supplied TD v0.1 does not define identifiers named BR1–BR4. It defines GDD design pillars P1–P4 and a requirement register using R-T, R-B, R-K, R-E, R-P, R-Q, R-R, R-U and R-TECH. This audit maps against those source identifiers rather than inventing BR IDs.

## 1. Executive conclusion

- **Day 1 — Codebase understanding:** substantially complete. The module diagram, reduced class inventory and Knight/Enemy flow traces are recorded in `docs/CODEBASE_OVERVIEW.md`.
- **Day 2 — Logic audit:** source-level review substantially complete, but acceptance is not fully closed. The matrix below distinguishes source implementation, test presence and execution evidence.
- **Build/test evidence:** user reported `.\gradlew.bat clean build` with `BUILD_EXIT_CODE=0` after syncing to the audited head `50877ca`. This is a user-run local result, not a GitHub Actions result.
- **Desktop smoke test:** not yet confirmed in this audit record.
- **Merge:** do not merge PR #17 until review/approval and the remaining DEC-12 integration gates are resolved.

No gameplay rule change is proposed by this audit. TD v0.1 remains unchanged.

## 2. Requirement → Code → Test → Status

Status meanings:
- **Pass (source + test present):** source responsibility and corresponding automated test suite were identified. The test suite's pass is only asserted where an execution result is recorded for the exact revision.
- **Partial:** implementation or integration evidence is incomplete, or a decision-sensitive edge remains.
- **Not verified:** requires manual/UI acceptance or evidence not available in this source audit.

| TD requirement | Code / responsibility | Test mapping | Status at 50877ca |
|---|---|---|---|
| P1 / R-T01–T03: initial Turn, invalid request atomicity, one Turn+1 after accepted non-terminal Player Action | `battle.BattleController`, `KnightRules`, `ActionResolver`, `BattleState` | `BattleControllerTest`, `KnightMoveTest`, `BattleStateTest`; TC-T01–T03 | **Pass — source/test present.** Invalid requests are rejected before resolution; controller owns turn advancement. Local clean build reported pass at this head. |
| R-T04–T07: due Enemy queue order, latest state per activation, terminal stop, explicit no-action skip | `TurnScheduler`, `EnemyTurnRunner`, `EnemyRules`, `battle.BattleController` | `TurnSchedulerTest`, `EnemyTurnRunnerTest`, `EnemyTurnIntegrationTest`; TC-T04–T07 | **Pass — source/test present.** Queue is snapshotted; each ID is re-read from current state; terminal checks stop the queue. |
| R-B01–B05: 8×8 bounds, one occupant, blockers, Knight jump, Map01 baseline | `Position`, package-private `battle.Board`, `BattleState`, `MapLoader`, `MapValidator` | `PositionTest`, `BoardTest`, `BattleStateTest`, loader/validator/factory tests; TC-B01–B05 | **Pass — source/test present.** Board occupancy and state consistency are enforced; Map01 has loader/validator/factory coverage. Visual Map01 acceptance remains a desktop check. |
| R-K01–K06: Knight baseline stats, Move, Sword, Capture, Wait | `KnightRules`, `ActionResolver`, `BattleFactory` | `KnightMoveTest`, `BattleControllerTest`, factory/data tests; TC-K01–K06 | **Pass — source/test present.** Geometry, target type/HP, Wait and action resolution are centralized in logic. |
| R-E01–E07: Enemy stats/cycle, Move/Attack, geometry, deterministic policy and threat/capture evaluation | `EnemyRules`, `EnemyAI`, `GenericEnemyPolicy`, `EnemyTurnRunner`, `HypotheticalView` | `EnemyRulesTest`, `GenericEnemyPolicyTest`, `EnemyTurnRunnerTest`, integration/AI tests; TC-E01–E07 | **Pass — source/test present, policy semantics decision-sensitive.** AI consumes read-only `StateView`; generic Move→Attack is evaluated on post-Move state. No policy/balance change proposed. |
| R-P01–P02: Pawn direction and Attack-before-BFS | `PawnPolicy`, `PawnPathfinder`, `PawnAttackRule` | `PawnPathfinderTest`, `PawnTurnTest`; TC-P01–P02 | **Pass — source/test present** for implemented direction/path behavior. |
| R-P03–P06: Pawn BFS, fallback, movement and attack edge semantics | `PawnPathfinder`, `PawnPolicy`, `EnemyRules` | `PawnPathfinderTest`, `PawnTurnTest`; TC-P03–P06 | **Partial / decision-sensitive.** Core behavior is tested; no-path/fallback edge semantics must be checked against the approved DEC-01 wording before claiming every edge accepted. |
| R-P07–P08: promotion identity and Queen activation schedule | `ActionResolver`, `TurnScheduler` | `TurnSchedulerTest`, `PawnTurnTest`; TC-P07–P08, IT-05 | **Pass — source/test present.** Promotion is part of the validated Pawn MOVE resolution; the integration test checks identity, spawnOrder, Queen stats, cleared direction and T+5 schedule. |
| R-Q01–Q04: loadout, one charge, valid/invalid consumption, exhausted equipment | `Inventory`, `KnightRules`, `ActionResolver`, `BattleFactory` | `InventoryTest`, `EquipmentActionIntegrationTest`; TC-Q01–Q04 | **Pass — source/test present.** Invalid actions do not consume charge; valid equipment action consumes one charge; exhausted equipment remains in inventory but is unusable. |
| R-Q05–Q08: Cannon Movement screen/landing/capture/blocker rules | `KnightRules`, `ActionResolver` | `EquipmentActionIntegrationTest`; TC-Q05–Q08, FX-04 | **Pass — source/test present.** Includes empty landing choices before terminal Enemy and capture as an additional option. |
| R-Q09: Cannon Attack exactly one screen and first terminal Enemy | `KnightRules`, `ActionResolver` | `EquipmentActionIntegrationTest`; TC-Q09 | **Pass — source/test present.** |
| R-Q10–Q11: Diagonal Skewer two cells, Enemy continuation, Rock/edge behavior | `KnightRules`, `ActionResolver` | `EquipmentActionIntegrationTest`; TC-Q10–Q11 | **Pass — source/test present.** No-hit direction follows DEC-03. |
| R-R01–R03: Victory/Defeat and immediate stop | `battle.ResultEvaluator`, `BattleController.endIfTerminal`, `EnemyTurnRunner` | `ResultEvaluatorTest`, `BattleControllerTest`, `EnemyTurnRunnerTest`; TC-R01–R03 | **Pass — gameplay terminal path.** Runtime failure remains a separate DEC-12 state and must not be represented as Victory/Defeat. |
| R-R04–R06: Stuck warning, Continue, Accept Defeat | selection/stuck lifecycle and controller contracts | lifecycle/controller tests; TC-R04–R06, IT-06 | **Partial / UI integration not verified.** Logic contract/test inventory exists; full desktop flow is not established here. |
| R-U01–U03: screen flow, hover details, attack range/cues | core screen/view layer | TC-U01–U03 | **Not verified in logic audit.** Requires UI/desktop walkthrough. |
| R-U04–U06: UI reads legal options and consumes snapshots/events | `BattleControllerAdapter`, `BoardInputHandler`, `BattlePresentationModel`, `BattlePlayback`, `BoardRenderer` | adapter/input/presentation/playback tests; TC-U04–U06 | **Partial.** Adapter/event playback exists; full hover/animation/screen acceptance remains outside this source review. |
| R-TECH01–02: pure-Java logic and shared legality between UI/AI | Gradle module boundary, `StateView`, `KnightRules`, `EnemyRules`, AI policies | logic and adapter/rule tests; TC-X01–X02 | **Pass by structure / test evidence present.** User's full Gradle build at 50877ca passed locally. |
| R-TECH03: single owner for state mutation and turn clock | package-private `BattleState`/`Board`, internal battle controller/resolver, public `battle.BattleController` entry point | `BattleStateTest`, `BattleControllerTest`; TC-X03 | **Addressed at public API boundary.** Mutable aggregate and Board are package-private; public `battle.BattleController` is created from immutable `BattleSnapshot` + read-only `EnemyPolicy` and does not expose `BattleState`. A dedicated external-package compile/API test would strengthen regression protection. |
| R-TECH04: playback cannot change gameplay and blocks duplicate input | `BattlePlayback`, `BattlePresentationModel`, `Main`, input handler | playback/input tests; TC-X04 / FX-17 | **Pass — source/test present; desktop timing not verified here.** |
| R-TECH05: invalid config rejected before Battle with actionable setup error | `BalanceLoader`, `MapLoader`, validators, `BattleFactory`, `Main.create` | loader/validator/factory tests; TC-X05, FX-15 | **Partial.** Loaders wrap parse/validation errors with resource context and Main catches IOException/IllegalArgumentException, but startup still escalates to IllegalStateException rather than a user-facing setup-error/recovery path. |
| R-TECH06–07: handoff process and extension-point extensibility | project process docs; `PieceCatalog`/geometry/policy strategies | REV-X06–X07 | **Not fully verified by runtime audit.** Process evidence and whole-system extensibility review are separate tasks. |

## 3. Architecture / ownership findings

### AUD-01 — Public mutable aggregate boundary (P0, DEC-04)

**Current result: addressed at the public API boundary by source inspection.**

Evidence at 50877ca:
- `BattleState` is package-private; constructor and mutators are package-private.
- `battle.Board` is package-private and only reachable inside the battle package.
- Public `action.BattleController` is a facade created from immutable `BattleSnapshot` and `EnemyPolicy`; it does not accept or return `BattleState`.
- `EnemyPolicy.choose` receives `StateView`, not mutable aggregate.
- `ActionResolver` and `ActionPlan` are internal to the battle package.

Residual: add a compile-time/API boundary regression test from a non-battle package if the team wants explicit proof that public consumers cannot regain mutation access accidentally. This is hardening, not evidence of a current public bypass.

### AUD-02 — DEC-12 application fail-stop and recovery (P1)

**Current result: partially implemented.**

Evidence:
- `EnemyTurnRunner` throws for invalid AI request and null/unsupported decision; it does not retry or silently skip.
- `Main.render` catches `RuntimeException`, stores a runtime failure message, calls `playback.stop()`, logs the exception and renders a runtime-failure overlay.
- The application battle loop stops; already committed valid actions are not rolled back.
- There is no menu/restart route from the runtime-failure overlay in the inspected Main flow.

Remaining: provide a user-controlled route out of failure (menu/restart) and integration coverage proving gameplay input/playback remain stopped and runtime failure is distinct from Victory/Defeat.

### AUD-03 — Null EnemyDecision treated as Skip (P1)

**Current result: fixed in source.**

Evidence:
- Explicit `EnemyDecision.Skip` returns the valid no-action path.
- Null or unsupported decision throws `IllegalStateException` with DEC-12 context.
- Invalid Enemy request rejected by `EnemyRules.validate` throws and stops the enemy phase.

### AUD-04 — Setup/configuration error boundary (P1)

**Current result: partially implemented.**

Evidence:
- `BalanceLoader` and `MapLoader` convert parse/validation failures into resource-context `IOException`.
- `Main.create` catches `IOException | IllegalArgumentException` and wraps them in `IllegalStateException`.
- No user-facing setup-error screen or recovery route was identified.

Remaining: unify malformed JSON/null/missing field/invalid range/map setup handling at the application boundary and show a useful setup error without constructing a partial Battle.

### AUD-05 — Stale equipment test comment (P2)

**Current result: not rechecked as a separate cleanup item in this pass.** No gameplay behavior change is justified by the finding; clean up only if the comment is still present.

## 4. Verification ledger

| Item | Evidence | Status |
|---|---|---|
| Static source review | Current source files inspected at 50877ca, including BattleState, Board, public controller facade, EnemyTurnRunner, EnemyDecision, EnemyPolicy, KnightRules, EnemyRules, loaders and Main | **Reviewed** |
| Test mapping | TD IDs mapped to named test suites in section 2 | **Mapped; presence does not imply pass** |
| Logic/full build | User ran `.\gradlew.bat clean build` after syncing to 50877ca; `BUILD_EXIT_CODE=0` | **Passed locally on 50877ca (user-reported)** |
| GitHub Actions / commit status | No CI success evidence is asserted by this document | **Not verified** |
| Desktop smoke test | No completed walkthrough result recorded for this audit pass | **Not verified** |
| PR review approval | PR #17 remains open; no approval recorded in the checked metadata | **Not approved / not merge-ready** |

## 5. Severity-ranked remaining work

| Priority | Item | Reason / exit condition |
|---|---|---|
| P1 | DEC-12 runtime recovery | Add menu/restart route and integration tests for runtime failure; prove input/playback stay stopped and no false Victory/Defeat is emitted. |
| P1 | Setup error UX | Present actionable startup error for malformed/invalid data; test Main/bootstrap boundary. |
| P1 | Desktop smoke test | Run Map01 and check Move/Attack/Wait, Enemy activation, playback lock/speed, Knight death HUD and terminal rendering. Record revision and observed result. |
| P2 | Requirement edge closure | Resolve DEC-01 no-path/fallback acceptance against approved decision; verify Stuck UI and R-U flows manually. |
| P2 | API-boundary regression test | Add a compile/package boundary test from an external package if feasible. |
| P2 | AUD-05 comment cleanup | Recheck and update stale test comment only if still present. |
| P2 | CI/review | Obtain CI evidence if configured and reviewer approval before merge. |

## 6. Merge gate

PR #17 must remain **Open / not merged** until:
1. Reviewer approval is obtained.
2. DEC-12 runtime recovery and setup-error expectations are either implemented or explicitly accepted as deferred by the reviewer with follow-up ownership.
3. Desktop smoke test is recorded against the current head.
4. The exact current head is re-verified by local build or CI; do not reuse a pass from an older commit.
5. PR description and audit status match the actual current head and outstanding work.

GitHub `mergeable=true` means no merge conflict was detected; it is not review approval or a quality gate.

## 7. Source files inspected at 50877ca

- `logic/src/main/java/rogueknight/battle/BattleState.java`
- `logic/src/main/java/rogueknight/battle/Board.java`
- `logic/src/main/java/rogueknight/action/BattleController.java`
- `logic/src/main/java/rogueknight/battle/EnemyTurnRunner.java`
- `logic/src/main/java/rogueknight/action/EnemyDecision.java`
- `logic/src/main/java/rogueknight/action/EnemyPolicy.java`
- `logic/src/main/java/rogueknight/battle/KnightRules.java`
- `logic/src/main/java/rogueknight/battle/EnemyRules.java`
- `logic/src/main/java/rogueknight/battle/TurnScheduler.java`
- `logic/src/main/java/rogueknight/data/BalanceLoader.java`
- `logic/src/main/java/rogueknight/data/MapLoader.java`
- `core/src/main/java/rogueknight/Main.java`
- Corresponding test suites listed in section 2.


## 5. Simplification pass — audited after 7906fa1

The following changes are present at audited head `5e4bc78` (source simplification commits after `7906fa1`):

- Removed the unused `action.BattleController` forwarding facade. Production bootstrap and core adapter use `battle.BattleController`; tests in the current branch use the battle package entry point.
- Removed `PawnAttackRule.canAttackKnight`, which only validated the Pawn and forwarded to `EnemyRules.canAttackKnight`. Pawn attack geometry remains in `PawnAttackRule.attackTargets`; the corresponding test assertions now target `EnemyRules`.
- Moved `ResultEvaluator` into `battle` and made the class/method package-private. It is part of battle terminal evaluation and is called by the battle controller.
- Removed unused `BoardPosition.chebyshevDistance` and `BoardPosition.manhattanDistance`; no production consumer was found in the core source.
- Updated the `FixtureBattleController` test-fixture comment to describe its current role.

Deferred candidates:
- `PieceCatalog` / `PieceSpec`: retained in this pass because they have dedicated tests and are the validated conversion point from balance DTOs to factory-ready stats. Reconsider only with a replacement that preserves validation and null semantics.
- `BattleEventQueue`: retained because it has independent tests and makes FIFO batch semantics explicit.
- `BattleViewSnapshot`: no fields removed in this pass. The suspected redundant fields need a complete consumer audit and coordinated adapter/input/test migration before changing its contract.

**Verification:** this pass was applied through GitHub branch commits. Gradle tests, clean build, `git diff --check`, and desktop smoke tests have not been run against `5e4bc78` by the assistant. Previous user-reported results apply to the earlier baseline only and must not be carried forward as verification of this head.


## 6. Simplification pass — canonical presentation snapshot

At the simplification head following `1bfb3a7`:

- Reduced `BattleViewSnapshot` to five fields: `globalTurn`, `knightPosition`, `rocks`, `legalMoves`, and `piecesById`.
- Removed `knightAlive`: alive/dead is derived from the canonical presentation-piece collection. When the Knight is defeated and removed, its last known square remains available separately for the fallen-piece marker.
- Removed `enemyPieces`: enemy occupancy is derived from `piecesById` through `hasEnemyAt`; both the input adapter and click-feedback path use that query.
- Removed `statusMessage` and `withMessage`: status text was presentation formatting duplicated from global turn, Knight state and battle result. The renderer already reads turn/result and piece HP from `BattlePresentationModel`; input feedback remains in `BoardInputHandler`.
- Updated the production adapter, input handler, fixture controller and Knight-death tests to use the reduced contract.

This keeps one canonical piece collection at the UI boundary and avoids storing a second enemy-position index or preformatted HUD string in every snapshot. `knightPosition` remains because the UI must retain the Knight's last square after its removal.

**Verification status:** source changes were committed to the PR branch. They have not yet been run through Gradle on the user's machine. Re-run `.\gradlew.bat :logic:test`, `.\gradlew.bat test`, `.\gradlew.bat clean build`, and `git diff --check` against the updated branch head before treating the pass as verified.


## 7. Simplification pass — validated data and playback queue

At base head `6256062899ddb931a4abd2b114a17e341957f68a`, the following redundant layers were identified and removed:

- Removed `PieceSpec`: it copied the five fields from `PieceStatsData` without adding validation, defaults, invariants, or behavior.
- Removed `PieceCatalog`: the only production caller was `BattleFactory`, which loaded balance through `BalanceLoader`. That loader already runs `BalanceValidator`; the catalog repeated the same validation and copied each DTO into a second map. `BattleFactory` now reads the validated immutable `PieceStatsData` directly from `BalanceData`.
- Removed `BattleEventQueue`: it had exactly one production consumer, `BattlePlayback`, and only wrapped `ArrayDeque` operations. `BattlePlayback` now owns the deque directly.
- Preserved queue guarantees: `enqueue` copies the entire input with `List.copyOf` before appending, so a null-containing batch fails without partially mutating the pending event stream. FIFO order, playback timing, instant mode, stop/clear behavior and pending-count reporting remain unchanged.
- Migrated the null-batch atomicity regression test into `BattlePlaybackTest`; the existing playback sequence tests continue to cover event order, skipped activations and terminal events.
- `MapLoader` validates piece types and `BalanceLoader` validates required balance entries/stat ranges before `BattleFactory` consumes either DTO. No validation rule was removed.

**Verification:** source and test changes are committed in this pass. The user should rerun `.\\gradlew.bat :logic:test`, `.\\gradlew.bat test`, `.\\gradlew.bat clean build`, and `git diff --check` against the resulting branch head. No new Gradle result is asserted here.

## 8. Simplification pass — reuse canonical coordinate/direction utilities

At head `4ac9544`, `PawnDirectionResolver` duplicated two existing domain utilities:

- Its private `isInside(column, row)` repeated the bounds check already owned by `Position.isInBounds`.
- Its private `directionOf(dc, dr)` repeated the exact delta lookup already provided by `Direction.fromDelta`.

The resolver now calls those canonical methods and removes both private helpers. BFS traversal, goal selection, shortest-path comparison, tie-breaking order (straight SOUTH first, then column/row scan order), and fallback to SOUTH are unchanged. This is a mechanical deduplication; no gameplay rule or data contract changes.

**Verification boundary:** this source/doc change is committed to the PR branch. Run the local Gradle checks listed in the handoff message against the new head before treating this pass as verified.

## 9. Simplification pass — canonical map validation types and coordinates

At head `17f6d61`, `MapValidator` duplicated two existing domain definitions:

- The accepted piece-type string set repeated the constants in `PieceType`.
- The coordinate bounds check and string-encoded occupied-cell key repeated the semantics already represented by `Position`.

The validator now checks type names with `PieceType.valueOf`, uses `Position.BOARD_SIZE` / `Position.isInBounds`, and tracks occupied cells as `Set<Position>`. Existing resource validation remains in place: null fields, invalid dimensions, duplicate occupancy, duplicate IDs, Knight identity, required Knight/King, and spawn order are still rejected with the existing contextual messages. The invalid-type error text is preserved.

This removes parallel representations without moving validation out of the loader boundary or changing the JSON DTO shape.

**Verification boundary:** committed to the PR branch; local Gradle verification is required against the new head.

## 10. Simplification pass — inherit StateView.isEmpty

At head `e9f2a6e`, `BattleState.isEmpty(Position)` only forwarded to `Board.isEmpty(Position)`, while `StateView` already provides the exact same operation as a default method: no rock and no piece at the position.

Removed the redundant `BattleState` override. Calls through `StateView` now use the interface default, which delegates to the same `BattleState.isRock` and `BattleState.pieceAt` methods. `Board.isEmpty` remains because it is a direct Board operation and is covered by Board tests. No behavior or null handling changes.

**Verification boundary:** committed to the PR branch; run the local Gradle checks against the new head.

## 11. Simplification pass — keep parsed map piece type canonical

At head `4907c6d`, `MapValidator` parsed each piece's type string with `PieceType.valueOf` only to validate it, then continued comparing the original string against `"KNIGHT"` and `"KING"`.

The validator now retains the parsed `PieceType` and uses enum comparisons for Knight identity checks and required Knight/King detection. The original string remains only for the contextual invalid-type error message. This removes parallel string-based type checks while preserving the exact accepted enum names and validation order.

No DTO, JSON, or gameplay behavior changes. Existing tests cover invalid type names, Knight ID/type mismatch, and missing required piece types.

**Verification boundary:** committed to the PR branch; run the local Gradle checks against the new head.

## 12. Simplification batch — remove redundant snapshot round-trip and unify lethal damage

At head `6bb9b1b`, two independent simplification opportunities were confirmed:

- `BattleController.fromSnapshot` first reconstructed a mutable `BattleState`, then passed it to the defensive-copy constructor, which immediately serialized it back to a snapshot and reconstructed it again. A dedicated private snapshot constructor now performs the single required snapshot-to-state conversion. The package-private constructor that accepts an existing mutable `BattleState` still takes a defensive copy, so callers cannot retain a mutable alias.
- `ActionResolver` had two special-case implementations for lethal damage: `CAPTURE` and the capture branch of `CANNON_MOVEMENT` manually removed the target and emitted damage/removal events. Both now use the existing `applyDamage(..., 2)` path. Their validators guarantee the target has at most 2 HP, so the same target removal and event ordering are preserved.

No rule, public API, phase/turn lifecycle, or snapshot immutability contract changes. Existing controller and equipment integration tests cover defensive ownership, capture, and Cannon Movement.

**Verification boundary:** committed to the PR branch; local Gradle verification is required against this batch head.

## 13. Architecture simplification batch — controller and package ownership

At head `33afa65`:
- Core input contract now exposes one targeted submission method (`submitAction`) and one targetless method (`submitWait`). Removed `submitMove` forwarding methods and permissive default implementations that previously hid incomplete test adapters.
- Renamed core `MoveResult` to `UiActionResult`; it covers every UI action, not just movement.
- Renamed the limited test double `FixtureBattleController` to `StubBattleController` and removed its self-test. The stub remains only to isolate input-handler behavior; actual gameplay legality is tested in logic.
- Moved Pawn attack geometry to `battle.PawnAttackRule` and Pawn BFS path selection to `ai.PawnPathfinder`.
- Retained distinct controller, adapter, policy, decision, plan, validation outcome, scheduler and runner responsibilities. No subsystem merge met the evidence threshold.

**Verification boundary:** the branch contains the full source/test migration; local Gradle verification is required against the latest batch head.

## 14. Simplification pass — consolidate adapter result conversion and target mapping

At head `89145cf`:
- `BattleControllerAdapter.submitAction` and `submitWait` duplicated the same accepted/rejected `ActionResult` conversion, Knight-removal bookkeeping, and snapshot conversion. Both now delegate to one private `toUiResult` method.
- `legalTargets(..., MOVE)` repeated the domain Move-target conversion already implemented by `legalMoves`. It now reuses `legalMoves(actorId)` and adds the separately validated Sword targets.
- Removed `BoardProjection.isInsideBoard`, an unused boolean wrapper around `worldToBoard(...).isPresent()`. No production or test caller existed; `worldToBoard` remains the single coordinate-hit-test API.

No action validation, event order, snapshot content, or board coordinate behavior is intentionally changed.

**Verification boundary:** committed to the PR branch; run local Gradle checks against the new head.

## 15. Simplification batch — one canonical board coordinate type

At head `4d6c1b1`, the UI module's `BoardPosition` duplicated the immutable 8×8 `logic.board.Position` value type. That duplication required adapter and event-to-presentation coordinate copies, separate bounds logic, and a second notation API.

- Replaced `BoardPosition` throughout core input, snapshots, renderer, projection, presentation pieces, and core tests with `rogueknight.board.Position`.
- Removed `core.view.BoardPosition` and its duplicate `BoardPositionTest`; `logic.board.PositionTest` remains the canonical coordinate test suite.
- `BoardProjection` checks `Position.isInBounds` before constructing the validated position.
- Removed the now-identity coordinate conversion helpers in the adapter and presentation model.
- Core retains the UI-facing adapter and presentation model; logic remains pure Java and has no dependency on core/libGDX.

This is a substantive type/layer deletion, not a rename-only change. No gameplay rules or board coordinate conventions are changed.

## 16. Remove redundant board/query and AI result wrappers

- `PawnPathfinder.findNextMove` returns `Optional<Position>` directly. Its `NextMove` record carried a direction that production never consumed; the resolver derives the step direction from the action's from/to positions.
- Removed `Board.isEmpty`: production state queries use the canonical `StateView.isEmpty` default, composed from `isRock` and `pieceAt`. Board remains responsible for storage and occupancy invariants.
- Removed `Board.positionsCopy`, which had no production caller and existed only to test a redundant copy accessor. Kept `pieceIds`, used by BattleState's aggregate consistency check.
- Removed the backward-compatible `BattleController.legalMoves` forwarding method. Callers use `legalTargets(actorId, ActionKind.MOVE)`; the UI port retains its presentation-level move-highlight method.
- These changes remove redundant API surface without combining the scheduler, activation runner, policy, rules, or resolver responsibilities.

## 17. Collapse duplicate target data in ActionPlan

`ActionPlan` previously stored overlapping target data in `targetId`, `landingPosition`, and `affectedTargetIds`. It now stores the action target coordinate and the ordered affected-ID list plus optional equipment type. Single-target actions populate a one-element affected-ID list; multi-target Skewer retains its ordered list; Cannon Movement uses the action target as its landing square. This removes duplicated state while preserving target ordering and action resolution semantics.


## 18. Simplification — inline Pawn promotion into ActionResolver

At head `2a2200e`, `PawnPromotionApplier` had exactly one production caller: `ActionResolver.applyPawnStepEffects`. It exposed no independent lifecycle, injection point or reusable behavior. Its eligibility/type/position checks repeated facts guaranteed by the validated MOVE plan and the resolver's just-completed move.

- Moved the promotion mutation and `PiecePromoted` event creation into `ActionResolver`, where TD §4.1 and §6.4 assign action effects and promotion.
- Avoided replacing the Pawn with an intermediate direction-updated state before immediately replacing it with the Queen.
- Kept Queen HP/cycle baseline, original ID and spawnOrder, null direction, and first activation at current Turn + 5.
- Removed `PawnPromotionApplier` and its helper-level test. Expanded `PawnTurnTest` to assert the preserved spawnOrder and cleared direction in the real Enemy-turn integration flow; it already checks the promotion event, Queen stats and T+5/T+9 schedule.
- No package or domain terminology was renamed; no gameplay rule or TD decision was changed.

**Verification boundary:** source/test/doc changes are committed together. Local Gradle verification is required against the new head.
