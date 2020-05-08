package com.duyangs.photowall

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import com.duyangs.photowall.PhotoWall.Companion.SCALE_DEFAULT
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

    /**
     * 判断当前触摸点是否作用与image
     * @param x 当前触摸点x
     * @param y 当前触摸点y
     * @param iv [PhotoWallImageView]
     * @param pw [PhotoWall]
     * @return [Boolean] true 作用于图片 false 非作用于图片
     */
    fun inspectionImageRange(
        x: Float,
        y: Float,
        iv: PhotoWallImageView,
        pw: PhotoWall
    ): Boolean {
        val imageRangeArray = getImageRange(iv, pw)
        return ((x >= imageRangeArray[0]) and (x <= imageRangeArray[1]) and (y >= imageRangeArray[2]) and (y <= imageRangeArray[3]))
    }

    /**
     * 设置图片位置，缩放比
     * @param iv [PhotoWallImageView]
     * @param scale 缩放比 默认[SCALE_DEFAULT]
     * @param translationX 位于x轴的位置 默认0f
     * @param translationY 位于y轴的位置 默认0f
     */
    fun setImageLocation(
        iv: PhotoWallImageView,
        scale: Float? = SCALE_DEFAULT,
        translationX: Float? = 0f,
        translationY: Float? = 0f
    ) {
        val animatorSet = AnimatorSet()
        animatorSet.setTarget(iv)
        val animatorList: MutableList<Animator> = ArrayList()

        animatorList.add(ObjectAnimator.ofFloat(iv, "scaleX", scale ?: SCALE_DEFAULT))
        animatorList.add(ObjectAnimator.ofFloat(iv, "scaleY", scale ?: SCALE_DEFAULT))
        animatorList.add(ObjectAnimator.ofFloat(iv, "translationX", translationX ?: 0f))
        animatorList.add(ObjectAnimator.ofFloat(iv, "translationY", translationY ?: 0f))
        animatorSet.setDuration(225).playTogether(animatorList)
        animatorSet.start()
    }

    /**
     * todo 此方法需要修改
     * 对于图片本身的width小于ImageView的width且width>height，<br></br>
     * 设置ImageView宽度match_parent使其顶格显示；height同样处理；<br></br>
     * 已经超过imageView宽高的图片，维持原状
     * @param iv [PhotoWallImageView]
     * @param pw [PhotoWall]
     * @param imgBitmap [Bitmap]
     */
    fun getFitImageViewBitmap(iv: PhotoWallImageView, pw: PhotoWall, imgBitmap: Bitmap?): Bitmap? {
        var ivHeight = ViewGroup.LayoutParams.WRAP_CONTENT
        imgBitmap?.let {
            ivHeight = ((pw.width.toFloat() / it.width.toFloat()) * it.height).toInt()
        }
        val layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ivHeight
        )
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        iv.layoutParams = layoutParams
        return imgBitmap
    }

    /**
     * 检查图片若超出边界，则返回边界
     * @param iv [PhotoWallImageView]
     */
    fun backImgToBorder(iv: PhotoWallImageView) {
        //计算需要回弹的超出的边界大小
        val canOverRight = ((iv.width
                - iv.pivotX)
                * (iv.scaleX - 1))
        val canOverLeft = iv.pivotX * (iv.scaleX - 1)
        if (iv.translationX > canOverLeft
            || iv.translationX < -canOverRight
        ) {
            iv.animate().translationX(
                if (iv.translationX > 0) canOverLeft else -canOverRight
            )
        }
        val canOverTop = iv.pivotY * (iv.scaleY - 1)
        val canOverBottom = ((iv.height
                * iv.scaleY
                * ((iv.height - iv.pivotY) / iv.height))
                - (iv.height / 2f - iv.pivotY)
                - iv.height / 2f)
        if (iv.translationY > canOverTop
            || iv.translationY < -canOverBottom
        ) {
            iv.animate().translationY(
                if (iv.translationY > 0) canOverTop else -canOverBottom
            )
        }
    }

    /**
     * 复原ImageView位置
     * @param iv [PhotoWallImageView]
     * @param pw [PhotoWall]
     * @param relativeLocation 相对父布局坐标比 数组 长度为2
     * @param scale 原缩放比
     */
    fun recoveryImage(
        iv: PhotoWallImageView,
        pw: PhotoWall,
        relativeLocation: FloatArray,
        scale: Float
    ) {
        if (relativeLocation.size < 2) {
            //原始坐标不完整 重置原始坐标 0x 0y
            relativeLocation[0] = 0f
            relativeLocation[1] = 0f
        }
        fun scaleSize(originSize: Float) = originSize * scale

        /**
         * 缩放会基于中心点，所以直接移动缩放后 如果scale值不为1 则会发生偏移
         * 通过计算图片尺寸及缩放比 在移动过程中减去差值
         * @param originSize 原始尺寸 width height
         * @param relativeLocation 记录的坐标位置 x or y
         * @return x or y 轴移动距离
         */
        fun translation(originSize: Float, relativeLocation: Float): Float {
            val currentOrigin = (originSize - scaleSize(originSize)) / 2
            return relativeLocation - currentOrigin
        }

        val maxDistance = getMaxDistance(
            iv,
            pw,
            scaleSize(iv.width.toFloat()),
            scaleSize(iv.height.toFloat())
        )
        val translationX = translation(iv.width.toFloat(), relativeLocation[0] * pw.width)
        val translationY = translation(iv.height.toFloat(), relativeLocation[1] * pw.height)
        this.setImageLocation(
            iv,
            scale,
            if (translationX > maxDistance[0]) maxDistance[0] else translationX,
            if (translationY > maxDistance[1]) maxDistance[1] else translationY
        )
        iv.setDeleteBtnScale(scale)
    }
}