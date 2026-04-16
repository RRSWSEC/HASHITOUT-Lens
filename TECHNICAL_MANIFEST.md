# TECHNICAL MANIFEST: HashItOut Lens (High-Performance Forensic Edition)

## 1. Engine Architecture
*   **Parallel Beam Search Decoder:** A multi-threaded cryptanalysis engine (Width: 64, Depth: 8) that runs thousands of decryption hypotheses concurrently using Kotlin Coroutines on `Dispatchers.Default`.
*   **Sentient Text Scoring (English Bias):** A scoring algorithm that utilizes tetragram analysis, letter frequency, and Index of Coincidence (IC). It features a "Statement Bonus" (+25 pts) for coherent English sentences and a "Single-Letter Island Penalty" to filter out gibberish.
*   **Hardware Acceleration:** Utilizes JVM parallel streams for O(N) fuzzy matching in the discovery cache, leveraging processor-level SIMD (Neon) instructions for low-latency AR overlays.

## 2. Forensic Features
*   **Surgical Isolation:** OCR processing is restricted to a user-defined selection box, preventing noise from surrounding text from polluting the cipher input.
*   **Persistent Lexicon:** A dynamic `verified_lexicon.json` file that learns new vocabulary from every successful decode, strengthening the engine's predictive power over time.
*   **Forensic Product Export:** Generates high-resolution composite PNGs including the original image, the visual decode overlay (Magazine style), and the analysis log.

## 3. Real-Time UX
*   **Live Workstream:** The analysis sheet provides a real-time log of text fragments being discovered by individual decoders, offering transparency into the "thought process" of the AI.
*   **Fuzzy Snap-Back:** AR overlays are pinned to targets using a Levenshtein-distance fuzzy cache. Even if the camera angle changes, the decoded "Magazine Tab" remains snapped to the physical target.
