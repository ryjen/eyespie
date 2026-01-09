package com.micrantha.bluebell.platform

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use

interface FileSystem {

    fun filesPath(): Path

    fun sharedFilesPath(): Path

    fun fileWrite(path: Path, data: ByteArray) {
        FileSystem.SYSTEM.sink(path).use { sink ->
            sink.buffer().use { buf ->
                buf.write(data)
                buf.flush()
            }
        }
    }

    fun fileRead(path: Path): ByteArray {
        return FileSystem.SYSTEM.source(path).use { src ->
            src.buffer().use { buf ->
                buf.readByteArray()
            }
        }
    }
}
