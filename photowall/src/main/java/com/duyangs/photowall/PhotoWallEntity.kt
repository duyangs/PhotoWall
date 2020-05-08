package com.duyangs.photowall

import android.graphics.Bitmap

/**
 * Project: PhotoWall
 * Package: com.duyangs.photowall
 * Author: Ryan Du (duyangs1994@gmail.com)
 * Date: 2020/5/8 11:22 ( Friday May )
 * Description:
 */
data class PhotoWallEntity(
    var relativeLocation: FloatArray,//相对父布局坐标比 数组 长度为2
    var scale: Float,//原缩放比
    var path: String //图片
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhotoWallEntity

        if (!relativeLocation.contentEquals(other.relativeLocation)) return false
        if (scale != other.scale) return false
        if (path != other.path) return false
        return true
    }

    override fun hashCode(): Int {
        var result = relativeLocation.contentHashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}