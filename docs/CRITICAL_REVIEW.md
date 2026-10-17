# Subscribly — Critical Architectural, Secret Safety & Disaster Recovery Review

**Document Version:** 5.0.0 (Pre-Execution Audit & Rigorous Verification Edition)  
**Audit Date:** 2026-10-17  
**Auditor:** Principal Backend Engineer, Software Architect, AppSec & SRE  
**Scope:** Critical evaluation of `PROJECT_AUDIT.md`, `IMPLEMENTATION_PLAN.md`, `ARCHITECTURE.md`, `TEST_STRATEGY.md`, and `DECISIONS/001-modular-monolith-vs-microservices.md`.  
**Repository:** [https://github.com/himanshukumar8/subscribly](https://github.com/himanshukumar8/subscribly)  
**Verified Commit:** `f1353e1` (HEAD of both `master` and `microservices-v2`)  
**Execution Status:** NOT EXECUTED (Read-Only Planning Phase)  

---

## 1. Executive Verdict & Core Safety Principles

The Phase Zero documentation correctly identified that Lombok 1.18.36 is incompatible with JDK 25 internal `javac` APIs and demonstrated that the multi-module project compiles cleanly under JDK 21.

However, a rigorous technical review of the audit and planning documents reveals **fundamental operational misconceptions, Git preservation risks, secret exposure vectors, hidden test dependencies, broken database constraints, and unverified architectural claims**:

1. **The Branch Switching Fallacy & Original Monolith Commit:** The documents assume that switching branches to `master` automatically restores the original monolith. In reality, **commit `f1353e1` is the exact commit containing the original monolith**, and both `master` and `microservices-v2` point to this identical commit. Switching branches via `git checkout master` shifts `HEAD` without touching the working tree, leaving all 32 deleted files, the modified `pom.xml`, and 15 untracked directories completely unresolved.
2. **False Default Branch Assumption:** The planning documents repeatedly refer to `main` as the production target. In reality, `git branch -a` confirms that **no branch named `main` exists**. The upstream default branch is `master` (`origin/master`).
3. **The Anti-Pattern of Deleting Backups on Rollback:** Previous drafts proposed deleting the external backup as a "rollback" step if a subsequent step failed. A backup must **never be deleted** during a rollback. Backups must be retained as immutable safety nets until successful recovery is independently verified. Rollback must be strictly distinguished from eventual backup cleanup.
4. **Complete Cryptographic Parity, Dual-Manifest Verification, Preflight Space Guard & Strict Quiescence Required:** Claiming that `robocopy /L /MIR`, two-file hash sampling, or simple post-copy hash comparison proves backup integrity or source consistency is false. A post-copy comparison alone cannot prove source consistency throughout copying and verification, because source files could be modified during copy operations without detection. A true, resilient verification procedure must enforce:
   - **Executable Preflight Space Guard:** Drive `D:` must possess a minimum of 2 GB available free space, verified programmatically before creating any backup parent directories, logs, or manifest artifacts; failure to query or `< 2 GB` triggers immediate fatal abort.
   - **Mandatory Quiescence Requirement:** The repository must remain strictly quiescent (zero write operations from any process) throughout the entire backup and verification window. The operator must verify and enforce before execution that all IDE language servers, indexing daemons, file watchers, Maven/Gradle/test runner background processes, Docker volume mounts, and background terminal scripts are completely stopped. If quiescence cannot be guaranteed or maintained, execution must stop immediately.
   - **Dual Source-Manifest Verification & Full State Persistence:**
     - Capture a Pre-Copy Source Manifest ($T_{pre}$) before copying begins, recording normalized relative paths, file sizes, and SHA-256 hashes for every regular file, plus directory inventories (including empty directories).
     - Persist the complete pre-copy state into the manifest schema (`RunId`, `TimestampUtc`, `RootPath`, `BackupLogPath`, `DirCount`, `FileCount`, `UnreadableFilesCount`, `DuplicateFilesCount`, `DuplicateDirsCount`, `ReparsePoints`, `Dirs`, and `Files`) to enable safe state reconstruction across sessions without relying on in-memory counters.
     - Capture a Post-Verification Source Manifest ($T_{post}$) after backup copying and verification.
     - Fail closed if either manifest cannot be completed, if any file is unreadable, or if the two source manifests differ in any way (`$srcDriftCount -gt 0`).
     - Require the destination backup manifest to match the verified source manifest across all directory structures and regular files.
   - **Technical Limitation (Transient State Modifications & Non-Transactional Consistency):** Manifest comparison detects persistent differences between observation points $T_{pre}$ and $T_{post}$, but cannot prove that no transient modification occurred strictly *during* copying that was subsequently reverted, nor does it provide atomic point-in-time transactional snapshots in the absence of an operating-system-level volume snapshot mechanism (such as Windows Volume Shadow Copy Service / VSS). Therefore, operational quiescence is an indispensable procedural prerequisite and cannot be replaced by post-hoc algorithmic checks alone.
   - **Full Inventory Cryptographic Parity:** Examine every regular file (including hidden files, untracked files, ignored files, and `.git`), compute SHA-256 hashes for all source-versus-backup pairs, and compare byte sizes.
   - **Bi-Directional Inventory Checks:** Compare directory structures (including empty subdirectories) and regular files bi-directionally: source-to-destination (detecting missing entries) AND destination-to-source (detecting extra entries).
   - **Standard Copy Mode & Permission Verification:** Standard recursive copy (`/E /DCOPY:DAT /R:1 /W:1 /XJ`) is used. Privileged backup flags (`/ZB` or `/B`) are strictly omitted because they require elevated Windows `SeBackupPrivilege` and fail with exit code 16 in non-elevated user consoles. Do not assume normal user permissions are sufficient; verification must fail closed on any source or destination enumeration errors, copy failures, unreadable files, size mismatches, or hash mismatches.
   - **Failed-Attempt Evidence Preservation & Collision Prevention:** Prior attempt logs (e.g. `d:\Projects\Subscribly_backups\backup_phase0.log`) are permanently retained for forensic audit. Subsequent attempts generate collision-resistant unique log paths (`backup_phase0_<RunId>.log`) and pre-manifest paths (`manifest_source_pre_<RunId>.json`) and verify neither exists before writing.
   - **Single-Attempt Binding & Mandatory Association Checks:**
     - **RunId attempt binding:** Encode a unique, timestamped attempt identifier (`RunId`) in both filenames (`backup_phase0_<RunId>.log`, `manifest_source_pre_<RunId>.json`) and within the manifest schema.
     - **Manifest-recorded paths:** Manifest records `RootPath` (source root) and `BackupLogPath` (exact log path).
     - **Verification association assertions:** Verification validates that filename-encoded RunIds match each other, the manifest internal RunId matches the filenames, manifest `RootPath` matches the configured source root, and manifest `BackupLogPath` matches `$backupLog`. Missing, mismatched, malformed, or inconsistent artifacts trigger immediate fatal abort.
     - **Intended execution:** Both command blocks run in the same continuous PowerShell session, passing session variables automatically.
     - **Separate session execution:** The operator must explicitly provide the exact bound log and pre-manifest paths from the authorized attempt; automatic discovery of the "latest" log or manifest is strictly prohibited. Missing or unassigned path variables fail closed immediately. Pre-copy counters and reparse points are safely reconstructed from the validated JSON schema.
   - **Fail Closed on Reparse Points:** Report the full paths of all detected reparse points (junctions/symlinks); reject any unapproved reparse point to prevent required repository data from being silently omitted via `/XJ`.
   - **Fail Closed on Robocopy Logs:** Verify the bound log exists, is readable, and contains an authentic summary table with `Failed : 0`. Reject missing, truncated, or unparseable summaries. Differentiate Robocopy exit codes: 0–7 indicate nonfatal outcomes (files copied or differences detected), while codes 8 and above represent fatal failures.
   - **Mandatory Fail-Closed Pass Expression:** Enforce strict zero-tolerance criteria across all 28 verification conditions: manifest attempt association validation, manifest schema validation, zero source drift, zero unapproved reparse points, zero log failures, zero directory mismatches, zero duplicate paths, zero missing files, zero extra files, zero unreadable files, zero size mismatches, zero hash mismatches, non-trivial positive file count, and exact pre-to-post count parity.
5. **Archive Commit Rollback Must Be Stepped and Inspected (No Automated Resets):** Automated rollback scripts that execute `git reset --soft HEAD~1` or `git branch -D` without pre-inspection risk destroying work or orphaning commits. Any recovery from an archive commit must preserve the commit and branch as immutable recovery points, requiring inspection of `git status`, `git branch --show-current`, `git log -1 --stat`, and the commit graph before formulating targeted recovery steps.
6. **`.gitignore` Is Insufficient Protection for Tracked or Staged Files:** Adding files to `.gitignore` does **not** protect files that are already tracked by Git or staged in the index. Secret prevention requires verifying that the Git index (`git diff --cached`) is empty, using explicit path staging (never wildcard `git add -A`), and inspecting staged files with `git ls-files --stage` before committing.
7. **Provisional Secret-Scan Classification & Source File Reconciliation:** A regex audit identified 62 secret-like occurrences across the workspace. All 62 findings must be treated as provisional until reconciled against actual source lines. Development and sandbox credentials must **never** be treated as "safe" merely because they are non-production values; they must be classified, excluded, sanitized, or externalized to environment variables before any public commit. Local archive preservation must never be conflated with permission to publish development credentials.
8. **Hidden Database Dependency with Exact Credentials:** The smoke test (`SubscriblyApplicationTests`) is not an isolated unit test. It boots Spring Data JPA and connects to `jdbc:postgresql://localhost:5432/subscribly` with user `subscribly_user` and password `1`. It succeeded during local testing solely because a local PostgreSQL instance was listening on port 5432 with those credentials. Without an isolated PostgreSQL service container initialized with these exact parameters, the test will fail in CI with `Connection refused`.
9. **Flawed Database Constraint for Active Subscriptions:** The proposed `UNIQUE (user_id, plan_id, status)` constraint prevents a subscriber from ever having more than one historical `CANCELLED` subscription for a plan. PostgreSQL requires a **Partial Unique Index** (`WHERE status = 'ACTIVE'`).
10. **Overstated Transactional Guarantees:** A Modular Monolith cannot make external payment gateway HTTP calls (Razorpay/Stripe) atomic with database updates via `@Transactional`. If the payment gateway succeeds but the database rolls back, money has been debited without a database record.
11. **Fictitious Testing Tooling Claims:** `TEST_STRATEGY.md` documents executable Maven commands for JaCoCo, Testcontainers, and Surefire exclusion profiles that do not exist in `pom.xml`.
12. **Unverified Memory & Hosting Claims:** Promising "permanent free hosting" on Railway (which eliminated its free tier) and Render/Neon without accounting for cold-start connection timeouts and 30-day database expiration policies is technically indefensible.

---

## 2. Itemized Critical & High-Severity Issues

### Issue CRIT-01: False "main" Branch Assumption
- **Severity:** Critical
- **Document & Section:** `IMPLEMENTATION_PLAN.md` (Tasks 102, 103, 704)
- **The Exact Claim:** "Establish clean, working baseline on `main`" and configure GitHub Actions CI on "pushes to `main`".
- **Why It Is Incorrect & Risky:** 
  Running `git branch -a` reveals:
  ```text
  * microservices-v2
    master
    remotes/origin/HEAD -> origin/master
    remotes/origin/master
    remotes/origin/microservices-v2
  ```
  There is **no branch named `main`** in this repository. The upstream default branch is `master`. Any automated script, workflow trigger, or developer command targeting `main` will fail immediately.
- **Evidence:** `git branch -a` output confirms local branches are `master` and `microservices-v2`. Remote HEAD is `origin/master`.
- **Recommended Correction:** Align all documentation, CI triggers, and branch references to `master` (or plan an explicit, deliberate branch rename with user approval).
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-02: The Branch Switching Fallacy & Working-Tree Retention
- **Severity:** Critical
- **Document & Section:** `IMPLEMENTATION_PLAN.md` (Task 102), `DECISIONS/001-modular-monolith-vs-microservices.md`
- **The Exact Claim:** "Switch to `master` to establish the clean production modular monolith baseline: `git checkout master`."
- **Why It Is Incorrect & Risky:** 
  Running `git rev-parse master` and `git rev-parse microservices-v2` shows:
  - `master`: `f1353e1e85f87a14b924f603b11fe6eb96cdbbf4`
  - `microservices-v2`: `f1353e1e85f87a14b924f603b11fe6eb96cdbbf4`
  Both branches point to the **exact same commit: `f1353e1`**. 
  When two branches point to the same commit, Git switches `HEAD` **without touching the working tree**. Running `git checkout master` leaves the working tree in the exact same dirty state: 32 deleted files under `src/`, 1 modified `pom.xml`, and 15 untracked directories (`monolith/`, `user-service/`, etc.). 
  Switching branches does **not** restore the original monolith.
- **Evidence:** 
  - `git log --stat` proves commit `f1353e1` ("Add method to find payments to process with query", committed 2026-07-09) is the commit containing the original monolithic codebase under `src/` (32 source files, single root `pom.xml`).
  - Git standard checkout behavior preserves dirty working tree changes when moving between branches pointing to the same commit.
- **Recommended Correction:** 
  To restore the working tree to the original monolithic state of `f1353e1`, one must explicitly restore tracked files (`git restore --source=f1353e1 --staged --worktree .`), but **only after** all uncommitted work has been verified, backed up externally, and committed to an archive branch.
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-03: The Anti-Pattern of Backup Deletion on Rollback
- **Severity:** Critical
- **Document & Section:** `PHASE_1_EXECUTION_PLAN.md` (Gate A Rollback)
- **The Exact Claim:** "Rollback Procedure: `Remove-Item -Recurse -Force 'd:\Projects\Subscribly_backups\...'`"
- **Why It Is Incorrect & Risky:** 
  A backup is an immutable recovery point, not a temporary state change. If an operator encounters an error during backup verification or a subsequent gate, **deleting the backup destroys the safety net**. 
  - An external backup does not alter the repository working tree; therefore, repository rollback from Gate A is a no-op (the repository was untouched).
  - Backups must be retained as immutable recovery points until the entire migration is complete and verified.
- **Evidence:** Core site reliability engineering (SRE) and disaster recovery principles: never delete backups during incident rollback.
- **Recommended Correction:** 
  Redefine Gate A rollback: The repository state was untouched; leave the backup directory intact for inspection. Distinguish **operational rollback** (reverting repository state) from **backup cleanup** (which must occur only after independent verification and explicit user sign-off).
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-04: Inadequate Backup Verification via Sampling, Robocopy Alone, or Unsupported Consistency Claims
- **Severity:** High
- **Document & Section:** `PHASE_1_EXECUTION_PLAN.md` (Gate A Verification)
- **The Exact Claim:** "robocopy /L /MIR or two-file SHA-256 sampling verifies mirror equality and proves byte-for-byte consistency" along with the unsupported assertion that "final hash comparison guarantees source consistency throughout copying and verification."
- **Why It Is Incorrect & Risky:** 
  1. **Sampling & Robocopy Inadequacy:** Robocopy's `/L /MIR` comparison compares file sizes and timestamps; it does **not** perform cryptographic hashing or read file contents. Sampling only two files (`pom.xml` and `.git/config`) leaves 99.5% of the repository unverified.
  2. **Point-in-Time Post-Copy Hash Fallacy:** A point-in-time comparison of source vs. destination hashes executed *after* copying only verifies that the destination matches the source *at the time of verification*. It cannot prove source consistency throughout copying and verification. If a source file was modified mid-copy, the copy could be torn or partial, or source modifications could go undetected if the source drifted during execution.
  3. **Technical Limitation of Manifest Comparison:** Capturing manifests before and after copying ($T_{pre}$ and $T_{post}$) detects persistent changes between observations, but **cannot prove that no transient modification occurred strictly during copying** (e.g., a file written and immediately reverted), nor does it provide atomic point-in-time transactional snapshots in the absence of an operating-system-level volume snapshot mechanism (such as Windows Volume Shadow Copy Service / VSS). Therefore, claiming transactional snapshot consistency without an OS-level mechanism is technically invalid.
  4. **Arithmetic Masking:** Arithmetic count differences (`$extraFiles = $dstFiles.Count - $srcFiles.Count`) can mask simultaneous missing and extra files.
  5. **Silent Reparse Point Omission:** Windows file permissions and junction points (`/XJ`) can cause directories or files to be silently skipped without surfacing as size mismatches unless reparse points are explicitly enumerated, reported, and evaluated against a whitelist.
- **Evidence:** Cryptographic data integrity verification standards, NTFS filesystem mechanics, and disaster recovery principles.
- **Recommended Correction:** 
  Implement an automated, full-pass cryptographic verification script with dual source manifests, strict operational quiescence, and fail-closed error handling:
  1. **Executable Preflight Space Guard & Quiescence:** Enforce a minimum 2 GB free disk space on drive `D:` via an executable preflight check prior to creating any parent directories, logs, or manifest artifacts; failure to query or `< 2 GB` triggers immediate fatal abort. Enforce zero repository writes throughout the backup window by halting IDE language servers, indexing daemons, file watchers, build tools (`mvn`, `gradle`), Docker write mounts, and background terminal scripts. If quiescence cannot be maintained, execution must stop immediately.
  2. **Pre-Copy Source Manifest ($T_{pre}$) & Complete Schema Persistence:** Discover all regular files, empty directories, and reparse points in source before copying. Record normalized relative paths, file sizes, and SHA-256 hashes to a collision-resistant JSON manifest (`manifest_source_pre_<RunId>.json`), explicitly persisting `RunId`, `RootPath`, `BackupLogPath`, enumeration counters (`UnreadableFilesCount`, `DuplicateFilesCount`, `DuplicateDirsCount`), and `ReparsePoints`. Fail closed on any unreadable file or duplicate path.
  3. **Standard Recursive Copy Mode:** Use standard recursive copy (`/E /DCOPY:DAT /R:1 /W:1 /XJ`) without elevated `/ZB` or `/B` flags, avoiding exit code 16 failures while enforcing strict fail-closed handling on any permission or read/write errors.
  4. **Preserve Prior Logs & Prevent Collisions:** Retain all prior attempt logs (e.g. `d:\Projects\Subscribly_backups\backup_phase0.log`) permanently for audit; write subsequent attempts to collision-resistant unique log paths (`backup_phase0_<RunId>.log`) verifying they do not already exist.
  5. **Single-Attempt Binding & Mandatory Association Validation:** Require both command blocks to run in the same continuous PowerShell session as the intended mode. If verification runs in a separate session, require explicit paths to the exact bound log and pre-manifest from the authorized attempt; automatic fallback to the "latest" log or manifest is strictly prohibited. Strictly validate filename RunId matching, manifest internal RunId matching, manifest `RootPath` matching configured source root, and manifest `BackupLogPath` matching bound log. Safely reconstruct pre-copy counters and reparse points from the validated JSON schema; missing state fails closed.
  6. **Post-Verification Source Manifest ($T_{post}$):** Capture a second full source manifest after Robocopy completes. Compare $T_{pre}$ against $T_{post}$ across all files, sizes, SHA-256 hashes, and directories. If any source drift is detected (`$srcDriftCount -gt 0`), reject the snapshot and fail closed.
  7. **State Technical Limitation Precisely:** Document that dual manifests prove invariance between $T_{pre}$ and $T_{post}$, but do not provide atomic transactional snapshots or detect transient mid-copy modifications without VSS. Quiescence is an indispensable procedural prerequisite.
  8. **Destination Backup Manifest & Bidirectional Comparison:** Discover all destination regular files, directories (including empty ones), sizes, and SHA-256 hashes. Compare bi-directionally against the verified post-copy source manifest: source-to-destination (missing entries) and destination-to-source (extra entries).
  9. **Fail Closed on Reparse Points:** Print full paths of all detected source and backup reparse points. Require `$unapproved...Count -eq 0` against `$approvedReparsePoints`.
  10. **Validate Robocopy Log:** Confirm bound log exists, is readable, and contains an authentic summary table with `Failed : 0`. Reject missing, truncated, or unparseable summaries, and fail closed if Robocopy exit code >= 8.
  11. **Mandatory Fail-Closed Pass Evaluation:** Require all 28 verification conditions to evaluate to `$true` in the `$pass` expression (including attempt association validation, schema integrity, positive file count guard, and pre-to-post count parity); throw a fatal terminating exception otherwise.
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-05: Unsafe Archive-Commit Rollback (`git reset --soft HEAD~1` as Default)
- **Severity:** High
- **Document & Section:** `PHASE_1_EXECUTION_PLAN.md` (Gate D Rollback)
- **The Exact Claim:** "Rollback: `git reset --soft HEAD~1 && git checkout microservices-v2`."
- **Why It Is Incorrect & Risky:** 
  - Automatically running resets or branch switches without pre-inspection is dangerous. If the commit succeeded, resetting it discards the verified snapshot.
  - The default recovery posture must be: **Preserve the commit and branch as an immutable record.**
  - If a mistake was made during commit, the operator must inspect `git status`, `git branch --show-current`, `git log -1 --stat`, and the commit graph before determining whether an amendment or a forward fix commit is appropriate.
- **Evidence:** Standard Git disaster recovery procedures.
- **Recommended Correction:** 
  Preserve the archive commit and branch. Require mandatory pre-rollback inspection (`git status`, `git branch`, `git log -1`). Never run automated resets or forced branch deletions (`git branch -D`).
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-06: `.gitignore` Does Not Protect Tracked or Staged Files
- **Severity:** High
- **Document & Section:** `PHASE_1_EXECUTION_PLAN.md` (Gate C & Secret Remediation)
- **The Exact Claim:** "Add `scratch/` to `.gitignore` to prevent secret leakage in the archive commit."
- **Why It Is Incorrect & Risky:** 
  In Git:
  - Adding a pattern to `.gitignore` applies **only to untracked files**. If a file is already tracked in Git history (e.g. `src/main/java/com/subscribly/util/JwtUtil.java`), `.gitignore` is completely ignored.
  - If a file has already been staged in the index (`git add` was previously run), `.gitignore` will **not** unstage it.
  - If someone runs `git add -A` or stages before editing `.gitignore`, sensitive files will be committed regardless of `.gitignore` rules.
- **Evidence:** Git official documentation for `.gitignore`.
- **Recommended Correction:** 
  Before staging for the archive commit:
  1. Verify the Git index is empty (`git diff --cached` must be empty).
  2. Use explicit file and directory paths in `git add`, **never** blanket wildcards (`git add .` or `git add -A`).
  3. Run `git ls-files --stage` after staging to verify that no ignored or diagnostic file (such as `scratch/`) is staged.
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-07: Exact PostgreSQL Credentials in Smoke Test
- **Severity:** Critical
- **Document & Section:** `PROJECT_AUDIT.md` (Section 2.2) & `IMPLEMENTATION_PLAN.md` (Task 003)
- **The Exact Claim:** "SubscriblyApplicationTests.contextLoads() in monolith executed and PASSED... single smoke test for the entire codebase."
- **Why It Is Incorrect & Risky:** 
  `SubscriblyApplicationTests` boots the complete Spring ApplicationContext with Spring Data JPA and HikariCP. 
  Inspecting `monolith/src/main/resources/application.yml` and the original `src/main/resources/application.yml` reveals the exact datasource configuration:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/subscribly
      username: subscribly_user
      password: 1
      driver-class-name: org.postgresql.Driver
  ```
  The test did not pass in isolation—it passed because a PostgreSQL daemon was running on `localhost:5432` with a database named `subscribly` accessible by `subscribly_user` with password `1`. 
  In GitHub Actions CI or on any machine where PostgreSQL is not running or has different credentials (e.g. `postgres/password`), **the test fails immediately with `org.postgresql.util.PSQLException: Connection refused` or `password authentication failed`**.
- **Evidence:** Direct inspection of `monolith/src/main/resources/application.yml` lines 13–16 and `git show f1353e1:src/main/resources/application.yml` lines 6–9.
- **Recommended Correction:** In `.github/workflows/ci.yml`, the PostgreSQL service container must be initialized with:
  ```yaml
  env:
    POSTGRES_DB: subscribly
    POSTGRES_USER: subscribly_user
    POSTGRES_PASSWORD: 1
  ```
  And the test execution step must pass explicit environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- **Phase to Fix:** Phase 1.

---

### Issue CRIT-08: Flawed Active-Subscription Database Constraint
- **Severity:** High
- **Document & Section:** `IMPLEMENTATION_PLAN.md` (Task 204), `ARCHITECTURE.md` (Section 2)
- **The Exact Claim:** "Add unique index on `(user_id, plan_id, status)` for active subscriptions."
- **Why It Is Incorrect & Risky:** 
  In relational databases, a composite unique constraint on `(user_id, plan_id, status)` applies uniformly across all rows:
  1. A user subscribes to Plan A -> row inserted: `(user_1, plan_A, 'ACTIVE')`.
  2. The user cancels -> row updated: `(user_1, plan_A, 'CANCELLED')`.
  3. The user re-subscribes to Plan A later -> row inserted: `(user_1, plan_A, 'ACTIVE')`.
  4. The user cancels the second subscription -> row update to `(user_1, plan_A, 'CANCELLED')` -> **PostgreSQL throws `UniqueConstraintViolationException`!**
  A standard unique constraint across all statuses prevents any user from having more than one historical `CANCELLED` or `EXPIRED` subscription for the same plan.
- **Evidence:** PostgreSQL SQL standard behavior for `UNIQUE` constraints.
- **Recommended Correction:** Enforce active uniqueness using a **PostgreSQL Partial Unique Index**:
  ```sql
  CREATE UNIQUE INDEX uq_user_plan_active ON subscriptions (user_id, plan_id) WHERE status = 'ACTIVE';
  ```
- **Phase to Fix:** Phase 2.

---

### Issue CRIT-09: Overstated Transactional Guarantees for External Payment Gateways
- **Severity:** High
- **Document & Section:** `ARCHITECTURE.md` (Section 2.2), `DECISIONS/001-modular-monolith-vs-microservices.md` (Section 4)
- **The Exact Claim:** "A modular monolith allows ACID transactions (`@Transactional`) across the subscription and payment tables in PostgreSQL. There is zero risk of unhandled FeignException leaving subscriptions in inconsistent states."
- **Why It Is Incorrect & Risky:** 
  While moving from Feign to in-process calls removes inter-service network hops between subscription and payment tables, **an external payment gateway call (Razorpay/Stripe) is outside the database transaction.**
  1. If a payment service makes a synchronous gateway HTTP call inside `@Transactional`, database connections in HikariCP are held open during network latency, causing pool exhaustion.
  2. If the gateway successfully charges the customer's credit card, but the database connection drops or the JVM crashes right before commit, the database rolls back, but **the customer was still charged**.
  3. `@Transactional` does not and cannot roll back external credit card charges.
- **Evidence:** Fundamental distributed systems principle: external HTTP mutations cannot participate in local relational ACID transactions.
- **Recommended Correction:** Document the requirement for an **Outbox Pattern**, idempotency keys, and asynchronous webhook reconciliation for financial consistency.
- **Phase to Fix:** Phase 4 / Phase 5.

---

### Issue CRIT-10: Unverified Memory & "Permanent Free Hosting" Claims
- **Severity:** High
- **Document & Section:** `PROJECT_AUDIT.md` (Section 9.2), `IMPLEMENTATION_PLAN.md` (Section 1), `ARCHITECTURE.md` (Section 2)
- **The Exact Claim:** "Running 8 microservices requires 3.5 to 5.0 GB RAM... the modular monolith will run under 400 MB... achievable at zero cost permanently on Railway/Render/Neon."
- **Why It Is Incorrect & Risky:** 
  1. The numbers "3.5 to 5.0 GB" and "under 400 MB" are unmeasured estimates, not empirical benchmarks.
  2. **Railway:** Completely eliminated its free tier in 2023. It now requires a minimum $5/month paid subscription.
  3. **Render:** Free web services sleep after 15 minutes of inactivity; maximum 750 free instance hours pooled across all services per month; free PostgreSQL expires and is deleted after 30 days.
  4. **Neon:** Computes sleep after 5 minutes of inactivity. When a cold request arrives, compute wakes up with a 1.5–3.0 second delay. If HikariCP `connection-timeout` is set to standard 1000ms, the application throws connection acquisition timeout exceptions.
- **Evidence:** Public pricing and terms of service documentation for Railway, Render, and Neon.
- **Recommended Correction:** Remove claims of "permanent free hosting". Document the exact operational trade-offs: Render free web service + Neon free tier with HikariCP `connection-timeout` tuned to `10000ms` (10s) to absorb serverless compute wake-up latency.
- **Phase to Fix:** Phase 1 (docs) / Phase 7 (deployment).

---

## 3. Reconciled Classification of All 62 Sensitive Findings

Our automated regex scan identified **62 occurrences** of secret-like keywords across the repository. These findings are provisional until reconciled against actual source files and lines. 

**Mandatory Security Rule:** Non-production, sandbox, and development credentials must **never** be treated as "safe" merely because they are non-production. Any credential embedded in code poses security, compliance, and credential-stuffing risks if exposed publicly. Local archive preservation must never be conflated with authorization to publish credentials.

Below is the line-by-line reconciled technical classification:

| Group | Occurrences | Example Locations | Technical Nature | Required Remediation Action |
|---|---|---|---|---|
| **Group 1: Code Identifiers & Framework Boilerplate** | 22 | `frontend/src/...`, `JwtAuthFilter.java`, `AuthController.java`, `mvnw` | Parameter names (`password`), header parsing (`Bearer token`), DTO fields, script comments. | **Verified Safe:** Harmless code identifiers. Retain in codebase. |
| **Group 2: Documentation Secret Exposure** | 1 | `docs/PROJECT_AUDIT.md` (line 204) | Verbatim HMAC-SHA256 secret key string printed in Markdown table. | **Must Sanitize:** Redact to `[REDACTED_JWT_SECRET]` in Gate B before any staging. |
| **Group 3: Diagnostic Scripts with Test Credentials** | 16 | `scratch/e2e_qa_test.ps1`, `scratch/scan_secrets.py` | Local PowerShell automation scripts containing test passwords, mock tokens, and diagnostic scan logic. | **Must Exclude:** Add `scratch/` to `.gitignore` and omit from Git staging in Gate C/D. Preserved in external physical backup. |
| **Group 4: Mock Sandbox Payment Gateway Keys** | 8 | `config-repo/payment-service.yml`, `config-server/...`, `payment-service/...` | Razorpay sandbox test keys (`rzp_test_...`) and mock webhook secret. | **Externalize Before Public Commit:** Safe for local unpushed archive branch, but must be externalized to environment variables (`RAZORPAY_KEY_ID`, etc.) before any public branch or remote push. |
| **Group 5: Hardcoded HMAC-SHA256 Secret in Java** | 4 | `user-service`, `subscription-service`, `payment-service`, `monolith` (`JwtUtil.java`) | 256-bit static secret string used for local development JWT token signing. | **Externalize Before Public Commit:** Must be replaced with `JWT_SECRET` environment variable in Phase 3. |
| **Group 6: Local Development Database Passwords** | 11 | `monolith`, `user-service`, `docker-compose.yml`, `config-repo` | Local development PostgreSQL passwords (`password: 1` or `POSTGRES_PASSWORD=password`). | **Externalize Before Public Commit:** Must be replaced with `DB_PASSWORD` environment variable in Phase 2/3. |

### Credential Rotation Policy:
- **Sandbox Keys:** The Razorpay keys in the repository start with `rzp_test_`. These are sandbox simulator keys with zero access to real banking networks or live currency.
- **Mandatory Rotation Rule:** If any genuine production API key (e.g. live Stripe, live Razorpay `rzp_live_...`, AWS access key, or live database password) was ever entered into local files or history, **it must be immediately revoked and regenerated in the provider's administrative dashboard**. Merely deleting or git-ignoring a file does not rotate an exposed credential.

---

## 4. Git Recovery Mechanics: Reference Guide

| Git Command | Target | Effect on `HEAD` | Effect on Index (Staging) | Effect on Working Tree | Risk Level |
|---|---|---|---|---|---|
| `git restore --worktree <file>` | Tracked File(s) | None | None | Discards uncommitted modifications in working tree to match Index or commit. | **DESTRUCTIVE** (Irreversible without backup) |
| `git restore --staged <file>` | Index | None | Unstages changes; copies commit state to Index. | Working tree untouched. | **SAFE** |
| `git reset --soft <commit>` | Commit Ref | Moves `HEAD` pointer to `<commit>`. | Staged changes are **retained** in Index. | Working tree untouched. | **SAFE** (Fully reversible) |
| `git reset --mixed <commit>` | Commit Ref | Moves `HEAD` pointer to `<commit>`. | Index is reset to match `<commit>` (unstaged). | Working tree untouched. | **SAFE** (Files stay in working tree) |
| `git reset --hard <commit>` | Commit Ref | Moves `HEAD` pointer to `<commit>`. | Index reset to `<commit>`. | **Overwrites all tracked files** in working tree to match `<commit>`! | **EXTREME DANGER** (Irreversible for uncommitted tracked work) |
| `git clean -fd` | Untracked Files | None | None | **Deletes all untracked files and directories!** | **EXTREME DANGER** (Irreversible permanent loss of untracked files) |

---

## 5. Decoupled Lifecycle: Retention, Verification, and Cleanup

To eliminate operational confusion, Phase 1 strictly separates:
1. **Backup Retention (Gate A):** The backup directory `d:\Projects\Subscribly_backups\subscribly_backup_phase0` is immutable. It is preserved throughout Phase 1 and all future migration phases.
2. **Recovery Verification (Gate A & Ongoing):** Verification confirms that the backup directory contains identical files, byte counts, and SHA-256 hashes compared to the verified source tree, with dual source manifests confirming zero persistent source drift under strict operational quiescence.
3. **Eventual Backup Cleanup (Post-Migration):** An independent administrative operation performed only after the entire project migration is complete, verified in production, and explicitly approved by the user. It is **never** executed during Phase 1.

---

## 6. Summary of Remaining Risks & Actions Requiring Explicit Approval

### Remaining Operational Risks:
1. **Uncommitted Secrets in Working Tree:** The untracked files contain development mock keys (`rzp_test_...` in `payment-service.yml`) and HMAC secrets. While safe for a local archive branch, they must never be pushed to a public remote without externalization to environment variables.
2. **Local PostgreSQL Daemon Dependency:** In Gate E, running `mvnw clean test` requires that your local PostgreSQL daemon on port 5432 is running with database `subscribly` accessible by `subscribly_user` / `1`.
3. **Cloud Free-Tier Sleep Behavior:** In Phase 7, deploying to Render + Neon means the service will sleep after 5–15 minutes of inactivity. HikariCP `connection-timeout` must be set to `10000ms` (10s) to handle the 1.5–3.0 second wake-up cold start without dropping requests.

### Independent Actions Requiring Explicit User Approval:
1. **[Approval A] External Physical Backup & Verification:** Authorize running `robocopy` and the full cryptographic verification script (with dual source manifests and operational quiescence enforcement) for `d:\Projects\Subscribly_backups\subscribly_backup_phase0`.
2. **[Approval B] Documentation Secret Redaction:** Authorize redacting the literal secret in [`docs/PROJECT_AUDIT.md`](file:///d:/Projects/Subscribly/subscribly/docs/PROJECT_AUDIT.md) line 204.
3. **[Approval C] Ignore Rule Addition:** Authorize adding `scratch/`, `*.log`, `*.tmp` to [`.gitignore`](file:///d:/Projects/Subscribly/subscribly/.gitignore).
4. **[Approval D] Archive Branch Creation & Commit:** Authorize `git checkout -b archive/microservices-wip` and committing the microservices snapshot.
5. **[Approval E] Monolith Restoration on `master`:** Authorize restoring tracked files to commit `f1353e1` on `master`.
6. **[Approval F] CI Workflow Implementation:** Authorize creating [`.github/workflows/ci.yml`](file:///d:/Projects/Subscribly/subscribly/.github/workflows/ci.yml).
