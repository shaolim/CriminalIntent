package com.example.criminalintent.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.criminalintent.R
import com.example.criminalintent.util.getScaledBitmap
import kotlinx.android.synthetic.main.dialog_photo_preview.*

private const val ARG_PHOTO_PATH = "photo_path"

class PhotoPreviewFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_photo_preview, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.isCancelable = false

        val photoPath = arguments?.getString(ARG_PHOTO_PATH)

        if (photoPath == null) {
            dismiss()
        }

        val bitmap = photoPath?.let {
            getScaledBitmap(
                it,
                requireActivity()
            )
        }
        imPhotoPreview.setImageBitmap(bitmap)
    }

    companion object {
        fun newInstance(photoPath: String) : PhotoPreviewFragment {
            val bundle = Bundle().apply {
                putString(ARG_PHOTO_PATH, photoPath)
            }

            return PhotoPreviewFragment().apply {
                arguments = bundle
            }
        }
    }
}