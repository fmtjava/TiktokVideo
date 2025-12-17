package com.fmt.tiktokvideo.ext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified VM: ViewModel> invokeViewModel() = ViewModelProperty(VM::class.java)

class ViewModelProperty<VM: ViewModel>(private val clazz: Class<VM>) : ReadOnlyProperty<Any, VM> {
    private var vm: VM? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): VM {
        if (thisRef !is ViewModelStoreOwner) {
            throw java.lang.IllegalStateException("invokeViewModel can only be used in ViewModelStoreOwner instance")
        }
        if (vm == null) {
            vm = ViewModelProvider(thisRef)[clazz]
        }
        return vm!!
    }
}