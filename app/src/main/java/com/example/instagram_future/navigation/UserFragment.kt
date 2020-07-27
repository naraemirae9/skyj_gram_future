package com.example.instagram_future.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram_future.LoginActivity
import com.example.instagram_future.MainActivity
import com.example.instagram_future.R
import com.example.instagram_future.navigation.Model.ContentDTO
import com.example.instagram_future.navigation.Model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){
    var fragmentView:View?=null
    var firestore:FirebaseFirestore?=null
    var uid:String?=null
    var auth : FirebaseAuth?=null
    var cuurentUserUid:String?=null
    companion object{
        var PICK_PROFILE_FROM_ALBUM=10
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view=LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid=arguments?.getString("destinationUid")
        firestore= FirebaseFirestore.getInstance()
        auth= FirebaseAuth.getInstance()
        cuurentUserUid=auth?.currentUser?.uid

        if(uid==cuurentUserUid){
            //mypage
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener{
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //otheruserpage
            fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
            var mainactivity =(activity as MainActivity)
            mainactivity?.toolbar_usename?.text=arguments?.getString("userId")
            mainactivity?.toolbar_btn_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId=R.id.action_home
            }
            mainactivity?.toolbar_title_image?.visibility=View.GONE
            mainactivity?.toolbar_usename?.visibility=View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility=View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_recyclerview?.adapter=UserFragemnetRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager=GridLayoutManager(activity!!,3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent=Intent(Intent.ACTION_PICK)
            photoPickerIntent.type="image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot== null) return@addSnapshotListener
            var followDTO=documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text=followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text=followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(cuurentUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid !=cuurentUserUid){
                        fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter=null
                    }

                }
            }
        }
    }


    fun requestFollow(){
        //save data to my account
        var tsDocFollowing =firestore?.collection("users")?.document(cuurentUserUid!!)
        firestore?.runTransaction{transaction ->
            var followDTO=transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO ==null){
                followDTO = FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                //it remove following third person when a third person follow me
                followDTO?.followingCount=followDTO?.followingCount-1
                followDTO?.followers?.remove(uid)
            }else{
                //it add following third person when a third person do not follow me
                followDTO?.followingCount=followDTO?.followingCount+1
                followDTO?.followers[uid!!]=true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction

        }
        //save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction{transaction ->
            var followDTO=transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followerCount =1
                followDTO!!.followers[cuurentUserUid!!]=true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(cuurentUserUid)){
                //it cancel my follower when I follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount-1
                followDTO!!.followers.remove(cuurentUserUid!!)

            }else{
                followDTO!!.followerCount=followDTO!!.followerCount+1
                followDTO!!.followers[cuurentUserUid!!]=true
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction

        }
    }


    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot==null) return@addSnapshotListener
            if(documentSnapshot.data!=null){
                var url=documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().centerCrop()).into(fragmentView?.account_iv_profile!!)

            }
        }

    }
    inner class UserFragemnetRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
                init{
                    firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        //sometimes, This code return null of querySnapshot when it signout
                        if(querySnapshot==null)return@addSnapshotListener

                        //get data
                        for(snapshot in querySnapshot.documents){
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                        }
                        fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }
                }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {

            var width=resources.displayMetrics.widthPixels/3
            var imageview=ImageView(p0.context)
            imageview.layoutParams=LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {


        }

        override fun getItemCount(): Int {
           return contentDTOs.size
        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
           var imageview=(p0 as CustomViewHolder).imageview
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}