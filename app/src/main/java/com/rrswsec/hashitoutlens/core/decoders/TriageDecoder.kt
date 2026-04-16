package com.rrswsec.hashitoutlens.core.decoders

import com.rrswsec.hashitoutlens.core.Decoder
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import com.rrswsec.hashitoutlens.core.model.FindingType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Port of PE, ELF, and PDF triage logic from hashitout_clean.py.
 * Detects binary file signatures and extracts basic header metadata.
 */
class TriageDecoder : Decoder {
    override fun decode(input: String): List<DecodeFinding> {
        val bytes = input.toByteArray(Charsets.ISO_8859_1)
        if (bytes.size < 4) return emptyList()

        val results = mutableListOf<DecodeFinding>()
        
        // PE (Windows)
        if (bytes.size >= 2 && bytes[0] == 'M'.code.toByte() && bytes[1] == 'Z'.code.toByte()) {
            triagePE(bytes)?.let { results.add(it) }
        }
        
        // ELF (Linux)
        if (bytes.size >= 4 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && 
            bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            triageELF(bytes)?.let { results.add(it) }
        }

        // PDF
        if (input.startsWith("%PDF-")) {
            triagePDF(input)?.let { results.add(it) }
        }

        return results
    }

    private fun triagePE(data: ByteArray): DecodeFinding? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val peOffset = buffer.getInt(0x3C)
            if (peOffset + 24 > data.size) return null
            
            val signature = buffer.getInt(peOffset)
            if (signature != 0x00004550) return null // "PE\0\0"
            
            val machine = buffer.getShort(peOffset + 4).toInt() and 0xFFFF
            val numSections = buffer.getShort(peOffset + 6).toInt() and 0xFFFF
            val timestamp = buffer.getInt(peOffset + 8)
            val chars = buffer.getShort(peOffset + 22).toInt() and 0xFFFF
            
            val arch = when (machine) {
                0x14c -> "x86"
                0x8664 -> "x64"
                0x1c0 -> "ARM"
                0xaa64 -> "ARM64"
                else -> "0x${machine.toString(16)}"
            }
            val kind = if (chars and 0x2000 != 0) "DLL" else "EXE"
            
            DecodeFinding(
                method = "PE Triage",
                resultText = "$kind $arch, sections: $numSections, ts: $timestamp",
                confidence = Confidence.HIGH,
                score = 90.0,
                family = "triage",
                why = "Detected Windows Portable Executable header.",
                chain = listOf("triage"),
                findingType = FindingType.ENCRYPTION_HINT
            )
        } catch (e: Exception) { null }
    }

    private fun triageELF(data: ByteArray): DecodeFinding? {
        return try {
            val eiClass = data[4].toInt()
            val eiData = data[5].toInt()
            val bits = if (eiClass == 1) 32 else 64
            val endian = if (eiData == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            
            val buffer = ByteBuffer.wrap(data).order(endian)
            val type = buffer.getShort(16).toInt() and 0xFFFF
            val machine = buffer.getShort(18).toInt() and 0xFFFF
            
            val typeStr = when (type) {
                1 -> "relocatable"
                2 -> "executable"
                3 -> "shared"
                else -> "type-$type"
            }
            val arch = when (machine) {
                0x03 -> "x86"
                0x3E -> "x86-64"
                0x28 -> "ARM"
                0xB7 -> "AArch64"
                else -> "0x${machine.toString(16)}"
            }

            DecodeFinding(
                method = "ELF Triage",
                resultText = "$bits-bit $typeStr, arch: $arch",
                confidence = Confidence.HIGH,
                score = 90.0,
                family = "triage",
                why = "Detected Linux Executable and Linkable Format header.",
                chain = listOf("triage"),
                findingType = FindingType.ENCRYPTION_HINT
            )
        } catch (e: Exception) { null }
    }

    private fun triagePDF(input: String): DecodeFinding? {
        val versionMatch = Regex("%PDF-(\\d+\\.\\d+)").find(input)
        val version = versionMatch?.groupValues?.get(1) ?: "?"
        val hasJS = input.contains("/JavaScript") || input.contains("/JS")
        
        return DecodeFinding(
            method = "PDF Triage",
            resultText = "PDF v$version${if (hasJS) " [Contains JS]" else ""}",
            confidence = if (hasJS) Confidence.HIGH else Confidence.MEDIUM,
            score = 80.0,
            family = "triage",
            why = "Detected PDF document header and structure.",
            chain = listOf("triage"),
            findingType = FindingType.ENCRYPTION_HINT
        )
    }
}
