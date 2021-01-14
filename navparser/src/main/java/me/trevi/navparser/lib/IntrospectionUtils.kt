package me.trevi.navparser.lib

import timber.log.Timber as Log
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.typeOf

@Target(AnnotationTarget.PROPERTY)
annotation class Mutable

abstract class MutableContent {
    override fun equals(other: Any?): Boolean {
        if (other == null || other::class != this::class)
            return false

        this::class.members.forEach { m ->
            if (m is KProperty && !m.hasAnnotation<Mutable>()) {
                if (m.getter.call(this) != m.getter.call(other))
                    return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

interface Introspectable {
    fun asMap() : MapStringAny {
        val map = mutableMapOf<String, Any?>()

        this::class.members.forEach { m ->
            if (m is KProperty && m.visibility == KVisibility.PUBLIC && m.isFinal)
                map[m.name] = m.getter.call(this)
        }

        return MapStringAny(map)
    }

    fun diffMap(other: NavigationData) : MapStringAny {
        val diff = mutableMapOf<String, Any?>()

        this::class.members.forEach { m ->
            if (m is KProperty && m.visibility == KVisibility.PUBLIC && m.isFinal &&
                !m.hasAnnotation<Mutable>()
            ) {
                m.getter.call(other).also {
                    if (it != m.getter.call(this))
                        diff[m.name] = it
                }
            }
        }

        return MapStringAny(diff)
    }

    fun debugIntrospect(value: Any = this, klass: KClass<*> = this::class, pad : Int = 0) {
        var padStr = ""
        for (i in 0..pad)
            padStr += "  "

        Log.d("${padStr}Looking for members of $klass")

        for (m in klass.members) {
            if (m is KProperty && m.visibility == KVisibility.PUBLIC && m.hasAnnotation<Mutable>()) {
                val memberValue = m.getter.call(value)

                Log.d("${padStr}${m.name}: $memberValue (${m.returnType})")
                (m.returnType.classifier as KClass<*>).also { memberClass ->
                    if (memberValue != null && memberClass.isData)
                        debugIntrospect(memberValue, memberClass, pad + 1)
                }
            }
        }
    }
}
