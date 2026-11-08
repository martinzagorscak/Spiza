package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.appUpdate.AppVersionProvider

class FakeAppVersionProvider(
    val version: Long = 10_000L,
) : AppVersionProvider {
    override fun currentVersion(): Long = version
}
