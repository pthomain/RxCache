package uk.co.glass_software.android.cache_interceptor.interceptors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import uk.co.glass_software.android.cache_interceptor.R;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class RxCacheInterceptorBuilder<E extends Exception & Function<E, Boolean>> {

    private Logger logger;
    private Function<Throwable, E> errorFactory;
    private String databaseName;
    private Gson gson;
    private boolean compressData = true;
    private boolean encryptData = false;
    private boolean isCacheEnabled = true;

    RxCacheInterceptorBuilder() {
    }

    public RxCacheInterceptorBuilder<E> errorFactory(Function<Throwable, E> errorFactory) {
        this.errorFactory = errorFactory;
        return this;
    }

    public RxCacheInterceptorBuilder<E> noLog() {
        return logger(new Logger() {
            @Override
            public void e(Object caller, Throwable t, String message) {
            }

            @Override
            public void e(Object caller, String message) {
            }

            @Override
            public void d(Object caller, String message) {
            }
        });
    }

    public RxCacheInterceptorBuilder<E> logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public RxCacheInterceptorBuilder<E> gson(@NonNull Gson gson) {
        this.gson = gson;
        return this;
    }

    public RxCacheInterceptorBuilder<E> databaseName(@NonNull String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public RxCacheInterceptorBuilder<E> cache(boolean isCacheEnabled) {
        this.isCacheEnabled = isCacheEnabled;
        return this;
    }

    public RxCacheInterceptorBuilder<E> compress(boolean compressData) {
        this.compressData = compressData;
        return this;
    }

    public RxCacheInterceptorBuilder<E> encrypt(boolean encryptData) {
        this.encryptData = encryptData;
        return this;
    }

    @SuppressLint("RestrictedApi")
    public RxCacheInterceptor.Factory<E> build(Context context) {
        return build(context, null);
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    RxCacheInterceptor.Factory<E> build(Context context,
                                        @Nullable DependencyHolder holder) {
        if (logger == null) {
            logger = new SimpleLogger(context.getApplicationContext());
        }

        if (errorFactory == null) {
            throw new IllegalStateException("Please provide an error factory");
        }

        ErrorInterceptor.Factory<E> errorInterceptorFactory = new ErrorInterceptor.Factory<>(
                errorFactory,
                logger
        );

        CacheInterceptor.Factory<E> cacheInterceptorFactory = CacheInterceptor.<E>builder()
                .logger(logger)
                .databaseName(databaseName)
                .gson(gson)
                .build(context.getApplicationContext(), compressData, encryptData);


        if (holder != null) {
            holder.gson = gson;
            holder.errorFactory = this.errorFactory;
            holder.errorInterceptorFactory = errorInterceptorFactory;
            holder.cacheInterceptorFactory = cacheInterceptorFactory;
        }

        return new RxCacheInterceptor.Factory<>(errorInterceptorFactory,
                cacheInterceptorFactory,
                logger,
                isCacheEnabled
        );
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    class DependencyHolder {
        Function<Throwable, E> errorFactory;
        Gson gson;
        ErrorInterceptor.Factory<E> errorInterceptorFactory;
        CacheInterceptor.Factory<E> cacheInterceptorFactory;
    }
}
