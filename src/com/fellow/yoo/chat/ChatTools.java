package com.fellow.yoo.chat;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.DeliveryReceipt;
import org.jivesoftware.smackx.packet.DeliveryReceiptRequest;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.MUCAdmin;
import org.jivesoftware.smackx.packet.MUCAdmin.Item;
import org.jivesoftware.smackx.packet.MUCInitialPresence;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.DeliveryReceiptProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.json.JSONException;
import org.json.JSONObject;

import com.fellow.yoo.YooApplication;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.ContactDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.LabelledValue;
import com.fellow.yoo.model.YooBroadcast;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooMessage.CallStatus;
import com.fellow.yoo.model.YooMessage.YooMessageType;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.service.PacketQueue;
import com.fellow.yoo.service.PacketQueueService;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.xmpp.IQCalling;
import com.fellow.yoo.xmpp.IQCallingProvider;
import com.fellow.yoo.xmpp.IQFindUser;
import com.fellow.yoo.xmpp.IQFindUser.UserInfo;
import com.fellow.yoo.xmpp.IQFindUserProvider;
import com.fellow.yoo.xmpp.IQRegister;
import com.fellow.yoo.xmpp.IQRegisterProvider;
import com.fellow.yoo.xmpp.VCardTempXUpdateExtension;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.Log;


@SuppressLint({ "TrulyRandom", "UseSparseArrays" })
public class ChatTools implements ConnectionListener, ConnectionCreationListener, PacketListener {
	

	
	private static ChatTools instance;
	
	public final static String YOO_IP = "81.169.238.200";
	public final static int YOO_PORT = 5222;
	public final static String YOO_DOMAIN = "yoo-app.com";
	
	public final static String REGISTRATION_USER = "registration";
	public final static String CONFERENCE_DOMAIN = "conference.yoo-app.com";
	public final static int MAX_USER_HISTORY = 20;
	public final static int CALL_MAX_DELAY = 30; // old 180 - 3 minutes
	
	private XMPPConnection connection;
	private String login, password;
	private String regLogin;
	private List<ChatListener> listeners;
	private List<String> present;
	private List<String> visibility;
	private List<Long> tested;
	private List<String> callMemers;
	
	private boolean contactsReady;
	private Map<String,  List<String>> roomUsers;
	private Map<String, String> roomAliases;

	private String countryCode;
	private boolean countryReady;
	
	private YooRecipient callRecipient;
	private CallingListener callingDelegate;
	private Map<String, String> mappingConferenceNumbers;
	private ChatTools() {
	    listeners = new ArrayList<ChatListener>();
	    present = new ArrayList<String>();
	    visibility = new ArrayList<String>();
	    tested = new ArrayList<Long>();
	    roomUsers = new HashMap<String,  List<String>>();
	    roomAliases = new HashMap<String, String>();
	    mappingConferenceNumbers = new HashMap<String, String>();
	    countryCode = null;
	    countryReady = false;
	    contactsReady = false;
	    
	    // TODO
	    // ContactManager.sharedInstance().addListener(this);
	    mappingConferenceNumbers.put("", "");
	    mappingConferenceNumbers.get("");
	}

	public static ChatTools sharedInstance() {
		if (instance == null) {
			instance = new ChatTools();
		}
		return instance;
	}

	
	private void login(String pLogin, String pPassword) {
		
	    login = pLogin;
	    password = pPassword;
	    
	    Log.w("ChatTools : ", " >> Login : " + login + " , >> Password : " + password);	    
	    ConnectionConfiguration config = null;      // initConnectionConfiguration();
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	    	config = initConfigKeystore();
	    	config.setSASLAuthenticationEnabled(true);
	    	config.setSecurityMode(SecurityMode.enabled);
	    	config.setTruststoreType("BKS");  
	    	
	    }if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	    	config = new ConnectionConfiguration(YOO_IP, YOO_PORT, YOO_DOMAIN);
			
			// fixed the problem  KeyStore jks implementation not found
			config.setSASLAuthenticationEnabled(true); 
			// config.setCompressionEnabled(true);
			config.setSecurityMode(SecurityMode.enabled);
		    config.setTruststoreType("AndroidCAStore");
		    config.setTruststorePassword(null);
		    config.setTruststorePath(null);
	    }else{
	    	 config.setTruststoreType("BKS");
             String path = System.getProperty("javax.net.ssl.trustStore");
             if (path == null){
             	path = System.getProperty("java.home") + File.separator + "etc"
                         + File.separator + "security" + File.separator + "cacerts.bks";
             }
             config.setTruststorePath(path);
	    }
	    
	    connection = new XMPPConnection(config);
	    config.setDebuggerEnabled(true);
	    SASLAuthentication.supportSASLMechanism("PLAIN");
	
	    /*
		    config = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
			config.setSecurityMode(SecurityMode.required);
			config.setReconnectionAllowed(true);
			config.setRosterLoadedAtLogin(false);
			config.setSendPresence(false);
			config.setSocketFactory(SSLSocketFactory.getDefault());
	    */
	    
	    ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp", new VCardProvider());
	    ProviderManager.getInstance().addIQProvider("query", "yoo:iq:register", new IQRegisterProvider());
	    ProviderManager.getInstance().addIQProvider("query", "yoo:iq:finduser", new IQFindUserProvider());
	    ProviderManager.getInstance().addIQProvider("query", "yoo:iq:call", new IQCallingProvider());
	    ProviderManager.getInstance().addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
	    ProviderManager.getInstance().addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
	    ProviderManager.getInstance().addExtensionProvider("received", "urn:xmpp:receipts", new DeliveryReceiptProvider());
	    
	    
	    /*	
	        ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp", new VCardProvider());
		    ProviderManager.getInstance().addIQProvider("query", "yoo:iq:register", new IQRegisterProvider());
			ProviderManager.getInstance().addIQProvider("query", "yoo:iq:finduser", new IQFindUserProvider());
			ProviderManager.getInstance().addExtensionProvider("received", "urn:xmpp:receipts", new DeliveryReceiptProvider());
		*/
	    
	    // IQServiceProviders.Register_Providers(ProviderManager.getInstance());
		// connection.addConnectionListener(this);
	    
		connection.addPacketListener(this, null);
		XMPPConnection.addConnectionCreationListener(this);
		try {
			connection.connect();
			
			// login using yoo user
			String deviceId = Secure.getString(YooApplication.getAppContext().getContentResolver(), Secure.ANDROID_ID); 
			
			if(!com.fellow.yoo.utils.StringUtils.isEmpty(login) &&
				!com.fellow.yoo.utils.StringUtils.isEmpty(password) && 
				!com.fellow.yoo.utils.StringUtils.isEmpty(deviceId)){
				connection.login(login, password, deviceId);
			}
			
			
		} catch (XMPPException e) {
			Log.w("ChatTools", "Error during login", e);
		} catch (Exception e) {
			Log.w("ChatTools", "Error during login", e);
		}
		
		if (connection != null && !connection.isAuthenticated()) {
			for (ChatListener listener : listeners) {
				listener.didLogin(login, "NOT_AUTHENTICATED");
			}
			return;
		}
		
		// After establishing a session, a client SHOULD send initial presence to the server 
	 	// : order to signal its availability for communications.
		sendPresence();

    
		for (ChatListener listener : listeners) {
			listener.didLogin(login, "");
		}
		
		if (login != null && !login.equals(REGISTRATION_USER)) {
			IQ iq = new IQ() {
				@Override
				public String getChildElementXML() {
					return "<query xmlns=\"jabber:iq:roster\"/>";
				}
			};
			iq.setType(Type.GET);
			doSend(iq);
	        
	        // TODO check presence of phone contacts : we need the country code
	        // [[LocationTools sharedInstance] getCountryCode:self];
	        // set device token
			setDevice(YooApplication.getGcmId());
	        // [self setDevice:((AppDelegate *)[UIApplication sharedApplication].delegate).deviceToken];
	        
	        // send presence to all groups
	        for (YooGroup group : new GroupDAO().list()) {
	            // send presence to the group
	        	Presence presence = new Presence(Presence.Type.available);
	        	presence.setTo(group.getName() + "@" + CONFERENCE_DOMAIN + "/" + login);
	        	doSend(presence);
	        }
	        
	        // if our profile has no picture, ask for the VCard to get one
	        YooUser me = new UserDAO().find(login, YOO_DOMAIN);
	        if (me != null && me.getPicture() == null) {
	        	requestVCard(login + "@" + YOO_DOMAIN);
	        }
	    }
	}
	
	@SuppressLint("TrulyRandom")
	private ConnectionConfiguration initConfigKeystore() {
		ConnectionConfiguration config = new ConnectionConfiguration(YOO_IP, YOO_PORT, YOO_DOMAIN);
		
		try { // trust KeyStore certificate 
			if (YooApplication.getAppContext() != null) {
				Context context = YooApplication.getAppContext();
				Resources resources = context.getApplicationContext().getResources();
				String packageName = context.getApplicationContext().getPackageName();
				int id = resources.getIdentifier("yookeybks", "drawable", packageName);
				if (id > 0) {
					InputStream ins = resources.openRawResource(id);

					KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
					keyStore.load(ins, "fellow$29".toCharArray());

					Log.w("ChatTools : >> ", "####### kestore type #############" + keyStore.getType());
					Log.w("ChatTools : >> ", "####### kestore Provider #############" + keyStore.getProvider());
					

					TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init(keyStore);

					SSLContext sslctx = SSLContext.getInstance("TLS");
					sslctx.init(null, tmf.getTrustManagers(), new SecureRandom());
					
					
					// javax.net.ssl.SSLContext sslctx = javax.net.ssl.SSLContext.getInstance("SSL");
					/*
					javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
			        javax.net.ssl.TrustManager tm = new miTM();
			        trustAllCerts[0] = tm;
			        sslctx.init(null, trustAllCerts, new SecureRandom());
			        					
					SSLSocket socket = (SSLSocket) sslctx.getSocketFactory().createSocket();
				    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
				    */
					
			
					config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
					config.setDebuggerEnabled(true);
					config.setCustomSSLContext(sslctx);
					
					Log.w("ChatTools : >> ", "####### initConfigKeystore end and return #############" + config.toString());

					return config;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return config;

	}
	
	/*public Socket createSocket(String s, int i) throws IOException {

	    SSLSocket socket = (SSLSocket) factory.createSocket(s, i);

	    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

	    return socket;

	}*/
	
	/*
	 <iq type='set' id='juliet1'>
		  <query xmlns='urn:xmpp:mam:0'>
		    <x xmlns='jabber:x:data'>
		      <field var='FORM_TYPE'>
		        <value>urn:xmpp:mam:0</value>
		      </field>
		      <field var='start'>
		        <value>2010-08-07T00:00:00Z</value>
		      </field>
		    </x>
		  </query>
		</iq>
	 */

	/**
	 * 
	 *
	 * registerUser, processPacket
	 */
	public void registerUser(final String phone, final String code) {
	    // <query xmlns=\"yoo:iq:register\"><name>John Smith</name><phone>0085592652053</phone></query>
		
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"yoo:iq:register\"><phone>" + phone + "</phone><code>" + code + "</code></query>";
			}
		};
		iq.setType(Type.SET);
		doSend(iq);
	}

	public void registerUser(final String name, final String phone, final int countryCode) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"yoo:iq:register\"><name>" + name + "</name><phone>" 
						+ phone + "</phone><country>" + countryCode + "</country></query>";
			}
		};
		iq.setType(Type.SET);
		doSend(iq);
	}


	@Override
	public void processPacket(Packet packet) {
		Log.w("ChatTools >> ", packet.getClass().getName());
		Log.w("ChatTools >> ", packet.toXML());
		if (packet instanceof Message) {
			didReceiveMessage((Message)packet);
		}
		if (packet instanceof IQ) {
			didReceiveIQ((IQ)packet);
		}
		if (packet instanceof Presence) {
			didReceivePresence((Presence)packet);
		}
	}
	

	/**
	 * 
	 * 
	 * checkFriend, checkAddressBook, setNicknameAndPicture
	 */
	private YooUser checkFriend(String name, String domain) {
	    YooUser yooUser = new UserDAO().find(name, domain);
	    if (yooUser == null) {
	        Log.i("ChatTools", "Adding new friend : " + name);
	        yooUser = new YooUser(name, domain);
	        new UserDAO().upsert(yooUser);
	        for (ChatListener listener : listeners) {
	        	listener.friendListChanged(Arrays.asList(yooUser));
	        }
	    }
	    
	    if (!isPresent(yooUser)) {
	        // register for presence for the new friend
	    	Presence subscribe = new Presence(Presence.Type.subscribe);
	    	subscribe.setTo(yooUser.toJID());
	    	doSend(subscribe);
	    }
	    
	    
	    if (yooUser.getPicture() == null || yooUser.getAlias() == null) {
	    	requestVCard(yooUser.toJID());
	    }
	    
	    checkAddressBook(yooUser);
	    return yooUser;
	}
	
	public void requestCallStatusFromRecipient(final String  status) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"yoo:iq:callstatus\"><status>" + status + "</status></query>";
			}
		};
		iq.setType(Type.GET);
		doSend(iq);
	}
	
	
	// when member decline, remove from call list.
	public boolean declineCallMembers(String memberJid){
		if(callMemers != null){
			for (String Jid : callMemers) {
				 if(memberJid.equals(Jid)){
					 callMemers.remove(Jid);
					 break;
				 }
			}
		}
		return callMemers.size() == 1; // when only caller left. stop call.
	}
	
	public void makeCall(YooRecipient recipient){
		callRecipient = recipient;
		List<String> memebers = new ArrayList<String>();
		if(recipient instanceof YooGroup){
			callMemers = new ArrayList<String>();
			YooGroup group = (YooGroup) recipient; 
			for (String memberJid : new GroupDAO().listMembers(group.toJID())) {  
				YooUser memberUser = new UserDAO().findByJid(memberJid);
				if(memberUser != null){
					memebers.add(memberJid);
					callMemers.add(memberJid);
				}
			}
		}else{
			memebers.add(recipient.toJID());
		}
		
		if(!memebers.isEmpty()){
			final StringBuffer users = new StringBuffer();
			for (String member : memebers) {
				users.append("<user>" + member + "</user>");
			}
			
			IQ iq = new IQ() {
				@Override
				public String getChildElementXML() {
					return "<query xmlns=\"yoo:iq:call\">" + users.toString() + "</query>";
				}
			};
			iq.setType(Type.GET);
			doSend(iq);
		}
	}
	
	public void requestCall(YooUser[] pMembers) {
		/*XMPPIQ *iq = [[XMPPIQ alloc] initWithType:@"get"];
		DDXMLNode *attrNS = [DDXMLNode attributeWithName:@"xmlns" stringValue:@"yoo:iq:call"];
		DDXMLNode *query = [DDXMLNode elementWithName:@"query" children:nil attributes:@[attrNS]];
		self.callingDelegate = listener;
		String names = @"";*/
		
		String names = "";
	    for(YooUser user : pMembers){
	    	if(names.length() > 0){
			   // names = [names stringByAppendingString:@", "];
	    		user.getAlias();
			}
			// names = [self toJSonStringWithKeysAndValues:@"Name", user.toJID, /*@"Country", [NSString stringWithFormat:@"%d", user.callingCode],*/ nil];
	    }
	}

	//    public void requestCallStatusFromRecipient(String  status) {
	//	    XMPPIQ *iq = [[XMPPIQ alloc] initWithType:@"get"];
	//	    DDXMLNode *attrNS = [DDXMLNode attributeWithName:@"xmlns" stringValue:@"yoo:iq:callstatus"];
	//	    DDXMLNode *query = [DDXMLNode elementWithName:@"query" children:nil attributes:@[attrNS]];
	//	    query.stringValue = status;
	//	    [iq addChild:query];
	//	    [connection sendElement:iq];
	//	}
	//
	//    /*
	//     * Request Call: send iq request to server to get conference number
	//     * @param: Members : conference room.
	//     * @param: Listener listen when success and do other functionality
	//    */
	//    public void requestCall:(NSArray *)pMembers listener:(NSObject<CallingListener> *)listener {
	//	    XMPPIQ *iq = [[XMPPIQ alloc] initWithType:@"get"];
	//	    DDXMLNode *attrNS = [DDXMLNode attributeWithName:@"xmlns" stringValue:@"yoo:iq:call"];
	//	    DDXMLNode *query = [DDXMLNode elementWithName:@"query" children:nil attributes:@[attrNS]];
	//	    self.callingDelegate = listener;
	//	    String names = @"";
	//	    for(YooUser user : pMembers){
	//	        if([names length] > 0){
	//	            names = [names stringByAppendingString:@", "];
	//	        }
	//	        names = [self toJSonStringWithKeysAndValues:@"Name", user.toJID, /*@"Country", [NSString stringWithFormat:@"%d", user.callingCode],*/ nil];
	//	    }
	//	    NSUserDefaults *userdefault = [NSUserDefaults standardUserDefaults];
	//	    String pCountryCode = [NSString stringWithFormat:@"%ld", [userdefault integerForKey:@"callingCode"]];
	//	    String pName = [userdefault stringForKey:@"nickname"];
	//	    String jsonData = [self toJSonStringWithKeyAndValueObject:@"Invitees" valueObject:[NSString stringWithFormat:@"[%@]", names] keysAndValues:@"Name", pName, /*@"Country", pCountryCode, */@"Type", @"Call", nil];
	//	    query.stringValue = jsonData;
	//	    [iq addChild:query];
	//	    [connection sendElement:iq];
	//	}
	//
	


    private void checkAddressBook(YooUser yooUser) {
	    if (!yooUser.isMe() && YOO_DOMAIN.equals(yooUser.getDomain()) && yooUser.getName() != null && yooUser.getAlias() != null) {
	        Contact contact = null;
	        if (yooUser.getContactId() != -1) {
	        	contact = new ContactManager().getDetail(yooUser.getContactId());
	        }
	        if (contact == null) {
	            contact = new ContactManager().findByName(yooUser.getAlias());
	            if (contact == null) {
	                // we have to create the new contact
	                contact = new Contact();
	                String [] parts = yooUser.getAlias().split(" ");
	                if (parts.length > 1) {
	                	String firstName = "";
	                	for (int i = 0; i < parts.length - 1; i++) {
	                		if (i > 0) firstName += " ";
	                		firstName += capitalize(parts[i]);
	                	}
	                	contact.setFirstName(firstName);
	                	contact.setLastName(capitalize(parts[parts.length - 1]));
	                } else {
	                    contact.setFirstName(capitalize(yooUser.getAlias()));
	                }
	                long contactId = new ContactManager().createFromYoo(contact);
	                if (contactId != -1) {
	                	contact.setContactId(contactId);
	                	new ContactDAO().upsert(contact);
	                	yooUser.setContactId(contactId);
	                	new UserDAO().upsert(yooUser);
	                	for (ChatListener listener : listeners) {
	                		listener.addressBookChanged();
	                	}
	                }
	            } else {
	                yooUser.setContactId(contact.getContactId());
	                new UserDAO().upsert(yooUser);
	                for (ChatListener listener : listeners) {
	                	listener.friendListChanged(Arrays.asList(yooUser));
	                }
	            }
	        }
	    }
	}
    
   
    public void setNicknameAndPicture(final String nickname, final byte [] picture) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<vCard xmlns=\"vcard-temp\">"
						+ "<FN>" + nickname + "</FN>"
						+ "<PHOTO><TYPE>image/png</TYPE><BINVAL>" + StringUtils.encodeBase64(picture) + "</BINVAL></PHOTO>"
						+ "</vCard>";
			}
		};
		
		iq.setType(Type.SET);
		doSend(iq);
	    
	    // send presence indicating the picture has changed
	    if (picture != null) {
    		VCardTempXUpdateExtension ext = new VCardTempXUpdateExtension();
    		ext.setPhotoData(picture);
			Presence presence = new Presence(Presence.Type.available);
			presence.addExtension(ext);
			doSend(presence);
	    }
	
	}

	
	/**
	 * 
	 * send message..
	 * 
	 * sendPresence, canSend, doSend, sendMessage
	 */
	private void sendPresence() {
		Presence presence = new Presence(Presence.Type.available);
		doSend(presence);
	}
	
	public boolean canSend(Packet packet) {
		if (connection != null && connection.isConnected() && connection.isAuthenticated()) {
			connection.sendPacket(packet);
			return true;
		} else {
			return false;
		}
	}
	
	private void doSend(final Packet packet) {
    	PacketQueue.addPacket(packet);
    	Intent intent = new Intent(YooApplication.getAppContext(), PacketQueueService.class);
    	YooApplication.getAppContext().startService(intent);
    	Log.w("ChatTools : doSend >> ", packet.toXML());
    }
	
	
    public void sendMessage(YooMessage yooMsg) {

    	yooMsg.setRead(true);
	    yooMsg.setFrom(new YooUser(login, YOO_DOMAIN));
	    if(com.fellow.yoo.utils.StringUtils.isEmpty(yooMsg.getIdent())){
	    	yooMsg.setIdent(genRandStringLength(16));
	    }
	    
	    
	    Message message = new Message();
	    try {
	    	 if(!com.fellow.yoo.utils.StringUtils.isEmpty(yooMsg.getMessage())){ 
	    		 message.setBody(URLEncoder.encode(yooMsg.getMessage(), "UTF8").replace("+", "%20")); 
	 	    }else{
	 	    	message.setBody(URLEncoder.encode(" ", "UTF8")); 
	 	    }
		} catch (UnsupportedEncodingException e) {
			Log.w("ChatTools", e.getMessage(), e);
		}
	    
	    message.setPacketID(yooMsg.getIdent());
	    if (yooMsg.getTo() instanceof YooGroup) {
	    	message.setType(Message.Type.groupchat);
	    } else {
	    	message.setType(Message.Type.chat);
	    }
	   
	    message.setThread(yooMsg.getThread());
	    message.setTo(yooMsg.getTo().toJID());
	    message.setProperty("type", String.valueOf(yooMsg.getTypeIndex())); 
	    
	    
	    if (yooMsg.getType() == YooMessageType.ymtPicture) {
	        int i = 1;
	        for (byte [] picData : yooMsg.getPictures()) {
	        	// message.setProperty("picture" + i, Base64.encode(picData));
	        	message.setProperty("picture" + i, StringUtils.encodeBase64(picData));
	            i++;
	        }
	    }else if (yooMsg.getType() == YooMessageType.ymtLocation) {
	    	message.setProperty("location", yooMsg.getLocation()[0] + "/" + yooMsg.getLocation()[1]);
	    }else if (yooMsg.getType() == YooMessageType.ymtSound) {
	    	message.setProperty("sound", StringUtils.encodeBase64(yooMsg.getSound()));
	    }else if (yooMsg.getType() == YooMessageType.ymtContact) {
	    	/*Contact contact = new ContactDAO().find(yooMsg.getShared());
            if (contact == null) {
                contact = new ContactManager().findById(yooMsg.getShared());
            }*/
            Contact contact = new ContactManager().getDetail(yooMsg.getShared());
            if(contact != null){
            	message.setProperty("contact-firstName", contact.getFirstName());
            	if(!com.fellow.yoo.utils.StringUtils.isEmpty(contact.getLastName())){
            		message.setProperty("contact-lastName", contact.getLastName());
            	}
            	
            	for (int i = 0; i < contact.getEmails().size(); i++) {
            		LabelledValue eValue = contact.getEmails().get(i);
            		message.setProperty("contact-email-label-" + i, eValue.getType());
                	message.setProperty("contact-email-value-" + i, eValue.getValue());
            	}
            	for (int i = 0; i < contact.getPhones().size(); i++) {
            		LabelledValue pValue = contact.getPhones().get(i);
            		message.setProperty("contact-phone-label-" + i, pValue.getType());
                	message.setProperty("contact-phone-value-" + i, pValue.getValue());
				}
            	for (int i = 0; i < contact.getMessaging().size(); i++) {
            		LabelledValue eValue = contact.getEmails().get(i);
            		message.setProperty("contact-messaging-label-" + i, eValue.getType());
                	message.setProperty("contact-messaging-value-" + i, eValue.getValue());
            	}
            	// share user Jid
            	YooUser user = new UserDAO().findByContactId(contact.getContactId());
            	if(user != null){
            		message.setProperty("jId", user.toJID());
            	}
            }
	    }if (yooMsg.getType() == YooMessageType.ymtInvite || yooMsg.getType() == YooMessageType.ymtRevoke) {
	    	String alias = yooMsg.getGroup().getAlias();
	    	message.setProperty("group-alias", !com.fellow.yoo.utils.StringUtils.isEmpty(alias)? alias : "");
	    	message.setProperty("group-name", yooMsg.getGroup().getName());
	    	
	    }else if(yooMsg.getType() == YooMessageType.ymtCallRequest){
	    	//	if(yooMsg.getType() == ymtCallRequest){
			//	       [body setStringValue:[NSString stringWithFormat:@"Call %@", yooM]];
			//	        yooMsg.conferenceNumber = [self.mappingConferenceNumbers objectForKey:self.login];
			//	        String receiverConferenceNumber = [self.mappingConferenceNumbers objectForKey:yooMsg.to.toJID];
			//	        [self addProperty:@"conferenceNumber" value:receiverConferenceNumber element:propsElt];
			//	    }
			    
	    	message.setProperty("conferenceNumber", yooMsg.getConferenceNumber());
	    }else if(yooMsg.getType() == YooMessageType.ymtCallStatus){
	    	message.setProperty("callReqId", yooMsg.getCallReqId());
	    	message.setProperty("callStatus", yooMsg.getCallStatusIndex());
	    }
	    
	    if(yooMsg.getDateCanNull() == null){
	    	message.setProperty("sendDate", DateUtils.formatTime(new Date())); 
	    }else{
	    	message.setProperty("sendDate", DateUtils.formatTime(yooMsg.getDate())); 
	    }
	    if (yooMsg.getType() != YooMessageType.ymtInvite 
	    		&& yooMsg.getType() != YooMessageType.ymtRevoke 
	    		&& yooMsg.getType() != YooMessageType.ymtMessageRead 
	    		&& !yooMsg.getTo().toJID().endsWith(CONFERENCE_DOMAIN)) {
	        // ask for a receipt
	    	message.addExtension(new DeliveryReceiptRequest());
	    }
	
	    if ( yooMsg.getType() != YooMessageType.ymtInvite && 
	    	 yooMsg.getType() != YooMessageType.ymtRevoke && 
	    	 yooMsg.getType() != YooMessageType.ymtCallStatus) {
	    	
	        addInHistory(yooMsg, true);
	    }
	
	    /*
	      // Check network status
		    Reachability *networkReachability = [Reachability reachabilityForInternetConnection];
		    NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
		    if (networkStatus == NotReachable) {
		        // clear the present table
		        [self.present removeAllObjects];
		        [self.invisible removeAllObjects];
		        self.xmppStream = nil;
		    }
	     */
	    
	    // Check network status
	    // ActivityUtils.checkNetworkConnected(activity, con)
	    
	    // check xmpp status
	    if (connection == null || !connection.isConnected() || !connection.isAuthenticated()) {
	    	for (ChatListener listener : listeners) {
	    		listener.didLogin(login, "DISCONNECT");
	    	}
			asyncLogin(login, password);
	        return;
	    }
	   
	    if (!(yooMsg.getTo() instanceof YooBroadcast)) {
	    	doSend(message);
	    	new ChatDAO().markAsSent(yooMsg.getIdent());
	    }
	} // end send message.

    public void reLogin(Context context){
    	// check xmpp status
	    if (connection == null || !connection.isConnected() || !connection.isAuthenticated()) {
	    	for (ChatListener listener : listeners) {
	    		listener.didLogin(login, "DISCONNECT");
	    	}
	    	
	    	if(context != null){
	    		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		    	String rlogin = preferences.getString("login", null);
		    	String rpassword = preferences.getString("password", null);
		    	
				if (!com.fellow.yoo.utils.StringUtils.isEmpty(rlogin) && 
						!com.fellow.yoo.utils.StringUtils.isEmpty(rpassword)) {
					asyncLogin(rlogin, rpassword);
				}
	    	}
	    }
    }
    
   
	//      public void addProperty(String name, String value, element:(NSXMLElement *)element {
	//	    NSXMLElement *propElt = [[NSXMLElement alloc] initWithName:@"property"];
	//	    [element addChild:propElt];
	//	    NSXMLElement *propNameElt = [[NSXMLElement alloc] initWithName:@"name"];
	//	    [propElt addChild:propNameElt];
	//	    [propNameElt setStringValue:name];
	//	    NSXMLElement *propValueElt = [[NSXMLElement alloc] initWithName:@"value"];
	//	    [propValueElt addAttribute:[DDXMLNode attributeWithName:@"type" stringValue:@"string"]];
	//	    [propElt addChild:propValueElt];
	//	    [propValueElt setStringValue:value];
	//	}


    /**
     * 
     * getUsersFromContacts, markAsRead, 
     */

    @SuppressLint("UseSparseArrays")
	private void getUsersFromContacts() {
	    if (!countryReady || !contactsReady) return;
    	boolean found = false;
    	List<Contact> contacts = new ContactDAO().list();
    	final Map<Long, List<String>> users = new HashMap<Long, List<String>>();
    	for (Contact contact : contacts) {
    		if (contact.hasPhone()) {
    			if (tested.contains(contact.getContactId())){
    				continue;
    			}
    			tested.add(contact.getContactId());
    			try {
    				// the contact : DB does not stores the phone, need to call the AddressBook API
        			Contact fullContact = new ContactManager().getContactPhone(contact); // new ContactManager().getDetail(contact.getContactId());    			
        			List<String> numbers = new ArrayList<String>();
        			for (LabelledValue labVal : fullContact.getPhones()) {
        				numbers.add(labVal.getValue());
        			}
        			users.put(contact.getContactId(), numbers);
        			found = true; 
				} catch (Exception e) {
					Log.w("ChatTools getUsersFromContacts >> ", e.toString());
				}
    		}
    	}
    	
    	if (found) {
			IQ iq = new IQ() {				
				@Override
				public String getChildElementXML() {
					StringBuilder sb = new StringBuilder();
					sb.append("<query xmlns=\"yoo:iq:finduser\">");
					for (long contactId : users.keySet()) {
						sb.append("<user>");
						sb.append("<id>" + contactId + "</id>");
						sb.append("<phones>");
						for (String phone : users.get(contactId)) {
							phone = formatePhoneNumber(phone);
							if(!com.fellow.yoo.utils.StringUtils.isEmpty(phone)){
								sb.append("<phone>" + phone + "</phone>");
							}
						}
						sb.append("</phones>");
						sb.append("</user>");
					}
					sb.append("</query>");
					return sb.toString();
				}
			};
			iq.setType(Type.GET);
			doSend(iq);
    	}
	}
    
    public void findUserByPhone(Contact contact) {
    	
    	final Map<Long, List<String>> users = new HashMap<Long, List<String>>();
    	if (contact.hasPhone()) {
			try { 			
    			List<String> numbers = new ArrayList<String>();
    			for (LabelledValue labVal : contact.getPhones()) {
    				numbers.add(labVal.getValue());
    			}
    			users.put(contact.getContactId(), numbers);
    			didFindUserByPhone(users);
			} catch (Exception e) {
				Log.w("ChatTools findUserByPhone >> ", e.toString());
			}
		}
	}
    
    private void didFindUserByPhone(final Map<Long, List<String>> users){
    	IQ iq = new IQ() {				
			@Override
			public String getChildElementXML() {
				StringBuilder sb = new StringBuilder();
				sb.append("<query xmlns=\"yoo:iq:finduser\">");
				for (long contactId : users.keySet()) {
					sb.append("<user>");
					sb.append("<id>" + contactId + "</id>");
					sb.append("<phones>");
					for (String phone : users.get(contactId)) {
						phone = formatePhoneNumber(phone);
						if(!com.fellow.yoo.utils.StringUtils.isEmpty(phone)){
							sb.append("<phone>" + phone + "</phone>");
						}
					}
					sb.append("</phones>");
					sb.append("</user>");
				}
				sb.append("</query>");
				return sb.toString();
			}
		};
		iq.setType(Type.GET);
		doSend(iq);
	}
    
    
    private String formatePhoneNumber(String phone){
    	String fPhone = "";
    	if(phone != null && phone.length() > 5){
    		Log.i("ChatTools : formatePhoneNumber >> ", "phone >> " + phone); 
    		phone = phone.replace("(", "").replace(")", "").replace("-", "").replace(" ", "").replace("+", "");  
    		if("0".equals(phone.subSequence(0, 1))){
    			fPhone = YooApplication.getCallingCode() + phone.substring(1);
    		}
    		Log.i("ChatTools : formatePhoneNumber >> ", "formatePhoneNumber >> " + phone); 
    	}
    	return fPhone;
    }

    public void markAsRead(YooRecipient recipient) {
	    List<YooMessage> unread = new ChatDAO().unreadList(recipient);
	    for (YooMessage yooMsg : unread) {
	        if (yooMsg.isReceipt()) {
	        	Message receipt = new Message();
	        	//send message ymtMessageRead
	        	receipt.setProperty("type", yooMsg.getTypeIndex(YooMessageType.ymtMessageRead)); 
	        	receipt.setProperty("sendDate", DateUtils.formatTime(new Date())); 
	        	receipt.setTo(yooMsg.getFrom().toJID());	
	        	receipt.setFrom(yooMsg.getTo().toJID()); 
	        	receipt.setPacketID(yooMsg.getIdent()); 
	        	 if(!com.fellow.yoo.utils.StringUtils.isEmpty(yooMsg.getMessage())){ 
	     	    	try {
	     	    		receipt.setBody(URLEncoder.encode(yooMsg.getMessage(), "UTF8").replace("+", "%20"));  
	     			} catch (UnsupportedEncodingException e) {
	     				Log.w("ChatTools", e.getMessage(), e);
	     			}
	     	    }
	        	doSend(receipt);
	        }
	    }
	    if(!unread.isEmpty()){
	    	new ChatDAO().markAsRead(recipient.toJID());
	    }
	    
	}
	
		//  <iq from='crone1@shakespeare.lit/desktop'
	    //    id='create1'
	    //    to='coven@chat.shakespeare.lit'
	    //    type='set'>
	    //    <query xmlns='http://jabber.org/protocol/muc#owner'>
	    //    <x xmlns='jabber:x:data' type='submit'>
	    //    <field var='muc#roomconfig_roomname'>
	    //    <value>A Dark Cave</value>
	    //    </field>
	    //    <field var='muc#roomconfig_persistentroom'>
	    //    <value>0</value>
	    //    </field>
	    //    </x>
	    //    </query>
	    //    </iq>
    
    
    /**
     * 
     * createGroup, editGroup, destroyGroup
     */
	public void createGroup(String groupName, List<String> users) {
		
		Log.w("ChatTools : createGroup >> ", " #######  >> " + groupName + "" + users.toArray());

	    String groupCode = login + "-" + getCodeFromString(groupName);

	    roomUsers.put(groupCode, users);
	    roomAliases.put(groupCode, groupName);

	    checkGroup(groupCode);
	    
	    Presence presence = new Presence(Presence.Type.available);
	    presence.setTo(groupCode + "@" + CONFERENCE_DOMAIN + "/" + login);
	    presence.addExtension(new MUCInitialPresence());
	    doSend(presence);
	    
			//	    XMPPPresence *presence = [[XMPPPresence alloc] initWithType:nil to:[XMPPJID jidWithUser:groupCode domain:@"conference.yoo-app.com" resource:self.login]];
			//	    DDXMLNode *xElt = [DDXMLNode elementWithName:@"x" URI:@"http://jabber.org/protocol/muc"];
			//	    [presence addChild:xElt];
			//	    [connection sendElement:presence];
	}
	
    
    public void editGroup(YooGroup yooGroup, List<String> selected) {
    	Log.w("ChatTools : editGroup >> ", " #######  >> " + yooGroup.getAlias());
    	YooUser me = YooApplication.getUserLogin();
    	List<String> members = new GroupDAO().listMembers(yooGroup.toJID());
    	// remove old members
    	for (String userId : members) { 
    		if(!selected.contains(userId) && !me.toJID().equals(userId)){ 
    			YooUser user = new UserDAO().findByJid(userId);
				if(user != null){
					removeUserFromGroup(userId, yooGroup.toJID());
				}
    		}
		}
    	
    	// add new members
    	for (String userId : selected) { 
    		if(!members.contains(userId) && !me.toJID().equals(userId)){
    			YooUser user = new UserDAO().findByJid(userId);
				if(user != null){
					addUserToGroup(user.toJID(), yooGroup.toJID()); 
				}
    		}
    	}
	}
    
    private YooGroup checkGroup(String code) {
    	Log.w("ChatTools : checkGroup >> ", " ### " + code);
    	
	    YooGroup yooGroup = new GroupDAO().find(code);
	    
        String alias = roomAliases.get(code);
        if (alias == null) {
            alias = code.substring(code.indexOf("-") + 1);
        }
        
	    if (yooGroup == null) {
	    	 yooGroup = new YooGroup(code, alias);
		     yooGroup.setDate(new Date());
	    }
	    
	    new GroupDAO().upsert(yooGroup);
	    
	    // send presence to the group
        Presence presence = new Presence(Presence.Type.available);
        presence.setTo(code + "@" + CONFERENCE_DOMAIN + "/" + login);
        doSend(presence);
        
      
        // query member list
        List<String> members = Arrays.asList("admin", "owner");
        for (final String affiliation : members) {
			IQ iq = new IQ() {
				@Override
				public String getChildElementXML() {
					String cXml =  "<query xmlns=\"http://jabber.org/protocol/muc#admin\"><item affiliation=\"" + affiliation + "\"/></query>";
					return cXml;
				}
			};
			
			iq.setType(Type.GET);
			iq.setTo(yooGroup.toJID());
			iq.setPacketID("groupmember"); 
			doSend(iq);
        }
        
       

        // update UI
        for (ChatListener listener : listeners) {
        	listener.friendListChanged(new ArrayList<YooUser>());
        }
	    
	    return yooGroup;
	}
    
    private void addUserToGroup(final String userJid, String groupJid) {
	    // add the user as group member
		Log.w("ChatTools : addUserToGroup >> ", " ####### >> " + groupJid + ", userId : " + userJid);
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"http://jabber.org/protocol/muc#admin\"><item jid=\"" + userJid + "\" nick=\"" + userJid + "\" affiliation=\"admin\"/></query>";
			}
		};
		
		iq.setType(Type.SET);
		iq.setFrom(login + "@" + YOO_DOMAIN);
		iq.setPacketID("invite1");
		iq.setTo(groupJid);
		doSend(iq);
	    
	    // invite the user to join
	    YooUser yooUser = new YooUser(userJid);
	    YooMessage yooMsg = new YooMessage();
	    yooMsg.setType(YooMessageType.ymtInvite);
	    yooMsg.setTo(yooUser);
	    String groupName = StringUtils.parseName(groupJid);
	    String groupAlias = roomAliases.get(groupName);
	    if (groupAlias == null) {
	    	YooGroup tmp = new GroupDAO().find(groupName);
	    	if (tmp != null) {
	    		groupAlias = tmp.getAlias();
	    	}
	    }
	    yooMsg.setGroup(new YooGroup(groupName, groupAlias));
	    sendMessage(yooMsg);
	    
	    new GroupDAO().addMemberToGroup(yooUser.toJID(), yooMsg.getGroup().toJID());
	
	}
    
    
	private void confirmGroup(String groupJid) {
		
		Log.w("ChatTools : confirmGroup >> ", " ####### >> " + groupJid);
	    
	    String groupCode = StringUtils.parseName(groupJid);
	    final String alias = roomAliases.get(groupCode);
	    if (alias == null) return;
	    
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				StringBuilder sb =  new StringBuilder();
				sb.append("<query xmlns=\"http://jabber.org/protocol/muc#owner\">");
				sb.append("<x xmlns=\"jabber:x:data\" type=\"submit\">");
				for (String field : new String [] {"muc#roomconfig_roomname", "muc#roomconfig_persistentroom", 
						/*"muc#roomconfig_membersonly", */"muc#roomconfig_allowinvites"}) {
					sb.append("<field var=\"" + field + "\">");
			        List<String> values = null;
			        if (field.endsWith("roomname")) {
			            values = Arrays.asList(alias);
			        } else if (field.endsWith("persistentroom") || field.endsWith("membersonly") || field.endsWith("allowinvites")) {
			            values = Arrays.asList("1");
			        }
			        for (String value : values) {
			        	sb.append("<value>" + value + "</value>");
			        }
			        sb.append("</field>");
				}
				sb.append("</x>");
				sb.append("</query>");
				return sb.toString();
			}
		};
		iq.setTo(groupJid);
		iq.setPacketID("creategroup");
		iq.setType(Type.SET);
		doSend(iq);  
	}
    
    // 		TODO
	//	    NSXMLElement *iqStanza = [NSXMLElement elementWithName: @"iq"];
	//	    [iqStanza addAttribute:[DDXMLNode attributeWithName:@"type" stringValue:@"set"]];
	//	    [iqStanza addAttribute:[DDXMLNode attributeWithName:@"from" stringValue:[NSString stringWithFormat:@"%@@%@", self.login, YOO_DOMAIN]]];
	//	    [iqStanza addAttribute:[DDXMLNode attributeWithName:@"to" stringValue:groupJid]];
	//	    [iqStanza addAttribute:[DDXMLNode attributeWithName:@"id" stringValue:@"destroyroom"]];
	//	    
	//	    NSXMLElement *queryElt = [NSXMLElement elementWithName: @"query" URI: @"http://jabber.org/protocol/muc#owner"];
	//	    NSXMLElement *destroyElt = [NSXMLElement elementWithName:@"destroy"];
	//	    [destroyElt addAttribute:[DDXMLNode attributeWithName:@"jid" stringValue:groupJid]];
	//	    [queryElt addChild:destroyElt];
	//	    [iqStanza addChild:queryElt];
	//	    [connection sendElement:iqStanza];
    
	
	//  <iq from='crone1@shakespeare.lit/desktop'
	//    id='begone'
	//    to='heath@chat.shakespeare.lit'
	//    type='set'>
	//    <query xmlns='http://jabber.org/protocol/muc#owner'>
	//    <destroy jid='coven@chat.shakespeare.lit'>
	//    <reason>Macbeth doth come.</reason>
	//    </destroy>
	//    </query>
	//    </iq>
	
    public void destroyGroup(final String groupJid) {
    	Log.w("ChatTools : destroyGroup >> ", " ####### >> " + groupJid);
    
	    String meJid = login + "@" + YOO_DOMAIN;
	    Log.w("ChatTools : destroyGroup >> ", " ### meJid ### meJid >> " + meJid);
	    List<String> memebers = new GroupDAO().listMembers(groupJid);
	    for (String userJid : memebers) {
	    	Log.w("ChatTools : destroyGroup >> ", " ### memeber ### userJid >> " + userJid);
	        if (!userJid.startsWith(meJid)) {
	            removeUserFromGroup(userJid, groupJid);
	        }
	    }
	    
	    IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				StringBuilder sb =  new StringBuilder();
				sb.append("<query xmlns=\"http://jabber.org/protocol/muc#owner\">");
				sb.append("<destroy jid=\"" + groupJid + "\">");
				sb.append("</destroy>");
				sb.append("</query>");
				return sb.toString();
			}
		};
		iq.setTo(groupJid);
		iq.setPacketID("destroyroom");
		iq.setType(Type.SET);
		doSend(iq);  
	    
	    
    }    
    
    private void removeUserFromGroup(final String userJid, String groupJid) {
    	Log.w("ChatTools : removeUserFromGroup >> ", " ####### groupJid>> " + groupJid + ", >> UserJid : " + userJid);
	    // remove the user from the group
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"http://jabber.org/protocol/muc#admin\"><item jid=\"" + userJid + "\" affiliation=\"none\"/></query>";
			}
		};
		iq.setType(Type.SET);
		iq.setFrom(login + "@" + YOO_DOMAIN);
		iq.setPacketID("revoke1");
		iq.setTo(groupJid);
		doSend(iq);
	    
	    if (userJid.startsWith(login + "@")) {
	        // we leave the group
	    	Presence presence = new Presence(Presence.Type.unavailable);
	    	presence.setTo(groupJid);
	    	doSend(presence);
	    } else {
	        // kick other user
	    	final String nick = StringUtils.parseName(userJid);
			iq = new IQ() {
				@Override
				public String getChildElementXML() {
					return "<query xmlns=\"http://jabber.org/protocol/muc#admin\"><item nick=\"" + nick + "\" role=\"none\"/></query>";
				}
			};
			iq.setType(Type.SET);
			iq.setFrom(login + "@" + YOO_DOMAIN);
			iq.setPacketID("kick1");
			iq.setTo(groupJid);
			doSend(iq);

	    }
	    
	    // revoke the user
	    YooUser yooUser = new YooUser(userJid);
	    
	    String groupName = StringUtils.parseName(groupJid);
	    YooGroup yooGroup = new YooGroup(groupName, roomAliases.get(groupName));
	    YooMessage yooMsg = new YooMessage();
	    yooMsg.setType(YooMessageType.ymtRevoke);
	    yooMsg.setTo(yooUser);
	    yooMsg.setGroup(yooGroup);
	    sendMessage(yooMsg);
	    
	    new GroupDAO().removeMemberFromGroup(yooUser.toJID(), yooGroup.toJID());
	}

    
    
    /**
     *
     * requestVCard, requestLastOneline, 
     */
    public void requestVCard(String jid) {
	    // request user's avatar and alias
    	Log.w("Chat Tool", "############## requestVCard ##################### >> " + jid); 
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<vCard xmlns=\"vcard-temp\"/>";
			}
		};
		iq.setType(Type.GET);
		iq.setTo(jid);
		doSend(iq);
		Log.i("ChatTools", "Request vcard for " + jid);
	}
	

    public void requestLastOneline(String jid) {
	    // request user last online status
    	Log.w("Chat Tool","########### getLastOneline ################ >> " + jid);  
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"jabber:iq:last\"/>";
			}
		};
		iq.setType(Type.GET);
		iq.setTo(jid);
		doSend(iq);
	
		Log.w("ChatTools", "Request last online " + jid + " >> " + iq.toXML());	
	}
    
    
    
    /**
     * 
     * dig receiveMessage, didReceiveIQ, didReceivePresence
     */
    private void didReceiveMessage(final Message message) {
 
    	Log.w("Chat Tool : Receive Message >> ", "" + message.getXmlns()); 
    	
		if (message.getType() == Message.Type.error) {
			return;
		}

		Contact shared = null;
		YooMessage yooMsg = new YooMessage();
		if (StringUtils.parseServer(message.getFrom()).equals(CONFERENCE_DOMAIN)) {
			String groupName = StringUtils.parseName(message.getFrom());
			YooGroup group = new YooGroup(groupName, groupName);
			group.setMember(StringUtils.parseResource(message.getFrom()));
			yooMsg.setFrom(group);
		} else {
			YooUser user = new YooUser(StringUtils.parseName(message.getFrom()), StringUtils.parseServer(message.getFrom()));
			yooMsg.setFrom(user);
		}
		
		yooMsg.setTo(new YooUser(login, YOO_DOMAIN));
		try {
			if(message.getBody() != null){
				String body = URLDecoder.decode(message.getBody(), "UTF8");
				yooMsg.setMessage(body);
			}
		} catch (UnsupportedEncodingException e) {
			Log.w("ChatTools", e.getMessage(), e);
		}
		
		yooMsg.setThread(message.getThread());
		yooMsg.setIdent(message.getPacketID());

		// handle message properties
		for (String propName : message.getPropertyNames()) {
			String propValue = "";
			if(message.getProperty(propName) instanceof Integer){
				propValue = ((Integer) message.getProperty(propName)).toString();
			}else if(message.getProperty(propName) instanceof String){
				propValue = (String) message.getProperty(propName);
			}
			
			if (propName.equals("type")) { // message type
                yooMsg.setType(YooMessageType.values()[Integer.parseInt(propValue)]);
            }else if (propName.equals("sound")) { // attached sound
                yooMsg.setSound(StringUtils.decodeBase64(propValue));
            }else if (propName.equals("picture") || propName.equals("picture1")) {  // attached picture
            	//don't know why it receive from server picture1 instead of picture.
            	byte [] imgData = StringUtils.decodeBase64(propValue);
                yooMsg.getPictures().add(imgData);
            }else if (propName.equals("location")) { // location
                String [] parts = propValue.split("/");
                if (parts.length == 2) {
                	yooMsg.setLocation(new double [] {Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
                }
            }else if (propName.equals("conferenceNumber")) { // call conference
                yooMsg.setConferenceNumber(propValue);
            }else if (propName.startsWith("group-")) { // group
                if (yooMsg.getGroup() == null) {
                    yooMsg.setGroup(new YooGroup());
                }
                if (propName.equals("group-name")) {
                    yooMsg.getGroup().setName(propValue);
                }
                if (propName.equals("group-alias")) {
                    yooMsg.getGroup().setAlias(propValue);
                }
            }else if (propName.startsWith("contact-")) { // contact
                if (shared == null){
                	shared = new Contact();
                }
       
                Log.w("ChatTools : didReceiveMessage >> ", "contactId : " + yooMsg.getShared());
                
                if(propName.startsWith("contact-firstName")) {
                	shared.setFirstName(propValue);
                }else if (propName.startsWith("contact-lastName")) {
                	shared.setLastName(propValue); 
                	
                // }else if (propName.startsWith("contact-messaging-label")) {
                }else if (propName.startsWith("contact-messaging-value")) {
                	String labelKey = propName.replace("value", "label");
                	String labelvValue = message.getProperty(labelKey).toString();
                	shared.getMessaging().add(new LabelledValue(labelvValue, propValue)); 
                	
                // }else if (propName.startsWith("contact-phone-label")) {
                }else if (propName.startsWith("contact-phone-value")) {
                	String labelKey = propName.replace("value", "label");
                	String labelvValue = message.getProperty(labelKey).toString();
                	shared.getPhones().add(new LabelledValue(labelvValue, propValue)); 
                	
                // }else if (propName.startsWith("contact-email-label")) {
                }else if (propName.startsWith("contact-email-value")) {
                	String labelKey = propName.replace("value", "label");
                	String labelvValue = message.getProperty(labelKey).toString();
                	shared.getEmails().add(new LabelledValue(labelvValue, propValue));
                }
            }else if (propName.equals("callReqId")) { // callReqId
				yooMsg.setCallReqId(propValue); 
			}else if (propName.equals("callStatus")) { // callStatus
				try {
					yooMsg.setCallStatus(CallStatus.values()[Integer.parseInt(propValue)]); 
				} catch (NumberFormatException e) {
					yooMsg.setCallStatus(CallStatus.csNone);
				}
			}else if (propName.equals("sendDate")) {
				yooMsg.setDate(DateUtils.parseString(propValue)); 
			}
        }
		
		// Handle call status from recipient
	    if (yooMsg.getType() == YooMessageType.ymtCallStatus){
	        
	    	if (CallStatus.csCancelled == yooMsg.getCallStatus()) {
	    		//  dismiss the call popup if visible
	    		if(yooMsg.getCallReqId().equals(YooApplication.callReqId)){
	    			// new ChatDAO().updateCallStatus(yooMsg.getCallReqId(), CallStatus.csCancelled); 
	    			System.err.println("######### Cancel Call ##### ChatTools : Call Request Id >> " + YooApplication.callReqId + ""); 
	    			callingDelegate.cancelCall(yooMsg);
	    		}
	    	}else if (CallStatus.csAccepted == yooMsg.getCallStatus()) {
	    		// show the window to call the conference number
	    		System.err.println("######### Accepted Call ##### ChatTools : Call Request Id >> " + YooApplication.callReqId + ""); 
	    		if(yooMsg.getCallReqId().equals(YooApplication.callReqId)){
	    			callingDelegate.acceptCallFromRecipient(yooMsg, true); 
	    		}
	    	}else if (CallStatus.csRejected == yooMsg.getCallStatus()) {
	    		System.err.println("######### Rejected Call ##### ChatTools : Call Request Id >> " + YooApplication.callReqId + ""); 
	    		if(yooMsg.getCallReqId().equals(YooApplication.callReqId)){
	    			callingDelegate.acceptCallFromRecipient(yooMsg, false); 
	    		}
	    	}
	    	
	    	// [self broadcast:@selector(handlePhoneCall:) param:yooMsg];
            return; // don't add this message in history
	    }else if (yooMsg.getType() == YooMessageType.ymtCallRequest){
	    	boolean  showPopup = true;
	    	YooRecipient recipient = yooMsg.getFrom();
	    	if(recipient != null && recipient instanceof YooGroup){
	    		YooGroup group = (YooGroup) recipient;
	    		if(group.getMember().equals(YooApplication.getUserLogin().getName())){
	    			showPopup = false;
	    		}
	    	}	
	    	
	    	long seconds = DateUtils.substructTimeAsSeconds(yooMsg.getDate());
	    	if(seconds >= CALL_MAX_DELAY){
	    		showPopup = false;
	    	}
	    	
    		if (showPopup) {
    			yooMsg.setCallReqId(yooMsg.getIdent()); 
    			callingDelegate.requestCallFromRecipient(yooMsg); 
    		}
	    }
	   
	    if(shared != null){
	    	Contact cont = new ContactDAO().findByFName(shared.getFirstName(), shared.getLastName());
	    	if(cont != null){
	    		shared.setContactId(cont.getContactId()); 
	    		shared = new ContactDAO().upsert(shared); 
	    		/*YooUser user = new UserDAO().findByContactId(shared.getContactId());
            	if(user == null && !shared.getPhones().isEmpty()){
            		findUserByPhone(shared);
            	}*/
	    	}else{
	    		long contactId = new ContactManager().insetContact(shared);
                if (contactId != -1) {
                	shared.setContactId(contactId);
                	new ContactDAO().upsert(shared);
                	/*YooUser user = new UserDAO().findByContactId(contactId);
                	if(user == null && !shared.getPhones().isEmpty()){
                		findUserByPhone(shared);
                	}*/
                }else{
                	// share contact fail.
                	shared.setContactId(-1); 
                }
	    	}
	    	yooMsg.setShared(shared.getContactId()); 
	    	
	    	// create other users
	    	Object jId = message.getProperty("JId");
	    	if(jId != null){
	    		YooUser user = new YooUser(jId.toString());
	    		user.setAlias(shared.toString()); 
	    		user.setContactId(shared.getContactId()); 
	    		new UserDAO().upsert(user); 
	    		// new ContactManager().createFromYoo(contact, jId.toString());
	    	}
	    }
	    
	   
	    // create/update group
	    if (yooMsg.getType() == YooMessageType.ymtInvite && yooMsg.getGroup() != null) {
	        if (yooMsg.getGroup().getAlias() != null) {
	        	// roomAliases.put(yooMsg.getGroup().getAlias(), yooMsg.getGroup().getName());
	        	roomAliases.put(yooMsg.getGroup().getName(), yooMsg.getGroup().getAlias());
	        }
	        checkGroup(yooMsg.getGroup().getName());
	    }
	    
	    // remove from group
	    if (yooMsg.getType() == YooMessageType.ymtRevoke && yooMsg.getGroup() != null) {
	        if (!yooMsg.getFrom().isMe()) {
	        	new GroupDAO().remove(yooMsg.getGroup().toJID());
	            // update UI
	        	for (ChatListener listener : listeners) {
	        		listener.friendListChanged(new ArrayList<YooUser>());
	        	}
	        }
	    }


	    // handle "received" tag
	    PacketExtension received = message.getExtension("received", "urn:xmpp:receipts");
	    if (received != null) {
	    	yooMsg.setIdent(((DeliveryReceipt)received).getId());
		    yooMsg.setType(YooMessageType.ymtAck);
		}
	    
	   
	    // handle request/receipt tag
	    PacketExtension requestReceipt = message.getExtension("request", "urn:xmpp:receipts");
	    if (requestReceipt != null) {
	   		yooMsg.setReceipt(true);
	   		// send receipt
	   		Message receipt = new Message();
	   		try {
	   			receipt.setBody(URLEncoder.encode(" ", "UTF8"));
			} catch (Exception e) {
				Log.w("ChatTools : add body message >> ", e.getMessage());
			}
	   		 
        	receipt.setProperty("sendDate", DateUtils.formatTime(new Date())); 
        	receipt.addExtension(new DeliveryReceipt(yooMsg.getIdent()));
        	receipt.setTo(yooMsg.getFrom().toJID());	
        	receipt.setFrom(yooMsg.getTo().toJID()); 
        	doSend(receipt);
        	for (ChatListener listener : listeners) {
        		listener.didReceiveMessage(yooMsg);
        	}
	    }
	   
	    //handle ymtMessageRead
	    if(yooMsg.getType().equals(YooMessageType.ymtMessageRead) && !yooMsg.getFrom().isMe()){
	    	new ChatDAO().markAsReadByOther(yooMsg.getIdent());
	    	for (ChatListener listener : listeners) {
        		listener.didReceiveMessage(yooMsg);
        	}
	    }
	   
	    if(yooMsg.getType().equals(YooMessageType.ymtMessageRead) && yooMsg.getFrom().isMe()){
	    	return;
	    }
	    
	    if (yooMsg.getType() != YooMessageType.ymtAck 
	    		&& yooMsg.getType() != YooMessageType.ymtMessageRead
	    		&& yooMsg.getType() != YooMessageType.ymtInvite 
	    		&& yooMsg.getType() != YooMessageType.ymtRevoke) {
	        if (message.getType() == Message.Type.groupchat) {
	            if (StringUtils.parseResource(message.getFrom()).equals(login)) {
	                // ignore own messages on group chat
	                return;
	            }
	            if (StringUtils.parseResource(message.getFrom()).length() == 0) {
	                // ignore message from the room itself
	                return;
	            }
	        }
	        addInHistory(yooMsg, false);
        	for (ChatListener listener : listeners) {
        		listener.didReceiveMessage(yooMsg);
        	}
	    } else if (yooMsg.getType() == YooMessageType.ymtAck) {
	        YooMessage originalMsg = new ChatDAO().acknowledge(yooMsg.getIdent());
	        if(originalMsg != null){
	        	for (ChatListener listener : listeners) {
	        		listener.didReceiveMessage(originalMsg);
	        	}
	        }
	    }else if(yooMsg.getType() == YooMessageType.ymtMessageRead){
	    	YooMessage originalMsg = new ChatDAO().findIdent(yooMsg.getIdent());
	    	if(originalMsg != null){
	    		for (ChatListener listener : listeners) {
	        		listener.didReceiveMessage(originalMsg);
	        	}
	    	}
	    }

	    // Add : Roster the sender
	    if (yooMsg.getFrom() instanceof YooUser && 
	    		new UserDAO().find(((YooUser)yooMsg.getFrom()).getName(), 
	    				((YooUser)yooMsg.getFrom()).getDomain()) == null) {
	    	
			IQ iq = new IQ() {
				@Override
				public String getChildElementXML() {
					return "<query xmlns=\"jabber:iq:roster\"><item jid=\"" + message.getFrom() + "\"/></query>";
				}
			};
			iq.setType(Type.SET);
			doSend(iq);
	    }
	}
    
    
    /*
     - (void)lastPresence:(NSString *)jid {
	    // request another user's last presence time
	    XMPPIQ *iq = [[XMPPIQ alloc] initWithType:@"get" to:[XMPPJID jidWithString:jid]];
	    DDXMLNode *attrNS = [DDXMLNode attributeWithName:@"xmlns" stringValue:@"jabber:iq:last"];
	    DDXMLNode *query = [DDXMLNode elementWithName:@"query" children:nil attributes:@[attrNS]];
	    [iq addChild:query];
	    [self mustSend:iq];
		}
     */
  
    
    // <presence to="qwerty@yoo-app.com" from="test3@yoo-app.com/9AA097C0-6F08-437C-9CC3-BEB727DC7AEA" type="unavailable"></presence>
    public static void lastPresence(String jid){
    	// request another user's last presence time
    }
    
    public void cancelCall(String callReqId){
    	YooMessage yooMsg = new ChatDAO().findIdent(callReqId);
    	if(yooMsg != null){
    		new ChatDAO().updateCallStatus(callReqId, CallStatus.csCancelled); 
    		
    		YooMessage yooMessage = new YooMessage();
        	yooMessage.setType(YooMessageType.ymtCallStatus); 
        	yooMessage.setTo(yooMsg.getTo());
        	yooMessage.setCallReqId(callReqId);
        	yooMessage.setIdent(callReqId);
        	yooMessage.setCallStatus(CallStatus.csCancelled); 
        	sendMessage(yooMessage); 
    	}
    }
    
    public void answerCall(Activity mainActivity, String callReqId, boolean accept){
    	YooMessage yooMsg = new ChatDAO().findIdent(callReqId);
    	if(yooMsg != null){
    		if(yooMsg.getFrom() instanceof YooGroup){
    			if(YooApplication.callMember != null){
    				yooMsg.setFrom(YooApplication.callMember);
    				YooApplication.callMember = null;
    			}
    			/*if(!accept){
    				return;
    			}*/
    		}
    		
    		YooMessage yooMessage = new YooMessage();
        	yooMessage.setType(YooMessageType.ymtCallStatus); 
        	yooMessage.setTo(yooMsg.getFrom());
        	yooMessage.setCallReqId(callReqId);
        	yooMessage.setIdent(callReqId);
        	yooMessage.setMessage(" "); 
        	if(accept){
        		yooMessage.setCallStatus(CallStatus.csAccepted); 
        		callingDelegate.acceptCall(yooMsg, accept); 
        	}else{
        		yooMessage.setCallStatus(CallStatus.csRejected); 
        	}
        	sendMessage(yooMessage); 
    	}
    }
    
    public static String getConferenceNumber(String conf, String jId){
		try {
			if(!com.fellow.yoo.utils.StringUtils.isEmpty(conf)){
				try {
					JSONObject objConf = new JSONObject(conf);
					if(objConf.has(jId)){ 
						return objConf.getString(jId);
					}
				} catch (JSONException e) {
					for (String confT : conf.split(",")) {
						JSONObject objConf = new JSONObject(confT);
						if(objConf.has(jId)){
							return objConf.getString(jId);
						}
					}
				}
			}
		} catch (Exception e) {
			Log.w("ChatFragment : getConferenceNumber >> ", e.getMessage()); 
		}
		return "";
	}
    
    
	/**
	 * didReceiveIQ
	 * @param iq
	 */
	private void didReceiveIQ(IQ iq) {
		Log.w("Chat Tool : Receive IQ >> Xmlns ", "" + iq.getXmlns()); 
		/*
		 	RECV: <iq xmlns="jabber:client" type="get" from="camboco10@yoo-app.com" to="test3@yoo-app.com/9AA097C0-6F08-437C-9CC3-BEB727DC7AEA"><vCard xmlns="vcard-temp"><FN>Camboco10</FN><PHOTO><TYPE>image/png</TYPE><BINVAL>
		*/
		
		if(iq instanceof IQCalling){
			// Handle request call and do call listener
			IQCalling iqCalling = (IQCalling) iq;
			String conferenceNumber = iqCalling.getConferenceNumberAsString(); 
			System.out.println(" #### ConferenceNumber >> " + conferenceNumber);
			
			if(com.fellow.yoo.utils.StringUtils.isEmpty(conferenceNumber)){
				// alert message conference number not available.
				callingDelegate.requestCall(null);
			}else{
				if(callRecipient != null){
					YooUser from = YooApplication.getUserLogin();
					
					YooMessage yooMsg = new YooMessage();
					yooMsg.setMessage(" "); 
					yooMsg.setIdent(iq.getPacketID());
					yooMsg.setConferenceNumber(conferenceNumber);
					yooMsg.setType(YooMessageType.ymtCallRequest); 
					yooMsg.setCallStatus(CallStatus.csNone);
					yooMsg.setRead(true); 
					/*  if ([yooMsg.to isKindOfClass:[YooGroup class]]) {
		                    yooMsg.callStatus = csAccepted;
		                    [self connectPhoneCall];
                		}
					 */

					yooMsg.setFrom(from); 
					yooMsg.setTo(callRecipient);
					if(yooMsg.getTo() != null){
						sendMessage(yooMsg); 
						callingDelegate.requestCall(yooMsg);
					}	
				}
			}
			
		}else if(iq instanceof LastActivity){
			LastActivity lastA = (LastActivity) iq;
			if(lastA != null && lastA.getIdleTime() > 0){
				Log.w("Chat Tool", "Rec IQ getIdleTime  >> " + lastA.getIdleTime()); 
				YooUser yooUser = new UserDAO().updateLastOnline(iq.getFrom(), DateUtils.substructDate(lastA.getIdleTime())); 
				for (ChatListener listener : listeners) {
	        		listener.friendListChanged(Arrays.asList(yooUser));
	        	}
			}
		}else if (iq instanceof RosterPacket) {
			if (!login.equals(REGISTRATION_USER)) {
				RosterPacket roster = (RosterPacket) iq;
				Log.w("Rec RosterPacket >>>> ", roster.getType().toString());
				for (RosterPacket.Item item : roster.getRosterItems()) {
					if (CONFERENCE_DOMAIN.equals(StringUtils.parseServer(item.getUser()))) {
						// received from group
						// checkGroup(StringUtils.parseName(item.getUser());
					} else {
						checkFriend(StringUtils.parseName(item.getUser()), StringUtils.parseServer(item.getUser()));
					}
				}
			}
		} else if (iq instanceof VCard) {
			VCard vcard = (VCard)iq;
			String fullName = vcard.getField("FN");
			byte [] picture = vcard.getAvatar();
			
	        if (fullName != null || picture != null) {
	            YooUser yooUser = new UserDAO().find(StringUtils.parseName(iq.getFrom()), StringUtils.parseServer(iq.getFrom()));
	            if (yooUser != null) {
	                if (picture != null) {
	                    yooUser.setPicture(picture);
	                }
	                if (fullName != null && !yooUser.isMe()) { // don't change the alias if it is the current user
	                    yooUser.setAlias(fullName);
	                }
	                new UserDAO().upsert(yooUser);
	                for (ChatListener listener : listeners) {
	                	listener.friendListChanged(Arrays.asList(yooUser));
	                }
	                checkAddressBook(yooUser);
	            }
	        }
		} else if (iq instanceof IQRegister) {
			IQRegister register = (IQRegister) iq;
			if (register.getUsername() != null) {
				regLogin = register.getUsername();
			}
			if (register.getPassword() != null) {
				for (ChatListener listener : listeners) {
					listener.didReceiveRegistrationInfo(regLogin, register.getPassword());
				}
			}
			if (register.getUsername() == null && register.getPassword() == null) {
				// wrong code
				for (ChatListener listener : listeners) {
					listener.didReceiveRegistrationInfo(null, null);
				}
			}
		} else if (iq instanceof IQFindUser) {
			IQFindUser findUser = (IQFindUser)iq;
			
			List<YooUser> changed = new ArrayList<YooUser>();
			for (UserInfo info : findUser.getUsers()) {
               
               YooUser yooUser = new UserDAO().find(info.getName(), YOO_DOMAIN);
               Contact contact = new ContactManager().findById(info.getContactId());
               if (yooUser == null) {
               	yooUser = checkFriend(info.getName(), YOO_DOMAIN);
               }
               if (contact != null) {
               	yooUser.setContactId(contact.getContactId());
               	yooUser.setCallingCode(info.getCallingCode());
               	new UserDAO().upsert(yooUser);
               	changed.add(yooUser);
               }
           }
			
		   for (ChatListener listener : listeners) {
			   listener.friendListChanged(changed);
		   }
		} else if (iq instanceof MUCAdmin) {
			MUCAdmin muc = (MUCAdmin)iq; 
			if (iq.getPacketID().equals("groupmember")) {
	            // received group information
				YooGroup group = new YooGroup(StringUtils.parseName(iq.getFrom()), null);
	            Iterator<?> it = muc.getItems();
	            while (it.hasNext()) {
	            	Item item = (Item)it.next();
	            	if (item.getJid() != null && item.getJid().length() > 0) {
	            		new GroupDAO().addMemberToGroup(item.getJid(), group.toJID());
	            	}
	            }
			}
		}
				

		if (StringUtils.parseServer(iq.getFrom()).equals(CONFERENCE_DOMAIN)
				&& iq.getPacketID().equals("creategroup") && iq.getType() != IQ.Type.ERROR) {
			checkGroup(StringUtils.parseName(iq.getFrom()));
	        // invite users to new chat room/group
			List<String> users = roomUsers.get(StringUtils.parseName(iq.getFrom()));	        
	        for (String user : users) {
	        	addUserToGroup(user, iq.getFrom());
	        }

		}
	    
	    //return NO;
	}


	/**
	 * didReceivePresence
	 * @param presence
	 */
	private void didReceivePresence(Presence presence) {
		Log.w("Chat Tool : Receive Presence >> ", "" + presence.toXML() + "\nform : " + presence.getFrom() + "\nto :" + presence.getTo()); 
		
    	if (CONFERENCE_DOMAIN.equals(StringUtils.parseServer(presence.getFrom()))) {
    		for (PacketExtension extension : presence.getExtensions()) {
    			Log.w("PacketExtension toXML : ", extension.toXML() + " >> name space : " + extension.getNamespace());
    			Log.w("PacketExtension : ",  " >> presence.getType() : " + presence.getType());
    			if (extension instanceof DefaultPacketExtension) {
    				DefaultPacketExtension pe = (DefaultPacketExtension)extension;
    				if (pe.getNamespace().equals("http://jabber.org/protocol/muc#user")) {
        				if (pe.getValue("status") != null && presence.getType() != Presence.Type.unavailable) {
        					confirmGroup(presence.getFrom());
        				}
    				}
    			}
    		}
    	
	    } else if (!REGISTRATION_USER.equals(StringUtils.parseName(presence.getFrom())) &&
	    		!REGISTRATION_USER.equals(StringUtils.parseName(presence.getTo())) &&
	    		!YOO_DOMAIN.equals(StringUtils.parseName(presence.getFrom()))
	    		) {
	    	
	    	YooUser yooUser = new UserDAO().find(StringUtils.parseName(presence.getFrom()), StringUtils.parseServer(presence.getFrom()));
	    	if (yooUser == null) {
	    		checkFriend(StringUtils.parseName(presence.getFrom()), StringUtils.parseServer(presence.getFrom()));  
	    	}
	    	
	    	yooUser = new UserDAO().updateLastOnline(yooUser.toJID(), DateUtils.substructDate(0));
	    	if (presence.getType() == Presence.Type.unavailable) {
	    		present.remove(yooUser.toJID());
	    	} else if (presence.getType() == Presence.Type.subscribe) {
	    		Presence rPresence = new Presence(Presence.Type.subscribed);
	    		rPresence.setTo(yooUser.toJID());
	    		doSend(rPresence);
	    	} else {
	    		
	    		String status = presence.getStatus();
	    		if("invisible".equals(status)){
	    			visibility.remove(yooUser.toJID());
	    		}else{
	    			if (!visibility.contains(yooUser.toJID())) {
	    				visibility.add(yooUser.toJID());
		    		}
	    		}
	    		
	    		if (!present.contains(yooUser.toJID())) {
	    			present.add(yooUser.toJID());
	    		}
	    		
	    		
	    		// RECV: <presence xmlns="jabber:client" from="camboco10@yoo-app.com/8C38F2FD-EAD1-4D3E-AFEA-D5C31F3E204A" to="test3@yoo-app.com">
	    		// <x xmlns="vcard-temp:x:update"><photo>87EA272C5E92C38C602FA8FC66A9D89FBE5607BC</photo></x></presence>
	    		// <iq type="get" to="camboco10@yoo-app.com"><vCard xmlns="vcard-temp"/></iq>
	    		// 
	    		String xml = presence.toXML();
	    		if(xml != null && xml.contains("vcard-temp:x:update")){
	    			try {
	    				String photo = xml.substring(xml.indexOf("<photo>") + 7, xml.indexOf("</photo>"));
	    				if(photo.length() > 0){
	    					IQ iq = new IQ() {
	    						@Override
	    						public String getChildElementXML() {
	    							return "<vCard xmlns=\"vcard-temp\"/>";
	    						}
	    					};
	    					iq.setTo(presence.getFrom().split("/")[0]); 
	    					iq.setType(Type.GET);
	    					doSend(iq);
	    				}
					} catch (Exception e) {
						Log.e("ChatTools : didReceivePresence >> ", e.toString());
					}
	    		}
	    		
	    		// TODO
	    		/*
                NSArray *xElts = [presence elementsForName:@"x"];
                if (xElts.count > 0) {
                    DDXMLElement *xElt = [xElts objectAtIndex:0];
                    NSString *nsReq = [[xElt namespaceForPrefix:@""] stringValue];
                    if ([nsReq isEqualToString:@"vcard-temp:x:update"]) {
                        // a contact has updated his photo !
                        // check if we have different SHA1, and
                        // obtain his new VCard, if it's not ourselves
                        NSArray *photoElts = [xElt elementsForName:@"photo"];
                        if (photoElts.count > 0) {
                            DDXMLElement *photoElt = (DDXMLElement *)[photoElts objectAtIndex:0];
                            NSString *newSha1 = [photoElt stringValue];
                            unsigned char digest[CC_SHA1_DIGEST_LENGTH];
                            NSMutableString *digestString = [[NSMutableString alloc] init];
                            if (CC_SHA1([yooUser.picture bytes], (unsigned int)[yooUser.picture length], digest)) {
                                for (int i = 0; i < CC_SHA1_DIGEST_LENGTH; i++) {
                                    [digestString appendString:[NSString stringWithFormat:@"%02X", digest[i]]];
                                }
                            }
                            if (![newSha1 isEqualToString:digestString]) {
                                if (![presence.from.user isEqualToString:self.login]
                                        || ![presence.from.domain isEqualToString:YOO_DOMAIN]) {
                                    [self requestVCard:presence.from.bare];
                                }
                            }
                        }
                    }
                }
	    		*/
	        }
	    	
	    	for (ChatListener listener : listeners) {
	    		listener.friendListChanged(Arrays.asList(yooUser));
	    	}
	    }
	}
    
    

	@Override
	public void connectionClosedOnError(Exception arg0) {
		
	}

	@Override
	public void reconnectingIn(int arg0) {
		
	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		
	}

	@Override
	public void reconnectionSuccessful() {
		
	}
	
	public void connectionClosed() {
		for (ChatListener listener : listeners) {
			listener.didLogin(login, "DISCONNECT");
		}
	}
	
	public void connectionRefresh() {
		for (ChatListener listener : listeners) {
			listener.didLogin(login, "REFRESH");
		}
	}
	
	public void logout() {
		if(connection != null){
			connection.disconnect();
			connection = null;
		}
		login = null;
		password = null;
	}
	
	
	public void connectionCreated(Connection connection) {
	    
	}

	
	/**
	 * 
	 * 
	 */
	public void setDevice(final String deviceToken) {
		IQ iq = new IQ() {
			@Override
			public String getChildElementXML() {
				return "<query xmlns=\"yoo:iq:device\"><device>" + deviceToken + "</device></query>";
			}
		};
		iq.setType(Type.SET);
		doSend(iq);    	
	}

	public boolean isPresent(YooUser user) {
		return present.contains(user.toJID());
	}
	
	public boolean isVisibility(YooUser user) { 
		if (YooApplication.getAppContext() != null) {
			if(ActivityUtils.isNoInternet(YooApplication.getAppContext())){
				/*for (ChatListener listener : listeners) {
		    		listener.didLogin(login, "DISCONNECT");
		    	}*/
				return false;
			}
		}	
		
		if(user != null){
			return present.contains(user.toJID()) && visibility.contains(user.toJID());
		}
		return false;
	}

	public List<YooUser> listUsers() {
		return new UserDAO().list();
	}
	
	
	private static String capitalize(String s) {
		if (s == null){
			return null;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	public void addListener(ChatListener listener) {
		if(!listeners.contains(listener)){
			listeners.add(listener);
		}
	}

	public void removeListener(ChatListener listener) {
		listeners.remove(listener);
	}

	public void asyncDisconnect(boolean refresh) {
	    if (connection != null) {
	    	if(connection.isConnected()){
	    		final Connection tmp = connection;
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						tmp.disconnect();
						return null;
					}
				}.execute();
	    	}
	    	
			connection = null;
			if(refresh){
				connectionRefresh();
			}
	    }
    }
	
	public static AsyncTask<Void, Void, Void> syncLoginTask; 
	public void asyncLogin(final String pLogin, final String pPassword) {
		if(syncLoginTask == null){ // to avoid login to many time.
			syncLoginTask = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					login(pLogin, pPassword);
					return null;
				}
				
				protected void onPostExecute(Void result) {
					syncLoginTask = null;
				};
				
			}.execute();
		}
	}
	

	public void setCountry(String pCountry) {
		countryCode = pCountry;
	    countryReady = true;
	    getUsersFromContacts();
	}

	public void contactsLoaded() {
	    contactsReady = true;
	    countryCode =YooApplication.geCountryCode();
	    countryReady = !com.fellow.yoo.utils.StringUtils.isEmpty(countryCode);
	    getUsersFromContacts();
	}
	
	public List<YooMessage> messagesForRecipient(YooRecipient recipient, boolean withPictures) {
		return messagesForRecipient(recipient, withPictures, 0);
	}

	public List<YooMessage> messagesForRecipient(YooRecipient recipient, boolean withPictures, int limit) {
    	return new ChatDAO().list(recipient, withPictures, limit);
	}
	
	public List<YooMessage> messagesForBroadcast(boolean withPictures, int limit) {
    	return new ChatDAO().list(null, withPictures, limit);
	}

    
    public void addInHistory(YooMessage yooMsg, boolean send) {
    	
    	// log send/received data bytes.
    	saveSendReceivedBytes(yooMsg, send);
    	new ChatDAO().insert(yooMsg);
    }
    
	/*public List<String> getContactFields() {
		return Arrays.asList("firstName", "lastName", "company", "jobTitle");
	}*/

   
	private String genRandStringLength(int len) {
	    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	    StringBuilder randomString = new StringBuilder(len);
	    Random rand = new Random();
	    for (int i = 0; i < len; i++) {
	    	randomString.append(letters.charAt(rand.nextInt(letters.length())));
	    }
	    return randomString.toString();
	}

	private String getCodeFromString(String s) {
    	s = s.toLowerCase(Locale.US);
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
	            sb.append(c);
	        }
	    }
	    return sb.toString();
	}


	public String getStatus() {
		if (connection != null && connection.isAuthenticated() && connection.isConnected()) {
			return "Online";
		} else {
			return "Disconnected";
		}
	}

	
	
	public void sendPrensenc(boolean invisible){
		Presence presence = new Presence(Presence.Type.available);
		if(invisible){
			presence.setStatus("invisible");
		}
		doSend(presence);
	}
	
	
	
	public long stats(boolean sent) {
		if(connection != null){
			connection.getChatManager().toString(); 
			connection.getHost().getBytes();
			
		}
		return 0;
	}
	
	public CallingListener getCallingDelegate() {
		return callingDelegate;
	}

	public void setCallingDelegate(CallingListener callingDelegate) {
		this.callingDelegate = callingDelegate;
	}
	
	
	// send received byte messages.
	private static void saveSendReceivedBytes(YooMessage yooMessage, boolean send){
		
		long count = getSendReceivedBytes(send) + getCountMessageBytes(yooMessage);
		
		String key = send? "send_bytes" : "received_bytes";
		Context context = YooApplication.getAppContext();
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Data", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(key, count); 
		editor.commit();
	}
	
	
	public static long getSendReceivedBytes(boolean send){
		String key = send? "send_bytes" : "received_bytes";
		Context context = YooApplication.getAppContext();
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Data", Context.MODE_PRIVATE);  
		return preferences.getLong(key, 0);
	}
	
	private static long getCountMessageBytes(YooMessage yooMessage){
		long count = 0;
		if(!yooMessage.getPictures().isEmpty()){
			for (byte[] btyes : yooMessage.getPictures()) {
				count += btyes.length;
			}
		}else if(yooMessage.getSound() != null){
			count += yooMessage.getSound().length;
		}
		
		if(!com.fellow.yoo.utils.StringUtils.isEmpty(yooMessage.getMessage())){
			try {
				String s = yooMessage.getMessage();
				byte[] msgBytes = s.getBytes("UTF-8");
				count += msgBytes.length;
				Log.i("ChatTools : Read Message bytes >> ", "" + msgBytes.length);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Log.i("ChatTools : getCountMessageBytes >> ", "" + count);
		return count;
	}
    


}
