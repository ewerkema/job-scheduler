package api;

import java.net.InetSocketAddress;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import server.Server;

@Path("/removeclientservice")
public class removeClientService{
	@Context
    Configuration config;
	@Path("{address}")
	@GET
	@Produces("application/json")
	public Response removeClient(@PathParam("address") String address) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		Server server = (Server) config.getProperty("server");
		String[] split = address.split("-");
		InetSocketAddress clientaddr = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
		if(!server.clientExists(clientaddr)) {
			jsonObject.put("result", "Client with address " + address + " doesn't exist");
		} else { 
			server.removeClient(clientaddr);
			if(!server.clientExists(clientaddr)) {
				jsonObject.put("result", "deletion succesful");
			} else { 
				jsonObject.put("result", "deletion failed");
			}
		}
		String result = "@Produces(\"application/json\") Output: \n\nRemove Client Output: \n\n" + jsonObject;
		return Response.status(200).entity(result).build();
	 }

}