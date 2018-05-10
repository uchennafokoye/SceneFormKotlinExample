package com.example.codelab.sceneform

import android.graphics.*
import android.graphics.drawable.Drawable

class PointerDrawable : Drawable() {
    private final val paint: Paint = Paint()
    var enabled: Boolean = false


    override fun draw(canvas: Canvas) {
        val cx =  canvas.width / 2
        val cy = canvas.height / 2
        if (enabled){
            paint.color = Color.GREEN
            canvas.drawCircle(cx.toFloat(), cy.toFloat(), 10.0f, paint)
        } else {
            paint.color = Color.GRAY
            canvas.drawText("X", cx.toFloat(), cy.toFloat(), paint)
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

}