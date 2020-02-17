package com.pschsch.viewsset.bottomsheetarrow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.pschsch.viewsset.R

class BottomSheetArrow : View {

    companion object {
        private const val WIDTH_RATIO_DEFAULT = 4f
        private const val ARROW_WIDTH_DEFAULT = 4f
        private const val BOTTOMSHEET_ARROW_COLOR_DEFAULT: Int = Color.BLACK
    }

    private val densityDpi: Float
        get() = context.resources.displayMetrics.density

    private val bottomSheetArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        color =
            BOTTOMSHEET_ARROW_COLOR_DEFAULT
        strokeWidth =
            ARROW_WIDTH_DEFAULT
    }

    private var progressInternal: Float = 0f
    private var widthRatioInternal: Float =
        WIDTH_RATIO_DEFAULT

    var progress: Float
        get() = progressInternal
        set(value) {
            progressInternal = value
            invalidate()
        }

    var widthRatio
        get() = widthRatioInternal
        set(value) {
            widthRatioInternal = value
            invalidate()
            requestLayout()
        }

    var arrowWidth
        get() = bottomSheetArrowPaint.strokeWidth / densityDpi
        set(value) {
            bottomSheetArrowPaint.strokeWidth = value * densityDpi
            invalidate()
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, set: AttributeSet) : super(context, set) {
        init(set)
    }

    private fun init(set: AttributeSet?) {
        set?.let {
            val a = context.obtainStyledAttributes(it,
                R.styleable.BottomSheetArrow
            )
            try {
                progressInternal = a.getFloat(R.styleable.BottomSheetArrow_ba_progress, 0f)
                widthRatioInternal =
                    a.getFloat(
                        R.styleable.BottomSheetArrow_ba_width_ration,
                        WIDTH_RATIO_DEFAULT
                    )
                bottomSheetArrowPaint.color = a.getColor(
                    R.styleable.BottomSheetArrow_ba_color,
                    BOTTOMSHEET_ARROW_COLOR_DEFAULT
                )
                bottomSheetArrowPaint.strokeWidth =
                    a.getFloat(
                        R.styleable.BottomSheetArrow_ba_arrow_width,
                        ARROW_WIDTH_DEFAULT
                    )
            } finally {
                a.recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val maxAllowedHeight = (width / widthRatio).toInt()
        if (height > maxAllowedHeight) {
            height = maxAllowedHeight
        }
        setMeasuredDimension(width, height)
    }

    private fun yPosition(reversed: Boolean): Float {
        val minPadding = - (bottomSheetArrowPaint.strokeWidth / 2)
        val maxPadding = bottomSheetArrowPaint.strokeWidth / 2
        val diff = maxPadding - minPadding
        return if (!reversed) {
            (height - (height * progress)) + minPadding + diff * progress
        } else {
            height * progress + maxPadding - diff * progress
        }
    }

    override fun onDraw(canvas: Canvas) {
        val lines = floatArrayOf(
            bottomSheetArrowPaint.strokeWidth, yPosition(false),
            width / 2.toFloat(), yPosition(true),
            width / 2.toFloat(), yPosition(true),
            width - bottomSheetArrowPaint.strokeWidth, yPosition(false)
        )
        canvas.drawLines(lines, bottomSheetArrowPaint)
    }
}