# DEC-04 implementation note — package boundary for mutable battle state

- **Status:** Implementation direction accepted for the AUD-01 workstream; code changes remain subject to compile/test verification.
- **Parent decision:** DEC-04 — State ownership và contract request/result/options.
- **Source constraints:** RogueKnight TD v0.1 §§4.7, 6.1, 6.3–6.4; DEC-04.
- **Scope:** Enforce the existing ownership contract in Java visibility and API shape. No gameplay-rule, AI-policy, event-order, scheduler, or presentation behavior changes.

## Chosen implementation direction

Use a package boundary and a public facade; do not introduce JPMS solely for this refactor.

1. Keep the mutable aggregate implementation in one Java package, `rogueknight.battle`.
   - `BattleState` becomes package-private; its constructor and mutation methods are package-private.
   - `Board` becomes package-private and is placed in the same package; its constructor and mutation methods are package-private.
   - Mutation collaborators that require direct state access (`ActionResolver`, `TurnScheduler`, `PawnPromotionApplier`, enemy-turn execution, terminal evaluation, and internal validation/plan types) are placed in this package and are not public API.
2. `BattleController` is the public facade. Its public construction path accepts only immutable battle setup data and a read-only `EnemyPolicy`; it never accepts or returns `BattleState`.
3. `BattleFactory` remains the data/bootstrap entry point. It loads and validates data, builds immutable setup input, and returns a `BattleController`.
4. `EnemyPolicy` continues to receive `StateView` and return `EnemyDecision`. The public `EnemyPhase.run(BattleState)` callback is removed from the public extension surface; the concrete enemy runner and phase seam remain internal.
5. `BattleSnapshot`, `StateView`, `ActionRequest`, `ActionResult`, `BattleEvent`, legal options, and error codes remain read-only/public contracts. `ActionPlan` and validation outcomes remain internal to the validate→resolve call.
6. Tests that verify aggregate invariants may live in the aggregate package. Tests in `core` must use the public factory/controller API and must not construct, retain, or mutate `BattleState`.

## Why this direction

- It implements TD §6.3's explicit package/facade option without requiring a Java module-path migration across `logic`, `core`, and `lwjgl3`.
- Package-private mutation is enforceable because the aggregate and the mutation collaborators that need it share one package.
- `core` receives only the controller facade and immutable setup/request/result/snapshot contracts.
- AI remains on `StateView`; no mutable aggregate is passed to policy callbacks.
- This is a boundary refactor, not a rewrite of gameplay logic.

## Migration order

1. Move `BattleState`, `Board`, and mutation collaborators into the aggregate package boundary; reduce visibility.
2. Change `BattleController` construction to accept immutable setup/policy inputs only; make its mutable-state constructor internal.
3. Change `BattleFactory` and `core` integration so desktop code no longer creates `EnemyTurnRunner` or sees `BattleState`.
4. Migrate direct-mutation tests into package-local logic fixtures; migrate core tests to public contracts.
5. Add boundary tests for inaccessible mutators, controller-only action submission, read-only AI input, and detached snapshots.

## Acceptance checks

- [ ] `core` has no import, constructor call, field, parameter, or return type involving `BattleState` or mutable `Board`.
- [ ] No public method in the gameplay API exposes `BattleState`, mutable `Board`, `ActionPlan`, or a mutation callback.
- [ ] Only the controller's internal flow can advance phase/turn/result or invoke the resolver.
- [ ] AI/policies receive `StateView` only and cannot mutate the aggregate.
- [ ] Invalid requests still leave state and mutation events unchanged.
- [ ] Existing gameplay and event semantics are unchanged.
- [ ] `:logic:test` and full `build` pass; desktop integration is manually checked.

## Explicit non-goals

- Do not edit TD v0.1.
- Do not add JPMS/module descriptors as part of this task.
- Do not change gameplay rules, AI ranking, action/event ordering, scheduler semantics, or playback behavior.
- Do not merge PR #17 as part of this implementation note.
