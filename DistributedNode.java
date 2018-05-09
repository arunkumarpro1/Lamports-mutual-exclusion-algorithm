package com.advancedOS.project3A;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedNode0 {
	static TreeMap<Integer, Host> hosts;
	static TreeMap<Integer, PrintWriter> hostsOutputStreams;
	static volatile Queue<Request> pendingRequests;
	static volatile Request ownRequest;
	static int nodeId, N, portNumber = 3000;
	static volatile boolean localTermination = false, globalTermination = false, wait = false;
	static volatile AtomicInteger count = new AtomicInteger();
	static volatile AtomicInteger timestamp = new AtomicInteger();
	static HashMap<Integer, PrintWriter> outputStreams = new HashMap<Integer, PrintWriter>();
	static volatile int terminationCount;
	static PrintWriter out;

	public static void main(String[] args) throws IOException {

		List<Socket> neighbours = new ArrayList<>();
		int interRequestDelay, csExecutionTime, numberOfRequests;
		String parts[], line;

		hosts = new TreeMap<>();
		hostsOutputStreams = new TreeMap<>();
		Scanner s = new Scanner(new File("conf.txt"));

		line = s.nextLine();
		while (line.isEmpty() || line.startsWith("#"))
			line = s.nextLine();
		String[] tokens = line.split(" ");
		N = Integer.parseInt(tokens[0]);
		interRequestDelay = Integer.parseInt(tokens[1]);
		csExecutionTime = Integer.parseInt(tokens[2]);
		numberOfRequests = Integer.parseInt(tokens[3]);

		pendingRequests = new ConcurrentLinkedQueue<>();

		ExponentialDistributor delayGenerator = new ExponentialDistributor(new Random(), interRequestDelay);
		ExponentialDistributor csTimeGenerator = new ExponentialDistributor(new Random(), csExecutionTime);

		boolean scanning = true;
		Socket client = null;
		PrintWriter os;
		BufferedReader reader;

		for (int i = 0; i < N; i++) {
			line = s.nextLine().trim();
			while (line.isEmpty() || line.startsWith("#"))
				line = s.nextLine().trim();
			parts = line.split("\\s+");
			if (Integer.parseInt(parts[2]) == (portNumber)) {
				nodeId = Integer.parseInt(parts[0]);
				continue;
			}
			hosts.put(Integer.parseInt(parts[0]), new Host(parts[1], Integer.parseInt(parts[2])));
		}

		ServerSocket serverSocket = new ServerSocket(portNumber);

		for (Entry<Integer, Host> entry : hosts.entrySet()) {
			while (scanning) {
				try {
					client = new Socket(entry.getValue().hostname, entry.getValue().port);
					scanning = false;
				} catch (ConnectException e) {
					System.out.println("Connect failed, waiting and trying again");
					try {
						Thread.sleep(2000);// 2 seconds
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
				}
			}
			os = new PrintWriter(client.getOutputStream(), true);
			hostsOutputStreams.put(entry.getKey(), os);
			scanning = true;
		}

		for (PrintWriter writer : hostsOutputStreams.values())
			writer.println(nodeId);

		for (int i = 0; i < N - 1; i++) {
			client = serverSocket.accept();
			reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
			reader.readLine();
			neighbours.add(client);
			RecieveMessage r = new RecieveMessage(reader);
			new Thread(r).start();
		}

		out = new PrintWriter(new File("output" + nodeId + ".txt"));

		try {
			for (int i = 1; i <= numberOfRequests; i++) {
				Thread.sleep((long) delayGenerator.getNext());
				csEnter(i);
				Thread.sleep((long) csTimeGenerator.getNext());
				csLeave(i);
			}
			wait = true;
			synchronized (DistributedNode0.class) {
				timestamp.incrementAndGet();
				for (Integer hostId : hosts.keySet())
					sendMessage(hostId, "termination", timestamp.get(), 0);
			}
			wait = false;
			localTermination = true;
			while (globalTermination == false) {
			}
			System.out.println("Main Terminated");
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			out.flush();
			serverSocket.close();
			for (Socket socket : neighbours)
				socket.close();
			s.close();
		}
	}

	private static void csEnter(int i) throws IOException {
		wait = true;
		synchronized (DistributedNode0.class) {
			ownRequest = new Request(timestamp.incrementAndGet(), nodeId);
			count.set(0);
			for (Integer hostId : hosts.keySet())
				sendMessage(hostId, "request", timestamp.get(), 0);
		}
		wait = false;
		while (count.get() != N - 1) {
			/*
			 * System.out.println("Blocked " + pendingRequests.peek().nodeId +
			 * " " + pendingRequests.peek().timestamp + " count - " +
			 * count.get());
			 */
		}
		out.print(i + " " + timestamp.get());
	}

	private static void csLeave(int i) throws IOException {
		wait = true;
		synchronized (DistributedNode0.class) {
			out.println(" " + timestamp.get());
			ownRequest = null;
			timestamp.incrementAndGet();
			while (!pendingRequests.isEmpty())
				sendMessage(pendingRequests.poll().nodeId, "reply", timestamp.get(), 0);
		}
		wait = false;
	}

	public static synchronized void sendMessage(int destId, String messageType, int msgTimestamp, int reqTimestamp)
			throws IOException {
		PrintWriter os = hostsOutputStreams.get(destId);
		if (messageType.equals("reply"))
			os.println(nodeId + " " + timestamp.incrementAndGet() + " " + messageType);
		else if (messageType.equals("request"))
			os.println(nodeId + " " + msgTimestamp + " " + messageType);
		else
			os.println(nodeId + " " + msgTimestamp + " " + messageType);
		System.out.println("Sending " + messageType + " message " + nodeId + " " + timestamp + " " + messageType);
	}
}

class Request {
	int timestamp;
	int nodeId;

	public Request(int timestamp, int nodeId) {
		super();
		this.timestamp = timestamp;
		this.nodeId = nodeId;
	}

	@Override
	public String toString() {
		return "timestamp - " + timestamp + " nodeId - " + nodeId;
	}
}

class Host {
	String hostname;
	int port;

	public Host(String hostname, int port) {
		super();
		this.hostname = hostname;
		this.port = port;
	}
}

class ExponentialDistributor {

	Random rand;
	double lambda;

	public ExponentialDistributor(Random rand, double mean) {
		this.rand = rand;
		this.lambda = 1 / mean;
	}

	public double getNext() {
		return (long) Math.log(1 - rand.nextDouble()) / (-lambda);
	}
}

class TimeStampComp implements Comparator<Request> {

	@Override
	public int compare(Request r1, Request r2) {
		if (r1.timestamp > r2.timestamp)
			return 1;
		else if (r1.timestamp < r2.timestamp)
			return -1;
		else if (r1.nodeId < r2.nodeId)
			return -1;
		else
			return 1;
	}

}

class RecieveMessage implements Runnable {

	BufferedReader reader;

	public RecieveMessage(BufferedReader reader) {
		super();
		this.reader = reader;
	}

	@Override
	public void run() {
		String split[];
		try {
			String message;
			while (true) {
				if (DistributedNode0.globalTermination == true && DistributedNode0.localTermination == true) {
					System.out.println("Thread terminated");
					break;
				}
				message = reader.readLine();
				split = message.split(" ");
				while (DistributedNode0.wait == true) {
				}
				synchronized (DistributedNode0.class) {
					System.out.println("Received Message " + message);
					DistributedNode0.timestamp.set(
							Math.max(DistributedNode0.timestamp.incrementAndGet(), Integer.parseInt(split[1]) + 1));
					if (message.contains("reply")) {
						DistributedNode0.count.incrementAndGet();
					} else if (message.contains("termination")) {
						if (++DistributedNode0.terminationCount == DistributedNode0.N - 1)
							DistributedNode0.globalTermination = true;
					} else {
						if (DistributedNode0.ownRequest == null
								|| DistributedNode0.ownRequest.timestamp > Integer.parseInt(split[1])
								|| (DistributedNode0.ownRequest.timestamp == Integer.parseInt(split[1])
										&& DistributedNode0.nodeId > Integer.parseInt(split[0])))
							DistributedNode0.sendMessage(Integer.parseInt(split[0]), "reply", 0, 0);
						else
							DistributedNode0.pendingRequests
									.add(new Request(Integer.parseInt(split[1]), Integer.parseInt(split[0])));
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Thread terminated");
		}
	}

}
