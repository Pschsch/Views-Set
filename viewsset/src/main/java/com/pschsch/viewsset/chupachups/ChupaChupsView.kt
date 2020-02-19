package com.pschsch.viewsset.chupachups

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.pschsch.viewsset.R

class ChupaChupsView : FrameLayout {

    companion object {
        private const val DEFAULT_CHUPA_CHUPS_WIDTH_DP = 56
        private const val DEFAULT_CHUPA_CHUPS_INTERNAL_PADDING = 28
        private val DEFAULT_CHUPA_CHUPS_BODY_COLOR = Color.parseColor("#E53935")
        private val DEFAULT_CHUPA_CHUPS_STICK_COLOR = Color.parseColor("#A22724")
        private const val DEFAULT_CHUPA_CHUPS_STICK_SHADOW_COLOR = Color.GRAY
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_CHUPA_CHUPS_BODY_COLOR
    }

    private val stickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_CHUPA_CHUPS_STICK_COLOR
    }

    private val stickShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_CHUPA_CHUPS_STICK_SHADOW_COLOR
        strokeCap = Paint.Cap.ROUND
    }

    private val densityDpi: Float
        get() = context.resources.displayMetrics.density

    private var chupaChupsPaddingInternal: Int = DEFAULT_CHUPA_CHUPS_INTERNAL_PADDING
    private var chupaChupsProgressInternal: Float = 0f

    private var startPickingAnimator = startPickingAnimator()
    private var cancelPickingAnimator = cancelPickingAnimator()
    private var pickingAnimator = pickingAnimator()

    private val shadowDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#FFc2c2c2"), Color.parseColor("#00c2c2c2"))
    ).apply {
        shape = GradientDrawable.OVAL
        gradientType = GradientDrawable.RADIAL_GRADIENT
    }

    var contentView: FrameLayout? = null

    /**Responsible for how high will chupachups jump on progress change, set in dp, get in px*/
    var chupaChupsPadding: Int
        get() = (chupaChupsPaddingInternal * densityDpi).toInt()
        set(value) {
            chupaChupsPaddingInternal = value
            requestLayout()
            invalidate()
        }
    var chupaChupsProgress: Float
        get() = chupaChupsProgressInternal
        set(value) {
            when {
                value < 0f -> this.chupaChupsProgressInternal = 0f
                value > 1f -> this.chupaChupsProgressInternal = 1f
                else -> this.chupaChupsProgressInternal = value
            }
            updateShadowBounds()

            val alphaShadowEffectiveMultiplier = chupaChupsProgressInternal * (1f + chupaChupsProgressInternal * 2.5f)

            shadowDrawable.alpha = (255 - 50 * alphaShadowEffectiveMultiplier).toInt()
            contentView?.translationY = -(chupaChupsPadding / 2 * chupaChupsProgressInternal)
            stickShadowPaint.alpha = 255 - (127 * chupaChupsProgressInternal).toInt()
            progressListener?.invoke(chupaChupsProgressInternal)
            invalidate()
        }

    var progressListener: ((Float) -> Unit)? = null

    var pickingAnimationListener: ((Float) -> Unit)? = null

    var isPicking: Boolean = false
        set(value) {
            startPickingAnimator.cancel()
            pickingAnimator.cancel()
            cancelPickingAnimator.cancel()
            cancelPickingAnimator = cancelPickingAnimator()
            field = value
            if (value) {
                startPickingAnimator.start()
            } else {
                cancelPickingAnimator.start()
            }
        }

    constructor(c: Context) : super(c) {
        init(null)
    }

    constructor(c: Context, set: AttributeSet) : super(c, set) {
        init(set)
    }

    private fun init(set: AttributeSet?) {
        contentView = FrameLayout(context)
        addView(contentView)
        set?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ChupaChupsView)
            try {
                chupaChupsPaddingInternal = a.getInt(
                        R.styleable.ChupaChupsView_chupa_padding,
                        DEFAULT_CHUPA_CHUPS_INTERNAL_PADDING
                )
                chupaChupsProgress = a.getFloat(R.styleable.ChupaChupsView_chupa_progress, 0f)
                bodyPaint.color = a.getColor(
                        R.styleable.ChupaChupsView_chupa_body_color,
                        DEFAULT_CHUPA_CHUPS_BODY_COLOR
                )
                stickPaint.color = a.getColor(
                        R.styleable.ChupaChupsView_chupa_stick_color,
                        DEFAULT_CHUPA_CHUPS_STICK_COLOR
                )
            } finally {
                a.recycle()
            }
        }
    }
    
    fun setColor(@ColorInt body: Int, @ColorInt stick: Int) {
        bodyPaint.color = body
        stickPaint.color = stick
        invalidate()
    }

    /**Contract: height = 1.4 * width, circle radius => contentView size = width*/
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val measuredWidth: Int
        val measuredHeight: Int
        when (widthMode) {
            MeasureSpec.EXACTLY -> {
                measuredWidth = widthSize + chupaChupsPadding
                measuredHeight = (1.4f * widthSize).toInt() + chupaChupsPadding
                setMeasuredDimension(measuredWidth, measuredHeight)
            }
            else -> {
                measuredWidth =
                        (DEFAULT_CHUPA_CHUPS_WIDTH_DP * densityDpi).toInt() + chupaChupsPadding
                measuredHeight =
                        (DEFAULT_CHUPA_CHUPS_WIDTH_DP * densityDpi * 1.4f).toInt() + chupaChupsPadding
                setMeasuredDimension(measuredWidth, measuredHeight)
            }
        }
        stickShadowPaint.strokeWidth = (measuredWidth - chupaChupsPadding) / 16f
        stickPaint.strokeWidth = (measuredWidth - chupaChupsPadding) / 16f
        layoutChildContent(measuredWidth - chupaChupsPadding)
    }

    private fun layoutChildContent(measuredWidth: Int) {
        Log.d("MeasuredWidth", "$measuredWidth")
        val params = LayoutParams(measuredWidth, measuredWidth)
        params.topMargin = chupaChupsPadding / 2
        params.gravity = Gravity.CENTER_HORIZONTAL
        contentView?.layoutParams = params
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateShadowBounds()
    }

    private fun updateShadowBounds() {
        val centerX = width / 2.toFloat()
        val centerY = height - chupaChupsPadding / 2f
        val progressPaddingWidth = chupaChupsPadding / 2 * chupaChupsProgressInternal
        val progressPaddingHeight = progressPaddingWidth / 7 * 5
        val stickWidth = stickPaint.strokeWidth

        val borderBoundWidthAppend = stickWidth * 1.8f
        val borderBoundHeightAppend = borderBoundWidthAppend / 5 + stickWidth / 2

        val ovalBoundsBorderLeft = centerX - progressPaddingWidth - borderBoundWidthAppend
        val ovalBoundsBorderRight = centerX + progressPaddingWidth + borderBoundWidthAppend
        val ovalBoundsBorderTop = centerY - progressPaddingHeight - borderBoundHeightAppend
        val ovalBoundsBorderBottom = centerY + progressPaddingHeight + borderBoundHeightAppend

        shadowDrawable.gradientRadius = (ovalBoundsBorderRight - ovalBoundsBorderLeft) / 1.6f

        shadowDrawable.setBounds(
                ovalBoundsBorderLeft.toInt(),
                ovalBoundsBorderTop.toInt(),
                ovalBoundsBorderRight.toInt(),
                ovalBoundsBorderBottom.toInt()
        )
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child === contentView) {
            super.addView(child, index, params)
        } else {
            contentView?.addView(child, index, params)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val firstHeight =
                width - chupaChupsPadding / 2 - chupaChupsPadding / 2 * chupaChupsProgressInternal + (width * 0.015f)
        val lastHeight =
                height - chupaChupsPadding / 2 - chupaChupsPadding / 2 * chupaChupsProgressInternal
        val halfOfHeight = lastHeight - (lastHeight - firstHeight)
        shadowDrawable.draw(canvas)
        canvas.drawPoint(
                width / 2.toFloat(),
                (height - chupaChupsPadding / 2f),
                stickShadowPaint
        )
        canvas.drawCircle(
                width / 2.toFloat(),
                width / 2f - chupaChupsPadding / 2 * chupaChupsProgressInternal,
                width / 2f - chupaChupsPadding / 2,
                bodyPaint
        )
        stickPaint.strokeCap = Paint.Cap.SQUARE
        canvas.drawLine(
                width / 2.toFloat(),
                firstHeight,
                width / 2.toFloat(),
                halfOfHeight,
                stickPaint
        )
        stickPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(
                width / 2.toFloat(),
                halfOfHeight,
                width / 2.toFloat(),
                lastHeight,
                stickPaint
        )
    }

    private fun startPickingAnimator(): Animator = ValueAnimator.ofFloat(0f, 0.9f).apply {
        duration = 250
        addUpdateListener {
            val value = (it.animatedValue as Float)
            chupaChupsProgress = value
            if (value > 0.85f) {
                pickingAnimator.start()
            }
        }
    }

    private fun cancelPickingAnimator(): Animator =
            ValueAnimator.ofFloat(chupaChupsProgressInternal, 0f).apply {
                addUpdateListener {
                    val value = (it.animatedValue as Float)
                    chupaChupsProgress = value
                }
                duration = 250
            }

    private fun basePickingAnimator(): ValueAnimator = ValueAnimator.ofFloat(0.9f, 0.5f)

    private fun pickingAnimator(): Animator = basePickingAnimator().apply {
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            val value = (it.animatedValue as Float)
            pickingAnimationListener?.invoke((value - 0.5f) * 2.5f)
            chupaChupsProgress = value
        }
        duration = 1000
    }
}
