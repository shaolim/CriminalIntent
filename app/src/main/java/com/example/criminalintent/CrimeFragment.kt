package com.example.criminalintent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.dialog.DatePickerFragment
import com.example.criminalintent.dialog.PhotoPreviewFragment
import com.example.criminalintent.entity.Crime
import com.example.criminalintent.util.formatDate
import com.example.criminalintent.util.getScaledBitmap
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_crime.*
import java.io.File
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "dialogDate"
private const val DIALOG_PHOTO_PREVIEW = "dialogPhotoPreview"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val PERMISSION_REQUEST_CONTACT = 0

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, View.OnClickListener {

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "com.example.criminalintent.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // this space intentionally left blank
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // this space intentionally left blank
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
        }
        edtCrimeTitle.addTextChangedListener(titleWatcher)

        chkCrimeSolve.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        btnCrimeDate.setOnClickListener(this)
        btnCrimeReport.setOnClickListener(this)
        btnCallSuspect.setOnClickListener(this)
        imCrimePhoto.setOnClickListener(this)

        val packageManager: PackageManager = requireActivity().packageManager
        btnCrimeSuspect.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener(this@CrimeFragment)
        }

        ibtnCrimeCamera.apply {
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener(this@CrimeFragment)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnCrimeDate -> showDatePicker()
            R.id.btnCrimeReport -> sendReport()
            R.id.btnCallSuspect -> callSuspect()
            R.id.btnCrimeSuspect -> openContact()
            R.id.ibtnCrimeCamera -> openCamera()
            R.id.imCrimePhoto -> showPhotoPreview()
        }
    }

    private fun showDatePicker() {
        DatePickerFragment.newInstance(crime.date).apply {
            setTargetFragment(this@CrimeFragment, REQUEST_DATE)
            show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
        }
    }

    private fun sendReport() {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getCrimeReport())
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.label_crime_report_subject))
        }.also { intent ->
            val chooserIntent =
                Intent.createChooser(intent, getString(R.string.label_send_report))
            startActivity(chooserIntent)
        }
    }

    private fun callSuspect() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${crime.suspectPhoneNumber}")
        }
        startActivity(dialIntent)
    }

    private fun openContact() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startContact()
        } else {
            requestCameraPermission()
        }
    }

    private fun startContact() {
        startActivityForResult(
            Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            ), REQUEST_CONTACT
        )
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            view?.let {
                Snackbar.make(it, "need Permission", Snackbar.LENGTH_SHORT)
                    .setAction("Request Permission") {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.READ_CONTACTS),
                            REQUEST_CONTACT
                        )
                    }.show()
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACT
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CONTACT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startContact()
                }
            }
        }
    }

    private fun openCamera() {
        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        val packageManager = requireActivity().packageManager
        val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
            captureImage,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        for (cameraActivity in cameraActivities) {
            requireActivity().grantUriPermission(
                cameraActivity.activityInfo.packageName,
                photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        startActivityForResult(captureImage, REQUEST_PHOTO)
    }

    private fun showPhotoPreview() {
        if (photoFile.exists()) {
            PhotoPreviewFragment.newInstance(photoFile.path)
                .show(this.parentFragmentManager, DIALOG_PHOTO_PREVIEW)
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        revokeWriteUriPermission()
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        edtCrimeTitle.setText(crime.title)

        btnCrimeDate.text =
            formatDate(crime.date, resources)
        chkCrimeSolve.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()) {
            btnCrimeSuspect.text = crime.suspect
        }

        if (crime.suspectPhoneNumber.isNotBlank()) {
            btnCallSuspect.visibility = View.VISIBLE
        }

        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            imCrimePhoto.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (imCrimePhoto == null) {
                            return
                        }

                        imCrimePhoto.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        val bitmap =
                            getScaledBitmap(
                                photoFile.path, imCrimePhoto.width, imCrimePhoto.height
                            )
                        imCrimePhoto.setImageBitmap(bitmap)
                    }
                }
            )
        } else {
            imCrimePhoto.setImageDrawable(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val cursor = contactUri?.let {
                    requireActivity().contentResolver
                        .query(it, null, null, null, null)
                }

                cursor?.use {
                    if (it.count == 0) {
                        return
                    }

                    it.moveToFirst()
                    val suspect =
                        it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    crime.suspect = suspect
                    crime.suspectPhoneNumber =
                        PhoneNumberUtils.formatNumberToE164(phoneNumber, "ID")
                    crimeDetailViewModel.saveCrime(crime)
                    btnCrimeSuspect.text = suspect
                }
            }

            requestCode == REQUEST_PHOTO -> {
                revokeWriteUriPermission()
                updatePhotoView()
            }
        }
    }

    private fun revokeWriteUriPermission() {
        requireActivity().revokeUriPermission(
            photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.label_crime_report_solved)
        } else {
            getString(R.string.label_crime_report_unsolved)
        }

        val dateString =
            formatDate(crime.date, resources)
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.label_crime_report_no_suspect)
        } else {
            getString(R.string.label_crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.label_crime_report_body,
            crime.title,
            dateString,
            solvedString,
            suspect
        )
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}
