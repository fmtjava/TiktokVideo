package com.fmt.tiktokvideo.ext

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions

fun NavController.navigateTo(route: String, args: Bundle? = null, navOptions: NavOptions? = null) {
    navigate(route.hashCode(), args, navOptions)
}




