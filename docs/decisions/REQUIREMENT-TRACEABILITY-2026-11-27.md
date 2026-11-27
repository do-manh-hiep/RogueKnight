# Requirement traceability — Logic audit — 27/11/2026

- Repository: `do-manh-hiep/RogueKnight`
- Branch: `refactor/architecture-cleanup-7119`
- Source revision inspected: `6aa5a008f61621118f329a226443f6f858e5ecb8`
- Requirement source: `RogueKnight_TD_v0.1_Full.pdf`, section 2 and section 7.2.
- Scope: BR1–BR4 gameplay/logic requirements. UI-only requirements are marked separately and are not treated as proof of logic completion.
- Method: static source and test review. No test execution is claimed by this matrix.

## Status legend

- **Implemented / test present**: source implementation and corresponding test suite/test ID were found during static review. This does not mean Passed.
- **Partial / gap**: implementation exists but a known boundary, scenario, or acceptance condition is missing.
- **Not verified**: static evidence is insufficient to decide.
- **Out of logic scope**: requirement is UI/integration-specific and needs a separate core/desktop acceptance pass.

## 1. Requirement → Code → Test → Status

| Requirement | Code / responsibility found | TD test mapping | Static audit status |
|---|---|---|---|
| R-T01–T02 — initial Turn and invalid request leaves state unchanged | `BattleFactory`, `BattleController.submit`, `KnightRules`, `ActionResolver` | TC-T01–T02; BattleFactoryTest, BattleControllerTest | **Implemented / test present**. Invalid request returns rejected result before resolver; full state immutability is covered by existing controller/rule tests. Runtime pass not verified. |
| R-T03 — Player Action advances clock once only if battle continues | `BattleController.submit`, `endIfTerminal` | TC-T03; BattleControllerTest | **Implemented / test present**. Terminal result is checked before Turn+1. |
| R-T04–T05 — ordered due-enemy queue; each enemy reads latest state | `TurnScheduler`, `EnemyTurnRunner`, `EnemyRules` | TC-T04–T05; TurnSchedulerTest, EnemyTurnRunnerTest, EnemyTurnIntegrationTest | **Implemented / test present**. Runner builds due IDs in schedule order and reads state for each activation. |
| R-T06–T07 — terminal stops queue; no legal enemy action skips activation | `EnemyTurnRunner`, `BattleController.endIfTerminal` | TC-T06–T07; EnemyTurnRunnerTest, ResultEvaluatorTest | **Implemented / test present** for normal terminal/skip paths. Null decision is a separate gap (AUD-03), not a valid Skip. |
| R-B01 — 8×8 board and coordinate bounds | `Position`, `Board` | TC-B01; PositionTest, BoardTest | **Implemented / test present**. |
| R-B02 — one occupant per tile; capture is atomic at action level | `Board`, `BattleState`, `ActionResolver` | TC-B02; BoardTest, BattleStateTest, KnightMoveTest / equipment integration | **Implemented / test present**. Board rejects occupied/rock destinations; capture removes target before moving actor within resolver action. |
| R-B03–B04 — Rock and Piece blockers; Knight jump exception; line movement stops at first blocker | `Board`, `KnightRules`, `EnemyRules`, equipment rules | TC-B03–B04; BoardTest, KnightMoveTest, EnemyRulesTest, EquipmentActionIntegrationTest | **Implemented / test present** in reviewed paths. |
| R-B05 — Map01 exact baseline layout | `MapLoader`, `MapValidator`, `BattleFactory` | TC-B05; loader/validator/factory tests | **Implemented / test present** for setup data/validation. A separate visual/manual Map01 acceptance is not claimed. |
| R-K01 — Knight starts at HP 5 | `BattleFactory`, balance data | TC-K01; BattleFactoryTest / data tests | **Implemented / test present**. Restart-specific behavior belongs to lifecycle/UI integration and is not fully verified here. |
| R-K02–K03 — L-move and adjacent 8-way Move baseline | `KnightRules`, `ActionResolver` | TC-K02–K03; KnightMoveTest | **Implemented / test present**. Landing validation is distinct from intermediate blockers. |
| R-K04 — Sword Attack damage 1; Knight remains in place | `KnightRules`, `ActionResolver` | TC-K04; KnightMoveTest / action tests | **Implemented / test present**. |
| R-K05 — Capture requires eligible Enemy and HP threshold; Knight lands on target | `KnightRules`, `ActionResolver` | TC-K05; KnightMoveTest / action tests | **Implemented / test present**. |
| R-K06 — Wait is legal; Wait itself does not mutate board/equipment, lifecycle proceeds | `KnightRules`, `BattleController`, `ActionResolver` | TC-K06; BattleControllerTest, EnemyTurnIntegrationTest | **Implemented / test present**. |
| R-E01 — Enemy stats and cycle | `BattleFactory`, balance DTO/catalog, `TurnScheduler` | TC-E01; factory/data/scheduler tests | **Implemented / test present**. |
| R-E02–E03 — Attack if in range; otherwise Move then re-evaluate Attack from new state | `EnemyAI`, `GenericEnemyPolicy`, `EnemyTurnRunner`, `EnemyRules` | TC-E02–E03; GenericEnemyPolicyTest, EnemyTurnRunnerTest, EnemyTurnIntegrationTest | **Implemented / test present**. Runner resolves Move and checks terminal before subsequent Attack. |
| R-E04 — Rook/Bishop/King/Queen movement and attack geometry | `EnemyRules`, geometry table | TC-E04; EnemyRulesTest | **Implemented / test present**. |
| R-E05–E07 — AI priority, determinism, default Knight-only threat/capture evaluation | `GenericEnemyPolicy`, `PawnPolicy`, `EnemyAI`, `HypotheticalView` | TC-E05–E07; GenericEnemyPolicyTest and AI tests | **Implemented / test present** for reviewed deterministic policy and read-only evaluation. Policy acceptance remains subject to DEC-02 definition; no balance/policy change is proposed by this audit. |
| R-P01–P02 — Pawn initial direction and Attack-before-BFS behavior | `PawnPolicy`, `PawnPathfinder`, `PawnAttackRule` | TC-P01–P02; PawnPathfinderTest, PawnTurnTest | **Implemented / test present** for the chosen direction/path implementation. |
| R-P03–P06 — Pawn BFS/path selection, fallback, movement and attack behavior | `PawnPathfinder`, `PawnPolicy`, `EnemyRules` | TC-P03–P06; PawnPathfinderTest, PawnTurnTest | **Partial / decision-sensitive**. Core BFS/policy tests exist; confirm the no-path/fallback and edge semantics against the final approved DEC-01 record before calling every edge accepted. |
| R-P07–P08 — Promotion identity and Queen schedule | `PawnPromotionApplier`, `ActionResolver`, `TurnScheduler` | TC-P07–P08; PawnPromotionApplierTest, TurnSchedulerTest, PawnTurnTest, IT-05 | **Implemented / test present**. Existing tests cover identity retention and promotion schedule; no runtime pass asserted. |
| R-Q01–Q04 — Inventory/loadout, charge, invalid action, zero-charge behavior | `Inventory`, `BattleFactory`, `KnightRules`, `ActionResolver` | TC-Q01–Q04; InventoryTest, EquipmentActionIntegrationTest | **Implemented / test present**. Invalid request does not consume charge; valid equipment action consumes one charge. |
| R-Q05–Q08 — Cannon Movement screen, empty landing choices, Capture terminal enemy, second blocker | equipment rules / `KnightRules`, `ActionResolver` | TC-Q05–Q08; EquipmentActionIntegrationTest | **Implemented / test present** for reviewed screen/landing/capture edge cases. |
| R-Q09 — Cannon Attack requires exactly one screen and terminal Enemy | equipment rules / `KnightRules`, `ActionResolver` | TC-Q09; EquipmentActionIntegrationTest | **Implemented / test present**. |
| R-Q10–Q11 — Diagonal Skewer hits two consecutive tiles, passes Enemy, stops at Rock/edge | equipment rules / `KnightRules`, `ActionResolver` | TC-Q10–Q11; EquipmentActionIntegrationTest | **Implemented / test present** for reviewed multi-target and Rock/edge cases. DEC-03 governs the no-hit direction edge. |
| R-R01–R03 — Victory/Defeat and stop processing immediately | `ResultEvaluator`, `BattleController.endIfTerminal`, `EnemyTurnRunner` | TC-R01–R03; ResultEvaluatorTest, BattleControllerTest, EnemyTurnRunnerTest | **Implemented / test present** for gameplay terminal outcomes. Distinct runtime-failure outcome for DEC-12 is not implemented (AUD-02). |
| R-R04–R06 — Stuck warning, Continue, Accept Defeat | selection/stuck lifecycle and controller contracts | TC-R04–R06; lifecycle/controller tests, IT-06 | **Partial / integration gap**. Logic contracts and tests are present in reviewed test inventory; full UI path/desktop acceptance is not established by this logic audit. |
| R-U01–U03 — Screen flow, hover details, current attack range/cues | core screen/view layer | TC-U01–U03 | **Out of logic scope / not verified**. Requires core/desktop walkthrough; not inferable from logic tests. |
| R-U04–U06 — UI reads legal options and renders action/event cues | `BattleControllerAdapter`, `BoardInputHandler`, `BattlePresentationModel`, `BattlePlayback`, `BoardRenderer` | TC-U04–U06; adapter/input/presentation/playback tests | **Partial / test present**. Adapter/event playback path exists; complete screen/hover/animation acceptance is not claimed. |
| R-TECH01–02 — Pure-Java logic; shared legality between UI/AI/validation | Gradle module boundary, `StateView`, `KnightRules`, `EnemyRules`, `EnemyAI` | TC-X01–X02; logic tests, adapter/rule tests | **Implemented by structure / test evidence present**. Verify with clean CI/build and module dependency checks. |
| R-TECH03 — Single owner for clock/mutation | `BattleController`, `BattleState`, `Board`, `ActionResolver` | TC-X03; controller/state tests | **Partial — AUD-01 (P0)**. Runtime flow centralizes turn progression, but public mutation methods and public constructors/interfaces still permit bypassing the ownership boundary. |
| R-TECH04 — Playback does not control gameplay or accept duplicate input | `BattlePlayback`, `BattlePresentationModel`, `Main`, input handler | TC-X04 / FX-17; playback/input tests | **Implemented / test present** for deterministic event playback and input gating in the reviewed path. Manual desktop timing/responsiveness not verified. |
| R-TECH05 — Invalid config rejected before Battle; actionable setup error | `BalanceLoader`, `MapLoader`, validators, `BattleFactory`, `Main.create` | TC-X05; loader/validator/factory tests | **Partial — AUD-04 (P1)**. Validators reject invalid DTO data, but malformed Gson parse exceptions and the application setup-error route are not unified. |
| R-TECH06 — Task handoff ownership/dependency/acceptance | repository task/PR documentation | REV-X06 | **Not part of runtime logic**; project-process evidence is outside this source audit. |
| R-TECH07 — Adding a piece type does not scatter type switches | `PieceCatalog`, geometry/policy strategy structures | REV-X07 | **Partial / not fully verified**. Some geometry/policy selection is data/strategy-based; no full extensibility proof for every subsystem. |

## 2. Cross-cutting gaps and severity

| Finding | Severity | Affected requirements/contracts | Required follow-up |
|---|---|---|---|
| AUD-01 Public mutable state/Board APIs and construction/EnemyPhase entry points bypass intended ownership | P0 | R-TECH03, C-02, C-04, I01–I10 | Call-site/API-boundary design; enforce mutation capability; add compile/package boundary tests. |
| AUD-02 No application-level runtime-failure state/route for invalid AI request | P1 | DEC-12, C-04/C-06 lifecycle boundary | Catch at application boundary, preserve committed actions, stop queue/input/playback, expose runtime failure separately from Victory/Defeat. |
| AUD-03 Null/unknown EnemyDecision is treated as Skip | P1 | R-T07, R-E05, DEC-12 | Distinguish explicit Skip from Act; null/unknown decision must fail-stop; add tests. |
| AUD-04 Config parse/validation failures lack one setup-error boundary | P1 | R-TECH05 | Normalize parse/validation exception handling and test malformed JSON/bootstrap failure. |
| AUD-05 Stale comment in equipment integration test | P2 | Test maintenance only | Correct comment in a cleanup change; no gameplay change. |

## 3. Verification status — do not conflate test existence with pass

| Verification item | Evidence available | Status |
|---|---|---|
| Source review | Relevant logic and direct Main/core integration source inspected at branch head | **Reviewed statically** |
| Test mapping | TD section 7.2 IDs and corresponding test suites inventoried in audit | **Mapped; tests exist** |
| Test execution on revision `6aa5a008...` | No user-provided full test report/exit code for this exact revision in the audit record | **Pending** |
| Full Gradle build on revision `6aa5a008...` | No user-provided full build report/exit code for this exact revision in the audit record | **Pending** |
| Desktop manual playtest | No manual walkthrough evidence | **Not performed** |
| CI on this revision | Previous audit record found no workflow run/status evidence | **Not verified; re-check if CI is configured** |

## 4. Audit conclusion

The BR1–BR4 logic requirements now have a traceability map to source responsibilities and TD test IDs. The reviewed code has broad implementation and test coverage across Board, Knight actions, Enemy sequencing/AI, Pawn promotion, Equipment, and terminal evaluation.

This does **not** establish that every requirement has passed execution. The audit exit is:

- **Static requirement-to-code-to-test mapping: complete for the logic requirements listed above.**
- **Known implementation gaps: AUD-01 through AUD-04 remain open; AUD-05 is cleanup.**
- **Verification gate: pending exact-revision test/build output and desktop acceptance where applicable.**
- No gameplay rule, AI policy, or balance changes are authorized by this report.
- TD v0.1 remains unchanged. This file is supplemental audit documentation only.
