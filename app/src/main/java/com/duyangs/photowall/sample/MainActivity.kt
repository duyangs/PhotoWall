package com.duyangs.photowall.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.DrawableRes
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun getBmpFromDrawable(context: Context, @DrawableRes resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    private fun addImage(){
        photo_wall.setImage(getBmpFromDrawable(this, R.mipmap.timg))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_add -> {
                addImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
