package api;

import java.util.Date;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import jobs.Job;
import jobs.JobSchedulingEvent;
import server.Server;

@Path("/jobscheduleservice")
public class jobScheduleService{
	private transient static final Logger logger = Logger.getLogger( jobScheduleService.class.getName() );
	@Context
    Configuration config;

	@GET
	@Produces("application/json")
	public Response getJobSchedules(@QueryParam("date1") Date date1, @QueryParam("date2") Date date2, @QueryParam("job") Integer job,
									@QueryParam("status") Integer status, @QueryParam("client") Integer client) throws JSONException {
		job = (job == null) ? -1 : job;
		JSONObject jsonObject = new JSONObject();
		Server server = (Server) config.getProperty("server");
		ArrayList<Job> jobs = server.getDatabase().getAllJobs();
		int k=0;
		for(int i=0; i<jobs.size(); i++) {
			if(job != -1 & job != jobs.get(i).getId())
				continue;
			ArrayList<JobSchedulingEvent> schedules = server.getDatabase().getAllJobSchedulingEvents(jobs.get(i).getId());
			for(int j=0; j<schedules.size(); j++) 
				getJobScheduleEvent(schedules.get(i), date1, date2, status, client, jsonObject, k);
				k++;
		}

		String result = jsonObject.toString();
		return Response.status(200).entity(result).build();
	 } 

	private void getJobScheduleEvent(JobSchedulingEvent j, Date date1, Date date2, Integer status,
			Integer client, JSONObject jsonObject, int i) {
		if(date1 != null && date2 != null)
			if(date1.after(j.getEventDate()) || date2.before(j.getEventDate()))
				return;
		if(status != null)
			if(status != j.getSchedStatus())
				return;
		if(client != null)
			if(j.getClient() != client)
				return;
		ArrayList<Object> data = new ArrayList<Object>();
		data.add(j.getJob());
		data.add(j.getEventDate());
		data.add(j.getSchedStatus());
		data.add(j.getClient());
		try {
			jsonObject.put(Integer.toString(i), data);
		} catch (JSONException e) {
			logger.log( Level.SEVERE, e.toString(), e );
		}
	}
}