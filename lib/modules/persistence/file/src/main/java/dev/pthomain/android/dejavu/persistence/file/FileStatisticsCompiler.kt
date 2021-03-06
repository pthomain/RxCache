/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
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

package dev.pthomain.android.dejavu.persistence.file

import dev.pthomain.android.dejavu.cache.metadata.token.getCacheStatus
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser.Companion.isValidFormat
import dev.pthomain.android.dejavu.persistence.statistics.BaseStatisticsCompiler
import dev.pthomain.android.dejavu.persistence.statistics.CacheEntry
import java.io.File
import java.io.InputStream

/**
 * Provides a concrete StatisticsCompiler implementation for File entries.
 *
 * @param configuration the global cache configuration
 * @param cacheDirectory the cache directory
 * @param fileFactory a factory that returns a File for a given parent directory and file name
 * @param fileInputStreamFactory a factory that returns an InputStream for a given File
 * @param dateFactory the factory converting timestamps to Dates
 * @param keySerialiser a class that handles the serialisation of the cache metadata to a file name.
 */
internal class FileStatisticsCompiler(
        private val cacheDirectory: File,
        private val fileFactory: (File, String) -> File,
        private val fileInputStreamFactory: (File) -> InputStream,
        private val dateFactory: DateFactory,
        private val keySerialiser: KeySerialiser
) : BaseStatisticsCompiler<String, List<String>>() { //TODO use KeyValueStore

    /**
     * Returns an list of valid file names in the cache directory representing a cached response.
     *
     * @return the list of valid file names in the cache directory.
     */
    override fun loadEntries() =
            cacheDirectory.list()!!.filter(::isValidFormat)

    /**
     * Converts a file name to a CacheEntry.
     *
     * @param entry the current file name
     * @return the converted entry
     */
    override fun convert(entry: String): CacheEntry {
        val dataHolder = keySerialiser.deserialise(entry)

        val file = fileFactory(cacheDirectory, entry)
        val responseClassName = fileInputStreamFactory(file).reader().buffered().readLine()

        val status = dateFactory.getCacheStatus(dateFactory(dataHolder.expiryDate.time))

        return CacheEntry(
                Class.forName(responseClassName),
                status,
                dateFactory(dataHolder.requestDate.time),
                dateFactory(dataHolder.expiryDate.time)
        )
    }

}