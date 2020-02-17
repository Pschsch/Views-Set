package com.pschsch.viewsset.chupachups

import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.ShapeDrawable
import androidx.annotation.ColorInt

class NonLinearRadialGradientShaderFactory(
    @ColorInt private val colorFirst: Int,
    @ColorInt private val colorSecond: Int,
    private val gradientToSecondColorStart: Float,
    private val radius: Int = DEFAULT_RADIUS
) : ShapeDrawable.ShaderFactory() {

    override fun resize(width: Int, height: Int): Shader {
        return RadialGradient(
            width / 2.toFloat(), height / 2.toFloat(),
            if (radius == DEFAULT_RADIUS) (width + height / 2).toFloat() else radius.toFloat(),
            intArrayOf(colorFirst, colorFirst, colorSecond),
            floatArrayOf(0f, gradientToSecondColorStart, 1f),
            Shader.TileMode.REPEAT
        )
    }

    companion object {
        private const val DEFAULT_RADIUS = -1
    }
}