package com.duyangs.photowall.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.DrawableRes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        photo_wall.setImage(getBmpFromDrawable(this, R.mipmap.timg))
        photo_wall.setImage(getBmpFromDrawable(this, R.mipmap.timg))
    }

    fun getBmpFromDrawable(context: Context, @DrawableRes resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }
}
