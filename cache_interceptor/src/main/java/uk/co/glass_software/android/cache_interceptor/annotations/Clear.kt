package uk.co.glass_software.android.cache_interceptor.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Clear(val typeToClear: KClass<*>,
                       val clearOldEntriesOnly: Boolean = false)

