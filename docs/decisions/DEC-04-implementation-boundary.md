# DEC-04 implementation note — package boundary for mutable battle state

- **Status:** Implementation direction accepted for the AUD-01 workstream; code changes remain subject to compile/test verification.
- **Parent decision:** DEC-04 — State ownership và contract request/result/options.
- **Source constraints:** RogueKnight TD v0.1 §§4.7, 6.1, 6.3–6.4; DEC-04.
- **Scope:** Enforce the existing ownership contract in Java visibility and API shape. No gameplay-rule, AI-policy, event-order, scheduler, or presentation behavior changes.

## Chosen implementation direction

Use a package boundary and a public facade; do not introduce JPMS solely for this refactor.

1. Keep the mutable aggregate implementation in one Java package, `rogueknight.battle`.
   - `BattleState` becomes package-private; its constructor and mutation methods are package-private.
   - `Board` moves from `rogueknight.board` into `rogueknight.battle`, becomes package-private, and exposes no public mutation methods. `Position` remains a public immutable value in `rogueknight.board`.
   - Mutation collaborators that require direct state access (`ActionResolver`, `TurnScheduler`, `PawnPromotionApplier`, enemy-turn execution, terminal evaluation, and internal validation/plan types) move into `rogueknight.battle` and are package-private. Read-only rule queries may remain in `rogueknight.action` only if they accept `StateView` rather than `BattleState`.
2. `BattleController` moves into `rogueknight.battle` and is the public facade. Its public construction path accepts only immutable battle setup data and a read-only `EnemyPolicy`; it never accepts or returns `BattleState`. Its internal constructor may accept the package-private aggregate for package-local tests.
3. `BattleFactory` remains the data/bootstrap entry point. It loads and validates data, builds immutable setup input, and returns a `BattleController`; it does not expose a mutable state-building method.
4. `EnemyPolicy` continues to receive `StateView` and return `EnemyDecision`. The public `EnemyPhase.run(BattleState)` callback is removed. `BattleController` owns the internal enemy-turn runner and receives only an `EnemyPolicy` (which sees `StateView` and returns an immutable `EnemyDecision`).
5. `BattleSnapshot`, `StateView`, `ActionRequest`, `ActionResult`, `BattleEvent`, legal options, and error codes remain read-only/public contracts. `ActionPlan` and validation outcomes remain internal to the validate→resolve call.
6. Tests that verify aggregate invariants may live in `rogueknight.battle`. Logic integration tests outside that package use a package-local test fixture or public factory/controller API. Tests in `core` must use the public factory/controller API and must not construct, retain, or mutate `BattleState`.

## Why this direction

- It implements TD §6.3's explicit package/facade option without requiring a Java module-path migration across `logic`, `core`, and `lwjgl3`.
- Package-private mutation is enforceable because the aggregate and the mutation collaborators that need it share one package.
- `core` receives only the controller facade and immutable setup/request/result/snapshot contracts.
- AI remains on `StateView`; no mutable aggregate is passed to policy callbacks.
- This is a boundary refactor, not a rewrite of gameplay logic.

## Migration order

1. Move `BattleState`, `Board`, `BattleController`, and mutation collaborators into `rogueknight.battle`; reduce visibility. Keep public immutable request/result/event types in their contract packages.
2. Change `BattleController` construction to accept immutable setup/policy inputs only; keep the mutable-state constructor package-private.
3. Change `BattleFactory` and `core` integration so desktop code receives a controller and no longer creates `EnemyTurnRunner` or sees `BattleState`.
4. Migrate direct-mutation tests into package-local logic fixtures; migrate core tests to public contracts.
5. Add boundary tests for inaccessible mutators, controller-only action submission, read-only AI input, and detached snapshots.

## Acceptance checks

- [ ] `core` and `rogueknight.data` have no import, constructor call, field, parameter, or return type involving `BattleState` or mutable `Board`.
- [ ] No public method in the gameplay API exposes `BattleState`, mutable `Board`, `ActionPlan`, or a mutation callback.
- [ ] Only the controller's internal flow can advance phase/turn/result or invoke the resolver; the public controller API accepts requests and returns immutable results/snapshots.
- [ ] AI/policies receive `StateView` only and cannot mutate the aggregate.
- [ ] Invalid requests still leave state and mutation events unchanged.
- [ ] Existing gameplay and event semantics are unchanged.
- [ ] `:logic:test` and full `build` pass; desktop integration is manually checked.

## Explicit non-goals

- Do not edit TD v0.1.
- Do not add JPMS/module descriptors as part of this task.
- Do not change gameplay rules, AI ranking, action/event ordering, scheduler semantics, or playback behavior.
- Do not merge PR #17 as part of this implementation note.
