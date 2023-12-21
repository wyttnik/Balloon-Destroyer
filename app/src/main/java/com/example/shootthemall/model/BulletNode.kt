package com.example.shootthemall.model

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator

class BulletNode : Node() {

  private fun localPositionAnimator(vararg values: Any?): ObjectAnimator {
    return ObjectAnimator().apply {
      target = this@BulletNode
      duration = 250
      interpolator = LinearInterpolator()
      setPropertyName("localPosition")
      setAutoCancel(true)
      // * = Spread operator, this will pass N `Any?` values instead of a single list `List<Any?>`
      setObjectValues(*values)
      // Always apply evaluator AFTER object values or it will be overwritten by a default one
      setEvaluator(Vector3Evaluator())
    }
  }

  fun animateShot(end: Vector3, scene: Scene) {
    val start = localPosition

    val animation = localPositionAnimator(start, end)

    animation.addListener(object: Animator.AnimatorListener{
      override fun onAnimationStart(animation: Animator) {
      }

      override fun onAnimationEnd(animation: Animator) {
        scene.removeChild(this@BulletNode)
      }

      override fun onAnimationCancel(animation: Animator) {
      }

      override fun onAnimationRepeat(animation: Animator) {
      }

    })
    animation.start()
  }
}