package com.example.internet_connection_monitor.network

import android.content.Context
import com.example.internet_connection_monitor.data.AddressCheckOption
import com.example.internet_connection_monitor.data.ConnectivityStatus
import kotlinx.coroutines.flow.StateFlow

interface InternetConnectionMonitor {
    fun startMonitoring()
    val statusFlow: StateFlow<ConnectivityStatus?>
    suspend fun hasConnection(): Boolean
    fun configureCheckInterval(interval: Long)
    fun configureCheckOption(options: List<AddressCheckOption>)
    fun configureCheckOption(option: AddressCheckOption)
    fun stopMonitoring()
}