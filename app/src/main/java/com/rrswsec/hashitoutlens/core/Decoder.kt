package com.rrswsec.hashitoutlens.core

import com.rrswsec.hashitoutlens.core.model.DecodeFinding

interface Decoder {
    fun decode(input: String): List<DecodeFinding>
}
