/*
 *
 *  Copyright (C) 2017 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.metadata.ResponseMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.DoneException
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.EmptyResponseException
import dev.pthomain.android.dejavu.utils.Utils.isAnyInstance
import dev.pthomain.android.dejavu.utils.Utils.swapLambdaWhen
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.subjects.PublishSubject
import java.util.*

/**
 * Intercepts the response wrapper returned from the error and cache interceptors and returns the actual
 * response while adding the metadata if possible. Only response classes implementing CacheMetadata.Holder
 * will receive metadata and can be used with the mergeOnNextOnError directive, otherwise an error is emitted.
 *
 * This class also deals with filtering the returned responses according to the
 * filterFinal/freshOnly/allowNonFinalForSingle directives set for this call globally or specifically.
 *
 * @param logger the logger
 * @param dateFactory provides a date for a given timestamp or the current date with no argument
 * @param emptyResponseWrapperFactory factory providing empty response wrappers
 * @param configuration the cache configuration
 * @param metadataSubject the subject used to emit the current response's metadata (exposed as an Observable on DejaVu)
 * @param instructionToken the instruction cache token
 * @param isWrapped whether or not the response should be wrapped in a CacheResult
 * @param start the time the call started
 */
internal class ResponseInterceptor<O : Operation, T : InstructionToken<O>, E> private constructor(
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>,
        private val configuration: DejaVuConfiguration<E>,
        private val metadataSubject: PublishSubject<DejaVuResult<*>>,
        private val instructionToken: T,
        private val isWrapped: Boolean,
        private val start: Long
) : ObservableTransformer<ResponseWrapper<O, RequestToken<O>, E>, Any>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Composes an Observable call.
     *
     * @param upstream the Observable to compose
     * @return the composed Observable
     */
    override fun apply(upstream: Observable<ResponseWrapper<O, RequestToken<O>, E>>) =
            upstream.switchIfEmpty(Observable.defer {
                emptyResponseWrapperFactory.create(instructionToken, start).observable() //FIXME this should not happen
            }).flatMap(this::intercept)!!

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    private fun intercept(wrapper: ResponseWrapper<O, RequestToken<O>, E>): Observable<Any> {
        val errorFactory = configuration.errorFactory

        val exception = wrapper.metadata.exception.swapLambdaWhen({ it == null }) {
            errorFactory(IllegalStateException("The response is null but no exception is present in the metadata"))
        }

        val callDuration = wrapper.metadata.callDuration.copy(
                total = (dateFactory(null).time - start).toInt()
        )

        val metadata = wrapper.metadata.copy(
                exception = exception,
                callDuration = callDuration
        )

        val response = wrapper.response

        metadataSubject.onNext(convert(wrapper))

        //TODO check compatibility with Wrappable
        if (response is ResponseMetadata.Holder<*, *, *>
                && response.metadata.exceptionClass.isAssignableFrom(errorFactory.exceptionClass)) {
            @Suppress("UNCHECKED_CAST") // This is verified by the above check
            (response as ResponseMetadata.Holder<O, RequestToken<O>, E>).metadata = metadata
        }

        //FIXME
        val isEmptyException = exception?.cause.isAnyInstance(
                EmptyResponseException::class.java,
                DoneException::class.java
        )

        val cacheToken = metadata.cacheToken
        val instruction = cacheToken.instruction
        val requestMetadata = instruction.requestMetadata

        @Suppress("UNCHECKED_CAST")
        return when {
            isWrapped -> {
                if (response == null) {
                    if (instruction.operation is Local) {
                        Result<Local>(
                                cacheToken.asRequest(), //TODO convert for operation
                                metadata.callDuration
                        )
                    } else {
                        Empty<Remote>( //TODO check if the response isn't null
                                EmptyResponseException(metadata.exception!!), //FIXME
                                cacheToken.asRequest(), //TODO convert
                                metadata.callDuration
                        )
                    }
                } else {
                    Response<Any, Remote>( //TODO check if the response isn't null
                            response,
                            cacheToken.asResponse(), //TODO convert
                            metadata.callDuration
                    )
                }.observable() as Observable<Any>
            }

            exception != null -> {
                logger.d(this, "Returning error: $exception")
                Observable.error(exception)
            }

            else -> {
                logger.d(this, "Returning response: $metadata")
                Observable.just(response)
            }
        }
    }

    //TODO check this logic
    private fun convert(wrapper: ResponseWrapper<O, RequestToken<O>, E>): DejaVuResult<*> {
        val metadata = wrapper.metadata
        return if (wrapper.response == null) {
            val exception = metadata.exception!!.cause
            when (exception) {
                is DoneException -> Result<Local>(
                        metadata.cacheToken.asRequest(),
                        metadata.callDuration
                )
                is EmptyResponseException -> Empty<Remote>(
                        exception,
                        metadata.cacheToken.asRequest(),
                        metadata.callDuration
                )
                else -> throw IllegalStateException("This should not happen")
            } //check this logic
        } else Response<Any, Remote>(
                wrapper.response,
                metadata.cacheToken.asResponse(),
                metadata.callDuration
        )
    }

    class Factory<E>(private val configuration: DejaVuConfiguration<E>,
                     private val logger: Logger,
                     private val dateFactory: (Long?) -> Date,
                     private val metadataSubject: PublishSubject<DejaVuResult<*>>,
                     private val emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun <O : Operation, T : InstructionToken<O>> create(instructionToken: T,
                                                            isWrapped: Boolean,
                                                            start: Long) =
                ResponseInterceptor(
                        logger,
                        dateFactory,
                        emptyResponseWrapperFactory,
                        configuration,
                        metadataSubject,
                        instructionToken,
                        isWrapped,
                        start
                )
    }
}
