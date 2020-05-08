package com.duyangs.photowall

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper

/**
 * Project: PhotoWall
 * Package: com.duyangs.photowall
 * Author: Ryan Du (duyangs1994@gmail.com)
 * Date: 2020/5/7 14:35 ( Thursday May )
 * Description:
 */
class PhotoWallImageView : RelativeLayout {
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, set: AttributeSet) : super(context, set) {
        init()
    }

    constructor(context: Context, set: AttributeSet, defStyleAttr: Int) : super(
        context,
        set,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        initImageView()
        initTextView()
    }

    private fun initImageView() {
        imageView = ImageView(context)
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.layoutParams = layoutParams
        addView(imageView)
    }

    private fun initTextView() {
        textView = TextView(ContextThemeWrapper(context, R.style.PhotoWallImageViewDeleteBtn))
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(ALIGN_PARENT_RIGHT)
        textView.layoutParams = layoutParams
//        textView.text = "移除"

        addView(textView)
    }

    fun setDeleteBtnScale(ratio: Float) {
        val correctRatio = if (ratio < 1f) {
            (1f - ratio) + 1f
        } else {
            1f
        }
        textView.scaleX = correctRatio
        textView.scaleY = correctRatio
    }

    fun setImageBitmap(bm: Bitmap?) {
        imageView.setImageBitmap(bm)
    }

    fun getImageView() = imageView

    fun setScaleType(scaleType: ImageView.ScaleType) {
        imageView.scaleType = scaleType
    }

    fun setOnDeleteListener(click: OnClickListener) {
        textView.setOnClickListener { click.onClick(this) }
    }

    interface OnClickListener {
        fun onClick(view: PhotoWallImageView)
    }
}