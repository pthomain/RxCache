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

package dev.pthomain.android.dejavu.serialisation.compression

import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.serialisation.compression.decorator.CompressionSerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.di.SerialisationComponent
import dev.pthomain.android.dejavu.shared.di.SilentLogger
import dev.pthomain.android.dejavu.shared.utils.Function1
import dev.pthomain.android.dejavu.shared.utils.Function3
import org.iq80.snappy.Snappy
import javax.inject.Singleton

object Compression {

    class Builder(logger: Logger = SilentLogger)
        : Component by DaggerCompression_Component
            .builder()
            .module(Module(logger))
            .build()

    @Singleton
    @dagger.Component(modules = [Module::class])
    internal interface Component : SerialisationComponent

    @dagger.Module
    internal class Module(private val logger: Logger) {

        @Provides
        @Singleton
        internal fun provideCompresser() =
                object : Function1<ByteArray, ByteArray> {
                    override fun get(t1: ByteArray) =
                            Snappy.compress(t1)
                }

        @Provides
        @Singleton
        internal fun provideUncompresser() =
                object : Function3<ByteArray, Int, Int, ByteArray> {
                    override fun get(t1: ByteArray, t2: Int, t3: Int) =
                            Snappy.uncompress(t1, t2, t3)
                }

        @Provides
        @Singleton
        internal fun provideCompressionSerialisationDecorator(
                compresser: Function1<ByteArray, ByteArray>,
                uncompresser: Function3<ByteArray, Int, Int, ByteArray>
        ): SerialisationDecorator =
                CompressionSerialisationDecorator(
                        logger,
                        compresser::get,
                        uncompresser::get
                )
    }
}
