package com.oscar.smartcontrol.ir

import android.content.Context
import android.hardware.ConsumerIrManager

enum class IrButton(val label: String) {
    POWER("Power"),
    VOL_UP("Vol +"),
    VOL_DOWN("Vol -"),
    MUTE("Mute"),
    INPUT("Input / HDMI"),
    SETTINGS("Configuración"),
    HOME("Home"),
    BACK("Atrás"),
    UP("Arriba"),
    DOWN("Abajo"),
    LEFT("Izquierda"),
    RIGHT("Derecha"),
    OK("OK"),
    CH_UP("CH +"),
    CH_DOWN("CH -")
}

enum class TvBrand(val displayName: String) {
    SAMSUNG("Samsung"),
    LG("LG"),
    SONY("Sony"),
    TCL("TCL"),
    HISENSE("Hisense (VIDAA / Android)"),
    PHILIPS_RC5("Philips (RC5 / Clásico)"),
    PHILIPS_RC6("Philips (RC6 / Moderno)"),
    XIAOMI("Xiaomi / Mi TV"),
    PANASONIC("Panasonic"),
    SHARP("Sharp / Aquos"),
    TOSHIBA("Toshiba"),
    ROKU_TV("Roku TV (TCL/Hisense/Onn)"),
    VIZIO("Vizio"),
    JVC("JVC"),
    RCA_SMART("RCA Smart TV (NEC)"),
    RCA_CLASSIC_56("RCA Clásico (56 kHz)"),
    RCA_38("RCA (38 kHz)"),
    OEM_LATAM_1("Noblex / BGH / Admiral / RCA (Perfil A)"),
    OEM_LATAM_2("Philco / Sanyo / Kanji / Sansei (Perfil B)"),
    AUDIO_JBL_SOUNDBAR("Audio: JBL Soundbar"),
    AUDIO_SAMSUNG_SOUNDBAR("Audio: Samsung Soundbar"),
    AUDIO_LG_SOUNDBAR("Audio: LG Soundbar"),
    AUDIO_SONY_SYSTEM("Audio: Sony Home / Soundbar"),
    PROJECTOR_EPSON("Proyector: Epson"),
    PROJECTOR_BENQ("Proyector: BenQ"),
    STREAMING_APPLE_TV("Streaming: Apple TV (IR)"),
    LED_RGB_24KEY("Iluminación: Tira LED RGB (24/44 teclas)")
}

sealed class IrSignal {
    data class Nec(val code: Long, val freqHz: Int = 38000) : IrSignal()
    data class Samsung32(val code: Long, val freqHz: Int = 38000) : IrSignal()
    data class SonySirc(val data: Int, val bits: Int = 12, val freqHz: Int = 40000) : IrSignal()
    data class Rc5(val address: Int, val command: Int, val toggle: Int = 0, val freqHz: Int = 36000) : IrSignal()
    data class Rc6(val address: Int, val command: Int, val toggle: Int = 0, val freqHz: Int = 36000) : IrSignal()
    data class Panasonic(val address: Int, val data: Long, val freqHz: Int = 37000) : IrSignal()
    data class Jvc(val address: Int, val command: Int, val freqHz: Int = 38000) : IrSignal()
    data class Sharp(val address: Int, val command: Int, val freqHz: Int = 38000) : IrSignal()
    data class Rca(val address: Int, val command: Int, val freqHz: Int = 56000) : IrSignal()
    data class Raw(val freqHz: Int, val patternUs: IntArray) : IrSignal()
}

data class FlipperIrEntry(
    val name: String,
    val signal: IrSignal
)

/**
 * Lightweight universal IR Blaster controller using Android's ConsumerIrManager.
 * Supports NEC/NECext, Samsung32, Sony SIRC, Philips RC5 & RC6, Panasonic, JVC, Sharp,
 * Pronto Hex, and Flipper Zero .ir files.
 */
class IrController(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    private var rcToggle = 0

    val hasIrBlaster: Boolean
        get() = try {
            irManager?.hasIrEmitter() == true
        } catch (_: Exception) {
            false
        }

    fun transmit(signal: IrSignal): Boolean {
        val manager = irManager ?: return false
        if (!hasIrBlaster) return false
        return try {
            val (freq, pattern) = when (signal) {
                is IrSignal.Nec -> signal.freqHz to encodeNec(signal.code)
                is IrSignal.Samsung32 -> signal.freqHz to encodeSamsung32(signal.code)
                is IrSignal.SonySirc -> signal.freqHz to encodeSonySirc(signal.data, signal.bits)
                is IrSignal.Rc5 -> {
                    rcToggle = rcToggle xor 1
                    signal.freqHz to encodeRc5(signal.address, signal.command, rcToggle)
                }
                is IrSignal.Rc6 -> {
                    rcToggle = rcToggle xor 1
                    signal.freqHz to encodeRc6(signal.address, signal.command, rcToggle)
                }
                is IrSignal.Panasonic -> signal.freqHz to encodePanasonic(signal.address, signal.data)
                is IrSignal.Jvc -> signal.freqHz to encodeJvc(signal.address, signal.command)
                is IrSignal.Sharp -> signal.freqHz to encodeSharp(signal.address, signal.command)
                is IrSignal.Rca -> signal.freqHz to encodeRca(signal.address, signal.command)
                is IrSignal.Raw -> signal.freqHz to signal.patternUs
            }
            if (pattern.isEmpty()) return false
            manager.transmit(freq, pattern)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun transmitBrandButton(brand: TvBrand, button: IrButton): Boolean {
        val signal = BRAND_PRESETS[brand]?.get(button) ?: return false
        return transmit(signal)
    }

    fun transmitCustomCode(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.startsWith("0000")) {
            val raw = parseProntoHex(trimmed) ?: return false
            return transmit(raw)
        }
        val parsed = parseFlipperBlock(trimmed)
        if (parsed != null) return transmit(parsed)
        return false
    }

    private fun encodeNec(code: Long): IntArray {
        val list = ArrayList<Int>(68)
        list.add(9000)
        list.add(4500)
        for (i in 31 downTo 0) {
            list.add(562)
            val bit = (code ushr i) and 1L
            list.add(if (bit == 1L) 1687 else 562)
        }
        list.add(562)
        return list.toIntArray()
    }

    private fun encodeSamsung32(code: Long): IntArray {
        val list = ArrayList<Int>(68)
        list.add(4500)
        list.add(4500)
        for (i in 31 downTo 0) {
            list.add(560)
            val bit = (code ushr i) and 1L
            list.add(if (bit == 1L) 1690 else 560)
        }
        list.add(560)
        return list.toIntArray()
    }

    private fun encodeSonySirc(data: Int, bits: Int): IntArray {
        val list = ArrayList<Int>(bits * 2 + 2)
        list.add(2400)
        list.add(600)
        for (i in 0 until bits) {
            val bit = (data ushr i) and 1
            list.add(if (bit == 1) 1200 else 600)
            list.add(600)
        }
        return list.toIntArray()
    }

    private fun encodeRc5(address: Int, command: Int, toggle: Int): IntArray {
        val unit = 889
        val frame = (1 shl 13) or
            ((if (command < 64) 1 else 0) shl 12) or
            ((toggle and 1) shl 11) or
            ((address and 0x1F) shl 6) or
            (command and 0x3F)

        val halfBits = ArrayList<Pair<Boolean, Int>>(28)
        for (i in 13 downTo 0) {
            val bit = (frame ushr i) and 1
            if (bit == 1) {
                halfBits.add(false to unit)
                halfBits.add(true to unit)
            } else {
                halfBits.add(true to unit)
                halfBits.add(false to unit)
            }
        }
        return mergeHalfBits(halfBits)
    }

    private fun encodeRc6(address: Int, command: Int, toggle: Int): IntArray {
        val unit = 444
        val halfBits = ArrayList<Pair<Boolean, Int>>(48)
        // Leader: 6*unit Mark, 2*unit Space
        halfBits.add(true to unit * 6)
        halfBits.add(false to unit * 2)

        fun addBit(bit: Int, duration: Int) {
            if (bit == 1) {
                halfBits.add(true to duration)
                halfBits.add(false to duration)
            } else {
                halfBits.add(false to duration)
                halfBits.add(true to duration)
            }
        }

        // Start bit (1), Mode (0,0,0), Trailer (toggle, double width)
        addBit(1, unit)
        addBit(0, unit)
        addBit(0, unit)
        addBit(0, unit)
        addBit(toggle and 1, unit * 2)

        for (i in 7 downTo 0) addBit((address ushr i) and 1, unit)
        for (i in 7 downTo 0) addBit((command ushr i) and 1, unit)

        return mergeHalfBits(halfBits)
    }

    private fun mergeHalfBits(halfBits: List<Pair<Boolean, Int>>): IntArray {
        val merged = ArrayList<Int>(halfBits.size)
        var started = false
        var lastState = false
        for ((isMark, us) in halfBits) {
            if (!started) {
                if (!isMark) continue // ConsumerIrManager must start with a Mark
                started = true
                lastState = true
                merged.add(us)
            } else if (isMark == lastState) {
                merged[merged.lastIndex] = merged.last() + us
            } else {
                merged.add(us)
                lastState = isMark
            }
        }
        return merged.toIntArray()
    }

    private fun encodePanasonic(address: Int, data: Long): IntArray {
        val unit = 432
        val list = ArrayList<Int>(100)
        list.add(unit * 8)
        list.add(unit * 4)
        // 16-bit vendor/address + 32-bit data = 48 bits LSB first
        val full = (data shl 16) or (address.toLong() and 0xFFFFL)
        for (i in 0 until 48) {
            list.add(unit)
            val bit = (full ushr i) and 1L
            list.add(if (bit == 1L) unit * 3 else unit)
        }
        list.add(unit)
        return list.toIntArray()
    }

    private fun encodeJvc(address: Int, command: Int): IntArray {
        val list = ArrayList<Int>(36)
        list.add(8400)
        list.add(4200)
        val frame = ((command and 0xFF) shl 8) or (address and 0xFF)
        for (i in 0 until 16) {
            list.add(526)
            val bit = (frame ushr i) and 1
            list.add(if (bit == 1) 1574 else 526)
        }
        list.add(526)
        return list.toIntArray()
    }

    private fun encodeSharp(address: Int, command: Int): IntArray {
        val list = ArrayList<Int>(34)
        val frame = ((command and 0xFF) shl 5) or (address and 0x1F)
        for (i in 0 until 15) {
            list.add(320)
            val bit = (frame ushr i) and 1
            list.add(if (bit == 1) 1680 else 680)
        }
        list.add(320)
        return list.toIntArray()
    }

    private fun encodeRca(address: Int, command: Int): IntArray {
        val list = ArrayList<Int>(52)
        list.add(4000)
        list.add(4000)
        val payload = ((address and 0x0F) shl 8) or (command and 0xFF)
        val invPayload = payload xor 0xFFF
        val full24 = (payload shl 12) or invPayload
        for (i in 23 downTo 0) {
            list.add(500)
            val bit = (full24 ushr i) and 1
            list.add(if (bit == 1) 2000 else 1000)
        }
        list.add(500)
        return list.toIntArray()
    }

    companion object {
        fun parseProntoHex(pronto: String): IrSignal.Raw? {
            val tokens = pronto.trim().split(Regex("\\s+"))
            if (tokens.size < 6) return null
            val values = tokens.mapNotNull { it.toIntOrNull(16) }
            if (values.size != tokens.size || values[0] != 0) return null
            val freqDiv = values[1]
            if (freqDiv == 0) return null
            val freqHz = (1_000_000.0 / (freqDiv * 0.241246)).toInt()
            val periodUs = 1_000_000.0 / freqHz
            val pattern = values.drop(4).map { (it * periodUs).toInt().coerceAtLeast(1) }.toIntArray()
            if (pattern.isEmpty()) return null
            return IrSignal.Raw(freqHz, pattern)
        }

        fun parseFlipperFile(content: String): List<FlipperIrEntry> {
            val entries = mutableListOf<FlipperIrEntry>()
            val blocks = content.split(Regex("(?m)^#\\s*$"))
            for (block in blocks) {
                var name = ""
                for (line in block.lines()) {
                    if (line.trim().startsWith("name:", ignoreCase = true)) {
                        name = line.substringAfter(':').trim()
                        break
                    }
                }
                val signal = parseFlipperBlock(block)
                if (signal != null && name.isNotEmpty()) {
                    entries.add(FlipperIrEntry(name, signal))
                }
            }
            return entries
        }

        fun parseFlipperBlock(block: String): IrSignal? {
            var type = ""
            var protocol = ""
            var freq = 38000
            var addressBytes = emptyList<Long>()
            var commandBytes = emptyList<Long>()
            var rawData: IntArray? = null

            for (line in block.lines()) {
                val idx = line.indexOf(':')
                if (idx <= 0) continue
                val key = line.substring(0, idx).trim().lowercase()
                val value = line.substring(idx + 1).trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "protocol" -> protocol = value.lowercase()
                    "frequency" -> freq = value.toIntOrNull() ?: 38000
                    "address" -> addressBytes = parseFlipperHexBytes(value)
                    "command" -> commandBytes = parseFlipperHexBytes(value)
                    "data" -> {
                        if (type == "raw") {
                            rawData = value.split(Regex("\\s+"))
                                .mapNotNull { it.toIntOrNull() }
                                .toIntArray()
                        }
                    }
                }
            }

            if (type == "raw" && rawData != null && rawData.isNotEmpty()) {
                return IrSignal.Raw(freq, rawData)
            }
            if (type == "parsed" && addressBytes.isNotEmpty() && commandBytes.isNotEmpty()) {
                val a0 = addressBytes[0] and 0xFFL
                val a1 = addressBytes.getOrElse(1) { a0 xor 0xFFL } and 0xFFL
                val c0 = commandBytes[0] and 0xFFL
                val c1 = commandBytes.getOrElse(1) { c0 xor 0xFFL } and 0xFFL
                val nec32 = (a0 shl 24) or (a1 shl 16) or (c0 shl 8) or c1

                return when {
                    protocol == "nec" || protocol == "necext" || protocol == "nec42" ->
                        IrSignal.Nec(nec32, freq)
                    protocol.startsWith("samsung") ->
                        IrSignal.Samsung32((a0 shl 24) or (a0 shl 16) or (c0 shl 8) or (c0 xor 0xFFL), freq)
                    protocol.startsWith("sirc") ->
                        IrSignal.SonySirc(((a0.toInt() and 0x1F) shl 7) or (c0.toInt() and 0x7F), 12, 40000)
                    protocol == "rc5" || protocol == "rc5x" ->
                        IrSignal.Rc5(a0.toInt(), c0.toInt(), 0, 36000)
                    protocol == "rc6" ->
                        IrSignal.Rc6(a0.toInt(), c0.toInt(), 0, 36000)
                    protocol == "kaseikyo" ->
                        IrSignal.Panasonic(a0.toInt(), c0, 37000)
                    protocol == "rca" ->
                        IrSignal.Rca(a0.toInt(), c0.toInt(), if (freq > 0) freq else 56000)
                    else -> null
                }
            }
            return null
        }

        private fun parseFlipperHexBytes(hex: String): List<Long> {
            return hex.trim().split(Regex("\\s+")).mapNotNull { it.toLongOrNull(16) }
        }

        val BRAND_PRESETS: Map<TvBrand, Map<IrButton, IrSignal>> = mapOf(
            TvBrand.SAMSUNG to mapOf(
                IrButton.POWER to IrSignal.Samsung32(0xE0E040BF),
                IrButton.VOL_UP to IrSignal.Samsung32(0xE0E0E01F),
                IrButton.VOL_DOWN to IrSignal.Samsung32(0xE0E0D02F),
                IrButton.MUTE to IrSignal.Samsung32(0xE0E0F00F),
                IrButton.INPUT to IrSignal.Samsung32(0xE0E0807F),
                IrButton.SETTINGS to IrSignal.Samsung32(0xE0E058A7),
                IrButton.HOME to IrSignal.Samsung32(0xE0E09E61),
                IrButton.BACK to IrSignal.Samsung32(0xE0E01AE5),
                IrButton.UP to IrSignal.Samsung32(0xE0E006F9),
                IrButton.DOWN to IrSignal.Samsung32(0xE0E08679),
                IrButton.LEFT to IrSignal.Samsung32(0xE0E0A659),
                IrButton.RIGHT to IrSignal.Samsung32(0xE0E046B9),
                IrButton.OK to IrSignal.Samsung32(0xE0E016E9),
                IrButton.CH_UP to IrSignal.Samsung32(0xE0E048B7),
                IrButton.CH_DOWN to IrSignal.Samsung32(0xE0E008F7)
            ),
            TvBrand.LG to mapOf(
                IrButton.POWER to IrSignal.Nec(0x20DF10EF),
                IrButton.VOL_UP to IrSignal.Nec(0x20DF40BF),
                IrButton.VOL_DOWN to IrSignal.Nec(0x20DFC03F),
                IrButton.MUTE to IrSignal.Nec(0x20DF906F),
                IrButton.INPUT to IrSignal.Nec(0x20DFD02F),
                IrButton.SETTINGS to IrSignal.Nec(0x20DFC23D),
                IrButton.HOME to IrSignal.Nec(0x20DF3EC1),
                IrButton.BACK to IrSignal.Nec(0x20DF14EB),
                IrButton.UP to IrSignal.Nec(0x20DF02FD),
                IrButton.DOWN to IrSignal.Nec(0x20DF827D),
                IrButton.LEFT to IrSignal.Nec(0x20DFE01F),
                IrButton.RIGHT to IrSignal.Nec(0x20DF609F),
                IrButton.OK to IrSignal.Nec(0x20DF22DD),
                IrButton.CH_UP to IrSignal.Nec(0x20DF00FF),
                IrButton.CH_DOWN to IrSignal.Nec(0x20DF807F)
            ),
            TvBrand.SONY to mapOf(
                IrButton.POWER to IrSignal.SonySirc(0xA90),
                IrButton.VOL_UP to IrSignal.SonySirc(0x490),
                IrButton.VOL_DOWN to IrSignal.SonySirc(0xC90),
                IrButton.MUTE to IrSignal.SonySirc(0x290),
                IrButton.INPUT to IrSignal.SonySirc(0xA50),
                IrButton.SETTINGS to IrSignal.SonySirc(0x070),
                IrButton.HOME to IrSignal.SonySirc(0x070),
                IrButton.BACK to IrSignal.SonySirc(0xC70),
                IrButton.UP to IrSignal.SonySirc(0x2F0),
                IrButton.DOWN to IrSignal.SonySirc(0xAF0),
                IrButton.LEFT to IrSignal.SonySirc(0x2D0),
                IrButton.RIGHT to IrSignal.SonySirc(0xCD0),
                IrButton.OK to IrSignal.SonySirc(0xA70),
                IrButton.CH_UP to IrSignal.SonySirc(0x090),
                IrButton.CH_DOWN to IrSignal.SonySirc(0x890)
            ),
            TvBrand.TCL to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00DF02FD),
                IrButton.VOL_UP to IrSignal.Nec(0x00DF40BF),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00DFC03F),
                IrButton.MUTE to IrSignal.Nec(0x00DF906F),
                IrButton.INPUT to IrSignal.Nec(0x00DFD02F),
                IrButton.SETTINGS to IrSignal.Nec(0x00DFC23D),
                IrButton.HOME to IrSignal.Nec(0x00DF3EC1),
                IrButton.BACK to IrSignal.Nec(0x00DF14EB),
                IrButton.UP to IrSignal.Nec(0x00DF0AF5),
                IrButton.DOWN to IrSignal.Nec(0x00DF8A75),
                IrButton.LEFT to IrSignal.Nec(0x00DFEA15),
                IrButton.RIGHT to IrSignal.Nec(0x00DF6A95),
                IrButton.OK to IrSignal.Nec(0x00DF2AD5),
                IrButton.CH_UP to IrSignal.Nec(0x00DF00FF),
                IrButton.CH_DOWN to IrSignal.Nec(0x00DF807F)
            ),
            TvBrand.HISENSE to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00BF0DF2),
                IrButton.VOL_UP to IrSignal.Nec(0x00BF44BB),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00BF43BC),
                IrButton.MUTE to IrSignal.Nec(0x00BF0EF1),
                IrButton.INPUT to IrSignal.Nec(0x00BF12ED),
                IrButton.SETTINGS to IrSignal.Nec(0x00BF52AD),
                IrButton.HOME to IrSignal.Nec(0x00BF14EB),
                IrButton.BACK to IrSignal.Nec(0x00BF48B7),
                IrButton.UP to IrSignal.Nec(0x00BF16E9),
                IrButton.DOWN to IrSignal.Nec(0x00BF17E8),
                IrButton.LEFT to IrSignal.Nec(0x00BF19E6),
                IrButton.RIGHT to IrSignal.Nec(0x00BF18E7),
                IrButton.OK to IrSignal.Nec(0x00BF15EA),
                IrButton.CH_UP to IrSignal.Nec(0x00BF4AB5),
                IrButton.CH_DOWN to IrSignal.Nec(0x00BF4BB4)
            ),
            TvBrand.PHILIPS_RC5 to mapOf(
                IrButton.POWER to IrSignal.Rc5(0, 12),
                IrButton.VOL_UP to IrSignal.Rc5(0, 16),
                IrButton.VOL_DOWN to IrSignal.Rc5(0, 17),
                IrButton.MUTE to IrSignal.Rc5(0, 13),
                IrButton.INPUT to IrSignal.Rc5(0, 56),
                IrButton.SETTINGS to IrSignal.Rc5(0, 59),
                IrButton.HOME to IrSignal.Rc5(0, 84),
                IrButton.BACK to IrSignal.Rc5(0, 10),
                IrButton.UP to IrSignal.Rc5(0, 80),
                IrButton.DOWN to IrSignal.Rc5(0, 81),
                IrButton.LEFT to IrSignal.Rc5(0, 85),
                IrButton.RIGHT to IrSignal.Rc5(0, 86),
                IrButton.OK to IrSignal.Rc5(0, 87),
                IrButton.CH_UP to IrSignal.Rc5(0, 32),
                IrButton.CH_DOWN to IrSignal.Rc5(0, 33)
            ),
            TvBrand.PHILIPS_RC6 to mapOf(
                IrButton.POWER to IrSignal.Rc6(0, 12),
                IrButton.VOL_UP to IrSignal.Rc6(0, 16),
                IrButton.VOL_DOWN to IrSignal.Rc6(0, 17),
                IrButton.MUTE to IrSignal.Rc6(0, 13),
                IrButton.INPUT to IrSignal.Rc6(0, 56),
                IrButton.SETTINGS to IrSignal.Rc6(0, 191),
                IrButton.HOME to IrSignal.Rc6(0, 84),
                IrButton.BACK to IrSignal.Rc6(0, 10),
                IrButton.UP to IrSignal.Rc6(0, 88),
                IrButton.DOWN to IrSignal.Rc6(0, 89),
                IrButton.LEFT to IrSignal.Rc6(0, 90),
                IrButton.RIGHT to IrSignal.Rc6(0, 91),
                IrButton.OK to IrSignal.Rc6(0, 92),
                IrButton.CH_UP to IrSignal.Rc6(0, 32),
                IrButton.CH_DOWN to IrSignal.Rc6(0, 33)
            ),
            TvBrand.XIAOMI to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00FF02FD),
                IrButton.VOL_UP to IrSignal.Nec(0x00FF827D),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00FF42BD),
                IrButton.MUTE to IrSignal.Nec(0x00FFC23D),
                IrButton.INPUT to IrSignal.Nec(0x00FF22DD),
                IrButton.SETTINGS to IrSignal.Nec(0x00FFA25D),
                IrButton.HOME to IrSignal.Nec(0x00FF629D),
                IrButton.BACK to IrSignal.Nec(0x00FFE21D),
                IrButton.UP to IrSignal.Nec(0x00FF12ED),
                IrButton.DOWN to IrSignal.Nec(0x00FF926D),
                IrButton.LEFT to IrSignal.Nec(0x00FF52AD),
                IrButton.RIGHT to IrSignal.Nec(0x00FFD22D),
                IrButton.OK to IrSignal.Nec(0x00FF32CD)
            ),
            TvBrand.PANASONIC to mapOf(
                IrButton.POWER to IrSignal.Panasonic(0x0200, 0x3D000080),
                IrButton.VOL_UP to IrSignal.Panasonic(0x0200, 0x20000080),
                IrButton.VOL_DOWN to IrSignal.Panasonic(0x0200, 0x21000080),
                IrButton.MUTE to IrSignal.Panasonic(0x0200, 0x32000080),
                IrButton.INPUT to IrSignal.Panasonic(0x0200, 0x05000080),
                IrButton.SETTINGS to IrSignal.Panasonic(0x0200, 0x52000080),
                IrButton.HOME to IrSignal.Panasonic(0x0200, 0x5A000080),
                IrButton.BACK to IrSignal.Panasonic(0x0200, 0xD4000080),
                IrButton.UP to IrSignal.Panasonic(0x0200, 0x4A000080),
                IrButton.DOWN to IrSignal.Panasonic(0x0200, 0x4B000080),
                IrButton.LEFT to IrSignal.Panasonic(0x0200, 0x4E000080),
                IrButton.RIGHT to IrSignal.Panasonic(0x0200, 0x4F000080),
                IrButton.OK to IrSignal.Panasonic(0x0200, 0x49000080)
            ),
            TvBrand.SHARP to mapOf(
                IrButton.POWER to IrSignal.Sharp(1, 0x16),
                IrButton.VOL_UP to IrSignal.Sharp(1, 0x14),
                IrButton.VOL_DOWN to IrSignal.Sharp(1, 0x15),
                IrButton.MUTE to IrSignal.Sharp(1, 0x17),
                IrButton.INPUT to IrSignal.Sharp(1, 0x13),
                IrButton.SETTINGS to IrSignal.Sharp(1, 0x4D),
                IrButton.CH_UP to IrSignal.Sharp(1, 0x11),
                IrButton.CH_DOWN to IrSignal.Sharp(1, 0x12)
            ),
            TvBrand.TOSHIBA to mapOf(
                IrButton.POWER to IrSignal.Nec(0x02FD48B7),
                IrButton.VOL_UP to IrSignal.Nec(0x02FD58A7),
                IrButton.VOL_DOWN to IrSignal.Nec(0x02FD7887),
                IrButton.MUTE to IrSignal.Nec(0x02FD08F7),
                IrButton.INPUT to IrSignal.Nec(0x02FDF00F),
                IrButton.SETTINGS to IrSignal.Nec(0x02FDC23D),
                IrButton.HOME to IrSignal.Nec(0x02FD22DD),
                IrButton.BACK to IrSignal.Nec(0x02FD1AE5),
                IrButton.UP to IrSignal.Nec(0x02FD41BE),
                IrButton.DOWN to IrSignal.Nec(0x02FDC13E),
                IrButton.LEFT to IrSignal.Nec(0x02FDB847),
                IrButton.RIGHT to IrSignal.Nec(0x02FD9867),
                IrButton.OK to IrSignal.Nec(0x02FD847B)
            ),
            TvBrand.ROKU_TV to mapOf(
                IrButton.POWER to IrSignal.Nec(0x57E3E817),
                IrButton.VOL_UP to IrSignal.Nec(0x57E3F00F),
                IrButton.VOL_DOWN to IrSignal.Nec(0x57E308F7),
                IrButton.MUTE to IrSignal.Nec(0x57E304FB),
                IrButton.HOME to IrSignal.Nec(0x57E3C03F),
                IrButton.BACK to IrSignal.Nec(0x57E36699),
                IrButton.UP to IrSignal.Nec(0x57E39867),
                IrButton.DOWN to IrSignal.Nec(0x57E3CC33),
                IrButton.LEFT to IrSignal.Nec(0x57E37887),
                IrButton.RIGHT to IrSignal.Nec(0x57E3B44B),
                IrButton.OK to IrSignal.Nec(0x57E354AB),
                IrButton.SETTINGS to IrSignal.Nec(0x57E38679)
            ),
            TvBrand.VIZIO to mapOf(
                IrButton.POWER to IrSignal.Nec(0x20DF10EF),
                IrButton.VOL_UP to IrSignal.Nec(0x20DF40BF),
                IrButton.VOL_DOWN to IrSignal.Nec(0x20DFC03F),
                IrButton.MUTE to IrSignal.Nec(0x20DF906F),
                IrButton.INPUT to IrSignal.Nec(0x20DFD02F),
                IrButton.SETTINGS to IrSignal.Nec(0x20DFC23D),
                IrButton.UP to IrSignal.Nec(0x20DF02FD),
                IrButton.DOWN to IrSignal.Nec(0x20DF827D),
                IrButton.LEFT to IrSignal.Nec(0x20DFE01F),
                IrButton.RIGHT to IrSignal.Nec(0x20DF609F),
                IrButton.OK to IrSignal.Nec(0x20DF22DD)
            ),
            TvBrand.JVC to mapOf(
                IrButton.POWER to IrSignal.Jvc(0x03, 0x17),
                IrButton.VOL_UP to IrSignal.Jvc(0x03, 0x1E),
                IrButton.VOL_DOWN to IrSignal.Jvc(0x03, 0x1F),
                IrButton.MUTE to IrSignal.Jvc(0x03, 0x1C),
                IrButton.INPUT to IrSignal.Jvc(0x03, 0x13),
                IrButton.CH_UP to IrSignal.Jvc(0x03, 0x19),
                IrButton.CH_DOWN to IrSignal.Jvc(0x03, 0x18)
            ),
            TvBrand.RCA_SMART to mapOf(
                IrButton.POWER to IrSignal.Nec(0xA05F02FD),
                IrButton.VOL_UP to IrSignal.Nec(0xA05F40BF),
                IrButton.VOL_DOWN to IrSignal.Nec(0xA05FC03F),
                IrButton.MUTE to IrSignal.Nec(0xA05F906F),
                IrButton.INPUT to IrSignal.Nec(0xA05FD02F),
                IrButton.SETTINGS to IrSignal.Nec(0xA05FC23D),
                IrButton.HOME to IrSignal.Nec(0xA05F3EC1),
                IrButton.BACK to IrSignal.Nec(0xA05F14EB),
                IrButton.UP to IrSignal.Nec(0xA05F0AF5),
                IrButton.DOWN to IrSignal.Nec(0xA05F8A75),
                IrButton.LEFT to IrSignal.Nec(0xA05FEA15),
                IrButton.RIGHT to IrSignal.Nec(0xA05F6A95),
                IrButton.OK to IrSignal.Nec(0xA05F2AD5),
                IrButton.CH_UP to IrSignal.Nec(0xA05F00FF),
                IrButton.CH_DOWN to IrSignal.Nec(0xA05F807F)
            ),
            TvBrand.RCA_CLASSIC_56 to mapOf(
                IrButton.POWER to IrSignal.Rca(0x0F, 0x2A, 56000),
                IrButton.VOL_UP to IrSignal.Rca(0x0F, 0x2E, 56000),
                IrButton.VOL_DOWN to IrSignal.Rca(0x0F, 0x2F, 56000),
                IrButton.MUTE to IrSignal.Rca(0x0F, 0x3F, 56000),
                IrButton.INPUT to IrSignal.Rca(0x0F, 0x38, 56000),
                IrButton.SETTINGS to IrSignal.Rca(0x0F, 0x52, 56000),
                IrButton.UP to IrSignal.Rca(0x0F, 0x59, 56000),
                IrButton.DOWN to IrSignal.Rca(0x0F, 0x58, 56000),
                IrButton.LEFT to IrSignal.Rca(0x0F, 0x56, 56000),
                IrButton.RIGHT to IrSignal.Rca(0x0F, 0x57, 56000),
                IrButton.OK to IrSignal.Rca(0x0F, 0x5B, 56000),
                IrButton.CH_UP to IrSignal.Rca(0x0F, 0x2D, 56000),
                IrButton.CH_DOWN to IrSignal.Rca(0x0F, 0x2C, 56000)
            ),
            TvBrand.RCA_38 to mapOf(
                IrButton.POWER to IrSignal.Rca(0x00, 0x0B, 38000),
                IrButton.VOL_UP to IrSignal.Rca(0x00, 0x12, 38000),
                IrButton.VOL_DOWN to IrSignal.Rca(0x00, 0x13, 38000),
                IrButton.MUTE to IrSignal.Rca(0x00, 0x0D, 38000),
                IrButton.INPUT to IrSignal.Rca(0x00, 0x38, 38000),
                IrButton.SETTINGS to IrSignal.Rca(0x00, 0x52, 38000),
                IrButton.CH_UP to IrSignal.Rca(0x00, 0x10, 38000),
                IrButton.CH_DOWN to IrSignal.Rca(0x00, 0x11, 38000)
            ),
            TvBrand.OEM_LATAM_1 to mapOf(
                IrButton.POWER to IrSignal.Nec(0x40BF48B7),
                IrButton.VOL_UP to IrSignal.Nec(0x40BF58A7),
                IrButton.VOL_DOWN to IrSignal.Nec(0x40BF7887),
                IrButton.MUTE to IrSignal.Nec(0x40BF08F7),
                IrButton.INPUT to IrSignal.Nec(0x40BFF00F),
                IrButton.SETTINGS to IrSignal.Nec(0x40BFC23D),
                IrButton.HOME to IrSignal.Nec(0x40BF22DD),
                IrButton.BACK to IrSignal.Nec(0x40BF1AE5),
                IrButton.UP to IrSignal.Nec(0x40BF41BE),
                IrButton.DOWN to IrSignal.Nec(0x40BFC13E),
                IrButton.LEFT to IrSignal.Nec(0x40BFB847),
                IrButton.RIGHT to IrSignal.Nec(0x40BF9867),
                IrButton.OK to IrSignal.Nec(0x40BF847B)
            ),
            TvBrand.OEM_LATAM_2 to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00FF4AB5),
                IrButton.VOL_UP to IrSignal.Nec(0x00FF0AF5),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00FF8A75),
                IrButton.MUTE to IrSignal.Nec(0x00FFCA35),
                IrButton.INPUT to IrSignal.Nec(0x00FF5AA5),
                IrButton.SETTINGS to IrSignal.Nec(0x00FF2AD5),
                IrButton.HOME to IrSignal.Nec(0x00FFAA55),
                IrButton.BACK to IrSignal.Nec(0x00FF6A95),
                IrButton.UP to IrSignal.Nec(0x00FF48B7),
                IrButton.DOWN to IrSignal.Nec(0x00FFC837),
                IrButton.LEFT to IrSignal.Nec(0x00FF8877),
                IrButton.RIGHT to IrSignal.Nec(0x00FF08F7),
                IrButton.OK to IrSignal.Nec(0x00FFA857)
            ),
            TvBrand.AUDIO_JBL_SOUNDBAR to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00FF02FD),
                IrButton.VOL_UP to IrSignal.Nec(0x00FF827D),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00FF42BD),
                IrButton.MUTE to IrSignal.Nec(0x00FFC23D),
                IrButton.INPUT to IrSignal.Nec(0x00FF22DD),
                IrButton.SETTINGS to IrSignal.Nec(0x00FFA25D)
            ),
            TvBrand.AUDIO_SAMSUNG_SOUNDBAR to mapOf(
                IrButton.POWER to IrSignal.Samsung32(0xC2CA807F),
                IrButton.VOL_UP to IrSignal.Samsung32(0xC2CAC837),
                IrButton.VOL_DOWN to IrSignal.Samsung32(0xC2CA08F7),
                IrButton.MUTE to IrSignal.Samsung32(0xC2CA48B7),
                IrButton.INPUT to IrSignal.Samsung32(0xC2CA8877),
                IrButton.SETTINGS to IrSignal.Samsung32(0xC2CAD02F)
            ),
            TvBrand.AUDIO_LG_SOUNDBAR to mapOf(
                IrButton.POWER to IrSignal.Nec(0x20DF10EF),
                IrButton.VOL_UP to IrSignal.Nec(0x20DF40BF),
                IrButton.VOL_DOWN to IrSignal.Nec(0x20DFC03F),
                IrButton.MUTE to IrSignal.Nec(0x20DF906F),
                IrButton.INPUT to IrSignal.Nec(0x20DFD02F)
            ),
            TvBrand.AUDIO_SONY_SYSTEM to mapOf(
                IrButton.POWER to IrSignal.SonySirc(0xA81, 12),
                IrButton.VOL_UP to IrSignal.SonySirc(0x481, 12),
                IrButton.VOL_DOWN to IrSignal.SonySirc(0xC81, 12),
                IrButton.MUTE to IrSignal.SonySirc(0x281, 12),
                IrButton.INPUT to IrSignal.SonySirc(0xA01, 12)
            ),
            TvBrand.PROJECTOR_EPSON to mapOf(
                IrButton.POWER to IrSignal.Nec(0x8355906F),
                IrButton.VOL_UP to IrSignal.Nec(0x8355C43B),
                IrButton.VOL_DOWN to IrSignal.Nec(0x8355C53A),
                IrButton.MUTE to IrSignal.Nec(0x8355916E),
                IrButton.INPUT to IrSignal.Nec(0x8355926D),
                IrButton.SETTINGS to IrSignal.Nec(0x8355A15E),
                IrButton.UP to IrSignal.Nec(0x8355B24D),
                IrButton.DOWN to IrSignal.Nec(0x8355B34C),
                IrButton.LEFT to IrSignal.Nec(0x8355B44B),
                IrButton.RIGHT to IrSignal.Nec(0x8355B54A),
                IrButton.OK to IrSignal.Nec(0x8355B649),
                IrButton.BACK to IrSignal.Nec(0x8355A25D)
            ),
            TvBrand.PROJECTOR_BENQ to mapOf(
                IrButton.POWER to IrSignal.Nec(0x000C02FD),
                IrButton.VOL_UP to IrSignal.Nec(0x000C827D),
                IrButton.VOL_DOWN to IrSignal.Nec(0x000C42BD),
                IrButton.MUTE to IrSignal.Nec(0x000CC23D),
                IrButton.INPUT to IrSignal.Nec(0x000C22DD),
                IrButton.SETTINGS to IrSignal.Nec(0x000CA25D),
                IrButton.UP to IrSignal.Nec(0x000C0AF5),
                IrButton.DOWN to IrSignal.Nec(0x000C8A75),
                IrButton.LEFT to IrSignal.Nec(0x000C4AB5),
                IrButton.RIGHT to IrSignal.Nec(0x000CCA35),
                IrButton.OK to IrSignal.Nec(0x000C2AD5),
                IrButton.BACK to IrSignal.Nec(0x000C6A95)
            ),
            TvBrand.STREAMING_APPLE_TV to mapOf(
                IrButton.UP to IrSignal.Nec(0x77E15080),
                IrButton.DOWN to IrSignal.Nec(0x77E13080),
                IrButton.LEFT to IrSignal.Nec(0x77E19080),
                IrButton.RIGHT to IrSignal.Nec(0x77E16080),
                IrButton.OK to IrSignal.Nec(0x77E1BA80),
                IrButton.BACK to IrSignal.Nec(0x77E1C080),
                IrButton.HOME to IrSignal.Nec(0x77E1C080)
            ),
            TvBrand.LED_RGB_24KEY to mapOf(
                IrButton.POWER to IrSignal.Nec(0x00F7C03F),
                IrButton.VOL_UP to IrSignal.Nec(0x00F700FF),
                IrButton.VOL_DOWN to IrSignal.Nec(0x00F7807F),
                IrButton.MUTE to IrSignal.Nec(0x00F740BF),
                IrButton.INPUT to IrSignal.Nec(0x00F7E01F),
                IrButton.UP to IrSignal.Nec(0x00F720DF),
                IrButton.DOWN to IrSignal.Nec(0x00F7A05F),
                IrButton.LEFT to IrSignal.Nec(0x00F7609F),
                IrButton.RIGHT to IrSignal.Nec(0x00F7D02F),
                IrButton.OK to IrSignal.Nec(0x00F7E01F)
            )
        )
    }
}
