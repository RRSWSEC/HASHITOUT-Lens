# HashItOut Lens Expanded

# We want you to contribute!

Please, help us improve. This is open source for a reason, and I would like to see this project become faster and stronger. There are brighter minds than mine out there, and I value and respect your time and thoughts. Let's work together! contact me at eric(at)rrs.works 

link to apk; https://drive.google.com/file/d/1zE30L3SExnf54WwqCIfSYdogmnQx1AdX/view?usp=drive_link

Build-ready Android Studio project for a camera/import driven cipher and hidden-text scanner.

## Included in this drop
- Live CameraX preview
- ML Kit OCR
- ML Kit barcode / QR scanning
- Ranked decoder results with a best-result overlay
- Full results sheet and detail sheet
- Import image / screenshot flow via Photo Picker
- Text viewer for OCR / barcode / extracted hidden text
- Encryption / encoding type hints with confidence scores
- High-confidence stego / hidden text checks that are feasible on-device:
  - zero-width and whitespace hidden text in recognized content
  - PNG tEXt / iTXt / zTXt chunk extraction
  - JPEG COM segment extraction
  - lightweight RGB / grayscale LSB text extraction on imported images
  - inverted-image retry for QR / barcode and OCR on imports

## Exclusions
- password cracking / password-protected archive workflows
- background screen monitoring
- Accessibility-service capture
- remote code loading
- native stego password attacks
- heavy file carving / malware forensics passes

## Build
1. Open the `hashitout-lens` folder in Android Studio.
2. Let Android Studio install the requested Android SDK components.
3. Sync Gradle.
4. Run on device or choose **Build > Build APK(s)**.

## Notes
- This environment does not include the Android SDK, so the deliverable is a complete Android Studio project zip rather than a pre-baked APK.
- The decode and ranking model is based on the source HashItOut structure: many candidate findings, confidence labels, notes/why fields, family caps, and stego/file-analysis branches that can be split by product profile. See the uploaded source snippets for the original scope and design direction.
