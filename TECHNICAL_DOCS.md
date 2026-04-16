# HashItOut Lens VNext - Technical Documentation

## 1. Architectural Overview
The application follows a **Unidirectional Data Flow (UDF)** pattern using Jetpack Compose and ViewModel. The core analytical logic is decoupled from the UI, allowing for high-performance parallel execution.

### 1.1 Core Components
- **LensViewModel:** Orchestrates the live camera stream, frame freezing, and selection-based analysis.
- **DecoderRegistry:** The central dispatcher for all decryption tasks. It manages thread pools via Coroutines.
- **BeamSearchDecoder:** Implements the recursive tree-traversal logic for multi-layered cipher cracking.
- **EncryptionIdentifier:** A statistical profiling engine that provides $O(1)$ hints to prioritize decoders.

## 2. Decryption Methodology

### 2.1 Beam Search Algorithm
The `BeamSearchDecoder` uses a beam search with `width=24` and `max_depth=5`. 
- **Scoring:** Each node in the search tree is scored using `TextScorer`, which combines Tetragram frequency, English word density, and Index of Coincidence.
- **Pruning:** At each depth level, only the top 24 most promising transformation chains are kept for the next level.
- **Depth Bonus:** Deeply nested solutions (e.g., 3+ layers) receive a scoring bonus to offset the inherent entropy of multi-layered encryption.

### 2.2 Parallel Execution Pipeline
In `DecoderRegistry.runAll`, the app uses `async/awaitAll` to execute every base decoder in parallel.
- **Execution Context:** `Dispatchers.Default` is used to offload heavy mathematical computations from the Main thread.
- **Adaptive Sorting:** The order of execution is dynamically adjusted based on:
    1. Statistical hints from `EncryptionIdentifier`.
    2. A "Winning Path Cache" that tracks which ciphers were successfully cracked in previous frames.

## 3. Visual Crop (Surgical Analysis)
The crop tool maps screen-space coordinates (0.0 to 1.0) to the underlying `frozenBitmap` resolution. 
- When a selection is made, a sub-bitmap is generated.
- This sub-bitmap is processed by a dedicated OCR pass, and the resulting text is fed into the "Full-Nasty" deep analysis pipeline.
- This bypasses OCR noise from surrounding objects, significantly increasing the signal-to-noise ratio for complex ciphers.

## 4. UI Rendering (AR HUD)
The "AR-style" visuals are achieved through:
- **Shadow-based Outlining:** Custom `Shadow` implementation in `outlinedTextStyle` creates a 1px solid black outline (90% alpha).
- **Layered Opacity:** Background cards use `0xCC` (80% alpha) to provide a "glassmorphism" effect while maintaining readability over live video.
- **Monospace Visuals:** All meta-data and debug information uses `FontFamily.Monospace` for a technical analyst aesthetic.

## 5. Deployment & Optimization
- **C25 Target:** Memory usage is optimized by recycling bitmaps and using lifecycle-aware coroutines to prevent background leaks during intensive brute-forcing.
- **Hardware Acceleration:** Uses CameraX and ML Kit's hardware-accelerated text/barcode recognition.
