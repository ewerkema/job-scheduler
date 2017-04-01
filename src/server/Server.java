package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import api.RestAPI;
import database.SQLite;

public class Server extends Observable implements Runnable {
	private transient static final Logger logger = Logger.getLogger( Server.class.getName() );
	private static List<String> serverLogger = new ArrayList<String>();
	private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private InetSocketAddress address;
	private ServerInputHandler serverInputHandler;
	private ArrayList<ConnectedClient> clients = new ArrayList<ConnectedClient>();
	private SQLite database;
	public static final String API_URI = "http://localhost:8080/api/";
	public static final String BROKER = "asa"; //TODO: Store broker setup in configuration file
	
	//Message queue stuff
	private Channel channel;
	private Connection connection;
	private boolean queueConnected = false;
	private Consumer reportConsumer;
	
	public Server(InetSocketAddress address) {
		this.address = address;
		logger.setLevel(Level.ALL);
		logger.addHandler(new Handler() {
			@Override
			public void close() throws SecurityException {
			}

			@Override
			public void flush() {
			}

			@Override
			public void publish(LogRecord logRecord) {
				//StackTraceElement e = Thread.currentThread().getStackTrace()[2];
				Date date = new Date(logRecord.getMillis());
				serverLogger.add(sdf.format(date) + ": "
						+ logRecord.getSourceClassName() + ": "
						+ logRecord.getSourceMethodName() + ": "
						+ "\"" + logRecord.getMessage() + "\""
						+ "\n");
				Server.this.update();
			}
        	
        });
	}
	
	@Override
	public void run() {
		update();
		
		serverInputHandler = new ServerInputHandler(this);
		serverInputHandler.start();
		this.setDatabase(new SQLite(this));
		RestAPI rest = new RestAPI(this, API_URI);
		logger.log(Level.FINE, "Server is initiated");
	}
	
	public void handleReport(String result) throws JSONException{
		JSONObject json = new JSONObject(result);
		
		String clientAddress = json.getString("address").trim();
		if (clientAddress.equals("localhost/127.0.0.1"))
			clientAddress = "localhost";
		int clientPort = Integer.parseInt(json.getString("port"));
		InetSocketAddress client = new InetSocketAddress(clientAddress, clientPort);
		
		if(json.getString("type").equals("report")){
			ConnectedClient connectedClient = getClient(client);
			double cpuLoad = Double.parseDouble(json.getString("cpuLoad"));
			double memAvailable = Double.parseDouble(json.getString("memAvailable"));
			double cpuTemp = Double.parseDouble(json.getString("cpuTemp"));
			ClientReport report = new ClientReport(connectedClient.getClientAddress(), cpuLoad, memAvailable, cpuTemp);
			connectedClient.addReport(report);
			log("Received report from client on " + clientAddress);
		} else {
			//TODO: faulty report handling??
		}
	}
	
	public void update() {
		this.setChanged();
		this.notifyObservers();
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}
	
	public void log(Level lvl, String msg, Throwable thrown) {
		logger.log(lvl, msg, thrown);
	}
	
	public void log(String text) {
		logger.log(Level.FINE, text);
	}
	
	public String getLoggerText() {
		String messageText = "";
        
        for (int i = serverLogger.size() - 1; i >= 0; i--) {
        	messageText += serverLogger.get(i);
		}
		return messageText;
	}

	public void addClient(InetSocketAddress client) {
		if (!clientExists(client)) {
			log("Adding client: " + client.getAddress() + " on port: " + client.getPort());
			
			int clientId = database.findClient(client);
			if (clientId == 0)
				database.addClient(client);
			
			ConnectedClient connectedClient = new ConnectedClient(this, client);
			
			clients.add(connectedClient);
			update();
		}
	}
	
	public synchronized void removeClient (InetSocketAddress client) {
		if (clientExists(client)) {
			log("Removing client: " + client.getHostName() + " on port: " + client.getPort());
			ConnectedClient removeClient = getClient(client);
			clients.remove(removeClient);
			update();
		}		
	}
	
	public synchronized boolean clientExists (InetSocketAddress client) {
		return (getClient(client) != null);
	}
	
	public synchronized ConnectedClient getClient (InetSocketAddress searchClient) {
		ConnectedClient findClient = null;
		
		for (ConnectedClient client : clients) {
			if (searchClient.equals(client.getClientAddress()))
				findClient = client;
		}
		
		return findClient;
	}
	
	public ArrayList<ConnectedClient> getClients() {
		return clients;
	}

	public synchronized void requestReport() {
		if (clients.isEmpty()) {
			log("Client list is empty");
			return;
		}
		
		for (ConnectedClient client : clients) {
			client.requestReport();
		}
	}

	public String getClientsText() {
		String messageText = "";
		
		if (clients.isEmpty())
			messageText = "No clients";
        
		int i = 1;
        for (ConnectedClient client : clients) {
        	messageText += "(" + i + ") "
    				+ client.getClientAddress().getHostName() 
    				+ ":" + client.getClientAddress().getPort();
        	
        	ClientReport report = client.getLatestReport();
        	if (report != null)
        		messageText += " (CPU:" + Math.round(report.getCpuLoad() * 100) + "%, "
        				+ " MEM:" + Math.round(report.getMemAvailable() / Math.pow(10,6)) + "MB, "
        				+ " TEMP:" + Math.round(report.getCpuTemp()) + "�C)";
        	
        	messageText += "\n";
        	i++;
		}
		return messageText;
	}

	public SQLite getDatabase() {
		return database;
	}

	public void setDatabase(SQLite database) {
		this.database = database;
	}
	
}
