package com.fellow.yoo.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class IQRegisterProvider implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		System.err.println("################### IQRegisterProvider ###################### >> " + parser.toString()); 
		String currentTag = null;
		IQRegister iq = new IQRegister();
		int event = parser.getEventType();
		while (true) {
			if (event == XmlPullParser.START_TAG) {
				currentTag = parser.getName();
			}
			if (event == XmlPullParser.TEXT) {
				if ("username".equals(currentTag)) {
					iq.setUsername(parser.getText());
				}
				if ("password".equals(currentTag)) {
					iq.setPassword(parser.getText());
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
