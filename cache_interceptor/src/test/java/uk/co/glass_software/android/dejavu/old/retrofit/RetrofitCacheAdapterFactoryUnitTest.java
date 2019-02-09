/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.old.retrofit;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

//@SuppressWarnings("unchecked")
//public class RetrofitCacheAdapterFactoryUnitTest {
//
//    private RxCacheInterceptorFactory mockRxCacheFactory;
//    private RxJava2CallAdapterFactory mockRxJava2CallAdapterFactory;
//
//    private RetrofitCallAdapterFactory spyTarget;
//
//    @Before
//    public void setUp() throws Exception {
//        mockRxCacheFactory = mock(RxCacheInterceptorFactory.class);
//        mockRxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create();
//
//        spyTarget = spy(new RetrofitCallAdapterFactory(
//                mockRxJava2CallAdapterFactory,
//                mockRxCacheFactory
//        ));
//    }
//
//    @Test
//    public void testGet() throws Exception {
//        Type mockType = new TypeToken<Observable<TestResponse>>() {
//        }.getType();
//        Annotation[] mockAnnotations = new Annotation[]{mock(Annotation.class), mock(Annotation.class), mock(Annotation.class)};
//        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://test.com").build();
//        RetrofitCallAdapter mockAdapter = mock(RetrofitCallAdapter.class);
//        CallAdapter mockCallAdapter = mock(CallAdapter.class);
//
//        doReturn(mockCallAdapter).when(spyTarget)
//                                 .getCallAdapter(
//                                         eq(mockType),
//                                         eq(mockAnnotations),
//                                         eq(retrofit)
//                                 );
//
//        when(spyTarget.create(
//                eq(mockRxCacheFactory),
//                instruction, eq(TestResponse.class),
//                eq(mockCallAdapter)
//        )).thenReturn(mockAdapter);
//
//        assertEquals(
//                mockAdapter,
//                spyTarget.get(
//                        mockType,
//                        mockAnnotations,
//                        retrofit
//                )
//        );
//    }
//
//    @Test
//    public void testClearOlderEntries() {
//        spyTarget.clearOlderEntries();
//
//        verify(mockRxCacheFactory).clearOlderEntries();
//    }
//
//    @Test
//    public void testFlushCache() {
//        spyTarget.flushCache();
//
//        verify(mockRxCacheFactory).flushCache();
//    }
//}
