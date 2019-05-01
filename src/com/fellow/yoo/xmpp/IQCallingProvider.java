package com.fellow.yoo.xmpp;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class IQCallingProvider implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		System.err.println("################### IQCallingProvider ###################### >> " + parser.toString()); 
		String currentTag = null;
		IQCalling iq = new IQCalling();
		int event = parser.getEventType();
		while (true) {
			if (event == XmlPullParser.START_TAG) {
				currentTag = parser.getName();
			}
			if ("user".equals(currentTag) && iq.getConferenceNumber() == null) {
				iq.setConferenceNumber(new ArrayList<String>()); 
			}
			if (event == XmlPullParser.TEXT) {
				if ("id".equals(currentTag)) {
					if(iq.getConferenceNumber() != null){
						iq.getConferenceNumber().add(parser.getText());
					}
				}
				
				if ("conf".equals(currentTag)) {
					if(iq.getConferenceNumber() != null){
						iq.getConferenceNumber().add(parser.getText());
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
