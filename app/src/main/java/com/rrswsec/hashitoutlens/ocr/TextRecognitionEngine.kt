package com.rrswsec.hashitoutlens.ocr

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognize(image: InputImage): String {
        return process(image).text
    }

    fun process(image: InputImage): Text {
        return Tasks.await(recognizer.process(image))
    }
}
