package com.duyangs.photowall

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Project: PhotoWall
 * Package: com.duyangs.photowall
 * Author: Ryan Du (duyangs1994@gmail.com)
 * Date: 2020/5/8 08:59 ( Friday May )
 * Description: PhotoWall implement
 */
interface PhotoWallImpl {

    /**
     * 创建PhotoWallImageView
     * @param context 上下文 [Context]
     * @param onDeleteClick 删除按钮点击监听 [PhotoWallImageView.OnClickListener]
     */
    fun createImageView(
        context: Context,
        onDeleteClick: PhotoWallImageView.OnClickListener
    ): PhotoWallImageView {
        val imageView = PhotoWallImageView(context)
        imageView.setScaleType(ImageView.ScaleType.FIT_XY)
        imageView.setOnDeleteListener(onDeleteClick)
        return imageView
    }

    /**
     * 根据两根手指的坐标，设置View的Pivot。
     */
    fun setPivot(view: View, pointer1: PointF, pointer2: PointF) {
        var newX = (abs(pointer1.x + pointer2.x) / 2 - view.left)
        newX = if (newX > 0) newX else 0f
        var newY = (abs(pointer1.y + pointer2.y) / 2 - view.top)
        newY = if (newY > 0) newY else 0f
        view.pivotX = newX
        view.pivotY = newY
    }

    /**
     * 获取两点间距离
     * @param p1 点1 [PointF]
     * @param p2 点2 [PointF]
     *
     * @return 距离 [Float]
     */
    fun getDistance(p1: PointF, p2: PointF): Float {
        return sqrt((p2.x - p1.x.toDouble()).pow(2.0) + (p2.y - p1.y.toDouble()).pow(2.0)).toFloat()
    }

    /**
     * 判断是否越界,越界则返回需要的偏移量
     * @param iv [PhotoWallImageView] child view
     * @param pw [PhotoWall] parent view
     * @return 长度为2的[FloatArray] 对应 x , y 坐标
     */
    fun getBoundaryOffset(iv: PhotoWallImageView, pw: PhotoWall): FloatArray {
        val imageLocation = getImageLocation(iv, pw)
        val maxDistance = getMaxDistance(iv, pw)

        /**
         * 获取左、上越界偏移量 translation 小于0  则取translation - location (减去超出父布局的距离) 反之则取 0
         */
        fun getLTOffset(translation: Float, location: Float): Float {
            return if (translation < 0F) translation - location else 0F
        }

        fun getRBOffset(translation: Float, location: Float, maxDistance: Float): Float {
            return if (translation > 0F) translation - (location - maxDistance) else maxDistance
        }


        val dx = when {
            imageLocation[0] < 0 -> getLTOffset(
                iv.translationX,
                imageLocation[0]
            )// image.x < 0 左越界
            imageLocation[0] > maxDistance[0] -> getRBOffset(
                iv.translationX,
                imageLocation[0],
                maxDistance[0]
            ) // image.x > maxX 右越界 偏移量 -(image.x - maxX)
            else -> -1F
        }
        val dy = when {
            imageLocation[1] < 0 -> getLTOffset(
                iv.translationY,
                imageLocation[1]
            )// image.y < 0 上越界 偏移量 -image.y
            imageLocation[1] > maxDistance[1] -> getRBOffset(
                iv.translationY,
                imageLocation[1],
                maxDistance[1]
            ) // image.y > maxY 右越界 偏移量 -(image.y - maxY)
            else -> -1F
        }
//        Log.d("PhotoWallImpl", "getBoundaryOffset loc ${imageLocation.asList()}")
//        Log.d(
//            "PhotoWallImpl",
//            "getBoundaryOffset translation translationX ${iv.translationX}  translationY ${iv.translationY}"
//        )
//        Log.d("PhotoWallImpl", "getBoundaryOffset max ${maxDistance.asList()}")
//        Log.d("PhotoWallImpl", "getBoundaryOffset dx $dx dy $dy")
        return floatArrayOf(dx, dy)
    }

    /**
     * 获取图片宽度 图片原宽度 * 图片缩放比
     */
    fun getImageWidth(iv: PhotoWallImageView) = iv.width * iv.scaleX

    /**
     * 获取ImageView基于父布局的位置（通过ImageView基于屏幕位置与父布局基于屏幕位置计算差值）
     */
    fun getImageHeight(iv: PhotoWallImageView) = iv.height * iv.scaleY

    /**
     * 获取ImageView基于父布局的位置（通过ImageView基于屏幕位置与父布局基于屏幕位置计算差值）
     */
    fun getImageLocation(iv: PhotoWallImageView, pw: PhotoWall): FloatArray {
        val imageLocation = IntArray(2)
        iv.getLocationOnScreen(imageLocation)
        val parentLocation = IntArray(2)
        pw.getLocationOnScreen(parentLocation)
        val x =
            imageLocation[0] - parentLocation[0] // mImageView距离Parent左边的距离（即x轴方向） image.x - parent.x
        val y =
            imageLocation[1] - parentLocation[1] // mImageView距离Parent顶边的距离（即y轴方向） image.y - parent.y
//        Log.d("PhotoWallImpl", "getImageLocation v.x $x v.y $y")
        return floatArrayOf(x.toFloat(), y.toFloat())
    }

    /**
     * 获取最大移动距离，
     * maxXDistance = parent.w - image.w
     * maxYDistance = parent.h - image.h
     */
    fun getMaxDistance(
        iv: PhotoWallImageView,
        pw: PhotoWall,
        imageW: Float? = getImageWidth(iv),
        imageH: Float? = getImageHeight(iv)
    ): FloatArray {
        return floatArrayOf(
            pw.width - (imageW ?: getImageWidth(iv)),
            pw.height - (imageH ?: getImageHeight(iv))
        )
    }

    /**
     * 获取image当前范围
     * @param iv [PhotoWallImageView] child view
     * @param pw [PhotoWall] parent view
     * @return [FloatArray] 范围数组，长度为4 按顺序依次为 startX endX startY endY
     */
    fun getImageRange(iv: PhotoWallImageView, pw: PhotoWall): FloatArray {
        val imageLoc = getImageLocation(iv, pw)
        val endX = imageLoc[0] + getImageWidth(iv)
        val endY = imageLoc[1] + getImageHeight(iv)
        val imageRangeArray = FloatArray(4)
        imageRangeArray[0] = imageLoc[0] //startX
        imageRangeArray[1] = endX
        imageRangeArray[2] = imageLoc[1] //startY
        imageRangeArray[3] = endY
        return imageRangeArray
    }
}