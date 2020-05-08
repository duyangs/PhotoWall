package com.duyangs.photowall.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.DrawableRes
import com.duyangs.photowall.PhotoWall
import com.duyangs.photowall.PhotoWallEntity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var reductionList = arrayListOf<PhotoWallEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setPhotoWallDisplayListener()
    }

    private fun setPhotoWallDisplayListener(){
        photo_wall.setOnPhotoWallDisplayListener(object :PhotoWall.OnPhotoWallDisplayListener{
            override fun onDisplay(
                context: Context,
                path: String,
                callback: PhotoWall.OnPhotoWallDisplayCallback
            ) {
                callback.onDisplayCallback(getBmpFromDrawable(this@MainActivity,R.mipmap.timg))
            }
        })
    }

    private fun getBmpFromDrawable(context: Context, @DrawableRes resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    private fun addImage(){
        photo_wall.setImage("")
    }

    private fun saveImage(){
        reductionList.clear()
        reductionList.addAll(photo_wall.getImages())
        val jsonArray = Gson().toJson(reductionList)
        Log.d("MainActivity","json format $jsonArray")
    }

    private fun reductionImage(){
        photo_wall.recoveryImage(reductionList)
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
            R.id.action_save -> {
                saveImage()
                true
            }
            R.id.action_reduction -> {
                reductionImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
