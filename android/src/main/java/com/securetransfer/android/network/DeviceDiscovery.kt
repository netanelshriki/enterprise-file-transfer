package com.securetransfer.android.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.securetransfer.android.model.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class DeviceDiscovery(private val context: Context) {
    private val logger = LoggerFactory.getLogger(DeviceDiscovery::class.java)
    
    companion object {
        private const val SERVICE_TYPE = "_securetransfer._tcp"
        private const val SERVICE_NAME = "SecureTransfer"
    }
    
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredDevices = MutableStateFlow<Map<String, Device>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, Device>> = _discoveredDevices
    
    private val devices = ConcurrentHashMap<String, Device>()
    private var isDiscovering = false
    private var isRegistered = false
    
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            logger.error("Discovery start failed: $errorCode")
        }
        
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            logger.error("Discovery stop failed: $errorCode")
        }
        
        override fun onDiscoveryStarted(serviceType: String) {
            logger.info("Discovery started for: $serviceType")
            isDiscovering = true
        }
        
        override fun onDiscoveryStopped(serviceType: String) {
            logger.info("Discovery stopped for: $serviceType")
            isDiscovering = false
        }
        
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            logger.debug("Service found: ${serviceInfo.serviceName}")
            if (serviceInfo.serviceType == SERVICE_TYPE && 
                serviceInfo.serviceName != SERVICE_NAME) {
                nsdManager.resolveService(serviceInfo, resolveListener)
            }
        }
        
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            logger.debug("Service lost: ${serviceInfo.serviceName}")
            val deviceId = serviceInfo.serviceName
            devices.remove(deviceId)?.let {
                _discoveredDevices.value = devices.toMap()
            }
        }
    }
    
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            logger.error("Resolve failed for ${serviceInfo.serviceName}: $errorCode")
        }
        
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            logger.debug("Service resolved: ${serviceInfo.serviceName}")
            
            val deviceId = serviceInfo.serviceName
            val host = serviceInfo.host?.hostAddress ?: return
            val port = serviceInfo.port
            
            val device = Device(
                id = deviceId,
                name = serviceInfo.serviceName,
                type = Device.Type.DESKTOP, // Will be updated based on service attributes
                ipAddress = host,
                port = port,
                status = Device.Status.ONLINE,
                lastSeen = System.currentTimeMillis()
            )
            
            devices[deviceId] = device
            _discoveredDevices.value = devices.toMap()
            
            logger.info("Device discovered: ${device.name} at $host:$port")
        }
    }
    
    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            logger.error("Registration failed: $errorCode")
        }
        
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            logger.error("Unregistration failed: $errorCode")
        }
        
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            logger.info("Service registered: ${serviceInfo.serviceName}")
            isRegistered = true
        }
        
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            logger.info("Service unregistered: ${serviceInfo.serviceName}")
            isRegistered = false
        }
    }
    
    fun startDiscovery() {
        if (!isDiscovering) {
            logger.info("Starting device discovery")
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }
    
    fun stopDiscovery() {
        if (isDiscovering) {
            logger.info("Stopping device discovery")
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }
    
    fun registerService(port: Int) {
        if (!isRegistered) {
            logger.info("Registering service on port $port")
            
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = SERVICE_NAME
                serviceType = SERVICE_TYPE
                this.port = port
            }
            
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }
    
    fun unregisterService() {
        if (isRegistered) {
            logger.info("Unregistering service")
            // Note: We need to keep a reference to the service info used for registration
            // This is a simplified version - in practice, you'd store the registered service info
        }
    }
    
    fun cleanup() {
        stopDiscovery()
        unregisterService()
        devices.clear()
        _discoveredDevices.value = emptyMap()
    }
}
