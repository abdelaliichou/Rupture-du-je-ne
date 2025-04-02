package com.example.qrcodescanner

import android.content.res.Resources
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qrcodescanner.R.color.validatedToast
import okhttp3.internal.cache2.Relay

class usersAdapter(private val userList: List<User>) :
    RecyclerView.Adapter<usersAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameSurname: TextView = view.findViewById(R.id.nameText)
        val phoneNumber: TextView = view.findViewById(R.id.phoneText)
        val email: TextView = view.findViewById(R.id.mailText)
        val validationContainer: RelativeLayout = view.findViewById(R.id.validationContainer)
        val validationIMG: ImageView = view.findViewById(R.id.validatoinIMG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.nameSurname.text = user.nameSurname
        holder.phoneNumber.text = user.phoneNumber
        holder.email.text = user.email
        if (user.validation_date == "true") {
            holder.validationIMG.setImageDrawable( ContextCompat.getDrawable( holder.itemView.context, R.drawable.valide))
            holder.validationContainer.setBackgroundColor( ContextCompat.getColor(holder.itemView.context, R.color.validatedToast))
        } else {
            holder.validationIMG.setImageDrawable( ContextCompat.getDrawable( holder.itemView.context, R.drawable.error ))
            holder.validationContainer.setBackgroundColor( ContextCompat.getColor(holder.itemView.context, R.color.unvalidatedToast))
        }

    }

    override fun getItemCount() = userList.size
}