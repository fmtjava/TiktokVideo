package com.fmt.tiktokvideo.ext

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.load
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import coil3.transform.RoundedCornersTransformation

fun ImageView.loadUrl(
    lifecycleOwner: LifecycleOwner,
    imageUrl: String?,
    isCircle: Boolean = false,
    radius: Float = 0.0f
) {
    if (TextUtils.isEmpty(imageUrl)) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    val disposable = if (isCircle) {
        load(imageUrl) {
            transformations(CircleCropTransformation())
        }
    } else if (radius > 0) {
        load(imageUrl) {
            transformations(RoundedCornersTransformation(radius))
        }
    } else {
        load(imageUrl)
    }
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            disposable.dispose()
        }
    })
}