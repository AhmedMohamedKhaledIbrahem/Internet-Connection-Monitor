# Internet-Connection-Monitor
A lightweight Kotlin library to monitor actual internet connectivity (not just network availability) in Android
# Feature
- Detects real internet access, not just Wi-Fi or mobile connection.
- Uses `StateFlow` for reactive updates.
- Periodic checks with configurable interval.
- Option to add or replace URL endpoints for checking and time interval.
# Tech Stack
- Kotlin.
- StateFlow.
- coroutines.
# How it Works
### Instead of relying only on `ConnectivityManager`, this library sends a **HEAD** request to public URLs (e.g., `8.8.8.8`) to confirm internet access.
It emits one of the following states:
```kotlin
enum class ConnectivityStatus {
    CONNECTED,
    DISCONNECTED
}
```
# Install
### Step 1.Add the JitPack repository to your build file
Add it in your `settings.gradle.kts` at the end of repositories:
```kotlin
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```
### Step 2.Add the dependencies
Add the following to your Module-level `build.gradle.kts` file :
```kotlin
dependencies {
	        implementation("com.github.AhmedMohamedKhaledIbrahem:Internet-Connection-Monitor:1.0.1")
	}
```
### Step 3.Add the permission 
Add the following to your `AndroidManifest.xml` :
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.INTERNET"/>
</manifest>
```
# Usage
### 1.Start monitoring
Starts continuous monitoring of internet connectivity status using the provided context. Emits updates to `statusFlow`.
```kotlin
val monitor:InternetConnectionMonitor = InternetConnectionMonitorImpl()
monitor.startMonitoring(context)
```
### 2.Observe connection state Ui
A reactive stream that emits the current internet connectivity status (CONNECTED or DISCONNECTED).
```kotlin
lifecycleScope.launch {
    monitor.statusFlow.collect { status ->
        when (status) {
            ConnectivityStatus.CONNECTED -> {handle logic here when the status connected}
            ConnectivityStatus.DISCONNECTED -> { handle logic here when the status disconnected}
            null -> {} // Optional handling
        }
    }
}
```
in compose
```kotlin
    val status by monitor.statusFlow.collectAsState()
        when (status) {
            ConnectivityStatus.CONNECTED -> {handle logic here when the status connected}
            ConnectivityStatus.DISCONNECTED -> { handle logic here when the status disconnected}
            null -> {} // Optional handling
        }
```
### 3.Has connection
Performs an immediate internet connectivity check by verifying both network and remote address reachability.
if you want to use it , you should run in coroutine scope or suspend function
```kotlin
suspend fun fetchData(context:Context){
if(!monitor.hasConnection(context = context)){
//do somthing
}
```
### 4.Configure Check Interval
Sets how often the internet status is checked (in milliseconds).
```kotlin
monitor.configureCheckInterval(5000L) // Every 5 seconds

```
### 5.Configure Check Option
Adds a single URL or list of url to the endpoints used to verify internet access.
```kotlin
monitor.configureCheckOption(
    AddressCheckOption("https://example.com", timeout = 1000)
)
val urls = listOf(
 AddressCheckOption("https://example.com", timeout = 1000),
 AddressCheckOption("https://example2.com", timeout = 2000)
)
monitor.configureCheckOption(
    urls
)
```
### 6.stop Monitoring
Stops the active monitoring process and cancels status updates. Safe to call even if monitoring hasn’t started.
```kotlin
 override fun onDestroy() {
        super.onDestroy()
        monitor.stopMonitoring()
    }
```
# Default Configuration
- Default URLs:
   - `https://1.1.1.1` (Cloudflare)
   - `https://8.8.8.8` (Google DNS)
- Default Interval: `1000ms` (1 second)
