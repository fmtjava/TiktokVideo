package com.fmt.tiktokvideo.ext

import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner

/**
 *  ViewBinding 的扩展抽象接口
 */
interface IViewBinding {

    fun getLayoutInflater(): LayoutInflater

    fun getLifecycleOwner(): LifecycleOwner

}