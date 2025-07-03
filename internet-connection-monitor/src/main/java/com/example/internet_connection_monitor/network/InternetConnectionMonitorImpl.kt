package com.example.internet_connection_monitor.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.example.internet_connection_monitor.data.AddressCheckOption
import com.example.internet_connection_monitor.data.ConnectivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class InternetConnectionMonitorImpl(

) : InternetConnectionMonitor {
    private var checkOptions: List<AddressCheckOption> = defaultCheckOptions
    private var checkInterval: Long = CHECK_INTERVAL

    private val _statusFlow = MutableStateFlow<ConnectivityStatus?>(ConnectivityStatus.CONNECTED)
    override val statusFlow: StateFlow<ConnectivityStatus?> get() = _statusFlow.asStateFlow()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Starts continuous monitoring of internet connectivity status.
     * Periodically checks connectivity at the interval specified by [checkInterval],
     * emitting status updates to [statusFlow].
     * Monitoring runs indefinitely until [stopMonitoring] is called.
     * @param context The application context used for connectivity checks
     * @throws SecurityException if ACCESS_NETWORK_STATE permission is not granted
     * @see stopMonitoring
     * @see statusFlow
     */
    override fun startMonitoring(context: Context) {
        scope.launch {
            while (isActive) {
                val isConnected = hasConnection(context = context)
                _statusFlow.emit(if (isConnected) ConnectivityStatus.CONNECTED else ConnectivityStatus.DISCONNECTED)
                delay(checkInterval)
            }
        }
    }

    /**
     * Checks internet connectivity.
     * @param context Application context
     * @return Boolean true if connection is available
     * @throws SecurityException if ACCESS_NETWORK_STATE permission is missing
     */
    @SuppressLint("MissingPermission")
    override suspend fun hasConnection(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("ACCESS_NETWORK_STATE permission not granted")
        }

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

        val hasTransport = networkCapabilities != null && (
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )

        return hasTransport && checkUrls()
    }


    /**
     * Checks configured URLs to verify actual internet connectivity.
     * @return `true` if any of the configured URLs responds successfully, `false` otherwise
     * @throws MalformedURLException if any configured URL is invalid
     */
    private suspend fun checkUrls(): Boolean {
        val dispatcher = Dispatchers.IO
        return withContext(dispatcher) {
            checkOptions.any { option ->
                try {
                    val url = URL(option.url)
                    with(url.openConnection() as HttpURLConnection) {
                        connectTimeout = option.timeout
                        readTimeout = option.timeout
                        requestMethod = "HEAD"
                        responseCode == HttpURLConnection.HTTP_OK
                    }
                } catch (e: MalformedURLException) {
                    throw e
                } catch (e: Exception) {
                    false
                }
            }
        }

    }

    /**
     * Replaces all configured URL check options with the provided list.
     * @param options List of [AddressCheckOption] to use for connectivity verification
     * @see AddressCheckOption
     */
    override fun configureCheckOption(options: List<AddressCheckOption>) {
        checkOptions = options
    }

    /**
     * Adds a single URL check option to the existing configuration.
     * @param option Additional [AddressCheckOption] to include in connectivity checks
     * @see AddressCheckOption
     */
    override fun configureCheckOption(option: AddressCheckOption) {
        checkOptions = checkOptions + option
    }

    /**
     * Configures the interval between automatic connectivity checks.
     *
     * Updates the frequency at which the monitor verifies internet connectivity
     * when [startMonitoring] is active. Changes take effect on the next check cycle.
     *
     * @param interval The time delay between checks in milliseconds (must be positive)
     * @throws IllegalArgumentException if interval is less than or equal to zero
     *
     * @see startMonitoring
     * @see checkInterval
     *
     * @note The default interval is 1000ms (1 second)
     * @note For immediate checks, call [hasConnection] directly
     * @note Very short intervals (<500ms) may impact battery life
     */
    override fun configureCheckInterval(interval: Long) {
        require(interval > 0) { "Check interval must be positive" }
        checkInterval = interval
    }


    /**
     * Stops active internet connectivity monitoring.
     * Cancels all ongoing connectivity checks and stops status updates to [statusFlow].
     * Safe to call even when monitoring isn't active.
     * @see startMonitoring
     */
    override fun stopMonitoring() {
        job.cancel()
    }


    companion object {
        private val defaultCheckOptions = listOf(
            AddressCheckOption("https://1.1.1.1"),
            AddressCheckOption("https://8.8.8.8"),
        )
        private const val CHECK_INTERVAL: Long = 5000L
    }

}
