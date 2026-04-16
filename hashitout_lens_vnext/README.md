# hashitout lens vnext

this is the polished android studio package for the lens side.

the vibe is deliberate.
it is supposed to feel like a little terminal gremlin got hold of google lens and taught it to care about ciphers.

## what it does
- live camera preview with camerax
- live ocr with ml kit
- live barcode and qr scanning with ml kit
- ranked decode attempts on top of ocr and symbol reads
- import image or screenshot with the photo picker
- view raw ocr text, barcode text, and lightweight hidden-text hits
- show the best candidate fast and keep the rest in a scrollable result sheet

## what it does not pretend to do
- it does not background-scrape other apps
- it does not use accessibility abuse tricks
- it does not download code at runtime
- it does not try to out-forensics a desktop stego suite inside a phone camera loop

## build
1. open this folder in android studio
2. let studio install any missing sdk pieces
3. sync gradle
4. run on a device or use build > build apk(s)

## wrapper note
this environment did not include the android sdk or gradle wrapper generator, so this package is set up for android studio import rather than a prebaked apk.

## references and inspiration
- google lens for the broad camera-first feel
- ml kit samples and docs for ocr and barcode flow
- cyberchef for the idea that ranked transform guesses beat dumb one-shot decode buttons

## how this tries to do it better for this niche
- it keeps the operator-facing result list tight and readable
- it treats barcode hits, hidden text hits, and cipher guesses as one ranked feed
- it stays honest about confidence instead of pretending every decode is a win
- it keeps the terminal-skunk face instead of flattening into generic material sludge
