package mnleak

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Singleton

@Controller("/api")
class RestController(private val sessionService: SessionService) {

    private val mapper = XmlMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
        setDefaultUseWrapper(false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Post("/sessions")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun createSession(@Body rawBody: String, httpHeader: HttpHeaders): Mono<out HttpResponse<String>> {
        return parseRequest(rawBody)
                .flatMap { sessionService.createSession(it.deviceContext) }
                .map { session ->
                    val sessionResponse = SessionResponse(Status.OK, session)
                    mapper.writeValueAsString(sessionResponse)
                }.onErrorResume { throwable ->
                    when (throwable) {
                        is XmlParseException -> mapper.writeValueAsString(SessionResponse(Status.BAD_SYNTAX)).toMono()
                        else -> mapper.writeValueAsString(SessionResponse(Status.UNEXPECTED_ERROR)).toMono()
                    }
                }.map { responseBody ->
                    HttpResponse.ok(responseBody)
                }


    }

    private fun parseRequest(body: String): Mono<SessionRequest> {
        return try {
            mapper.readValue(body, SessionRequest::class.java).toMono()
        } catch (t: Throwable) {
            Mono.error(XmlParseException(t))
        }

    }
}

/**
 * This exception is thrown to identify errors when parsing the XML request as we need to return a different error code when that happens.
 */
class XmlParseException(cause: Throwable) : RuntimeException(cause)

@Singleton
class SessionService {

    private val times = AtomicLong()

    /**
     * Simplified version from our Session Service.
     * Our actual service calls other services remotely via HTTP using non blocking calls.
     * Additionally, we access MongoDB to read some values needed to create the session. I left that out because I don't think that is relevant
     * to the problem at hand.
     */
    fun createSession(deviceContext: DeviceContext): Mono<Session> {
        if (times.incrementAndGet() % 5 == 0L) {
            throw RuntimeException("Some exception")
        }
        return Session(SessionId(UUID.randomUUID().toString()), Instant.now()).toMono()
    }
}

// XML Payload

@JacksonXmlRootElement(localName = "Session-Request")
class SessionRequest(@field:JacksonXmlProperty(localName = "device-id") val deviceId: DeviceId,
                     @field:JacksonXmlProperty(localName = "device-context") val deviceContext: DeviceContext)

class DeviceId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val value: String)
class DeviceContext @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val value: String)

@JacksonXmlRootElement(localName = "Session-Response")
class SessionResponse(val status: Status, val session: Session? = null)

class Session(val sessionId: SessionId, val startAt: Instant)
class SessionId(@JsonValue val value: String)
class Status(val status: String, val code: Int) {

    companion object {
        val OK = Status("OK", 0)
        val BAD_SYNTAX = Status("ERROR", 4)
        val UNEXPECTED_ERROR = Status("ERROR", 1)
    }
}

