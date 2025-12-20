package com.fmt.tiktokvideo.utils

import android.content.Context
import android.util.TypedValue

object PixelConverter {
    /**
     * dp 转 px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * sp 转 px
     */
    fun spToPx(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * px 转 dp
     */
    fun pxToDp(context: Context, px: Float): Float {
        val density = context.resources.displayMetrics.density
        return px / density
    }

    /**
     * px 转 sp
     */
    fun pxToSp(context: Context, px: Float): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    /**
     * pt 转 px
     */
    fun ptToPx(context: Context, pt: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PT,
            pt,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * in 转 px
     */
    fun inToPx(context: Context, inch: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_IN,
            inch,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * mm 转 px
     */
    fun mmToPx(context: Context, mm: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_MM,
            mm,
            context.resources.displayMetrics
        ).toInt()
    }
}