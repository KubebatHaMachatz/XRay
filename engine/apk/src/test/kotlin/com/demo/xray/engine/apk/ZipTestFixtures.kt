package com.demo.xray.engine.apk

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

internal fun minimalApkBytes(manifestBytes: ByteArray = byteArrayOf(1, 2, 3)): ByteArray =
    buildZip(
        ANDROID_MANIFEST_ENTRY_NAME to manifestBytes,
        "classes.dex" to ByteArray(16),
    )

/**
 * Raw ZIP (STORED, no compression) encoder that, unlike [java.util.zip.ZipOutputStream], allows
 * writing duplicate entry names. Test-only: used to exercise [ApkValidator]'s own duplicate
 * detection, which real ZIP-writing libraries won't let us trigger.
 */
internal fun buildRawZipAllowingDuplicates(entries: List<Pair<String, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()
    val crc = java.util.zip.CRC32()
    data class Written(val name: ByteArray, val data: ByteArray, val crc: Long, val offset: Int)
    val written = mutableListOf<Written>()

    for ((name, data) in entries) {
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        crc.reset()
        crc.update(data)
        val offset = out.size()
        writeU32(out, 0x04034b50) // local file header signature
        writeU16(out, 20) // version needed
        writeU16(out, 0) // flags
        writeU16(out, 0) // method: STORED
        writeU16(out, 0) // mod time
        writeU16(out, 0x21) // mod date
        writeU32(out, crc.value)
        writeU32(out, data.size.toLong())
        writeU32(out, data.size.toLong())
        writeU16(out, nameBytes.size)
        writeU16(out, 0) // extra length
        out.write(nameBytes)
        out.write(data)
        written += Written(nameBytes, data, crc.value, offset)
    }

    val centralDirStart = out.size()
    for (w in written) {
        writeU32(out, 0x02014b50) // central directory signature
        writeU16(out, 20) // version made by
        writeU16(out, 20) // version needed
        writeU16(out, 0) // flags
        writeU16(out, 0) // method
        writeU16(out, 0) // mod time
        writeU16(out, 0x21) // mod date
        writeU32(out, w.crc)
        writeU32(out, w.data.size.toLong())
        writeU32(out, w.data.size.toLong())
        writeU16(out, w.name.size)
        writeU16(out, 0) // extra length
        writeU16(out, 0) // comment length
        writeU16(out, 0) // disk number start
        writeU16(out, 0) // internal attrs
        writeU32(out, 0) // external attrs
        writeU32(out, w.offset.toLong())
        out.write(w.name)
    }
    val centralDirSize = out.size() - centralDirStart

    writeU32(out, 0x06054b50) // end of central directory signature
    writeU16(out, 0) // disk number
    writeU16(out, 0) // cd start disk
    writeU16(out, written.size) // entries on this disk
    writeU16(out, written.size) // total entries
    writeU32(out, centralDirSize.toLong())
    writeU32(out, centralDirStart.toLong())
    writeU16(out, 0) // comment length

    return out.toByteArray()
}

private fun writeU16(out: ByteArrayOutputStream, v: Int) {
    out.write(v and 0xFF)
    out.write((v shr 8) and 0xFF)
}

private fun writeU32(out: ByteArrayOutputStream, v: Long) {
    out.write((v and 0xFF).toInt())
    out.write(((v shr 8) and 0xFF).toInt())
    out.write(((v shr 16) and 0xFF).toInt())
    out.write(((v shr 24) and 0xFF).toInt())
}
