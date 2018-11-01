package me.varunon9.saathmetravel.ui.chat;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.varunon9.saathmetravel.ChatFragmentActivity;
import me.varunon9.saathmetravel.R;
import me.varunon9.saathmetravel.constants.AppConstants;
import me.varunon9.saathmetravel.models.Chat;
import me.varunon9.saathmetravel.models.User;
import me.varunon9.saathmetravel.utils.FirestoreDbOperationCallback;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private ChatFragmentActivity chatFragmentActivity;
    private ChatViewModel chatViewModel;
    private EditText nameEditText;
    private EditText preferenceEditText;
    private Button updateProfileButton;
    private Button chatWithTravellerButton;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton;
    private RadioButton femaleRadioButton;
    private User currentUser;
    private String TAG = "ProfileFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.profile_fragment, container, false);
        chatFragmentActivity = (ChatFragmentActivity) getActivity();

        nameEditText = rootView.findViewById(R.id.nameEditText);
        preferenceEditText = rootView.findViewById(R.id.preferenceEditText);
        updateProfileButton = rootView.findViewById(R.id.updateProfileButton);
        chatWithTravellerButton = rootView.findViewById(R.id.chatWithTravellerButton);
        genderRadioGroup = rootView.findViewById(R.id.genderRadioGroup);
        maleRadioButton = rootView.findViewById(R.id.maleRadioButton);
        femaleRadioButton = rootView.findViewById(R.id.femaleRadioButton);

        updateProfileButton.setOnClickListener(this);
        chatWithTravellerButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        chatViewModel = ViewModelProviders.of(this.getActivity()).get(ChatViewModel.class);
        getTravellerProfileFromFirestore();
    }

    private void getTravellerProfileFromFirestore() {
        chatFragmentActivity.showProgressDialog("Fetching Traveller info",
                "Please wait", false);
        chatFragmentActivity.firestoreDbUtility.getOne(AppConstants.Collections.USERS,
                chatFragmentActivity.chatRecipientUid, new FirestoreDbOperationCallback() {
                    @Override
                    public void onSuccess(Object object) {
                        DocumentSnapshot documentSnapshot = (DocumentSnapshot) object;
                        currentUser = documentSnapshot.toObject(User.class);
                        setProfileDetails(currentUser);
                        chatFragmentActivity.dismissProgressDialog();
                    }

                    @Override
                    public void onFailure(Object object) {
                        chatFragmentActivity.dismissProgressDialog();
                        chatFragmentActivity.showMessage("Failed to fetch traveller info");
                    }
                });
    }

    private void setProfileDetails(User user) {
        nameEditText.setText(user.getName());
        preferenceEditText.setText(user.getPreference());
        if (user.getGender().toLowerCase().equals(AppConstants.Gender.MALE)) {
            maleRadioButton.setChecked(true);
        } else if (user.getGender().toLowerCase().equals(AppConstants.Gender.FEMALE)) {
            femaleRadioButton.setChecked(true);
        }

        if (!chatFragmentActivity.chatInitiatorUid.equals(chatFragmentActivity.chatRecipientUid)) {
            updateProfileButton.setVisibility(View.INVISIBLE);

            // disabling form fields
            nameEditText.setEnabled(false);
            preferenceEditText.setEnabled(false);
            maleRadioButton.setEnabled(false);
            femaleRadioButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.updateProfileButton: {
                if (chatFragmentActivity.chatInitiatorUid.equals(chatFragmentActivity.chatRecipientUid)) {
                    String name = nameEditText.getText().toString();
                    String preference = preferenceEditText.getText().toString();
                    String gender = AppConstants.Gender.MALE;
                    int selectedGenderButtonId = genderRadioGroup.getCheckedRadioButtonId();
                    if (selectedGenderButtonId == R.id.femaleRadioButton) {
                        gender = AppConstants.Gender.FEMALE;
                    }

                    if (name == null || preference == null || gender == null) {
                        chatFragmentActivity.showMessage("All fields are mandatory");
                        return;
                    }

                    Map<String, Object> hashMap = new HashMap<>();
                    hashMap.put("name", name);
                    hashMap.put("preference", preference);
                    hashMap.put("gender", gender);
                    updateProfile(hashMap);
                }
                break;
            }

            case R.id.chatWithTravellerButton: {
                Chat chat = new Chat();
                chat.setInitiatorUid(chatFragmentActivity.chatInitiatorUid);
                chat.setInitiatorName(chatFragmentActivity.chatInitiatorName);
                chat.setRecipientName(currentUser.getName());
                chat.setRecipientUid(currentUser.getUid());

                List<String> participants = new ArrayList<>();
                participants.add(chatFragmentActivity.chatInitiatorUid);
                participants.add(currentUser.getUid());
                chat.setParticipantsUid(participants);

                chatViewModel.setSelectedChat(chat);
                chatFragmentActivity.goToChatFragment();
                break;
            }
        }

    }

    private void updateProfile(Map<String, Object> hashMap) {
        chatFragmentActivity.showProgressDialog("Updating profile",
                "Please wait", false);
        chatFragmentActivity.firestoreDbUtility.update(AppConstants.Collections.USERS,
                chatFragmentActivity.chatInitiatorUid,
                hashMap, new FirestoreDbOperationCallback() {
                    @Override
                    public void onSuccess(Object object) {
                        chatFragmentActivity.dismissProgressDialog();
                        chatFragmentActivity.showMessage("Profile updated!");
                    }

                    @Override
                    public void onFailure(Object object) {
                        chatFragmentActivity.dismissProgressDialog();
                        chatFragmentActivity.showMessage("Failed to update profile!");
                    }
                });
    }
}
