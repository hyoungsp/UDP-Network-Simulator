import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.*;


/**
 * Client to generate a ping requests over UDP.
 * Code has started in the PingServer.java
 */

public class PingClient extends TimerTask {

	static InetAddress server = null; // server address
	static String IPString = ""; // server address stored as string
	static int port = -1; // port number
	static int count = 0; // number of packets
	static int period = 0; // wait
	static int timeout = 0; //timeout in milliseconds

	static int received_number = 0; // number of packets received back from the server
	static int fail = 0; // number of packets failed in receiving
	static long msStart; // time of start
	static long totalTime = 0; // total time for all the packets
	static int successTime = 0; // total time of successful packets, to calculate avg RTT
	static long minTime = Integer.MAX_VALUE; // min RTT
	static long maxTime = 0; // max RTT

	int sequence_number; // sequence number

	public PingClient(int seq_number) {
		this.sequence_number = seq_number;
	}
//	public PingClient() {};

	public void run() {

		Date now = new Date();

		long msSend = now.getTime();
		// Create string to send, and transfer i to a Byte Array
		String str = "PING " + this.sequence_number + " " + msSend + "\r\n";
		byte[] buf = new byte[1024];
		buf = str.getBytes();
		// Create a datagram packet to send as an UDP packet.
		DatagramPacket ping = new DatagramPacket(buf, buf.length, server, port);

		// Try to send and receive the packet - catch exception handles (timeout)
		try {
			// DatagramSocket socket = new DatagramSocket(port);
			DatagramSocket socket = new DatagramSocket();
			// Send the Ping datagram to the specified server
			socket.send(ping);
			// Set up the timeout
			socket.setSoTimeout(timeout);
			// Set up an UDP packet for receiving
			DatagramPacket response = new DatagramPacket(new byte[1024], 1024);

			// Try to receive the response from the ping
			socket.receive(response);

			now = new Date();
			long msReceived = now.getTime();
			long time = msReceived - msSend;
			// update the total time and the number of success packets
			totalTime = msReceived - msStart;
			received_number++;
			// Print the packet and the delay information
			printData(response, this.sequence_number,time);
			successTime += time;
			if (time > maxTime) {
				maxTime = time;
			}
			if (time < minTime) {
				minTime = time;
			}
			socket.close();

		} catch (Exception e) {
			// catch the exception - when packet has timed out
//			System.out.println("Timeout for packet " + this.sequence_number);
			fail++;
			now = new Date();
			long msReceived = now.getTime();
			totalTime = msReceived - msStart;
		}

		// print the ping statistics in the end
		if (received_number + fail >= count) {
			double loss_rate = 100 * (1 - (double) received_number / (double) count);
			System.out.println("\n--- " + IPString + " ping statistics ---");
			System.out.printf("%d transmitted, %d received, %d%% loss, time %d ms\n", count, received_number, Math.round(loss_rate), totalTime);
			if(received_number != 0) {
				System.out.printf("rtt min/avg/max = %d/%d/%d\n",minTime, (successTime/received_number), maxTime);
			} else {
				System.out.printf("rtt min/avg/max = 0/0/0\n");
			}
			System.exit(0);
		}

	}

	public static void main(String[] argv) throws Exception
	{
		// Get command line arguments.
		// Process command-line arguments.
		for (String arg : argv) {
			String[] splitArg = arg.split("=");
			if (splitArg.length == 2 && splitArg[0].equals("--server_ip")) {
				server = InetAddress.getByName(splitArg[1]);
				IPString = splitArg[1];
			} else if (splitArg.length == 2 && splitArg[0].equals("--server_port")) {
				port = Integer.parseInt(splitArg[1]);
			} else if (splitArg.length == 2 && splitArg[0].equals("--count")) {
				count = Integer.parseInt(splitArg[1]);
			} else if (splitArg.length == 2 && splitArg[0].equals("--period")) {
				period = Integer.parseInt(splitArg[1]);
			} else if (splitArg.length == 2 && splitArg[0].equals("--timeout")) {
				timeout = Integer.parseInt(splitArg[1]);
			} else {
				System.err.println("Usage: java PingClient --server_port=<server ip addr> --server_port=<server port> --count=<number of pings to send> --period=<wait interval> --time=<timeout>");
				return;
			}
		}

		System.out.println("PING " + IPString);
		Date now = new Date();
		msStart = now.getTime();
		int seq_number = 1;
		// array of timers
//		Timer[] timers = new Timer[count];

		while (seq_number <= count) {

			Timer timer = new Timer();
			PingClient task = new PingClient(seq_number);
//			timers[seq_number-1] = timer;
			timer.schedule(task, (seq_number-1)*period);
			seq_number++;
		}
		while (received_number + fail < count){
//			System.out.println(received_number);
			continue;
		}

	}

	private static void printData(DatagramPacket request,int seq_number, long delayTime) throws Exception
	{
		// Obtain references to the packet's array of bytes.
		byte[] buf = request.getData();

		// Wrap the bytes in a byte array input stream,
		// so that you can read the data as a stream of bytes.
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);

		// Wrap the byte array output stream in an input stream reader,
		// so you can read the data as a stream of characters.
		InputStreamReader isr = new InputStreamReader(bais);

		// Wrap the input stream reader in a bufferred reader,
		// so you can read the character data a line at a time.
		// (A line is a sequence of chars terminated by any combination of \r and \n.)
		BufferedReader br = new BufferedReader(isr);

		// The message data is contained in a single line, so read this line.
		String line = br.readLine();

		// Print host address and data received from it.
		System.out.println(
				"PONG " + request.getAddress().getHostAddress() +
				": seq=" + seq_number + " time=" + delayTime );
	}
}
