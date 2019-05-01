package com.fellow.yoo.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.ContactGroupActivity;
import com.fellow.yoo.MainActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.SelectContactActivity;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.adapter.ChatListAdapter;
import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.chat.Updatable;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.ContactDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.model.Chat;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ChatListFragment extends YooListFragment implements ChatListener, Updatable {

	public final static int SELECT_CONTACT = 1;
	public final static int SELECT_GROUP = 2;
	public final static int SELECT_BROADCAST = 3;

	private List<String> sections;
	private Map<String, List<Chat>> chats;
	private Map<String, Contact> contacts;
	private Map<String, YooMessage> yooMessages;
	private boolean refresh = false;
	private boolean popupShow = false;
	/**
	 * A pointer to the current callbacks instance (the Activity).
	 */
	private ChatListCallbacks mCallbacks;

	public static ChatListFragment newInstance(String jId) {

		ChatListFragment f = new ChatListFragment();
		Bundle args = new Bundle();
		args.putString("jId", jId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ChatTools.sharedInstance().addListener(this);
		yooMessages = new HashMap<String, YooMessage>();
		contacts = new HashMap<String, Contact>();
		for (Contact contact : new ContactDAO().list()) { 
			contacts.put(String.valueOf(contact.getContactId()), contact);
		}
	}
	
	private Contact getContact(Long contactId){
		Contact contact = contacts.get(String.valueOf(contactId));
		if(contact == null){
			contact = new ContactDAO().find(contactId);
		}
		return contact;
	}
	
	private YooMessage getUserMessage(YooUser yooUser){
		if(yooMessages.containsKey(yooUser.toJID())){
			return yooMessages.get(yooUser.toJID());
		}
		
		List<YooMessage> userMsgs = ChatTools.sharedInstance().messagesForRecipient(yooUser, false);
		if(userMsgs.size() > 0){
			// yooMessages.put(yooUser.toJID(), userMsgs.get(0));
			return userMsgs.get(0);
		}
		return null;
	}

	int i = 0; 
	public void updateData() {
		sections = new ArrayList<String>();
		chats = new HashMap<String, List<Chat>>();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);

		// add users
		List<YooUser> users = new UserDAO().list();
		for (YooUser yooUser : users) {
			if (!yooUser.isMe() && yooUser.getContactId() != -1) {
				Contact contact = getContact(yooUser.getContactId());
				if (contact == null) {
					contact = new ContactManager().findById(yooUser.getContactId());
					if (contact != null) { // insert new contact to Yoo Contacts.
						new ContactDAO().upsert(contact);
					}
				}
				
				if (contact != null) {
					// List<YooMessage> userMsgs = ChatTools.sharedInstance().messagesForRecipient(yooUser, false);
					YooMessage last = getUserMessage(yooUser);
					// if (userMsgs.size() > 0) {
					if (last != null) {
						// YooMessage last = userMsgs.get(0);
						int unread = new ChatDAO().unreadCountForSender(yooUser);
						Chat chat = new Chat(yooUser, last, unread);
						String section = sdf.format(last.getDate());
						if (!sections.contains(section)) {
							sections.add(section);
							chats.put(section, new ArrayList<Chat>());
						}
						List<Chat> sectionChats = chats.get(section);
						sectionChats.add(chat);
					}
				}
			}
		}

		// add groups 
		for (YooGroup yooGroup : new GroupDAO().list()) {
			String section = sdf.format(yooGroup.getDate() != null ? yooGroup.getDate() : new Date());
			YooMessage last = null;
			List<YooMessage> groupMsgs = ChatTools.sharedInstance()
					.messagesForRecipient(yooGroup, false);
			if (groupMsgs.size() > 0) {
				last = groupMsgs.get(0);
				section = sdf.format(last.getDate());
			}

			if (!sections.contains(section)) {
				sections.add(section);
				chats.put(section, new ArrayList<Chat>());
			}
			int unread = new ChatDAO().unreadCountForSender(yooGroup);
			List<Chat> sectionChats = chats.get(section);
			sectionChats.add(new Chat(yooGroup, last, unread));
		}

		
		Collections.sort(sections);
		Collections.reverse(sections);
		for (String section : sections) {
			List<Chat> chatsList = chats.get(section);
			Collections.sort(chatsList, new Comparator<Chat>() {
				@Override
				public int compare(Chat lhs, Chat rhs) {
					try {
						return rhs.getLastMsg().getDate().compareTo(lhs.getLastMsg().getDate());
					} catch (Exception e) {
						return 0;
					}
				}
			});
		}
		

		/** Setting the list adapter for the ListFragment **/
		// setListAdapter(new ChatListAdapter(sections, chats));
		getListView().setAdapter(new ChatListAdapter(getActivity(), sections, chats));
		
		checkNewRefresh();
	}

	@Override
	@SuppressLint("UseSparseArrays")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		((YooActivity) getActivity()).getBtnAddChat().setVisibility(View.VISIBLE);
		((YooActivity) getActivity()).getBtnGroup().setVisibility(View.VISIBLE);

		// To refresh left slide notification
		((MainActivity) getActivity()).updateMenu();

		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		view.setBackgroundColor(Color.WHITE);

		getListView().setOnCreateContextMenuListener(
				new OnCreateContextMenuListener() {
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenu.ContextMenuInfo menuInfo) {
						menu.setHeaderTitle(R.string.chat_list);
						menu.add(0, v.getId(), 0, R.string.chat);
						menu.add(0, v.getId(), 0, R.string.action_delete);
					}
				});

		updateData();

		// load recent chat.
		/*
		 * if(getArguments() != null){ YooRecipient recipient = new
		 * UserDAO().findByJid(getArguments().getString("jId")); if(recipient !=
		 * null){ ChatFragment chatFragment =
		 * ChatFragment.newInstance(recipient, false); ((YooActivity)
		 * getActivity()).pushFragment(chatFragment);
		 * getArguments().putString("jId", ""); // clear recent Jid when press
		 * back button } }
		 */

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onListItemClick(AdapterView<?> adapter, View v, int position,
			long id) {
		startChat(position);
	}

	private void startChat(int position) {
		// Chat chat = (Chat)getListAdapter().getItem(position);
		Chat chat = (Chat) getListView().getAdapter().getItem(position);
		if (chat != null && chat.getRecipient() != null) {
			if (getActivity() instanceof YooActivity) {
				ChatFragment chatFragment = ChatFragment.newInstance(
						chat.getRecipient(), false);
				((YooActivity) getActivity()).pushFragment(chatFragment);
			} else {
				mCallbacks.onChatSelected(chat.getRecipient());
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallbacks = (ChatListCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement ChatListCallbacks.");
		}
	}

	@Override
	public void actionClicked(int id) {
		YooApplication.setLeaveApp(false); // = false;
		if (id == R.id.btn_group) {
			ListView list = new ListView(getActivity());
			
			final AlertDialog.Builder dia = new AlertDialog.Builder(getActivity());
			List<String> options = Arrays.asList(getString(R.string.action_broadcast),
					getString(R.string.new_group));
			list.setAdapter(new ArrayAdapter<String>(getActivity(),
					android.R.layout.simple_list_item_1, options));
			dia.setTitle(R.string.chat_list);
			dia.setView(list);
			dia.setNegativeButton(getString(R.string.cancel), null);
			final Dialog newDia = dia.create();
			newDia.show();

			list.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View v, int pos,
						long arg3) {
					if (pos == 1) {
						Intent intent = new Intent(getActivity(), ContactGroupActivity.class);
						startActivityForResult(intent, SELECT_GROUP);
					} else {
						Intent intent = new Intent(getActivity(), ContactGroupActivity.class);
						intent.putExtra("broadcast", true);
						startActivityForResult(intent, SELECT_BROADCAST);
					}
					newDia.dismiss();
				}
			});

		} else if (id == R.id.btn_add_chart) {
			Intent intent = new Intent(getActivity(),
					SelectContactActivity.class);
			intent.putExtra("type", ContactListFragment.Type.ContactSelect);
			startActivityForResult(intent, SELECT_CONTACT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == SELECT_CONTACT) {
				YooRecipient recipient = (YooRecipient) data
						.getSerializableExtra("recipient");
				if (recipient != null) { // create new chat
					ChatFragment chatFragment = ChatFragment.newInstance(
							recipient, false);
					((YooActivity) getActivity()).pushFragment(chatFragment);
				}
			} else if (requestCode == SELECT_GROUP) {// create new group
				updateData();
			} else if (requestCode == SELECT_BROADCAST) {
				ChatFragment chatFragment = ChatFragment
						.newInstance(null, true);
				((YooActivity) getActivity()).pushFragment(chatFragment);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		int position = info.position;
		if (menuItem.getTitle().equals(getResources().getString(R.string.chat))) {
			startChat(position);
		} else if (menuItem.getTitle().equals(getResources().getString(R.string.action_delete))) {
			// Chat chat = (Chat) getListAdapter().getItem(position);
			Chat chat = (Chat) getListView().getAdapter().getItem(position);
			if (chat != null && chat.getRecipient() != null) {
				YooRecipient recipient = chat.getRecipient();
				if (recipient instanceof YooUser) {
					if(!popupShow){
						confirmDelete(recipient, R.string.delete_chat);
					}
				} else if (recipient instanceof YooGroup) {
					if(!popupShow){
						confirmDelete(recipient, R.string.delete_group);
					}
				}
			}
		}
		return false;
	}

	public void confirmDelete(final YooRecipient recipient, int title) {
		
		AlertDialog.Builder dia = new AlertDialog.Builder(getActivity());
		dia.setTitle(title);

		LinearLayout view = new LinearLayout(getActivity());
		TextView txtMessage = new TextView(getActivity());
		txtMessage.setText(Html.fromHtml(getString(R.string.confirm_delete,
				recipient.toString())));
		int padding = ActivityUtils.dpToPixels(getActivity(), 15);
		txtMessage.setPadding(padding, padding, padding, padding);
		view.addView(txtMessage);

		dia.setView(view);
		dia.setPositiveButton(R.string.action_yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (recipient instanceof YooUser) { // remove chat
							new ChatDAO().removeChat(recipient.toJID());
							YooApplication.removeRecentChat(recipient.toJID());
						} else if (recipient instanceof YooGroup) { // remove
																	// group

							new ChatDAO().removeChat(recipient.toJID());

							YooApplication.removeRecentChat(recipient.toJID());
							ChatTools.sharedInstance().destroyGroup(
									recipient.toJID());
							new GroupDAO().remove(recipient.toJID());

						}
						popupShow = false;
						updateData();
					}
				});
		dia.setNegativeButton(R.string.action_no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						popupShow = false;
					}
				});
		dia.show();
		popupShow = true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * Callbacks interface that all activities using this fragment must
	 * implement.
	 */
	public static interface ChatListCallbacks {
		/**
		 * Called when a chat is selected.
		 */
		void onChatSelected(YooRecipient recipient);
	}

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
		refresh = true;
		checkNewRefresh();
	}

	@Override
	public void didReceiveMessage(YooMessage message) {
		refresh = true;
		checkNewRefresh();
	}

	@Override
	public void didLogin(String login, String error) {
		if(StringUtils.isEmpty(error)){
			refresh = true;
			checkNewRefresh();
		}
	}

	@Override
	public void addressBookChanged() {
		refresh = true;
		checkNewRefresh();
	}

	private void checkNewRefresh() {
		
	      new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					
					if (getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if(refresh){
									System.out.println("refreshData chat list : >>>> " + i); 
									updateData();
									i++;
									refresh = false;
								}
								
							}
						});
					}
					return null;
					
				}
	        }.execute();
	}

	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.chat_list);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onResume() {
		// ChatTools.sharedInstance().contactsLoaded();
		
		YooApplication.setLeaveApp(true); // = true;
		super.onResume();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
	}

	@Override
	public void onDestroy() {
		ChatTools.sharedInstance().removeListener(this);
		super.onDestroy();
	}
	


	@Override
	public void didReceiveRegistrationInfo(String user, String password) {
		// do nothing
	}

	@Override
	public void didReceiveUserFromPhone(Map<String, String> info) {
		// do nothing
	}

}
