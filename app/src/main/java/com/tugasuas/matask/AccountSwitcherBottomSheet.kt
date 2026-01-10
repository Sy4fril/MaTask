package com.tugasuas.matask

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.tugasuas.matask.databinding.BottomSheetAccountSwitcherBinding

class AccountSwitcherBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAccountSwitcherBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAccountSwitcherBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.ic_profile_avatar)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.ivProfile)

            binding.tvDisplayName.text = currentUser.displayName ?: "Pengguna yang terhormat,"
            binding.tvEmail.text = currentUser.email

            binding.btnAbout.setOnClickListener {
                startActivity(Intent(activity, AboutActivity::class.java))
                dismiss()
            }

            binding.btnSignOut.setOnClickListener {
                auth.signOut()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AccountSwitcherBottomSheet"
    }
}
