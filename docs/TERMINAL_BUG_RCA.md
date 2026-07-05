# Terminal Emulation Bug — Root Cause Analysis Report

## 1. ROOT CAUSE

Two distinct but related bugs share a common architectural root: **the terminal processing pipeline lacks proper state management for streaming/chunked input**.

**'u4;0m' residual characters:** SSH output arrives in arbitrary-sized chunks. When an ANSI escape sequence like `\x1b[34;0m` (set foreground color to blue, then reset) is split across two read boundaries — e.g., chunk1 ends with `\x1b[` and chunk2 begins with `34;0m` — the VirtualTerminal consumes the ESC-[ prefix but has no mechanism to buffer the incomplete sequence. The trailing `34;0m` is treated as printable text and rendered.

**TUI spinner duplication:** `VirtualTerminal.process()` returns the **entire terminal buffer state** as a joined string (`buffer.joinToString("\n")`). `TerminalViewModel.appendOutput()` then **appends** this full-state snapshot to the persistent `outputBuffer`. Each spinner tick (cursor-up → clear-line → rewrite) produces a new full-state snapshot that gets accumulated rather than replacing the previous state.

## 2. DIRECT CAUSE

**Bug 1 — Escape sequence splitting:**
- File: `VirtualTerminal.kt:72-147` (`processEscapeSequence`)
- When input is `\x1b[` and the string ends, `pos` is incremented past `[` (line 89), then `pos >= input.length` returns immediately (line 90-91). The partial sequence is consumed but not completed.
- The next chunk starts with `34;0m` — no `\x1b` prefix, so it falls into the `else` branch (line 42-58) and is rendered character-by-character.
- No cross-chunk buffering exists in `VirtualTerminal.process()` (line 13-63).

**Bug 2 — appendOutput accumulation:**
- File: `TerminalViewModel.kt:448-458` (`appendOutput`)
- Line 451: `val result = virtualTerminal.process(processed)` — returns full buffer state
- Line 452: `outputBuffer.append(result)` — appends full state as incremental text
- For a TUI app sending `ESC[A` (cursor up) + `ESC[K` (clear line) + new text, VirtualTerminal correctly manages its internal buffer (overwriting the same row), but `appendOutput` accumulates every snapshot.
- `TerminalScreen.kt:168-169` re-parses the accumulated `terminalOutput` via `AnsiParser.parse()`, which has no mechanism to deduplicate or replace.

## 3. INDIRECT CAUSE

The architectural gap: **the pipeline conflates two fundamentally different output modes into a single append-only string accumulator**.

| Mode | Characteristic | Example | Correct handling |
|------|---------------|---------|-----------------|
| **Append** | New content appears below existing | Login banner, command echo, `ls` output | Append to buffer |
| **Replace** | Same screen position updates in-place | TUI spinners, `top`, `htop`, progress bars | Replace buffer content |

The current architecture treats all output as Mode 1 (append). There is no detection, signaling, or handling of Mode 2 (replace) output. VirtualTerminal was designed to handle cursor movement but its output contract (`String` return = full buffer state) is mismatched with how the caller uses it (append as text).

Additionally, `VirtualTerminal` does not track or persist escape sequence state across `process()` calls — each call starts fresh with whatever characters arrive, making it vulnerable to chunk-boundary splits.

## 4. FIRST PRINCIPLES SOLUTION

A terminal emulator must maintain two invariants:

1. **Atomic escape sequence processing:** An escape sequence is an indivisible unit. If a sequence cannot be fully parsed from the available input, its partial characters must be buffered and prepended to the next input chunk. The parser must never emit partial sequences as output.

2. **State-replacement output contract:** When the remote end uses cursor movement to update in-place (detected by ESC[A, ESC[B, `\r`, cursor addressing), the output should reflect the **current screen state**, not accumulate historical snapshots. The correct output is always "here is what the screen looks like now."

## 5. PROPOSED SOLUTION

### Fix 1: Cross-chunk escape sequence buffering (Bug 1)

Add a `pendingSequence: StringBuilder` field to `VirtualTerminal`:
- In `process()`: if input starts with an incomplete escape sequence (detected by matching `\x1b` prefix without a valid terminator), prepend `pendingSequence` to the input and continue parsing.
- After `process()` completes, if the tail of the input is a partial sequence (ESC without terminator, or CSI params without final char), store it in `pendingSequence`.
- On next `process()` call, prepend `pendingSequence` to the new input before parsing.
- Clear `pendingSequence` after successful prepend+parse.

### Fix 2: Replace output instead of append (Bug 2)

Change `TerminalViewModel.appendOutput()` to **replace** `outputBuffer` content when processing VirtualTerminal output, rather than appending:
- Track whether the current output contains cursor-manipulation sequences (`\r`, `\x1b[A-D`, `\x1b[H`, etc.).
- When cursor-manipulated output is detected, **replace** the tail of `outputBuffer` with the new VirtualTerminal buffer state.
- For non-cursor-manipulated output (connection messages, command echoes), continue to append.

Alternatively (simpler): Always replace `outputBuffer` with the VirtualTerminal's current state for terminal-sourced output, and only append for ViewModel-originated messages (connection status, command echo). This requires separating the two message sources.

### Fix 3 (minimal — quick win)

As a simpler intermediate fix: change `appendOutput` to always replace `outputBuffer` with `virtualTerminal.process()` result for all output, and move ViewModel-originated messages (e.g., "正在连接...", "$ command") into the VirtualTerminal buffer via a direct write rather than going through appendOutput. This eliminates the accumulation problem without requiring complex cursor-detection heuristics.

## 6. RISK ANALYSIS

| Risk | Severity | Mitigation |
|------|----------|-----------|
| **Replacing output loses command history** | High — user sees connection messages disappear after scrolling past them | ViewModel messages should be written INTO VirtualTerminal buffer (as permanent lines) rather than accumulated separately. Test by verifying login banner + command history persist. |
| **Cross-chunk buffering changes parse timing** | Medium — sequences that were previously (incorrectly) rendered as text will now be consumed silently | Visually verify SSH login flow, password prompts, and color output still render correctly. |
| **Performance impact of full-buffer return on every chunk** | Low — VirtualTerminal buffer is capped at 500 lines (`maxLines=500`), joinToString is O(n) with n≤500 | No mitigation needed; already bounded. |
| **Race condition in appendOutput replacement** | Medium — two concurrent chunks could interleave replace operations | Existing `synchronized(outputLock)` at line 449 already serializes access. No change needed. |
| **Edge case: split in middle of multi-byte sequence** | Low — SSH uses UTF-8, and ANSI escape sequences use ASCII-only characters (bytes < 0x80), so multi-byte splits can't occur within escape sequences | No special handling needed. |
| **Regression in TUI apps that rely on specific line accumulation** | Low — no legitimate TUI app wants duplicate lines; the current behavior is always wrong for cursor-manipulated output | Verify with real TUI apps (htop, top, vim-like apps). |
