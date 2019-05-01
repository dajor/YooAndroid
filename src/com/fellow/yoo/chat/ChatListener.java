package com.fellow.yoo.chat;

import java.util.List;
import java.util.Map;

import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooUser;

public interface ChatListener {
	void friendListChanged(List<YooUser> newFriends);
	void didReceiveMessage(YooMessage message);
	void didLogin(String login, String error);
	void didReceiveRegistrationInfo(String user, String password);
	void didReceiveUserFromPhone(Map<String, String> info);
	void addressBookChanged();
}
