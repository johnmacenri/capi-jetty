package com.carapay.capi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javapns.Push;
import javapns.notification.PushedNotifications;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

@Controller
public class APIController {

	private static final Logger logger = LoggerFactory.getLogger(APIController.class);
	private static final String API_KEY = "AIzaSyAFNSPGv1cRrZIS-SU3pYO_g3F3Czi9tGY";
	private static final String IOS_CERT = "cpmp.p12";
	private static final String IOS_CERT_PASS = "statto6178";
	

	@RequestMapping(value = "/register/device/android", params = { "udak", "regId" }, method = RequestMethod.POST)
	public void registerDeviceAndroid(@RequestParam(value = "udak", required = true) String udak,
			@RequestParam(value = "regId", required = true) String regId, HttpServletResponse response)
			throws UnsupportedEncodingException, IOException {
		logger.info("registerDeviceAndroid called with udak [" + udak + "] and regId [" + regId + "]");
		String decryptionKey = "DEC_KEY_" + UUID.randomUUID().toString();
		String sharedSecret = "SH_SEC_" + UUID.randomUUID().toString();

		Sender sender = new Sender(API_KEY);
		Message message = new Message.Builder().addData("SharedSecret", sharedSecret).build();
		Result result = sender.send(message, regId, 3);

		if (result.getMessageId() != null) {
			String canonicalRegId = result.getCanonicalRegistrationId();
			if (canonicalRegId != null) {
				logger.info("same device has more than on registration ID: update database");
			}
			response.getOutputStream().write(decryptionKey.getBytes("UTF-8"));
		} else {
			String error = result.getErrorCodeName();
			if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				logger.error("application has been removed from device - unregister database");
			} else {
				logger.error("Error occurred calling GCM " + error);
			}
			response.getOutputStream().write(error.getBytes("UTF-8"));
		}
	}

	@RequestMapping(value = "/register/device/ios", params = { "udak", "regId" }, method = RequestMethod.POST)
	public void registerDeviceIOS(@RequestParam(value = "udak", required = true) String udak,
			@RequestParam(value = "regId", required = true) String regId, HttpServletResponse response)
			throws UnsupportedEncodingException, IOException {
		logger.info("registerDeviceIOS called with udak [" + udak + "] and regId [" + regId + "]");
		String decryptionKey = "DEC_KEY_" + UUID.randomUUID().toString();
		String sharedSecret = "SH_SEC_" + UUID.randomUUID().toString();
		
		//Strip any spaces in the regId;
		regId = regId.replace(" ", "");
		
		try {
			InputStream certIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(IOS_CERT);
			PushedNotifications nots =  Push.alert(sharedSecret, certIs, IOS_CERT_PASS, false, regId).getSuccessfulNotifications();
			if (nots.size() == 0) {
				response.getOutputStream().write("No Successful Notifications".getBytes("UTF-8"));
			} else {
				response.getOutputStream().write(decryptionKey.getBytes("UTF-8"));				
			}
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
			response.getOutputStream().write(e.getLocalizedMessage().getBytes("UTF-8"));
		}		
	}

}
