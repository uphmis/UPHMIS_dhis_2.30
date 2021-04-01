package org.hisp.dhis.automessage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Samta Pandey
 */
public class ScheduleAutoSMS extends AbstractJob
{
    private static final Log log = LogFactory.getLog( ScheduleAutoEmailMessage.class );

    private static final String KEY_TASK = "scheduleAutoSMSData";
    
    private static final String superUserGroup = "8792645"; // Facility User Group
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    private List<String> userMobileNumberList = new ArrayList<String>();   
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private UserService userService;
    
    private LockExceptionStore lockExceptionStore;
    
    private SimpleDateFormat simpleDateFormat;
    
    String currentDate = "";

    String currentMonth = "";

    String currentYear = "";

    String todayDate = "";
   
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.AUTO_SMS_MESSAGE;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration ) throws AddressException, MessagingException, MalformedURLException, IOException
    {
       
        boolean isAutoSMSEnabled = (Boolean) systemSettingManager.getSystemSetting( SettingKey.AUTO_SMS_MESSAGE );
        System.out.println( "IS Auto SMS Enabled \n" + isAutoSMSEnabled );
        
        if ( !isAutoSMSEnabled )
        {
            log.info( String.format( "%s aborted. Auto SMS  are disabled", KEY_TASK ) );

            return;
        }

        log.info( String.format( "%s has started", KEY_TASK ) );
        
        simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
        
        Date date = new Date();
        
        todayDate = simpleDateFormat.format( date );
        currentDate = simpleDateFormat.format( date ).split( "-" )[2];
        currentMonth = simpleDateFormat.format( date ).split( "-" )[1];
        currentYear = simpleDateFormat.format( date ).split( "-" )[0];
        
        //userMobileNumberList = new ArrayList<String>( getAllDataSetUserMobleNoList( ) );
        
        if (currentDate.equalsIgnoreCase( "12" ) || currentDate.equalsIgnoreCase( "13" ) || currentDate.equalsIgnoreCase( "15" ))
        {            
            String dataEntryUserMessage = "Please fill your data in UPHMIS portal before 16th, your data entry will be blocked on 16th";
            userMobileNumberList = new ArrayList<String>( getUserMobleNoList( superUserGroup ) );  
			System.out.println(userMobileNumberList);
            sendSMS( dataEntryUserMessage, userMobileNumberList );
        }
        
        System.out.println("INFO: Scheduler job has ended at : " + new Date() );  
             
    }
    public Map<String,String> getUserMap(){
    	Map<String,String> userMap = new HashMap<String,String>();
    	
    	List<LockException> lockList = new ArrayList<LockException>();
    	
    	lockList = lockExceptionStore.getAll();
    	
    	for(LockException le : lockList) {
    		userMap.put(le.getDataSet().getId()+"", le.getExpireDate()+"");    		
    	}
    	
    	System.out.println(lockExceptionStore.getAll());
    	
    	String datasetInfo = "SELECT expirydays, userid FROM dataset";
    	SqlRowSet sql2ResultSet = jdbcTemplate.queryForRowSet( datasetInfo );
	 		while ( sql2ResultSet != null && sql2ResultSet.next() )
 		{
        	String datasetid = sql2ResultSet.getString("id");
        	User user = userService.getUser(Integer.parseInt(datasetid));
        	if(user.getPhoneNumber() != null)
        	{
        		if(!userMap.containsKey(datasetid)) {
	        		String expiryDate = sql2ResultSet.getString("expirydays");
	        		userMap.put(datasetid, user.getPhoneNumber()+ ":"+expiryDate);
        		}
        	}
 		}
    	
    	return userMap;
    }
    
    public String getContentMessage(String expiryDate)
    {
    	String content = "";
    	Calendar now = Calendar.getInstance();
    	String dataEntryDate = "16/"+(now.get(Calendar.MONTH)+1)+"/"+now.get(Calendar.YEAR);
    	
    	content = "Dear UPHMIS User,\n Please fill your data till "+dataEntryDate+" , if you already fill please ignore this\n Thanks";
    	
    	return content;
    }
    
    // Send SMS to user who have phone number updated on application
    public void sendSMS( String message, List<String> phonenos ) throws MalformedURLException, IOException
    {
        String phoneNo = null;
        Iterator<String> it = phonenos.iterator();
        
        if( phonenos != null && phonenos.size() >0 )
        {
            while ( it.hasNext() )
            {
                if ( phoneNo == null )
                {
                    phoneNo = (String) it.next();
                }                
                else
                {
                    phoneNo += "," + it.next();
                }
            }
            
            System.out.println( "mobile no -- " + phoneNo );
            
            String username = "Indianhealthaction";
            String password = "12345678";

            String senderid = "LBWKMC";
            String channel = "TRANS";

            try 
            {
              String encoding = "UTF-8";
              
              String urlParameters = "user=" + URLEncoder.encode(username, encoding)
                + "&password=" + URLEncoder.encode(password, encoding)
                + "&senderid=" + URLEncoder.encode(senderid, encoding)
                + "&channel="+URLEncoder.encode(channel, encoding)
                + "&DCS="+URLEncoder.encode("0", encoding)
                + "&flashsms="+URLEncoder.encode("0", encoding)
                + "&number=" + URLEncoder.encode(phoneNo, encoding)
                + "&text=" + URLEncoder.encode(message, encoding)
                + "&route="+URLEncoder.encode("02", encoding);

              // Send request to the API servers over HTTPS
              URL url_string = new URL("http://aanviit.com/api/mt/SendSMS?" + urlParameters );
              
              HttpURLConnection urlConnection = (HttpURLConnection) url_string.openConnection();
              
              System.out.println( "SMS Response -- " + urlConnection.getResponseMessage() );
              
              urlConnection.disconnect();
            } 
            
            catch (Exception e) 
            {
              System.out.println( "Exception - " + e.getMessage() );
            }
        }
        else
        {
            System.out.println( "No Mobile No Found" );
        }

    }
    
    //Get All Dataset assign users
    public List<String> getAllDataSetUserMobleNoList(){
    	List<String> mobileNumberList = new ArrayList<>();
    	
    	return mobileNumberList;
    }
    
    //Get User Phone Number of user group 
    
    public List<String> getUserMobleNoList( String userGroupIds )
    {
        List<String> mobileNumberList = new ArrayList<>();
        try
        {
            String query = "SELECT us.username,usinfo.surname, usinfo.firstname, usinfo.email, usinfo.phonenumber from users us  " +
                            "INNER JOIN userinfo usinfo ON usinfo.userinfoid = us.userid " +
                            "INNER JOIN usergroupmembers usgm ON usgm.userid = us.userid " +
                            "WHERE usgm.usergroupid in ( "+ userGroupIds +" ); ";
              
            //System.out.println( "query = " + query );
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );
            
            //System.out.println( "-- RS " + rs.toString() + " -- " + rs.isFirst() + " -- " + rs.next() ) ;
            
            while ( rs.next() )
            {
                String mobileNumber = rs.getString( 5 );
                
                if( mobileNumber != null && mobileNumber.length() == 10 )
                {
                    mobileNumberList.add( mobileNumber );
                }
            }
            return mobileNumberList;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal UserGroupId ids", e );
        }
    }        

}