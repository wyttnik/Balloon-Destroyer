package com.example.shootthemall.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shootthemall.databinding.FragmentLeaderboardsBinding
import com.example.shootthemall.model.JsonResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LeaderboardsFragment : Fragment() {

    private var _binding: FragmentLeaderboardsBinding? = null
    private val binding get() = _binding!!
    private var sharedPreference: SharedPreferences? = null
    private var resultLabels = listOf("best1","best2", "best3", "best4", "best5")
    private var resultUsers = mutableListOf<String?>(null, null, null, null, null)
    private var users = mutableListOf<JsonResult?>(null, null, null, null, null)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLeaderboardsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreference = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        for(i in 0 until resultUsers.size){
            resultUsers[i] = sharedPreference!!.getString(resultLabels[i], null)
            users[i] = if (resultUsers[i] == null) null else Json.decodeFromString<JsonResult>(resultUsers[i]!!)
        }

        binding.textUsernameFirst.text = users[0]?.username ?: ""
        binding.textResultFirst.text = users[0]?.score?.toString() ?: ""
        binding.textUsernameSecond.text = users[1]?.username ?: ""
        binding.textResultSecond.text = users[1]?.score?.toString() ?: ""
        binding.textUsernameThird.text = users[2]?.username ?: ""
        binding.textResultThird.text = users[2]?.score?.toString() ?: ""
        binding.textUsernameFourth.text = users[3]?.username ?: ""
        binding.textResultFourth.text = users[3]?.score?.toString() ?: ""
        binding.textUsernameFifth.text = users[4]?.username ?: ""
        binding.textResultFifth.text = users[4]?.score?.toString() ?: ""
    }

    /**
     * Frees the binding object when the Fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}