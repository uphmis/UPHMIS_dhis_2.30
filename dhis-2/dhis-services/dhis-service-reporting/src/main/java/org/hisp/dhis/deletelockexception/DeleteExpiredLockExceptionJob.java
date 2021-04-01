package org.hisp.dhis.deletelockexception;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.cache.HibernateCacheManager;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Mithilesh Kumar Thakur
 */
public class DeleteExpiredLockExceptionJob extends AbstractJob
{
    private static final Log log = LogFactory.getLog( DeleteExpiredLockExceptionJob.class );

    private static final String KEY_TASK = "deleteExpiredLockExceptionTask";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    @Autowired
    private HibernateCacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.DELETE_EXPIRED_LOCK_EXCEPTION;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        System.out.println("INFO: Delete LOCK-EXCEPTION job has started at : " + new Date() +" -- " + JobType.DELETE_EXPIRED_LOCK_EXCEPTION );
        boolean isDeleteLockExceptionJobEnabled = (Boolean) systemSettingManager.getSystemSetting( SettingKey.DELETE_EXPIRED_LOCK_EXCEPTION );
        System.out.println( "isDeleteLockExceptionJobEnabled -- " + isDeleteLockExceptionJobEnabled );
        
        if ( !isDeleteLockExceptionJobEnabled )
        {
            log.info( String.format( "%s aborted. Delete LOCK-EXCEPTION Job are disabled", KEY_TASK ) );

            return;
        }

        log.info( String.format( "%s has started", KEY_TASK ) );
        
        deleteExpiredLockException();
        //List <OrganisationUnit> orgUnitList = new ArrayList<>( );
        String deleteFolderPath = System.getenv( "DHIS2_HOME" ) + File.separator + "ra_uphmis" + File.separator + "temp";
        
        //Creating the File object
        File fileForDelete = new File( deleteFolderPath );
        if( fileForDelete.exists() )
        {
            deleteTempFolder( fileForDelete );
            System.out.println( "temp Folder/ Files deleted ........" );
        }
        

        System.out.println("INFO: Delete LOCK-EXCEPTION job ended at : " + new Date() );
             
    }

    //--------------------------------------------------------------------------------
    // Supportive Methoda
    //--------------------------------------------------------------------------------

    //batch deletion lock exception
    public void deleteExpiredLockException()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
        try
        {
            String query = "DELETE FROM lockexception WHERE expiredate::date <= '" + simpleDateFormat.format( new Date() ) + "' ";

            System.out.println( " query "  + query);
            
            jdbcTemplate.execute( query );

        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal lockexception ids", e );
        }
        
        cacheManager.clearCache();
    }   
    
    //deleteTempFolder
    public void deleteTempFolder( File file )
    {
        for ( File subFile : file.listFiles() ) 
        {
            if( subFile.isDirectory() ) 
            {
                deleteTempFolder( subFile );
            } 
            else 
            {
               subFile.delete();
            }
         }
         file.delete();
      }     
    
}