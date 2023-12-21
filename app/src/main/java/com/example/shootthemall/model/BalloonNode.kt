package com.example.shootthemall.model

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.example.shootthemall.fragment.ShootingFragment.Companion.randomBalloon
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import kotlin.random.Random

class BalloonNode: Node() {
    var animation: ObjectAnimator? = null

    companion object{
        fun spawnBalloon(): Vector3 {
            val xOrZAxis = (0..1).shuffled().first()
            return if (xOrZAxis == 0) {
                val x = Random.nextFloat() * 5 - 2.5f;
                if ((0..1).shuffled().first() == 0)
                    Vector3(x, 1f,2.5f)
                else Vector3(x, 1f,-2.5f)
            } else {
                val z = Random.nextFloat() * 5 - 2.5f;
                if ((0..1).shuffled().first() == 0)
                    Vector3(2.5f, 1f,z)
                else Vector3(-2.5f, 1f,z)
            }
        }
    }
    private fun localPositionAnimator(vararg values: Any?): ObjectAnimator {
        return ObjectAnimator().apply {
            target = this@BalloonNode
            duration = 4000
            interpolator = LinearInterpolator()
            setPropertyName("localPosition")
            //setAutoCancel(true)
            // * = Spread operator, this will pass N `Any?` values instead of a single list `List<Any?>`
            setObjectValues(*values)
            // Always apply evaluator AFTER object values or it will be overwritten by a default one
            setEvaluator(Vector3Evaluator())
        }
    }

    fun animateBalloon(gotShoted: Boolean = false) {
        val start = localPosition
        val end = localPosition.apply { y = 3f }

        if (gotShoted)
            animation!!.cancel()

        animation = localPositionAnimator(start, end)

        if (!gotShoted) {
            animation!!.addListener(object: Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    this@BalloonNode.apply {
                        localPosition = spawnBalloon()
                        renderable = randomBalloon()
                        animateBalloon()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

            })
        }

        animation!!.start()
    }
}