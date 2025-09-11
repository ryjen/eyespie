package com.micrantha.bluebell.data.download

class DownloadTaskManager {
    private val downloadTasks = mutableMapOf<String, DownloadTask>()

    operator fun set(tag: String, task: DownloadTask) {
        downloadTasks[tag] = task
    }

    fun update(tag: String, update: (DownloadTask) -> DownloadTask) {
        downloadTasks[tag] = downloadTasks[tag]?.let(update) ?: return
    }

    operator fun get(tag: String) = downloadTasks[tag]
}
