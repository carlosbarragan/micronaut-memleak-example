package mnleak

import io.reactivex.Flowable
import io.reactivex.Single
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.kotlin.adapter.rxjava.toMono


// Context propagation funcitons

fun <T> Flowable<T>.asMono(): Mono<T> = this.firstOrError().asMono()

fun <T> Single<T>.asMono(): Mono<T> = this.toMono().propagateContext()

fun <T> Publisher<T>.asMono(): Mono<T> {
    return when (this) {
        is Mono -> this
        is Single<*> -> this.fromSingleToMono()
        else -> this.toMono().propagateContext()
    }

}

@Suppress("UNCHECKED_CAST")
private fun <T> Single<*>.fromSingleToMono(): Mono<T> {
    return this.asMono() as Mono<T>
}

private fun <T> Mono<T>.propagateContext(): Mono<T> {
    val contextMap = RequestContext.contextMap
    return this.doOnNext {
        RequestContext.contextMap = contextMap
    }.doOnError {
        RequestContext.contextMap = contextMap
    }
}