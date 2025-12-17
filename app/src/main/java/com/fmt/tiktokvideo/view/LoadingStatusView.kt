package com.fmt.tiktokvideo.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.fmt.tiktokvideo.databinding.LayoutLoadingStatusViewBinding

class LoadingStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val mBinding = LayoutLoadingStatusViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        mBinding.loading.show()
    }

    @SuppressLint("ResourceType")
    fun showEmpty(
        @DrawableRes iconRes: Int = 0,
        text: String? = null,
        @ColorRes tintColorRes: Int = android.R.color.white,
        @ColorRes emptyTxtColorRes: Int = android.R.color.white,
        @ColorRes retryTxtColorRes: Int = android.R.color.black,
        retry: OnClickListener? = null
    ) {
        mBinding.loading.hide()
        mBinding.emptyLayout.visibility = VISIBLE
        if (iconRes > 0) {
            mBinding.emptyIcon.setImageResource(iconRes)
        }

        // Caller might pass either a color resource or a raw color int; gracefully handle both.
        val tintColor = resolveColor(tintColorRes)
        val emptyTxtColor = resolveColor(emptyTxtColorRes)
        val retryTxtColor = resolveColor(retryTxtColorRes)

        mBinding.emptyIcon.imageTintList = ColorStateList.valueOf(tintColor)

        // 文案为空时仍然应用颜色，以便调用方通过 emptyTxtColorRes 覆盖默认颜色
        if (!TextUtils.isEmpty(text)) {
            mBinding.emptyText.text = text
            mBinding.emptyText.visibility = VISIBLE
        }
        mBinding.emptyText.setTextColor(emptyTxtColor)

        retry?.let {
            mBinding.retryAction.visibility = VISIBLE
            mBinding.retryAction.setOnClickListener(it)
            mBinding.retryAction.setTextColor(retryTxtColor)
        }
    }

    /**
     * Try to resolve a color resource; if it is not a valid resource id, treat it as a raw color int.
     */
    private fun resolveColor(colorOrRes: Int): Int {
        return try {
            ContextCompat.getColor(context, colorOrRes)
        } catch (_: Resources.NotFoundException) {
            colorOrRes
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility != VISIBLE) {
            mBinding.loading.hide()
        }
    }

}