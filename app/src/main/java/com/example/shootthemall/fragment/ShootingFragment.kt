package com.example.shootthemall.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.example.shootthemall.R
import com.example.shootthemall.ResultsActivity
import com.example.shootthemall.databinding.FragmentShootingBinding
import com.example.shootthemall.model.BalloonNode
import com.example.shootthemall.model.BalloonNode.Companion.spawnBalloon
import com.example.shootthemall.model.BulletNode
import com.example.shootthemall.model.JsonResult
import com.example.shootthemall.model.ShootingViewModel
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.TimeUnit


class ShootingFragment: Fragment() {
    companion object {
        // models
        var balloon_red: Renderable? = null
        var balloon_gold: Renderable? = null
        var balloon_black: Renderable? = null
        var bullet: Renderable? = null
        var tower: Renderable? = null
        var soundPool: SoundPool? = null
        var sound: Int? = null
        var sound_explosion: Int? = null

        fun randomBalloon():Renderable? {
            return when ((0..4).shuffled().first()){
                2 -> balloon_gold
                1,3 -> balloon_black
                else -> balloon_red
            }
        }
    }

    private lateinit var arFragment: ArFragment
    private val arSceneView get() = arFragment.arSceneView
    private val scene get() = arSceneView.scene
    private val camera get() = scene.camera
    private lateinit var point: Point
    private var globalAnchor: AnchorNode? = null
    private var gameStarted: Boolean = false
    private var hp = 3
    private var results = mutableListOf(0,0,0,0,0)
    private var resultUsers = mutableListOf<String?>(null, null, null, null, null)
    private var resultLabels = listOf("best1","best2", "best3", "best4", "best5")

    private var _binding: FragmentShootingBinding? = null
    private val binding get() = _binding!!
    private var sharedPreference: SharedPreferences? = null

    // viewModel
    private val viewModel: ShootingViewModel by viewModels()

    private fun changeResults(place: Int) {
        var i = 4
        while(resultUsers[i] == null && i != 0) --i // 1
        with (sharedPreference!!.edit()){
            while(i != place-2) {
                if (i == 4) {
                    --i
                    continue
                }
                putString(resultLabels[i+1], resultUsers[i])
                --i
            }
            apply()
        }
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK){
            val data = it.data!!
            val score = data.getIntExtra("score", 0)
            val username = data.getStringExtra("username")
            with (sharedPreference!!.edit()) {
                when {
                    score > results[0] -> {
                        changeResults(1)
                        putString("best1", Json.encodeToString(JsonResult(username!!, score)))
                    }
                    score > results[1] -> {
                        changeResults(2)
                        putString("best2", Json.encodeToString(JsonResult(username!!, score)))
                    }
                    score > results[2] -> {
                        changeResults(3)
                        putString("best3", Json.encodeToString(JsonResult(username!!, score)))
                    }
                    score > results[3] -> {
                        changeResults(4)
                        putString("best4", Json.encodeToString(JsonResult(username!!, score)))
                    }
                    score > results[4] -> {
                        changeResults(5)
                        putString("best5", Json.encodeToString(JsonResult(username!!, score)))
                    }
                }
                apply()
            }
            view?.findNavController()?.navigateUp()
            view?.findNavController()?.navigate(MainMenuFragmentDirections.actionMainMenuFragmentToLeaderboardsFragment())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity).supportActionBar?.hide()
        _binding = FragmentShootingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreference = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        for (i in 0 until resultUsers.size){
            resultUsers[i] = sharedPreference!!.getString(resultLabels[i], null)
            results[i] = if (resultUsers[i] == null) 0 else Json.decodeFromString<JsonResult>(resultUsers[i]!!).score
        }
        binding.buttonShoot.isGone = true
        binding.textTimer.isGone = true
        binding.textScore.isGone = true
        binding.hp1.isGone = true
        binding.hp2.isGone = true
        binding.hp3.isGone = true
        binding.buttonStart.setOnClickListener {
            startGame(it)
        }

        binding.buttonShoot.setOnClickListener {
            shoot()
        }

        viewModel.timeForCounter.observe(viewLifecycleOwner) {timeCounter ->
            val text = String.format(Locale.getDefault(), "%02d: %02d",TimeUnit.MILLISECONDS.toMinutes(timeCounter) % 60,
                TimeUnit.MILLISECONDS.toSeconds(timeCounter) % 60)
            binding.textTimer.text = text

            if (gameStarted && timeCounter <= 0) endGame()
        }

        viewModel.score.observe(viewLifecycleOwner) {currentScore->
            binding.textScore.text = currentScore.toString()
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.arFragment) as ArFragment).apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }
//            setOnViewCreatedListener { arSceneView ->
//                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
//                //scene.addOnUpdateListener(::onFrameUpdate)
//
//            }
            setOnTapArPlaneListener(::onTapPlane)
        }

        loadModels()
        loadSoundPool()
    }

    private fun endGame() {
        scene.removeChild(globalAnchor)
        globalAnchor = null
        binding.buttonStart.isGone = false
        binding.buttonShoot.isGone = true
        binding.textTimer.isGone = true
        binding.textScore.isGone = true
        gameStarted = false
        binding.hp1.apply {
            setImageResource(R.drawable.hp)
            isGone = true
        }
        binding.hp2.apply {
            setImageResource(R.drawable.hp)
            isGone = true
        }
        binding.hp3.apply {
            setImageResource(R.drawable.hp)
            isGone = true
        }
        hp = 3

        val intent = Intent(requireActivity(), ResultsActivity::class.java)
        intent.putExtra("points", viewModel.score.value)
        resultLauncher.launch(intent)
    }

    private fun startGame(view: View) {
        if (globalAnchor == null)
            Toast.makeText(context, "You need to choose standing position", Toast.LENGTH_SHORT).show()
        else {
            for (i in 0.. 19) {
                BalloonNode().apply {
                    parent = globalAnchor
                    renderable = balloon_red
                    localPosition = spawnBalloon()
                    localScale = Vector3(0.1f, 0.1f, 0.1f)
                    animateBalloon()
                }
            }
            binding.hp1.isGone = false
            binding.hp2.isGone = false
            binding.hp3.isGone = false
            binding.textScore.isGone = false
            view.isGone = true
            binding.buttonShoot.isGone = false
            binding.textTimer.isGone = false
            gameStarted = true
            viewModel.increaseScore(-viewModel.score.value!!)
            viewModel.counter()
        }
        return
    }

    private fun shoot() {
        point = Point(arSceneView.width/2, arSceneView.height/2)
        val ray = camera.screenPointToRay(point.x/1f, point.y/1f)
        val hit = scene.hitTest(ray,false)
        val hittedNode = hit.node

        if (hittedNode != null){
            scene.addChild(BulletNode().apply {
                renderable = bullet
                worldPosition = ray.getPoint(0f)
                animateShot(ray.getPoint(hit.distance), arSceneView.scene)
            })
            when(hittedNode.renderable){
                balloon_red -> {
                    (hittedNode as BalloonNode).apply {
                        localPosition = spawnBalloon()
                        renderable = randomBalloon()
                        animateBalloon(true)
                    }
                    soundPool!!.play(sound!!, 1f, 1f, 1, 0, 1f)
                    viewModel._timeForCounter.value = viewModel._timeForCounter.value?.plus(3000L)
                    viewModel.increaseScore(100)
                }
                balloon_black -> {
                    (hittedNode as BalloonNode).apply {
                        localPosition = spawnBalloon()
                        renderable = randomBalloon()
                        animateBalloon(true)
                    }
                    soundPool!!.play(sound_explosion!!, 1f, 1f, 1, 0, 1f)
                    when(hp) {
                        3 -> {
                            --hp
                            binding.hp1.setImageResource(R.drawable.heart_broken)
                        }
                        2 -> {
                            --hp
                            binding.hp2.setImageResource(R.drawable.heart_broken)
                        }
                        1 -> {
                            binding.hp3.setImageResource(R.drawable.heart_broken)
                            endGame()
                        }
                    }
                }
                balloon_gold -> {
                    (hittedNode as BalloonNode).apply {
                        localPosition = spawnBalloon()
                        renderable = randomBalloon()
                        animateBalloon(true)
                    }
                    soundPool!!.play(sound!!, 1f, 1f, 1, 0, 1f)
                    viewModel.increaseScore(1000)
                    viewModel._timeForCounter.value = viewModel._timeForCounter.value?.minus(10000L)
                }
            }
        }
        else{
            scene.addChild(BulletNode().apply {
                renderable = bullet
                worldPosition = ray.getPoint(0f)
                animateShot(ray.getPoint(3f), arSceneView.scene)
            })
        }
    }

    private fun loadSoundPool(){
        val audioAttribs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttribs)
            .build()

        sound = soundPool!!.load(context, R.raw.blop_sound,1)
        sound_explosion = soundPool!!.load(context, R.raw.explosion,1)
    }

    private fun loadModels() {
        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/balloon_red.glb"))
            .setIsFilamentGltf(true)
            .build().thenAccept {
                balloon_red = it
            }

        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/balloon_gold.glb"))
            .setIsFilamentGltf(true)
            .build().thenAccept {
                balloon_gold = it
            }

        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/balloon_black.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { balloon_black = it }

        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/tower.glb"))
            .setIsFilamentGltf(true)
            .build().thenAccept {
                tower = it
            }

        Texture.builder().setSource(context, R.drawable.texture).build()
            .thenAccept { texture ->
                MaterialFactory.makeOpaqueWithTexture(context, texture).thenAccept { material ->
                    bullet = ShapeFactory.makeSphere(0.01f, Vector3(0f,0f,0f),material)
                }
            }
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if(gameStarted) return
        if(globalAnchor != null)
            scene.removeChild(globalAnchor)
        globalAnchor = AnchorNode(hitResult.createAnchor()).apply {
            addChild(Node().apply {
                name = "tower"
                renderable = tower
            })
        }
        scene.addChild(globalAnchor)
    }

    /**
     * Frees the binding object when the Fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}