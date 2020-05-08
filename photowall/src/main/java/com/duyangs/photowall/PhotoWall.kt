package com.duyangs.photowall

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout

/**
 * Project: PhotoWall 拖动 缩放 ImageView 支持多张
 * Package: com.duyangs.photowall
 * Author: Ryan Du (duyangs1994@gmail.com)
 * Date: 2020/5/7 11:05 ( Thursday May )
 * Description:
 */
class PhotoWall : RelativeLayout, PhotoWallImpl {

    companion object {
        private const val TAG = "PhotoWall"
        private const val SCALE_RATIO = 5000f //计算缩放比例的参数
        private const val SCALE_MIN = 0.5f //最小允许缩放
        const val SCALE_DEFAULT = 1f //默认缩放
    }

    /* 移动和缩放 */
    private lateinit var mLastMovePoint: PointF //第一根手指上次移动的点，按下和移动时更新
    private var mPointerLastDownPoint: PointF? = null //第二根手指上次落下的点，按下时更新
    private var mPointerDownDistance = 0f
    private var isSliding = false
    private var isScaling = false
    private var isScalable = true
    private var isDraggable = true
    var mScaleMax: Float = 2f //最大允许缩放

    /* 图片Bitmap */
    private var imageViewList = arrayListOf<PhotoWallImageView>()
    private var imageList = arrayListOf<PhotoWallEntity>()
    private var onPhotoWallDisplayListener: OnPhotoWallDisplayListener? = null

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

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        Log.d(TAG, "init: ")
        setWillNotDraw(false)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        for (iv in imageViewList) {
            iv.isEnabled = enabled
        }
    }

    /**
     * 判断当前触摸点是否作用与PhotoWallImageView
     * @param x 当前触摸点x
     * @param y 当前触摸点y
     * @return [PhotoWallImageView] 返回受作用的PhotoWallImageView
     */
    private fun inspectionImageRange(x: Float, y: Float): Int? {
        for (i in (imageViewList.size - 1) downTo 0) {
            val iv = imageViewList[i]
            if (inspectionImageRange(x, y, iv, this)) return i
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        val position = inspectionImageRange(event.x, event.y)
        position?.let {
            return onTouchEventImpl(it, event)
        }
        return super.onTouchEvent(event)
    }

    private fun onTouchEventImpl(position: Int, event: MotionEvent): Boolean {
        val photoWallImageView = imageViewList[position]
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isSliding = false
                mLastMovePoint = PointF(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScalable) { //处理双指缩放
                    if (event.pointerCount == 2) {
                        isScaling = true
                        calcScale(
                            position, photoWallImageView, getDistance(
                                PointF(event.getX(1), event.getY(1)),
                                PointF(event.getX(0), event.getY(0))
                            )
                        )
                        mLastMovePoint[event.getX(0)] = event.getY(0)
                        mPointerLastDownPoint!![event.getX(1)] = event.getY(1)
                        return true
                    }
                }
                //处理单指滑动
                if (isDraggable) {
                    if (event.pointerCount == 1) {
                        if (!isScaling) {
                            slide(photoWallImageView, event.x, event.y)
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                imageList[position].scale = photoWallImageView.scaleX
                mPointerLastDownPoint = PointF(
                    event.getX(1), event.getY(1)
                )
                mPointerDownDistance = getDistance(
                    mLastMovePoint, mPointerLastDownPoint!!
                )
                //图片没有缩放时才设置Pivot
                if (photoWallImageView.scaleX == 1f) {
                    setPivot(photoWallImageView, mLastMovePoint, mPointerLastDownPoint!!)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_UP -> {
                if (!isSliding && !isScaling) {// && !isDrawing) {
                    performClick()
                }
                clearPointerState()
                repairImageLocation(photoWallImageView)
            }
        }
        return true
    }

    /**
     * 设置图片的Bitmap
     * @param path 图片地址
     */
    fun setImage(path: String) {
        onPhotoWallDisplayListener?.onDisplay(context, path, object : OnPhotoWallDisplayCallback {
            override fun onDisplayCallback(bitmap: Bitmap) {
                post {
                    imageList.add(PhotoWallEntity(floatArrayOf(0f, 0f), 1f, path))
                    val imageView = createImageView()
                    imageViewList.add(imageView)
                    this@PhotoWall.setImageLocation(imageView)
                    val newBm = getFitImageViewBitmap(imageView, this@PhotoWall, bitmap)
                    imageView.setImageBitmap(newBm)
                    addView(imageView)
                }
            }
        })
    }

    /**
     * 清除所有手指状态，绘图，移动，缩放等
     */
    private fun clearPointerState() {
        isSliding = false
        isScaling = false
    }

    /**
     * 根据当前坐标计算手指移动距离并滑动
     *
     * @param v     需要滑动的View
     * @param currX 当前坐标x
     * @param currY 当前坐标y
     */
    private fun slide(v: View?, currX: Float, currY: Float) {
        val dx = currX - mLastMovePoint.x
        val dy = currY - mLastMovePoint.y
        if (getDistance(mLastMovePoint, PointF(currX, currY)) > 10 || isSliding) {
            isSliding = true
            // 注：要在view.post里获取，即等布局变化后
            v?.let {
                it.x += dx
                it.y += dy
            }
            mLastMovePoint[currX] = currY
        }
    }

    /**
     * 计算缩放的比例并缩放
     *
     * @param currentDistance 当前手指的距离
     */
    private fun calcScale(position: Int, iv: PhotoWallImageView, currentDistance: Float) {
        val rate = (currentDistance - mPointerDownDistance) / SCALE_RATIO
        val scale = imageList[position].scale + rate//imageList[position].mLastScale + rate
        val scaleRatio = scale.coerceAtLeast(SCALE_MIN)
        Log.d(TAG, "calcScale rate $rate scale $scale scaleRatio $scaleRatio")
        scale(position, iv, scaleRatio)
    }

    /**
     * 缩放
     * @param ratio 缩放比例 [Float]
     */
    private fun scale(position: Int, iv: PhotoWallImageView, ratio: Float) {
        iv.scaleX = ratio
        iv.scaleY = ratio
        val scale = if (ratio > mScaleMax) {
            iv.animate().scaleX(mScaleMax).scaleY(mScaleMax)
            mScaleMax
        } else ratio
        imageList[position].scale = scale
        iv.setDeleteBtnScale(scale)
    }

    /**
     * 使图片回到原位置
     */
    private fun repairImageLocation(iv: PhotoWallImageView) {
        //缩放<=1时：图片位置超出边界，回弹至边界
        if (iv.scaleX <= 1) {
            val boundaryOffset = getBoundaryOffset(iv, this)
            if (boundaryOffset[0] != -1f) iv.animate().translationX(boundaryOffset[0])
            if (boundaryOffset[1] != -1f) iv.animate().translationY(boundaryOffset[1])
        } else { //缩放>1时：图片边界回到原位置。
            backImgToBorder(iv)
        }
    }

    /**
     * 复原远尺寸，原位置
     */
    fun setReturnLocation() {
        for (iv in imageViewList) {
            setImageLocation(iv)
        }
    }

    /**
     * 获取当前图片列表
     */
    fun getImages(): ArrayList<PhotoWallEntity> {
        for (i in 0 until imageViewList.size) {
            val iv = imageViewList[i]
            val location = getImageLocation(iv, this)
            val relativeX = location[0] / width
            val relativeY = location[1] / height
            imageList[i].relativeLocation = floatArrayOf(relativeX, relativeY)
        }
        return imageList
    }

    fun recoveryImage(entityList: List<PhotoWallEntity>) {
        for (entity in entityList) {
            recoveryImage(entity)
        }
    }

    /**
     * 复原图
     * @param photoWallEntity 图片墙实体[PhotoWallEntity]
     */
    fun recoveryImage(photoWallEntity: PhotoWallEntity) {
        onPhotoWallDisplayListener?.onDisplay(
            context,
            photoWallEntity.path,
            object : OnPhotoWallDisplayCallback {
                override fun onDisplayCallback(bitmap: Bitmap) {
                    imageList.add(photoWallEntity)
                    post {
                        val imageView = createImageView()
                        val newBm = getFitImageViewBitmap(imageView, this@PhotoWall, bitmap)
                        imageView.setImageBitmap(newBm)
                        imageViewList.add(imageView)
                        addView(imageView)
                        imageView.post {
                            recoveryImage(
                                imageView,
                                this@PhotoWall,
                                photoWallEntity.relativeLocation,
                                photoWallEntity.scale
                            )
                        }
                    }
                }
            })
    }

    fun setOnPhotoWallDisplayListener(listener: OnPhotoWallDisplayListener) {
        this.onPhotoWallDisplayListener = listener
    }

    private fun createImageView(): PhotoWallImageView {
        return createImageView(context, object : PhotoWallImageView.OnClickListener {
            override fun onClick(view: PhotoWallImageView) {
                val position = imageViewList.indexOf(view)
                if (position > -1) imageList.removeAt(position)
                imageViewList.remove(view)
                removeView(view)
            }
        })
    }

    /**
     * 清空
     */
    fun clear(){
        imageList.clear()
        imageViewList.clear()
        removeAllViews()
    }

    interface OnPhotoWallDisplayListener {
        fun onDisplay(context: Context, path: String, callback: OnPhotoWallDisplayCallback)
    }

    interface OnPhotoWallDisplayCallback {
        fun onDisplayCallback(bitmap: Bitmap)
    }
}