package com.wizpizz.onepluspluslauncher.service

import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object ModuleService {
    private val initialized = AtomicBoolean(false)
    private val mutableService = MutableStateFlow<XposedService?>(null)
    val service = mutableService.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                mutableService.value = service
            }

            override fun onServiceDied(service: XposedService) {
                if (mutableService.value === service) mutableService.value = null
            }
        })
    }
}
