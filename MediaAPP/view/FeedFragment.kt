package com.msamedcagli.socialmediaplatform.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.msamedcagli.socialmediaplatform.R
import com.msamedcagli.socialmediaplatform.adapter.FeedRecyclerAdapter
import com.msamedcagli.socialmediaplatform.databinding.FragmentFeedBinding
import com.msamedcagli.socialmediaplatform.model.Post

class FeedFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var adapter: FeedRecyclerAdapter
    private val postArrayList = ArrayList<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.floatingActionButton.setOnClickListener { floatingActionButtonClicked(it) }

        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FeedRecyclerAdapter(postArrayList)
        binding.feedRecyclerView.adapter = adapter

        getDataFromFirestore()
    }

    private fun getDataFromFirestore() {
        db.collection("Posts")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                } else {
                    if (snapshot != null && !snapshot.isEmpty) {
                        postArrayList.clear()
                        val documents = snapshot.documents
                        for (document in documents) {
                            val comment = document.getString("comment") ?: ""
                            val userEmail = document.getString("userEmail") ?: ""
                            val downloadUrl = document.getString("downloadUrl") ?: ""
                            val post = Post(userEmail, comment, downloadUrl)
                            postArrayList.add(post)
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
    }

    private fun floatingActionButtonClicked(view: View) {
        val popup = PopupMenu(requireContext(), binding.floatingActionButton)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.my_popup_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.yuklemeItem -> {
                val action = FeedFragmentDirections.actionFeedFragmentToUploadFragment()
                Navigation.findNavController(requireView()).navigate(action)
                true
            }
            R.id.cikisItem -> {
                auth.signOut()
                val action = FeedFragmentDirections.actionFeedFragmentToKullaniciFragment()
                Navigation.findNavController(requireView()).navigate(action)
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
