package org.hisp.dhis.reporting.action.scheduletask;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.opensymphony.xwork2.Action;


/**
 * @author Mithilesh Kumar Thakur
 */
public class ScheduleAutoApproveTrackerData implements Action
{
    private final static int  CURRENT_STATUS_DATAELEMENT_ID = 38576348;
    private final static String PROGRAM_STAGE_IDS = "38565722,38565580,38565712,38565696";
    

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
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
    
    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------
    
 
    // -------------------------------------------------------------------------
    // Action IMplementation
    // -------------------------------------------------------------------------
    
    public String execute() throws Exception
    {
        
        System.out.println("INFO: scheduler job has started at : " + new Date() );
        
        List<Integer> programStageInstanceIds = new ArrayList<Integer>( getProgramStageInstanceIds() );
        //System.out.println( " Size of programStageInstanceIds List -- " + programStageInstanceIds.size() );
        
        String storedBy = "admin";
       
        String importStatus = "";
        Integer updateCount = 0;
        Integer insertCount = 0;
        
        /*
        long t;
        Date d = new Date();
        t = d.getTime();
        java.sql.Date lastUpdatedDate = new java.sql.Date( t );
        java.sql.Date createdDate = new java.sql.Date( t );
        */
        Date date = new Date();
        java.sql.Timestamp lastUpdatedDate = new Timestamp(date.getTime());
        java.sql.Timestamp createdDate = new Timestamp(date.getTime());
        
        //System.out.println( new Timestamp(date.getTime() ) );
        
        
        String insertQuery = "INSERT INTO trackedentitydatavalue ( programstageinstanceid, dataelementid, value, providedelsewhere, storedby, created, lastupdated ) VALUES ";
        String updateQuery = "";
        String value = "Auto-Approved";
        int insertFlag = 1;
        int count = 1;
        
        if( programStageInstanceIds != null && programStageInstanceIds.size() > 0 )
        {
            try
            {
                for( Integer psiId : programStageInstanceIds )
                {
               
                    updateQuery = "SELECT value FROM trackedentitydatavalue WHERE dataelementid = " + CURRENT_STATUS_DATAELEMENT_ID + " AND programstageinstanceid = " + psiId;
                    
                    SqlRowSet updateSqlResultSet = jdbcTemplate.queryForRowSet( updateQuery );
                    if ( updateSqlResultSet != null && updateSqlResultSet.next() )
                    {
                        String tempUpdateQuery = "UPDATE trackedentitydatavalue SET value = '" + value + "', storedby = '" + storedBy + "',lastupdated='" + lastUpdatedDate + 
                                                  "' WHERE dataelementid = " + CURRENT_STATUS_DATAELEMENT_ID + " AND programstageinstanceid = " + psiId;

                        jdbcTemplate.update( tempUpdateQuery );
                        
                        updateCount++;
                    }
                    else
                    {
                        insertQuery += "( " + psiId + ", " + CURRENT_STATUS_DATAELEMENT_ID + ", '" + value + "', false ,'" + storedBy + "', '" + createdDate + "', '" + lastUpdatedDate + "' ), ";
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
        
        System.out.println("ImportStatus : " + importStatus );
        System.out.println("INFO: Scheduler job has ended at : " + new Date() );
        
        return SUCCESS;
    }
    

    //--------------------------------------------------------------------------------
    // Get ProgramStageInstanceIds
    //--------------------------------------------------------------------------------
    public List<Integer> getProgramStageInstanceIds()
    {
        List<Integer> programStageInstanceIds = new ArrayList<Integer>();

        //String current_date = "2018-06-06";
        //String endDateOfCurrentMonth1 = "2017-12-31";
        //SELECT trackedentityinstanceid, value FROM trackedentityattributevalue WHERE CURRENT_DATE > value::date and trackedentityattributeid = 1085;
        try
        {
            String query = "SELECT psi.programstageinstanceid, psi.completeddate from programstageinstance psi  " +
                            "WHERE psi.programstageid in ( "+ PROGRAM_STAGE_IDS +" ) AND " +
                            "psi.completeddate <= CURRENT_DATE - interval '7 day' AND psi.status = 'COMPLETED' order by psi.completeddate desc; ";
          
              
            //System.out.println( "query = " + query );
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );
            
            //System.out.println( "-- RS " + rs.toString() + " -- " + rs.isFirst() + " -- " + rs.next() ) ;
            
            while ( rs.next() )
            {
                Integer psiId = rs.getInt( 1 );
                //System.out.println( i + " -- psi Id added " + psiId ) ;
                if ( psiId != null )
                {
                    ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( psiId );
                    DataElement de = dataElementService.getDataElement( CURRENT_STATUS_DATAELEMENT_ID );
                    if( psi != null && de != null)
                    {
                        TrackedEntityDataValue teDataValue = trackedEntityDataValueService.getTrackedEntityDataValue( psi, de );
                        if( teDataValue == null || teDataValue.getValue().equalsIgnoreCase( "Re-submitted" ))
                        {
                            programStageInstanceIds.add( psi.getId() );
                        }
                       
                    }
                }
            }

            return programStageInstanceIds;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal ProgramStage ids", e );
        }
    }
}

