package com.fmt.tiktokvideo.ext

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 *  通过属性代理创建 ViewBinding 对象
 */
inline fun <reified VB : ViewBinding> invokeViewBinding() = InflateBindingProperty(VB::class.java)

class InflateBindingProperty<VB : ViewBinding>(private val clazz: Class<VB>) :
    ReadOnlyProperty<Any, VB> {

    private var mBinding: VB? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): VB {
        val layoutInflater: LayoutInflater?
        val viewLifecycleOwner: LifecycleOwner?

        when (thisRef) {
            is AppCompatActivity -> {
                layoutInflater = thisRef.layoutInflater
                viewLifecycleOwner = thisRef
            }

            is Fragment -> {
                layoutInflater = thisRef.layoutInflater
                viewLifecycleOwner = thisRef.viewLifecycleOwner
            }

            is IViewBinding -> {
                layoutInflater = thisRef.getLayoutInflater()
                viewLifecycleOwner = thisRef.getLifecycleOwner()
            }

            else -> {
                throw java.lang.IllegalStateException("invokeViewBinding can only be used in AppCompatActivity or Fragment,or IViewBinding")
            }
        }
        // 防止重复创建
        if (mBinding == null) {
            try {
                // 反射 inflate 静态方法，创建 XXBinding 对象
                mBinding = clazz.getMethod("inflate", LayoutInflater::class.java)
                    .invoke(null, layoutInflater) as VB
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
            // 监听页面销毁，将 mBinding 置空，避免内存泄漏
            viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    mBinding = null
                }
            })
        }

        return mBinding!!
    }
}

