package com.marcdejonge.ipad.simulator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.marcdejonge.codec.MixedList;
import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.ParseException;
import com.marcdejonge.codec.plist.PListCodec;

public class SimulatedDevice {
	private final String udid;
	private final URL serverUrl;

	public SimulatedDevice(String udid, String url) {
		this.udid = udid;
		try {
			serverUrl = new URL(url);
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	private MixedMap startMsg() {
		return new MixedMap().$("UDID", udid);
	}

	private MixedMap ack(String commandUUID) {
		return startMsg().$("CommandUUID", commandUUID).$("Status", "Acknowledged");
	}

	private MixedMap notNow(String commandUUID) {
		return startMsg().$("CommandUUID", commandUUID).$("Status", "NotNow");
	}

	public List<MixedMap> sendIdle() throws ParseException {
		return options(mdmCall(startMsg().$("Status", "Idle")));
	}

	public List<MixedMap> sendCommand(MixedMap command) throws ParseException {
		return options(mdmCall(command));
	}

	private List<MixedMap> options(MixedMap query) {
		if (query == null || query.isEmpty()) {
			return Collections.emptyList();
		}

		String commandUUID = query.getString("CommandUUID", null);
		MixedMap command = query.getMap("Command", null);
		if (commandUUID == null || command == null) {
			return Collections.emptyList();
		}

		MixedMap ack = ack(commandUUID);
		MixedMap notNow = notNow(commandUUID);

		switch (command.getString("RequestType", "")) {
		case "ManagedMediaList":
			ack.$("Books", new MixedList());
			notNow = null;
			break;
		case "DeviceInformation":
			ack.$("QueryResponses", new MixedMap().$("AvailableDeviceCapacity", 1)
			                                      .$("BuildVersion", "123")
			                                      .$("DeviceCapacity", 2)
			                                      .$("DeviceName", "Simulated iPad")
			                                      .$("Model", "SIMULATED")
			                                      .$("ModelName", "iPad")
			                                      .$("OSVersion", "9.3")
			                                      .$("ProductName", "iPhone3,3")
			                                      .$("SerialNumber", "SIMULATED")
			                                      .$("UDID", udid)
			                                      .$("BatteryLevel", 0.8)
			                                      .$("IsSupervised", true)
			                                      .$("IsCloudBackupEnabled", false)
			                                      .$("iTunesStoreAccountIsActive", false));
			notNow = null;
			break;
		case "InstalledApplicationList":
			ack.$("InstalledApplicationList", new MixedList());
			notNow = null;
			break;
		case "InstallProfile":
			Object payload = command.get("Payload");
			if (payload instanceof byte[]) {
				try {
					PListCodec codec = new PListCodec(new InputStreamReader(new ByteArrayInputStream((byte[]) payload),
					                                                        Charset.defaultCharset()));
					MixedMap profile = codec.parseExpectMap();

					String profileName = profile.getString("PayloadIdentifier", "unknown");

					FileWriter w = new FileWriter(profileName + ".xml");
					w.write(PListCodec.generatePropertyList(profile));
					w.close();
				} catch (IOException
				         | ParseException ex) {
				}
			}
			break;
		case "ProfileList":
			ack.$("ProfileList", new MixedList());
			notNow = null;
			break;
		case "Settings":
			for (MixedMap setting : command.getList("Settings", new MixedList()).objects()) {
				if ("Wallpaper".equals(setting.getString("Item", ""))) {
					Object image = setting.get("Image");
					if (image instanceof byte[]) {
						try {
							FileOutputStream out = new FileOutputStream("wallpaper.png");
							out.write((byte[]) image);
							out.close();
						} catch (IOException ex) {
						}
					}
				}
			}
			notNow = null;
			break;
		}

		if (notNow != null) {
			return Arrays.asList(query, ack, notNow);
		} else {
			return Arrays.asList(query, ack);
		}
	}

	public boolean register() {
		try {
			System.out.println(mdmCall(startMsg().$("MessageType", "Authenticate")
			                                     .$("Topic", "topic")
			                                     .$("SerialNumber", "SIMULATED")));

			System.out.println(mdmCall(startMsg().$("MessageType", "TokenUpdate")
			                                     .$("Topic", "topic")
			                                     .$("Token", "simulated-token")
			                                     .$("PushMagic", "simulated-push-magic")));

			return true;
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
	}

	private MixedMap mdmCall(MixedMap request) throws ParseException {
		HttpURLConnection c = null;
		try {
			System.out.println("Sending request:\n" + request);
			String xmlOut = PListCodec.generatePropertyList(request);
			c = (HttpURLConnection) serverUrl.openConnection();
			c.setDoOutput(true);
			c.getOutputStream().write(xmlOut.getBytes());
			PListCodec codec = new PListCodec(new InputStreamReader(c.getInputStream()));
			return codec.parseExpectMap();
		} catch (IOException ex) {
			if (ex.getMessage().contains("Server returned HTTP response code")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(c.getErrorStream()));
				String line = null;
				try {
					System.out.println("IO Error: response from server:");
					while ((line = reader.readLine()) != null) {
						System.out.println(line);
					}
				} catch (IOException e) {
				}
			}
			throw new ParseException("I/O error while reading from server: " + ex.getMessage(), ex);
		} finally {
		}
	}
}
