package edu.gatech.i3l.HealthPort.HV;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.hsg.ApplicationConfig;

@WebServlet("/Auth")
public class Authentication extends HttpServlet
{
	private static final String TOKEN_NAME = "wctoken";
	
	public Authentication() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		
	    StringBuffer url = new StringBuffer();
		url.append(Config.ShellUrl);
		
		
		url.append("/redirect.aspx?target=AUTH&targetqs=?appid=");
		url.append(ApplicationConfig.APP_ID);
		if (Config.RedirectUrl != null &&
				Config.RedirectUrl.trim().length()!=0)
		{
			url.append("%26redirect=");
			url.append(request.getScheme());
			url.append("://");
			url.append(request.getServerName());
			url.append(":");
			url.append(request.getServerPort());
			url.append(request.getContextPath());
			url.append(Config.RedirectUrl);
		}
		if (Config.ActionQS != null &&
				Config.ActionQS.trim().length()!=0)
		{
			url.append("%26actionqs=");
			url.append(Config.ActionQS);
		}
		
		 response.setStatus(response.SC_MOVED_TEMPORARILY);
	     response.setHeader("Location", url.toString());  
	   
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
