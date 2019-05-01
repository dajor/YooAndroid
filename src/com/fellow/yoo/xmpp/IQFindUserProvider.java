package com.fellow.yoo.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import com.fellow.yoo.xmpp.IQFindUser.UserInfo;

public class IQFindUserProvider implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		
		System.err.println("################ IQFindUserProvider ################# >> " + parser.toString()); 
		
		String currentTag = null;
		IQFindUser iq = new IQFindUser();
		int event = parser.getEventType();
		while (true) {
			if (event == XmlPullParser.START_TAG) {
				currentTag = parser.getName();
				System.out.println(" currentTag >>>> " + currentTag); 
			}
			if ("user".equals(currentTag)) {
				iq.getUsers().add(new UserInfo());
			}
			if (event == XmlPullParser.TEXT) {
				if (iq.getUsers().size() > 0) {
					UserInfo info = iq.getUsers().get(iq.getUsers().size() - 1);
					if ("id".equals(currentTag)) {
						info.setContactId(Long.parseLong(parser.getText()));
					}
					if ("name".equals(currentTag)) {
						info.setName(parser.getText());
					}
					if ("country".equals(currentTag)) {
						info.setCallingCode(Integer.parseInt(parser.getText()));
					}
				}
			}
			if (event == XmlPullParser.END_TAG) {
				if (parser.getName().equals("query")) break;
			}
			event = parser.next();
		}
		return iq;
	}

}
