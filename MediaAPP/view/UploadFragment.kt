package com.msamedcagli.socialmediaplatform.view

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.msamedcagli.socialmediaplatform.R
import com.msamedcagli.socialmediaplatform.databinding.FragmentUploadBinding
import java.io.IOException
import java.util.*

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var selectedPicture: Uri? = null
    private var selectedBitmap: Bitmap? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLaunchers()
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("UploadDebug", "onCreateView çalıştı")
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("UploadDebug", "onViewCreated çalıştı")
        binding.imageUploadView.setOnClickListener {
            Log.d("UploadDebug", "imageUploadView tıklandı")
            selectImage(it)
        }
        binding.uploadbutton.setOnClickListener {
            Log.d("UploadDebug", "uploadbutton tıklandı")
            uploadClicked(it)
        }
    }

    private fun selectImage(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), android.Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Galeriye erişim için izin gerekli", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin ver") {
                            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }.show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                openGallery()
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Galeriye erişim için izin gerekli", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin ver") {
                            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }.show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(intentToGallery)
    }

    private fun uploadClicked(view: View) {
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"

        val storage = Firebase.storage
        val reference = storage.reference
        val imageRef = reference.child("images").child(imageName)

        if (selectedPicture != null) {
            imageRef.putFile(selectedPicture!!)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()

                        val postMap = hashMapOf<String, Any>()
                        postMap["downloadUrl"] = downloadUrl
                        postMap["userEmail"] = auth.currentUser?.email ?: "anonim"
                        postMap["comment"] = binding.commentText.text.toString()
                        postMap["date"] = Timestamp.now()

                        db.collection("Posts").add(postMap)
                            .addOnSuccessListener {
                                Log.d("UploadDebug", "Gönderi Firestore'a kaydedildi, FeedFragment'a geçiliyor")

                                try {
                                    // 🔁 Safe Args yerine doğrudan R.id ile gidiyoruz
                                    findNavController().navigate(R.id.feedFragment)
                                    Log.d("UploadDebug", "Navigasyon başarılı")
                                } catch (e: Exception) {
                                    Log.e("UploadDebug", "Navigasyon hatası: ${e.localizedMessage}")
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
                            }

                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Lütfen bir görsel seçin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerLaunchers() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("UploadDebug", "Galeri sonucu döndü")
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data
                    Log.d("UploadDebug", "Resim seçildi: $selectedPicture")
                    try {
                        selectedBitmap = if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedPicture!!)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedPicture)
                        }
                        Log.d("UploadDebug", "Bitmap decode edildi")
                        binding.imageUploadView.setImageBitmap(selectedBitmap)
                    } catch (e: IOException) {
                        Log.e("UploadDebug", "Bitmap decode hatası: ${e.localizedMessage}")
                        e.printStackTrace()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                openGallery()
            } else {
                Toast.makeText(requireContext(), "İzin gerekli!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
