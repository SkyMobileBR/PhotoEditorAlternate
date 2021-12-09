package ja.burhanrashid52.photoeditor.shape

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import ja.burhanrashid52.photoeditor.shape.AbstractShape
import android.graphics.RectF
import ja.burhanrashid52.photoeditor.shape.ShapeType
import androidx.annotation.ColorInt
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder

abstract class AbstractShape : Shape {
    protected var TOUCH_TOLERANCE = 4f
    protected var path = Path()
    protected var left = 0f
    protected var top = 0f
    protected var right = 0f
    protected var bottom = 0f
    protected abstract val tag: String
    override fun draw(canvas: Canvas, paint: Paint?) {
        canvas.drawPath(path, paint!!)
    }

    val bounds: RectF
        get() {
            val bounds = RectF()
            path.computeBounds(bounds, true)
            return bounds
        }

    fun hasBeenTapped(): Boolean {
        val bounds = bounds
        return bounds.top < TOUCH_TOLERANCE && bounds.bottom < TOUCH_TOLERANCE && bounds.left < TOUCH_TOLERANCE && bounds.right < TOUCH_TOLERANCE
    }

    override fun toString(): String {
        return tag +
                ": left: " + left +
                " - top: " + top +
                " - right: " + right +
                " - bottom: " + bottom
    }
}