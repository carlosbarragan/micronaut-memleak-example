package mnleak

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.OncePerRequestHttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.web.router.Router
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*
import javax.inject.Singleton

@Filter("/**")
class RequestLogger(private val serviceNameResolver: ServiceNameResolver) : OncePerRequestHttpServerFilter() {

    private val logger = LoggerFactory.getLogger(RequestLogger::class.java)

    override fun doFilterOnce(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {

        return Mono.fromCallable {
            val serviceName = serviceNameResolver.resolveServiceNameFromRequest(request)
            initializeRequestContext(request, serviceName)
            logger.info("Application enter: {} {}", request.method, request.uri)

        }.flatMap {
            chain.proceed(request).asMono()
        }.doOnNext { response ->
            logger.info("Application exit. HTTP Code {}", response.status.code)
        }.doOnError { throwable ->
            logger.info("Application exti. HTTP Code 500")
        }
    }

    private fun initializeRequestContext(request: HttpRequest<*>, serviceName: String) {
        RequestContext.clear()
        RequestContext.trackingId = UUID.randomUUID().toString()
        RequestContext.serviceName = serviceName

    }
}

/**
 * We use the method name in the Rest Controller as service name to identify the business call.
 */
@Singleton
class ServiceNameResolver(private val router: Router) {

    fun resolveServiceNameFromRequest(request: HttpRequest<*>): String {
        return router.find<Any, Any>(request.method, request.path)
                .map { it.methodName }
                .findFirst().orElse("UNDEFINED")

    }
}