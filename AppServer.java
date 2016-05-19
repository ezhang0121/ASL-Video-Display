package com.socket.server;

import java.net.*;
import java.awt.EventQueue;
import java.io.*;

public class AppServer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EventQueue.invokeLater(new Runnable(){
			public void run(){
				AppServer server = new AppServer();
			}
		});
	}
	/*
	 * setup Sever
	 */
	public AppServer() {
		int filesize = 2000000;
		int bytesRead;
		String fileName = "audio.wav";
		String line = null;
		String response = null;
		boolean done = false;
		//create socket
		try{
			ServerSocket serverSocket = new ServerSocket(8080);
			while(!done){
				System.out.println("Waiting for connection ...");
				Socket socket = serverSocket.accept();
				System.out.println("Connection accepted ...");
				
				//receive audio file
				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				FileOutputStream fileOutputStream = new FileOutputStream(fileName);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
				System.out.println("Receiving ...");
				
				byte[] bytes = new byte[filesize];
				
				while((bytesRead = dataInputStream.read(bytes, 0, bytes.length)) != -1){
					bufferedOutputStream.write(bytes, 0, bytesRead);
				}
				
				// run bash shell script
				ProcessBuilder pb = new ProcessBuilder("run.sh");
				Process p = pb.start(); // start the process
				//p.waitFor(); 			// wait for the process to finish
				System.out.println("Recognition complete ...");
				
				// read output file 
				/*FileInputStream fileInputStream = new FileInputStream(new File("out.txt"));
				BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
				while((line = br.readLine()) != null){
					response = line.trim().toLowerCase();
				}
				System.out.println(response);*/
				ReadFile rf = new ReadFile(this);
				new Thread(rf).start();
			}
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
class ReadFile implements Runnable {
	private AppServer as;
	public ReadFile(AppServer server){
		this.as = server;
	}
	public void run(){
		String line = null;
		String response = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(new File("out.txt"));
			BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
			while((line = br.readLine()) != null){
				response = line.trim().toLowerCase();
			}
			System.out.println(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
