package com.example.protectmeapp.victim

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.protectmeapp.databinding.FragmentSosBinding
import com.example.protectmeapp.databinding.FragmentVictimInfoBinding

class VictimInfoFragment : Fragment() {

    companion object {
        fun newInstance(): VictimInfoFragment {
            return VictimInfoFragment()
        }

        var onPhone1Click: (() -> Unit)? = null
        var onPhone2Click: (() -> Unit)? = null
        var onEmailClick: (() -> Unit)? = null
    }

    private var _binding: FragmentVictimInfoBinding? = null
    private val binding get() = _binding!!

    private var isLegal: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVictimInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViewModel() {

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun FragmentVictimInfoBinding.initUi(order: VictimData) {

        clAdditionalClientInfo.updateUI(order)

        with(order) {
            tvDeliveryWindow.text = date
                ?.let {
                    DateConverter.convertDateToStringShortFormat(it)
                } + " " + deliveryWindow
            tvCompanyAddress.text = addressData.address

            tvAddressComment.isVisible = !addressData.addressComment.isNullOrEmpty()
            if (tvAddressComment.isVisible) tvAddressComment.text = addressData.addressComment.orEmpty()

            tvTransport.text = vehicleType.orEmpty()
            tvSalaryBank.text = salaryBank.orEmpty()
            tvAttendees.text = attendeesCount?.toString().orEmpty()
            tvCards.text = cardsCount?.toString().orEmpty()
            tvMpkpComment.text = commentMPKP.orEmpty()
            tvRgppComment.text = commentRGPP.orEmpty()
            tvDispatcher.text = assigneeDispatcher
            initClientTypeBlock(this, isLegal)
        }
    }

    private fun FragmentVictimInfoBinding.initClientTypeBlock(order: OrderData, isLegal: Boolean) {

        if (isLegal) {
            divider1.visibility = View.VISIBLE
            llContactPersons.visibility = View.VISIBLE
            tvCompanyProfile.text = order.company?.companyField.orEmpty()
            tvCompanyProfile.isVisible = !tvCompanyProfile.text.isNullOrEmpty()
            llContactPersons.removeAllViews()

            order.companyContactPersons?.forEach { p ->
                val view = ContactView(requireActivity())
                view.setName(p.contactName.orEmpty())
                view.setPosition(p.contactPosition.orEmpty())
                p.contactPhone?.let { phone ->
                    view.setPhone1(phone)
                }
                p.contactAdditionalPhone?.let { phone ->
                    view.setPhone2(phone)
                }
                view.setEmail(p.contactEmail.orEmpty())

                llContactPersons.addView(view)
            }
        } else {
            ivPhone.visibility = View.VISIBLE
            with(tvPhone1) {
                visibility = View.VISIBLE
                text = order.person?.personPhone.orEmpty()
                if (!text.isNullOrEmpty()) setOnClickListener {
                    if (onPhone1Click == null) openPhoneDialog(text.toString()) else onPhone1Click?.invoke()
                }
            }

            with(tvPhone2) {
                visibility = View.VISIBLE
                text = order.person?.personAdditionalPhone.orEmpty()
                if (!text.isNullOrEmpty()) setOnClickListener {
                    if (onPhone2Click == null) openPhoneDialog(text.toString()) else onPhone2Click?.invoke()
                }
            }

            ivEmail.visibility = View.VISIBLE
            with(tvEmail) {
                visibility = View.VISIBLE
                text = order.person?.personEmail
                if (!text.isNullOrEmpty()) setOnClickListener {
                    if (onEmailClick == null) openEmailDialog(text.toString()) else onEmailClick?.invoke()
                }

                text = order.person?.personEmail
            }
        }
    }

    /**
     * Вызов диалога набора номера
     */
    private fun openPhoneDialog(phone: String?) {
        val intent = Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:$phone"))
        context?.startActivity(intent)
    }

    /**
     * Вызов диалога отправки почты
     */
    private fun openEmailDialog(email: String?) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context?.startActivity(
            Intent.createChooser(
                emailIntent,
                context?.getString(R.string.email_title)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
