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

package dev.pthomain.android.dejavu.interceptors.cache.instruction

import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.*


/**
 * This class dictates the way the cache should behave in handling a request returning data.
 *
 * For the priorities with the CacheMode CACHE or REFRESH, there are 3 different ways to deal with
 * STALE cache data:
 *
 * - the default way (preference = DEFAULT) is to always emit STALE data from the cache
 * if available. This data can be displayed as a transient UI state while another call is automatically
 * made to refresh the data. Then a second response is emitted with the network result.
 * Keep in mind that calls using Singles will only ever receive responses with a final status.
 * If your response class implements CacheMetadata.Holder, this status will we returned in the
 * metadata field. It can be used to differentiate STALE data from the FRESH one and for the purpose
 * of filtering for instance.
 * @see CacheStatus.isFinal
 * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata.Holder
 *
 * - FRESH preferred (preference = FRESH_PREFERRED): this priority does not
 * emit STALE data from the cache initially. It will instead attempt a network call and return the
 * result of this call. However, if the network call fails, then the STALE cached data is returned
 * with a COULD_NOT_REFRESH status.
 *
 * - FRESH only (preference = FRESH_ONLY): this priority never emits STALE data
 * from the cache. It will instead attempt a network call and return theresult of this call.
 * However, if the network call fails, then an EMPTY response will be emitted.
 *
 * For the priorities with CACHE mode, no network call is made if the cached data is FRESH
 * and this cached data is returned.
 *
 * For the priorities with REFRESH mode, he cached data is always permanently invalidated
 * and considered STALE at the time of the call. This means a network call will always be attempted.
 *
 * @param usesNetwork whether or not the cache should attempt to fetch data from the network
 * @param invalidatesExistingData whether or not cached data should be permanently marked as STALE, regardless of its presence or existing status
 * @param emitsCachedStale whether or not STALE cached data should be returned (prior to a attempting a network call for instance)
 * @param emitsNetworkStale whether or not STALE cached data should be returned after a failed network call (as COULD_NOT_REFRESH)
 * @param hasSingleResponse whether the cache will emit a single response or 2 of them (usually starting with a transient STALE one)
 * @param mode the mode in which this priority operates
 * @param preference the preference regarding the handling of STALE data
 * @param returnedStatuses the possible statuses of the response(s) emitted by the cache as a result of this priority
 */
enum class CachePriority(
        val usesNetwork: Boolean,
        val invalidatesExistingData: Boolean,
        val emitsCachedStale: Boolean,
        val emitsNetworkStale: Boolean,
        val hasSingleResponse: Boolean,
        val mode: CacheMode,
        val preference: CachePreference,
        vararg val returnedStatuses: CacheStatus
) {

    /**
     * Returns cached data even if it is STALE then if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * failed (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data can be emitted from the cache and from the network (respectively STALE and COULD_NOT_REFRESH).
     *
     * Emits a single response if:
     * - the cached data is FRESH
     * - there is no cached data (NETWORK if the call succeeds or EMPTY otherwise)
     *
     * Or 2 responses otherwise, STALE first then:
     * - REFRESHED if it is successfully refreshed
     * - COULD_NOT_REFRESH if the network call failed.
     *
     * This priority is the most versatile and allows for showing transient STALE data
     * during a loading UI state for instance.
     */
    DEFAULT(
            true,
            false,
            true,
            true,
            false,
            CacheMode.CACHE,
            CachePreference.DEFAULT,
            FRESH,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns cached data only if it is FRESH or if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is not emitted from the cache but only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed
     * - EMPTY if no cached data was present and the network call failed.
     *
     * This priority ignores cached data if it is STALE but will return it
     * if it could not be refreshed.
     */
    FRESH_PREFERRED(
            true,
            false,
            false,
            true,
            true,
            CacheMode.CACHE,
            CachePreference.FRESH_PREFERRED,
            FRESH,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns cached data only if it is FRESH or if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call only
     * if the call succeeds (or EMPTY otherwise).
     *
     * No network call is attempted if the cached data is FRESH.
     * No STALE data is emitted from the cache or if the network call failed (EMPTY).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - EMPTY if the cached data is STALE or inexistent and the network call failed.
     *
     * This priority will never return STALE data, either transiently from the cache or
     * as a result of a failed network call.
     */
    FRESH_ONLY(
            true,
            false,
            false,
            false,
            true,
            CacheMode.CACHE,
            CachePreference.FRESH_ONLY,
            FRESH,
            NETWORK,
            REFRESHED,
            EMPTY
    ),

    /**
     * Invalidates the cached data then attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * A network call will always be attempted and the cached data will be invalidated.
     * STALE data can be emitted from the cache and from the network (respectively STALE and COULD_NOT_REFRESH).
     *
     * Emits a single response if:
     * - there is no cached data (NETWORK if the call succeeds or EMPTY otherwise)
     *
     * Or 2 responses otherwise, STALE first then:
     * - REFRESHED if it is successfully refreshed
     * - COULD_NOT_REFRESH if the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * DEFAULT. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. This STALE cached data will still be returned to
     * be displayed on a loading UI state for instance.
     */
    REFRESH(
            true,
            true,
            true,
            true,
            false,
            CacheMode.REFRESH,
            CachePreference.DEFAULT,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Invalidates the cached data then attempts to refresh the data by making a network call
     * and by emitting the result of this call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is not emitted from the cache but only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed
     * - EMPTY if no cached data was present and the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * DEFAULT. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. This STALE cached data will still be returned to
     * be displayed on a loading UI state for instance.
     */
    REFRESH_FRESH_PREFERRED(
            true,
            true,
            false,
            true,
            false,
            CacheMode.REFRESH,
            CachePreference.FRESH_PREFERRED,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Invalidates the cached data then attempts to refresh
     * the data by making a network call and by emitting the result of this call only
     * if the call succeeds (or EMPTY otherwise).
     *
     * A network call will always be attempted and the cached data will be invalidated.
     * No STALE data is emitted from the cache or if the network call failed (EMPTY).
     *
     * Only emits a single response:
     * - NETWORK if there was no cached data and the network call succeeds
     * - REFRESHED if there was some invalidated cached data and the network call succeeds
     * - EMPTY if the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * FRESH_ONLY. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. No STALE data will ever be returned either initially
     * from the cache or as the result of a failed network call.
     */
    REFRESH_FRESH_ONLY(
            true,
            true,
            false,
            false,
            true,
            CacheMode.REFRESH,
            CachePreference.FRESH_ONLY,
            NETWORK,
            REFRESHED,
            EMPTY
    ),

    /**
     * Returns cached data even if is STALE. No network call is attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response, either:
     * - FRESH
     * - STALE
     * - EMPTY.
     *
     * This priority returns cached data as is without ever using the network.
     */
    OFFLINE(
            false,
            false,
            true,
            false,
            true,
            CacheMode.OFFLINE,
            CachePreference.DEFAULT,
            FRESH,
            STALE,
            EMPTY
    ),

    /**
     * Returns cached data only if is FRESH. No network call is attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response, either:
     * - FRESH
     * - EMPTY if there is no cached data or if it is STALE.
     *
     * This priority returns only FRESH cached data without ever using the network.
     */
    OFFLINE_FRESH_ONLY(
            false,
            false,
            false,
            false,
            true,
            CacheMode.OFFLINE,
            CachePreference.FRESH_ONLY,
            FRESH,
            EMPTY
    );

    /**
     * Indicates the mode in which the cache should operate in relation to the handling of existing
     * cache data and as to whether a network call should be attempted (regardless of the device's
     * network availability).
     *
     * - CACHE is the default behaviour and checks the current state of the cached data to determine
     * whether it should be automatically refreshed or not.
     *
     * - REFRESH disregards the current state of the cached data and permanently invalidates it, which
     * means a network call will always be attempted, even if the cached data was considered FRESH
     * before the call was made.
     *
     * - OFFLINE will always return cached data without ever attempting to refresh it. This is
     * completely independent from the device's network availability and is not necessarily
     * the mode that should be used when the device as no connectivity (any of those modes can
     * handle the network being unavailable). OFFLINE specifically instructs the cache never to call
     * the network even if the data is STALE and the network is available.
     */
    enum class CacheMode {
        CACHE,
        REFRESH,
        OFFLINE;

        fun isCache() = this == CACHE
        fun isRefresh() = this == REFRESH
        fun isOffline() = this == OFFLINE
    }

    /**
     * Indicates how the cache should handle STALE data.
     *
     * - DEFAULT will return STALE cached data initially and then attempt to refresh it (if the
     * CacheMode is not OFFLINE)
     *
     * - FRESH_PREFERRED will never emit the STALE cached data initially but will emit it as the
     * result of a failure of the refresh network call (with the status COULD_NOT_REFRESH).
     *
     * - FRESH_ONLY will never emit any STALE data, even if the network call fails. Instead, it
     * will emit an EMPTY response. Beware that responses with an EMPTY status are emitted as
     * exceptions unless you have declared your call to return ResponseWrapper<R>, with R being the
     * type of your response, in which case this wrapper will contain this status in the metadata
     * field.
     * @see dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
     */
    enum class CachePreference {
        DEFAULT,
        FRESH_PREFERRED,
        FRESH_ONLY;

        fun isDefault() = this == DEFAULT
        fun isFreshOnly() = this == FRESH_ONLY
        fun isFreshPreferred() = this == FRESH_PREFERRED
    }

    companion object {

        /**
         * Commodity factory
         *
         * @param cacheMode the desired cache mode
         * @param preference the desired cache preference
         * @return the corresponding cache priority
         */
        fun with(cacheMode: CacheMode,
                 preference: CachePreference) =
                when (cacheMode) {
                    CacheMode.CACHE -> when (preference) {
                        CachePreference.DEFAULT -> DEFAULT
                        CachePreference.FRESH_PREFERRED -> FRESH_PREFERRED
                        CachePreference.FRESH_ONLY -> FRESH_ONLY
                    }

                    CacheMode.REFRESH -> when (preference) {
                        CachePreference.DEFAULT -> REFRESH
                        CachePreference.FRESH_PREFERRED -> REFRESH_FRESH_PREFERRED
                        CachePreference.FRESH_ONLY -> REFRESH_FRESH_ONLY
                    }

                    CacheMode.OFFLINE -> when (preference) {
                        CachePreference.FRESH_ONLY -> OFFLINE_FRESH_ONLY
                        else -> OFFLINE
                    }
                }
    }
}
