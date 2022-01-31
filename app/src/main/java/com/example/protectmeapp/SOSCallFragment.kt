package com.example.protectmeapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.protectmeapp.databinding.FragmentSosBinding
import com.example.protectmeapp.map.MapFragment
import com.example.protectmeapp.victim.VictimInfoFragment

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SOSCallFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null

    companion object {
        private const val FRAG_MAP = "FRAG_MAP"
        private const val FRAG_ORDER = "FRAG_VICTIM_INFO"

        fun newInstance() = SOSCallFragment().apply {
            arguments = bundleOf()
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    private fun initFragments() {

        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.map_container, MapFragment.newInstance(), FRAG_MAP)
            ?.commitAllowingStateLoss()

        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.order_container, VictimInfoFragment.newInstance(),FRAG_ORDER)
            ?.commitAllowingStateLoss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}