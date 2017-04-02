package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectedClient {
	private Server server;
	private InetSocketAddress clientAddress;
	private Socket clientSocket = null;
	private ArrayList<ClientReport> reports = new ArrayList<ClientReport>();
	
	private String cpuName;
	private int cpuCores;
	private int totalMemory;
	private String operatingSystem;
	private String hostname;
	private int performance;
	private int id;
	private String displayName;
	private int time;
	
	public ConnectedClient(Server server, InetSocketAddress clientAddress) {
		this.server = server;
		this.setClientAddress(clientAddress);
		findExistingClientRecords();
			
		try {
			clientSocket = new Socket(clientAddress.getAddress(), clientAddress.getPort());
		} catch (IOException exception) {
			server.log( Level.SEVERE, exception.toString(), exception );
		}
	}
	
	public ConnectedClient(Server server, InetSocketAddress clientAddress, String cpuName, int cpuCores, String os, int memory, String hostname, int performance, int id, int time) {
		this.server = server;
		this.setClientAddress(clientAddress);
		this.cpuName = cpuName;
		this.cpuCores = cpuCores;
		this.operatingSystem = os;
		this.totalMemory = memory;
		this.performance = performance;
		this.id = id;
		this.time=time;
		
		findExistingClientRecords();
			
		try {
			clientSocket = new Socket(clientAddress.getAddress(), clientAddress.getPort());
		} catch (IOException exception) {
			server.log( Level.SEVERE, exception.toString(), exception );
		}
	}

	private void findExistingClientRecords() {
		int client;
		if ((client = server.getDatabase().findClient(clientAddress)) != 0) {
			reports.addAll(server.getDatabase().getClientReports(client, clientAddress));
		}
	}

	public void send(JSONObject json) {
		send(json.toString().getBytes());
	}
	
	public void send(byte[] sendData) {
		send(sendData, 0, sendData.length);
	}
	
	public void send(byte[] sendData, int start, int len) {
		try {
			if (len < 0)
				throw new IllegalArgumentException("Negative byte packet length not allowed.");
			if (start < 0 || start >= sendData.length)
				throw new IndexOutOfBoundsException("Byte start index out of bounds: " + start);
			
			OutputStream out = clientSocket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(out);
			
			dos.writeInt(len);
			
			if (len > 0) {
				dos.write(sendData, start, len);
			}
			dos.flush();
			out.flush();
		} catch (IOException exception) {
			server.log( Level.SEVERE, exception.toString(), exception );
		}
	}
	
	public void requestReport() {
		JSONObject reportData = null;
		try {
			reportData = new JSONObject();
			reportData.put("type", "report");
		} catch (JSONException exception) {
			server.log( Level.SEVERE, exception.toString(), exception );
		}
		send(reportData);
	}

	public InetSocketAddress getClientAddress() {
		return clientAddress;
	}

	public void setClientAddress(InetSocketAddress clientAddress) {
		this.clientAddress = clientAddress;
	}
	
	public void addReport(ClientReport report) {
		this.reports.add(report);
		server.getDatabase().addReport(report);
	}

	public ArrayList<ClientReport> getReports() {
		return reports;
	}

	public void setReports(ArrayList<ClientReport> reports) {
		this.reports = reports;
	}
	
	public ClientReport getLatestReport() {
		return (reports.size() > 0) ? reports.get(reports.size() - 1) : null;
	}
	
	public void setCpuName(String cpuName){
		this.cpuName = cpuName;
	}
	public String getCpuName(){
		return cpuName;
	}
	
	public void setOperatingSystem(String os){
		this.operatingSystem = os;
	}
	public String getOperatingSystem(){
		return operatingSystem;
	}
	
	public void setHostname(String hostname){
		this.hostname = hostname;
	}
	public String getHostname(){
		return hostname;
	}
	
	public void setCpuCores(int cpuCores){
		this.cpuCores = cpuCores;
	}
	public int getCpuCores(){
		return cpuCores;
	}
	
	public void setTotalMemory(int memory){
		this.totalMemory = memory;
	}
	public int getTotalMemory(){
		return totalMemory;
	}
	
	public void setPerformance(int performance){
		this.performance = performance;
	}
	public int getPerformance(){
		return performance;
	}
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
