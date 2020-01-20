package mnleak

import org.slf4j.MDC

/**
 * Represents a request context backed by org.sl4j.MDC.
 */
object RequestContext {

    // These values are used in the logback.xml configuration.

    const val TRACKING_ID: String = "tracking_id"
    const val SERVICE_NAME: String = "service_name"

    var contextMap: Map<String, String>
        get() = MDC.getCopyOfContextMap() ?: HashMap()
        set(value) = MDC.setContextMap(value)

    var trackingId: String?
        get() = MDC.get(TRACKING_ID)
        set(value) = MDC.put(TRACKING_ID, value)

    var serviceName: String?
        get() = MDC.get(SERVICE_NAME)
        set(value) = MDC.put(SERVICE_NAME, value)
    

    fun clear(): Unit = MDC.clear()


}