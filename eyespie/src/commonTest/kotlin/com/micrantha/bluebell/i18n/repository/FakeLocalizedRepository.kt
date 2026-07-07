package com.micrantha.bluebell.i18n.repository

import com.micrantha.bluebell.i18n.entity.LocalizedString

class FakeLocalizedRepository : LocalizedRepository {
    override fun string(str: LocalizedString): String = str.key
    override fun string(str: LocalizedString, vararg args: Any): String = str.key
    override fun format(epochSeconds: Long, format: String, timeZone: String): String = ""
    override fun format(format: String, vararg args: Any): String = ""
}
