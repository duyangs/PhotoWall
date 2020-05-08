package com.duyangs.photowall

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        private const val SCALE_RATIO = 400f //计算缩放比例的参数
        private const val SCALE_MIN = 0.5f //最小允许缩放
        private const val SCALE_DEFAULT = 1f //默认缩放
    }

    /* 移动和缩放 */
    private lateinit var mLastMovePoint: PointF //第一根手指上次移动的点，按下和移动时更新
    private var mPointerLastDownPoint: PointF? = null //第二根手指上次落下的点，按下时更新
    private var mPointerDownDistance = 0f
    private var isSliding = false
    private var isScaling = false
    private var mLastScale = 1f
    private var finalScale = 1f
    private var isScalable = true
    private var isDraggable = true
    var mScaleMax: Float = 2f //最大允许缩放

    /* 图片Bitmap */
    private var imageViewList = arrayListOf<PhotoWallImageView>()
//    private var imgBitmap: Bitmap? = null

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
     * 判断当前触摸点是否作用与image
     * @param x 当前触摸点x
     * @param y 当前触摸点y
     * @return [Boolean] true 作用于图片 false 非作用于图片
     */
    private fun inspectionImageRange(x: Float, y: Float): PhotoWallImageView? {
        for (i in (imageViewList.size - 1) downTo 0) {
            val iv = imageViewList[i]
            val imageRangeArray = getImageRange(iv, this)
            if ((x >= imageRangeArray[0]) and (x <= imageRangeArray[1]) and (y >= imageRangeArray[2]) and (y <= imageRangeArray[3])) {
                return iv
            }
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
//        val rawX = event.rawX
//        val rawY = event.rawY
//        Log.d(TAG, "rawX $rawX rawY $rawY")
//        Log.d(TAG,"event.x ${event.x}, event.y ${event.y}")
        val iv = inspectionImageRange(event.x, event.y)
        iv?.let {
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
                                it, getDistance(
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
                                slide(it, event.x, event.y)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mLastScale = it.scaleX
                    mPointerLastDownPoint = PointF(
                        event.getX(1), event.getY(1)
                    )
                    mPointerDownDistance = getDistance(
                        mLastMovePoint, mPointerLastDownPoint!!
                    )
                    //图片没有缩放时才设置Pivot
                    if (it.scaleX == 1f) {
                        setPivot(it, mLastMovePoint, mPointerLastDownPoint!!)
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                }
                MotionEvent.ACTION_UP -> {
                    if (!isSliding && !isScaling) {// && !isDrawing) {
                        performClick()
                    }
                    clearPointerState()
                    repairImageLocation(it)
                }
            }
            return true
        }
        return super.onTouchEvent(event)

    }

    /**
     * 设置图片的Bitmap
     * @param imgBitmap 图片 [Bitmap]
     */
    fun setImage(imgBitmap: Bitmap?) {
        post {
            val imageView = createImageView()
            imageViewList.add(imageView)
            this.setImageLocation(imageView)
            val newBm = getFitImageViewBitmap(imageView, imgBitmap)
//            this.imgBitmap = newBm
            imageView.setImageBitmap(newBm)
            addView(imageView)
        }
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
        Log.d(TAG, "mLastMovePoint.x ${mLastMovePoint.x}")
        Log.d(TAG, "mLastMovePoint.y ${mLastMovePoint.y}")
        Log.d(TAG, "currX $currX")
        Log.d(TAG, "currY $currY")
        Log.d(TAG, "dx $dx")
        Log.d(TAG, "dy $dy")

        if (getDistance(mLastMovePoint, PointF(currX, currY)) > 10 || isSliding) {
            isSliding = true
            Log.d(TAG, "v.x begin ${v?.x}")
            Log.d(TAG, "v.y begin ${v?.y}")
            // 注：要在view.post（Runable）里获取，即等布局变化后
            v?.let {
                it.x += dx
                it.y += dy
                val location = IntArray(2)
                it.getLocationOnScreen(location)
                val x = location[0] // view距离 屏幕左边的距离（即x轴方向）
                val y = location[1] // view距离 屏幕顶边的距离（即y轴方向）
                val location2 = IntArray(2)
                getLocationOnScreen(location)
                val x2 = location2[0] // view距离 屏幕左边的距离（即x轴方向）
                val y2 = location2[1] // view距离 屏幕顶边的距离（即y轴方向）
                Log.d(TAG, "getLocationOnScreen v.x $x v.y $y")
                Log.d(TAG, "getLocationOnScreen parent.x $x2 parent.y $y2")
                Log.d(TAG, "v.x ${it.x}")
                Log.d(TAG, "v.y ${it.y}")
                Log.d(TAG, "v.top ${it.top}")
                Log.d(TAG, "v.right ${it.right}")
                Log.d(TAG, "v.bottom ${it.bottom}")
                Log.d(TAG, "v.left ${it.left}")
            }
            mLastMovePoint[currX] = currY
        }
    }

    /**
     * 计算缩放的比例并缩放
     *
     * @param currentDistance 当前手指的距离
     */
    private fun calcScale(iv: PhotoWallImageView, currentDistance: Float) {
        val rate = (currentDistance - mPointerDownDistance) / SCALE_RATIO
        val scale = mLastScale + rate
        val scaleRatio = scale.coerceAtLeast(SCALE_MIN)
        scale(iv, scaleRatio)
    }

    /**
     * 缩放
     * @param ratio 缩放比例 [Float]
     */
    private fun scale(iv: PhotoWallImageView, ratio: Float) {
        iv.scaleX = ratio
        iv.scaleY = ratio
        finalScale = if (ratio > mScaleMax) {
            iv.animate().scaleX(mScaleMax).scaleY(mScaleMax)
            mScaleMax
        } else ratio
        iv.setDeleteBtnScale(finalScale)
        Log.d(TAG, "mI.width ${getImageWidth(iv)}")
        Log.d(TAG, "mI.height ${getImageHeight(iv)}")
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
     * 设置图片位置，缩放比
     * @param scale 缩放比 默认[SCALE_DEFAULT]
     * @param translationX 位于x轴的位置 默认0f
     * @param translationY 位于y轴的位置 默认0f
     */
    private fun setImageLocation(
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
     * 检查图片若超出边界，则返回边界
     */
    private fun backImgToBorder(iv: PhotoWallImageView) {
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
     * 对于图片本身的width小于ImageView的width且width>height，<br></br>
     * 设置ImageView宽度match_parent使其顶格显示；height同样处理；<br></br>
     * 已经超过imageView宽高的图片，维持原状
     */
    private fun getFitImageViewBitmap(iv: PhotoWallImageView, imgBitmap: Bitmap?): Bitmap? {
//        val w = if (imgBitmap!!.width >= imgBitmap.height
//            && imgBitmap.width < width
//            || imgBitmap.height >= imgBitmap.width
//            && imgBitmap.height < height
//        ) {
//            ViewGroup.LayoutParams.MATCH_PARENT
//        } else {
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        }
        var ivHeight = ViewGroup.LayoutParams.WRAP_CONTENT
        imgBitmap?.let {
            ivHeight = ((width.toFloat() / it.width.toFloat()) * it.height).toInt()
        }
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ivHeight
        )
        layoutParams.addRule(ALIGN_PARENT_TOP)
        iv.layoutParams = layoutParams
        Log.d(TAG, "图片 Bitmap：" + imgBitmap?.width + " " + imgBitmap?.height)

        return imgBitmap
    }

    /**
     * todo  此方法需要修改
     * 获取当前缩放比
     */
    fun getScale(): Float = finalScale

    /**
     * todo  此方法需要修改
     * 获取当前图片位于Parent相对位置坐标
     */
    fun getRelativeLocation(): ArrayList<FloatArray> {
        val relativeLocationList = arrayListOf<FloatArray>()
        for (iv in imageViewList) {
            val location = getImageLocation(iv, this)
            val relativeX = location[0] / width
            val relativeY = location[1] / height
            relativeLocationList.add(floatArrayOf(relativeX, relativeY))
        }
        return relativeLocationList
    }

    /**
     * todo  此方法需要修改
     * 复原图
     * @param bitmap [Bitmap] 图片
     * @param relativeLocation 相对父布局坐标比 数组 长度为2
     * @param scale 原缩放比
     */
    fun recoveryImage(bitmap: Bitmap, relativeLocation: FloatArray, scale: Float) {
        if (relativeLocation.size < 2) {
            //原始坐标不完整 重置原始坐标
            Log.d(TAG, "原始坐标不完整 重置原始坐标 0x 0y")
            relativeLocation[0] = 0f
            relativeLocation[1] = 0f
        }
        fun scaleSize(originSize: Float) = originSize * scale

        /**
         * 缩放会基于中心点，所以直接移动缩放后 如果scale值不为1 则会发生偏移
         * 通过计算图片尺寸及缩放比 在移动过程中减去差值
         * @param translation 根据记录坐标得到的移动距离
         * @param originSize 原始尺寸 width height
         */
        fun translation(translation: Float, originSize: Float): Float {
            // 计算移动距离 - （（原尺寸 - 缩放后尺寸）/ 2 )
            val trans = translation - ((originSize - scaleSize(originSize)) / 2)
            return if (trans > 0) trans else 0f
        }
        post {
            val imageView = createImageView()
            imageViewList.add(imageView)
            val maxDistance = getMaxDistance(
                imageView,
                this,
                scaleSize(bitmap.width.toFloat()),
                scaleSize(bitmap.height.toFloat())
            )
            val translationX = translation(relativeLocation[0] * width, bitmap.width.toFloat())
            val translationY =
                translation(relativeLocation[1] * height, bitmap.height.toFloat())
            this.setImageLocation(
                imageView,
                scale,
                if (translationX > maxDistance[0])
                    translation(
                        maxDistance[0], bitmap.width.toFloat()
                    ) else translationX,
                if (translationY > maxDistance[1])
                    translation(
                        maxDistance[1], bitmap.height.toFloat()
                    ) else translationY
            )
            val newBm = getFitImageViewBitmap(imageView, bitmap)
//            this.imgBitmap = newBm
            imageView.setImageBitmap(newBm)
            mLastScale = scale
            finalScale = scale
        }

    }

    private fun createImageView(): PhotoWallImageView {
        return createImageView(context, object : PhotoWallImageView.OnClickListener {
            override fun onClick(view: PhotoWallImageView) {
                imageViewList.remove(view)
                removeView(view)
            }
        })
    }

    /**
     * 图片显示监听 用于开放图片加载方式
     */
    interface OnDisplayImageListener {
        /**
         * 显示图片
         * @param context 上下文 [Context]
         * @param img 图片 [Any]
         * @param iv [ImageView]
         */
        fun onDisplayImage(context: Context, img: Any, iv: ImageView)
    }
}