package com.micrantha.bluebell.platform

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use

interface FileSystem {

    fun filesPath(): Path

    fun modelsPath(): Path

    fun fileExists(path: Path): Boolean {
        return FileSystem.SYSTEM.exists(path)
    }

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

    fun fileStream(path: Path, onRead: (bytes: ByteArray) -> Unit) {
        return FileSystem.SYSTEM.source(path).use { src ->
            src.buffer().use { buf ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (buf.read(buffer).also { bytesRead = it } != -1) {
                    onRead(buffer.copyOf(bytesRead))
                }
            }
        }
    }

}
