package com.socket.server;

import java.net.*;
import java.io.*;


public class Server {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException {
		int filesize = 2000000;

		long start = System.currentTimeMillis();
		int bytesRead;
		String fileName = "audio.wav";
		// create socket
		ServerSocket serverSocket = new ServerSocket(8080);
		String line = null;
		boolean done = false;
		
		while (!done) {
			
			System.out.println("Waiting for connection ... ");
			Socket socket = serverSocket.accept();
			System.out.println("Connection accepted ... ");

			// recieve audio file
			String response = null;
			DataInputStream inputStream = new DataInputStream(
					socket.getInputStream());
			FileOutputStream fileOutputStream = new FileOutputStream(
					fileName);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
					fileOutputStream);
			System.out.println("Receiving...");

			byte[] bytes = new byte[filesize];

			while((bytesRead = inputStream.read(bytes, 0, bytes.length))!=-1){
				bufferedOutputStream.write(bytes, 0, bytesRead);
			}
			
			socket.shutdownInput();
			//Thread.sleep(5000);
			inputStream.close();
			
			FileInputStream fileInputStream = new FileInputStream(new File("out.txt"));
			BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
			while((line = br.readLine())!= null){
				response = line.trim().toLowerCase();
			}
			//System.out.println(response);
			
			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
			outputStream.writeUTF(response);
			
			bufferedOutputStream.close();
			outputStream.close();
			//socket.close();
			//serverSocket.close();
		}

	}
}
