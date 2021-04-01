package org.hisp.dhis.autoapprovetrackerdata;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Mithilesh Kumar Thakur
 */
public class ScheduleAutoApproveTrackerDataDoctorDiary extends AbstractJob
{
    private static final Log log = LogFactory.getLog( ScheduleAutoApproveTrackerDataDoctorDiary.class );

    //private final static int   UPHMIS_DOCTORS_DIARY_PROGRAM_ID = 73337033;
    private final static int   TEIA_USER_NAME_ID = 76755184;
    
    private final static int    CURRENT_STATUS_DOC_DIARY_DATAELEMENT_ID = 88199674;
    private final static String DOC_DIARY_PROGRAM_STAGE_IDS = "112222859,92415804,73337065,73397870,73397885,73397824,73397828,73397815,96982961,96983540,112223312,73397880,73337059,73397847,73397890,73397819,73397876,73337069,73397894,73337045,73397864";
    
    private final static String UPHMIS_DOCTORS_DIARY_USER_GROUP_ID = "116439552";
    private final static String   UPHMIS_DOCTORS_DIARY_APPROVAL_USER_GROUP_ID = "88254522,88254528";
    
    private static final String KEY_TASK = "scheduleAutoApproveTrackerDataTaskDoctorDiary";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private CurrentUserService currentUserService;
    
    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;
    
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private MessageSender emailMessageSender;
    
    private Set<String> userEMailList = new HashSet<String>();
    private List<String> userMobileNumberList = new ArrayList<String>();
    
    private Set<String> approvalEMailList = new HashSet<String>();
    private List<String> approvalMobileNumberList = new ArrayList<String>();
    
    private List<String> autoApprovedEMailList = new ArrayList<String>();
    
    private SimpleDateFormat simpleDateFormat;

    private String complateDate = "";

    private Period currentperiod;

    private String trackedEntityInstanceIds = "";

    String currentDate = "";

    String currentMonth = "";

    String currentYear = "";

    String todayDate = "";
    
    private String psiIdsByCommas = "-1";
    
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    
    
    @Override
    public JobType getJobType()
    {
        return JobType.AUTO_APPROVE_TRACKER_DATA_DOCTOR_DAIRY;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration ) throws AddressException, MessagingException, MalformedURLException, IOException
    {
        System.out.println("INFO: scheduler Auto Approve Tracker Data Doctor Diary job has started at : " + new Date() +" -- " + JobType.AUTO_APPROVE_TRACKER_DATA_DOCTOR_DAIRY );
        boolean isAutoApproveTrackerDataEnabled = (Boolean) systemSettingManager.getSystemSetting( SettingKey.AUTO_APPROVE_TRACKER_DATA_DOCTOR_DAIRY );
        System.out.println( "isAutoApproveTrackerDataEnabled -- " + isAutoApproveTrackerDataEnabled );
        
        if ( !isAutoApproveTrackerDataEnabled )
        {
            log.info( String.format( "%s aborted. Auto Approve Job Doctor Diary  are disabled", KEY_TASK ) );

            return;
        }

        log.info( String.format( "%s has started", KEY_TASK ) );
          
        simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
        
        Date date = new Date();
        
        todayDate = simpleDateFormat.format( date );
        currentDate = simpleDateFormat.format( date ).split( "-" )[2];
        currentMonth = simpleDateFormat.format( date ).split( "-" )[1];
        currentYear = simpleDateFormat.format( date ).split( "-" )[0];
 
        if ( currentDate.equalsIgnoreCase( "02" ) || currentDate.equalsIgnoreCase( "08" ))
        {
            String subject = "Doctor Diary Reminder";
            String dataEntryUserMessage = "Previous month will get disabled for data entry on 10th.";
            String approvalUserMessage = "Previous month will be frozen and auto approved on 21st of the month";
            userEMailList = new HashSet<String>( getUserEmailList( UPHMIS_DOCTORS_DIARY_USER_GROUP_ID ) );
            approvalEMailList = new HashSet<String>( getUserEmailList( UPHMIS_DOCTORS_DIARY_APPROVAL_USER_GROUP_ID ) );
            
            userMobileNumberList = new ArrayList<String>( getUserMobleNoList( UPHMIS_DOCTORS_DIARY_USER_GROUP_ID ) );
            approvalMobileNumberList = new ArrayList<String>( getUserMobleNoList( UPHMIS_DOCTORS_DIARY_APPROVAL_USER_GROUP_ID )  );
            
           
            dataEntryUserMessage  += "\n\n Thanks & Regards, ";
            dataEntryUserMessage  += "\n UPHMIS Doctor Dairy Team ";
            
            approvalUserMessage  += "\n\n Thanks & Regards, ";
            approvalUserMessage  += "\n UPHMIS Doctor Dairy Team ";
            
            OutboundMessageResponse emailResponsedataEntryUser = emailService.sendEmail( subject, dataEntryUserMessage, userEMailList );
            emailResponseHandler( emailResponsedataEntryUser );
            OutboundMessageResponse emailResponseapprovalUser = emailService.sendEmail( subject, approvalUserMessage, approvalEMailList );
            emailResponseHandler( emailResponseapprovalUser );
            
            //sendEmailOneByOne( subject, userEMailList, dataEntryUserMessage );
            //sendEmailOneByOne( subject, approvalEMailList, approvalUserMessage );
            
            sendSMS( dataEntryUserMessage, userMobileNumberList );
            sendSMS( approvalUserMessage, approvalMobileNumberList );
        }
        
        if( currentDate.equalsIgnoreCase( "15" ) || currentDate.equalsIgnoreCase( "18" ) )
        {
            String subject = "Doctor Diary Reminder";
            String approvalUserMessage = "Previous month will be frozen and auto approved on 21st of the month";
            approvalEMailList = new HashSet<String>( getUserEmailList( UPHMIS_DOCTORS_DIARY_APPROVAL_USER_GROUP_ID ) );
            
            approvalMobileNumberList = new ArrayList<String>( getUserMobleNoList( UPHMIS_DOCTORS_DIARY_APPROVAL_USER_GROUP_ID )  );
            approvalUserMessage  += "\n\n Thanks & Regards, ";
            approvalUserMessage  += "\n UPHMIS Doctor Dairy Team ";
            
            OutboundMessageResponse emailResponseapprovalUser = emailService.sendEmail( subject, approvalUserMessage, approvalEMailList );
            
            emailResponseHandler( emailResponseapprovalUser );
            sendSMS( approvalUserMessage, approvalMobileNumberList );
        }
        
        /*
        Set<String> recipients = new HashSet<>();
        recipients.add( "mithilesh.hisp@gmail.com" );
        recipients.add( "mithilesh.thakur@hispindia.org" );
        recipients.add( "harsh.atal@hispindia.org" );
         
        String finalMessage = "";
        finalMessage = "Dear User,";
        finalMessage  += "\n\n Your data has been Auto-Approved for " + "2019-01-01" + ".";
        finalMessage  += "\n Thank you for using UPHMIS Doctor Dairy application." ;
        finalMessage  += "\n\n Thanks & Regards, ";
        finalMessage  += "\n UPHMIS Doctor Dairy Team ";
        
        OutboundMessageResponse emailResponse = emailService.sendEmail( "Doctor Dairy Data Approval Status", finalMessage, recipients );
        */
        
        if( currentDate.equalsIgnoreCase( "21" ) )
        {
            autoApproveTrackedEntityDataValue();
            // sending e-mail one by one
            if( autoApprovedEMailList != null && autoApprovedEMailList.size() > 0 )
            {
                for( String eventDateAndEmail : autoApprovedEMailList )
                {
                    String eventDate = eventDateAndEmail.split( ":" )[0];
                    String email = eventDateAndEmail.split( ":" )[1];
                    String subject = "Doctor Dairy Data Approval Status";
                    
                    if( email != null && !email.equalsIgnoreCase( "" ) && isValidEmail( email ) )
                    {
                        String finalMessage = "";
                        finalMessage = "Dear User,";
                        finalMessage  += "\n\n Your data has been Auto-Approved for " + eventDate + ".";
                        finalMessage  += "\n Thank you for using UPHMIS Doctor Dairy application." ;
                        finalMessage  += "\n\n Thanks & Regards, ";
                        finalMessage  += "\n UPHMIS Doctor Dairy Team ";
                        
                        OutboundMessageResponse emailResponse = emailMessageSender.sendMessage( subject, finalMessage, email );
                        emailResponseHandler( emailResponse );
                    }
                }
            }
        }
        
        System.out.println("INFO: Scheduler job has ended at : " + new Date() );
    }

    //--------------------------------------------------------------------------------
    // Get ProgramStageInstanceIds
    //--------------------------------------------------------------------------------
    // for doctor diary
    public List<String> programStageInstanceIdsAndDataValue()
    {
        List<String> programStageInstanceIdsAndDataValue = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime( new Date() );
        calendar.set( Calendar.MONTH, (calendar.get( Calendar.MONTH ) - 1 ) );
        Date perivousMonth = calendar.getTime();
        
        Calendar lastDatePreviousMonth = Calendar.getInstance();
        lastDatePreviousMonth.setTime( perivousMonth );
        lastDatePreviousMonth.set( Calendar.DAY_OF_MONTH, calendar.getActualMaximum( Calendar.DAY_OF_MONTH ) );
        Date lastDatePreviousMonthDate = lastDatePreviousMonth.getTime();
        
        String endDatePreviousMonth = simpleDateFormat.format( lastDatePreviousMonthDate );
        
        String startDatePreviousMonth = simpleDateFormat.format( lastDatePreviousMonthDate ).split( "-" )[0] + "-" + simpleDateFormat.format( lastDatePreviousMonthDate ).split( "-" )[1] + "-01";

        System.out.println(startDatePreviousMonth +  "  = " + endDatePreviousMonth );
        
        //SELECT trackedentityinstanceid, value FROM trackedentityattributevalue WHERE CURRENT_DATE > value::date and trackedentityattributeid = 1085;
        try
        {
            String query = "SELECT psi.programstageinstanceid, psi.executiondate::date, pi.trackedentityinstanceid, " +
                           "teav.value,us.username,usinfo.email from programstageinstance psi  " +
                           "INNER JOIN programinstance pi ON pi.programinstanceid = psi.programinstanceid " +
                           "LEFT JOIN trackedentityattributevalue teav ON teav.trackedentityinstanceid = pi.trackedentityinstanceid " +
                           "LEFT JOIN users us ON teav.value = us.username " +
                           "LEFT JOIN userinfo usinfo ON usinfo.userinfoid = us.userid " +
                           "WHERE psi.programstageid in (  "+ DOC_DIARY_PROGRAM_STAGE_IDS +" )  AND " +
                           "psi.executiondate::date BETWEEN '" + startDatePreviousMonth + "' AND '" + endDatePreviousMonth + "'  AND psi.status = 'COMPLETED' " +
                           "and teav.trackedentityattributeid  = " + TEIA_USER_NAME_ID + " order by psi.completeddate desc; ";
             
            
            /*
            SELECT psi.programstageinstanceid, psi.executiondate::date, pi.trackedentityinstanceid, teav.value from programstageinstance psi
            INNER JOIN programinstance pi ON pi.programinstanceid = psi.programinstanceid
            INNER JOIN trackedentityattributevalue teav ON teav.trackedentityinstanceid = pi.trackedentityinstanceid
            WHERE psi.programstageid in 
            ( SELECT programstageid from programstage where  programid in ( 73337033 ) ) 
            AND psi.executiondate::date BETWEEN '2019-08-01' AND '2019-08-31'  
            AND psi.status = 'COMPLETED' and teav.trackedentityattributeid  = 76755184 order by psi.completeddate desc
                        
            String query = "SELECT psi.programstageinstanceid, psi.completeddate from programstageinstance psi  " +
                "WHERE psi.programstageid in ( SELECT programstageid from programstage where  programid in ( "+ UPHMIS_DOCTORS_DIARY_PROGRAM_ID +" ) ) AND " +
                "psi.completeddate <= CURRENT_DATE - interval '30 day' AND psi.status = 'COMPLETED' order by psi.completeddate desc; ";
            
            String query = "SELECT psi.programstageinstanceid, psi.completeddate from programstageinstance psi  " +
                            "WHERE psi.programstageid in ( SELECT programstageid from programstage where  programid in ( "+ UPHMIS_DOCTORS_DIARY_PROGRAM_ID +" ) ) AND " +
                            "psi.completeddate::date BETWEEN '" + startDatePreviousMonth + "' AND '" + endDatePreviousMonth + "'  AND psi.status = 'COMPLETED' order by psi.completeddate desc; ";
            */
            
            System.out.println( "query = " + query );
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );
            
            //System.out.println( "-- RS " + rs.toString() + " -- " + rs.isFirst() + " -- " + rs.next() ) ;
            
            while ( rs.next() )
            {
                Integer psiId = rs.getInt( 1 );
                String eventDate = rs.getString( 2 );
                Integer teiId = rs.getInt( 3 );
                String teav = rs.getString( 4 );
                String userName = rs.getString( 5 );
                String userEmail = rs.getString( 6 );
                //System.out.println( i + " -- psi Id added " + psiId ) ;
                if ( psiId != null )
                {
                    ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( psiId );
                    DataElement de = dataElementService.getDataElement( CURRENT_STATUS_DOC_DIARY_DATAELEMENT_ID );
                    if( psi != null && de != null)
                    {
                        TrackedEntityDataValue teDataValue = trackedEntityDataValueService.getTrackedEntityDataValue( psi, de );
                        if( teDataValue != null && teDataValue.getValue().equalsIgnoreCase( "Pending1" ))
                        {
                            programStageInstanceIdsAndDataValue.add( psi.getId() + ":" + "Auto-Approved" + ":" + eventDate + ":" + userEmail );
                        }
                        else if( teDataValue != null && teDataValue.getValue().equalsIgnoreCase( "Pending2" ) )
                        {
                            programStageInstanceIdsAndDataValue.add( psi.getId() + ":" + "Auto-Approved" + ":" + eventDate + ":" + userEmail  );
                        }
                    }
                }
            }

            return programStageInstanceIdsAndDataValue;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal ProgramStage ids", e );
        }
    }    
    
    
    // user E-mail
    public List<String> getUserEmailList( String userGroupIds )
    {
        List<String> emailList = new ArrayList<>();
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
                String eMail = rs.getString( 4 );
                if( eMail != null && isValidEmail( eMail ) )
                {
                    emailList.add( eMail );
                }
            }
            return emailList;
        }
        
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal UserGroupId ids", e );
        }
    }        
    
    // user Mobile no
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
    
    // e-mail validation
    public boolean isValidEmail(String email) 
    { 
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+ 
                            "[a-zA-Z0-9_+&*-]+)*@" + 
                            "(?:[a-zA-Z0-9-]+\\.)+[a-z" + 
                            "A-Z]{2,7}$"; 
                              
        Pattern pat = Pattern.compile(emailRegex); 
        if (email == null)
        {
            return false; 
        }
            
        return pat.matcher(email).matches(); 
    }
    
    // sending message to multiple mobile no
    public void sendSMS( String message, List<String> phonenos ) throws MalformedURLException, IOException
    {
        String phoneNo = null;
        String response = "";
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
    
    // autoApproveTrackedEntityDataValue
    public void autoApproveTrackedEntityDataValue() 
    { 
        
        List<String> programStageInstanceIdsAndDataValue = new ArrayList<String>( programStageInstanceIdsAndDataValue() );
        
        /*
        System.out.println( " PSI Size -- " + programStageInstanceIdsAndDataValue.size());
        for( String psiIdDataValue : programStageInstanceIdsAndDataValue )
        {
            String psiId = psiIdDataValue.split( ":" )[0];
            String value = psiIdDataValue.split( ":" )[1];
            System.out.println( psiId + " -- " + value );
        }
        */
        
        String storedBy = "admin";
       
        String importStatus = "";
        Integer updateCount = 0;
        Integer insertCount = 0;
       
        Date sqldate = new Date();
        java.sql.Timestamp lastUpdatedDate = new Timestamp( sqldate.getTime() );
        java.sql.Timestamp createdDate = new Timestamp( sqldate.getTime() );
        //System.out.println( new Timestamp(date.getTime() ) );

        String insertQuery = "INSERT INTO trackedentitydatavalue ( programstageinstanceid, dataelementid, value, providedelsewhere, storedby, created, lastupdated ) VALUES ";
        String updateQuery = "";
        //String value = "Auto-Approved";
        int insertFlag = 1;
        int count = 1;
        autoApprovedEMailList = new ArrayList<String>();
        if( programStageInstanceIdsAndDataValue != null && programStageInstanceIdsAndDataValue.size() > 0 )
        {
            try
            {
                for( String psiIdDataValue : programStageInstanceIdsAndDataValue )
                {
                    String psiId = psiIdDataValue.split( ":" )[0];
                    String value = psiIdDataValue.split( ":" )[1];
                    
                    String email = psiIdDataValue.split( ":" )[3];
                    
                    if( email != null && !email.equalsIgnoreCase( "" ) && isValidEmail( email ) )
                    {
                        String eventDateEmail = psiIdDataValue.split( ":" )[2] + ":" + psiIdDataValue.split( ":" )[3];
                        autoApprovedEMailList.add( eventDateEmail );
                    }
                    
                    updateQuery = "SELECT value FROM trackedentitydatavalue WHERE dataelementid = " + CURRENT_STATUS_DOC_DIARY_DATAELEMENT_ID + " AND programstageinstanceid = " + psiId;
                    
                    SqlRowSet updateSqlResultSet = jdbcTemplate.queryForRowSet( updateQuery );
                    if ( updateSqlResultSet != null && updateSqlResultSet.next() )
                    {
                        String tempUpdateQuery = "UPDATE trackedentitydatavalue SET value = '" + value + "', storedby = '" + storedBy + "',lastupdated='" + lastUpdatedDate + 
                                                  "' WHERE dataelementid = " + CURRENT_STATUS_DOC_DIARY_DATAELEMENT_ID + " AND programstageinstanceid = " + psiId;

                        jdbcTemplate.update( tempUpdateQuery );
                        
                        updateCount++;
                    }
                    else
                    {
                        insertQuery += "( " + psiId + ", " + CURRENT_STATUS_DOC_DIARY_DATAELEMENT_ID + ", '" + value + "', false ,'" + storedBy + "', '" + createdDate + "', '" + lastUpdatedDate + "' ), ";
                        insertFlag = 2;
                        insertCount++;
                    }
                    
                    if ( count == 1000 )
                    {
                        count = 1;

                        if ( insertFlag != 1 )
                        {
                            insertQuery = insertQuery.substring( 0, insertQuery.length() - 2 );
                            //System.out.println( " insert Query 2 -  " );
                            jdbcTemplate.update( insertQuery );
                        }

                        insertFlag = 1;

                        insertQuery = "INSERT INTO trackedentitydatavalue ( programstageinstanceid, dataelementid, value, providedelsewhere, storedby, created, lastupdated ) VALUES ";
                    }
                    count++;
                }
                //System.out.println(" Count - "  + count + " -- Insert Count : " + insertCount + "  Update Count -- " + updateCount );
                if ( insertFlag != 1 )
                {
                    insertQuery = insertQuery.substring( 0, insertQuery.length() - 2 );
                    //System.out.println(" insert Query 1 -  ");
                    jdbcTemplate.update( insertQuery );
                }
                
                importStatus = "Successfully populated tracker data : "; 
                importStatus += "<br/> Total new records : " + insertCount;
                importStatus += "<br/> Total updated records : " + updateCount;
                
                //System.out.println( importStatus );     
                
            }
            catch ( Exception e )
            {
                importStatus = "Exception occured while import, please check log for more details" + e.getMessage();
            }
        }
        
        System.out.println("ImportStatus : " + importStatus + " PSI Size -- " + programStageInstanceIdsAndDataValue.size() );
    }
    
    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private void emailResponseHandler( OutboundMessageResponse emailResponse )
    {
        if ( emailResponse.isOk() )
        {
            log.info( WebMessageUtils.ok( "Email sent" ) );
        }
        else
        {
            log.info( WebMessageUtils.ok( "Email sending failed" ) );
        }
    }
    
    
    
}