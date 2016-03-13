package com.marcdejonge.ipad.simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import com.marcdejonge.codec.MixedMap;
import com.marcdejonge.codec.ParseException;

public class Main {
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		TrustAllX509TrustManager.trustAll();
		SimulatedDevice device = new SimulatedDevice("simulated-device",
		                                             "https://dev-marc.zuludesk.com/server/index.php?company=999999&XDEBUG_SESSION_START=9999");

		if (!device.register()) {
			System.out.println("Registration failed");
			return;
		} else {
			System.out.println("Registered as a new device");
		}

		List<MixedMap> commands = Collections.emptyList();
		while (true) {
			try {
				if (commands.isEmpty()) {
					System.out.println("Send idle command?");
					in.readLine();

					commands = device.sendIdle();
				} else {
					System.out.println();
					System.out.println("Received command:");
					System.out.println(commands.get(0));

					int selectedCommand = -1;

					while (selectedCommand < 1 || selectedCommand >= commands.size()) {
						System.out.println(commands.size() - 1
						                   + " options available, press the number you want to send");
						for (int ix = 1; ix < commands.size(); ix++) {
							MixedMap command = commands.get(ix);
							System.out.println(ix + ") " + command.getString("Status", "Unknown"));
						}

						try {
							String line = in.readLine();
							selectedCommand = line.length() == 0 ? 1 : Integer.parseInt(line);
						} catch (NumberFormatException ex) {
							selectedCommand = -1;
						}
					}

					commands = device.sendCommand(commands.get(selectedCommand));
				}
			} catch (ParseException e) {
				e.printStackTrace();
				commands = Collections.emptyList();
			}
		}
	}
}
