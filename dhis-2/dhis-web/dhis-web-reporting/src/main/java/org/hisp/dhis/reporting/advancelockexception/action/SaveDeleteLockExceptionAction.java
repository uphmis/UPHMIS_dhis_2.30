package org.hisp.dhis.reporting.advancelockexception.action;


import static org.hisp.dhis.commons.util.ConversionUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class SaveDeleteLockExceptionAction implements Action
{
    private static final Log log = LogFactory.getLog( SaveDeleteLockExceptionAction.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }
    
    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }
    
    private HibernateCacheManager cacheManager;

    public void setCacheManager( HibernateCacheManager cacheManager )
    {
        this.cacheManager = cacheManager;
    }
    
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Collection<Integer> selectedPeriods = new ArrayList<Integer>();

    public void setSelectedPeriods( Collection<Integer> selectedPeriods )
    {
        this.selectedPeriods = selectedPeriods;
    }

    private Collection<Integer> selectedDataSets = new ArrayList<Integer>();

    public void setSelectedDataSets( Collection<Integer> selectedDataSets )
    {
        this.selectedDataSets = selectedDataSets;
    }

    private boolean selectBetweenLockException;

    public void setSelectBetweenLockException( boolean selectBetweenLockException )
    {
        this.selectBetweenLockException = selectBetweenLockException;
    }
    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    List<OrganisationUnit> selectedOrganisationUnits = new ArrayList<OrganisationUnit>();
    
    List<OrganisationUnit> selectedDataSetsSources = new ArrayList<OrganisationUnit>();
    
    private List<Period> periods = new ArrayList<Period>();

    private List<DataSet> dataSets = new ArrayList<DataSet>();
    
    private String message;

    public String getMessage()
    {
        return message;
    }
    
    private String expireOnDate;
    
    public void setExpireOnDate( String expireOnDate )
    {
        this.expireOnDate = expireOnDate;
    }

    String organisationUnitIdsByComma = "-1";
    String dataSetIdsByComma = "-1";
    String periodIdsByComma = " -1";
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------
    
    public String execute()
    {
        for ( Integer periodId : selectedPeriods )
        {
            periods.add( periodService.getPeriod( periodId.intValue() ) );
        }

        for ( Integer dataSetId : selectedDataSets )
        {
            dataSets.add( dataSetService.getDataSet( dataSetId.intValue() ) );
        }
        
        selectedOrganisationUnits = new ArrayList<OrganisationUnit>( selectionTreeManager.getSelectedOrganisationUnits() );
        
        if ( selectedOrganisationUnits == null && selectedOrganisationUnits.size() == 0 )
        {
            return INPUT;
        }
        
        if ( dataSets == null || periods == null )
        {
            return ERROR;
        }
        
        /*
        if( dataSets != null && dataSets.size() == 1 )
        {
            selectedDataSetsSources.addAll( dataSetService.getDataSet( dataSetId.intValue() ).getSources() );
            selectedOrganisationUnits.retainAll( selectedDataSetsSources );
        }
        */
        
        Collection<Integer> organisationUnitIds = new ArrayList<>( getIdentifiers( OrganisationUnit.class, selectedOrganisationUnits ) );        
        organisationUnitIdsByComma = getCommaDelimitedString( organisationUnitIds );

        System.out.println( " Start Time is : " + new Date() + " expireOnDate - " + expireOnDate );
        
        Integer insertCount = 0;
        int insertFlag = 1;
        int count = 1;
        String insertQuery = "INSERT INTO lockexception ( lockexceptionid, organisationunitid, periodid, datasetid, expiredate ) VALUES ";
        
        if ( selectBetweenLockException )
        {
            try
            {
                for ( String id : organisationUnitIdsByComma.split( "," ) )
                {
                    OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( Integer.parseInt( id.trim() ) );
                    if ( organisationUnit != null )
                    {
                        for ( DataSet  dataSet : dataSets )
                        {
                            for ( Period  period : periods )
                            {
                                if ( organisationUnit.getDataSets().contains( dataSet ) )
                                {
                                    if( !getLockExceptionByOrgUnitPeriodDataSet( organisationUnit.getId(), period.getId(), dataSet.getId() ) )
                                    {
                                        insertQuery += "( nextval('hibernate_sequence') " + ", " +organisationUnit.getId() + ", " +period.getId() +", "+ dataSet.getId()+ " , '" + expireOnDate + "' ), ";
                                        insertFlag = 2;
                                        insertCount++;
                                    }
                                }
                                else
                                {
                                    if ( log.isDebugEnabled() )
                                    {
                                        log.debug( "OrganisationUnit " + organisationUnit.getName() + " does not contain DataSet " +
                                            dataSet.getName() + ", ignoring." );
                                    }
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
                                    insertQuery = "INSERT INTO lockexception ( lockexceptionid, organisationunitid, periodid, datasetid, expiredate ) VALUES ";
                                }

                                count++;
                            }
                        }
                    }
                }
                
                if ( insertFlag != 1 )
                {
                    insertQuery = insertQuery.substring( 0, insertQuery.length() - 2 );
                    //System.out.println(" insert Query 1 -  ");
                    jdbcTemplate.update( insertQuery );
                }
                message = i18n.getString( "lock_exception_successfully_created" );
                message += "<br/> Total new records : " + insertCount;
            }
            catch ( Exception e )
            {
                message = "Exception occured while import, please check log for more details" + e.getMessage();
            }
 
            /*
            for ( String id : organisationUnitIdsByComma.split( "," ) )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( Integer.parseInt( id.trim() ) );

                if ( organisationUnit != null )
                {
                    for ( DataSet  dataSet : dataSets )
                    {
                        for ( Period  period : periods )
                        {
                            if ( organisationUnit.getDataSets().contains( dataSet ) )
                            {
                                if( !getLockExceptionByOrgUnitPeriodDataSet( organisationUnit.getId(), period.getId(), dataSet.getId() ) )
                                {
                                    LockException lockException = new LockException();

                                    lockException.setOrganisationUnit( organisationUnit );
                                    lockException.setDataSet( dataSet );
                                    lockException.setPeriod( period );
                                    dataSetService.addLockException( lockException );
                                    
                                    //insertQuery += "("+organisationUnit.getId()+","+period.getId()+","+dataSet.getId()+"), ";
                                }
                            }
                            else
                            {
                                if ( log.isDebugEnabled() )
                                {
                                    log.debug( "OrganisationUnit " + organisationUnit.getName() + " does not contain DataSet " +
                                        dataSet.getName() + ", ignoring." );
                                }
                            }
                        }
                    }
                }
            }
            
            //insertQuery = insertQuery.substring( 0, insertQuery.length()-2 );
            //jdbcTemplate.update( insertQuery );
            //reportService.createBatchLockExceptions( insertQuery );
            
            message = i18n.getString( "lock_exception_successfully_created" );
          */  
        }
        else
        {
            //reportService.deleteLockException( organisationUnitIdsByComma, periodIdsByComma, dataSetIdsByComma );
            batchDeleteLockException();
            System.out.println( " end delete Time is : " + new Date() );
            /*
            for ( String id : organisationUnitIdsByComma.split( "," ) )
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( Integer.parseInt( id.trim() ) );

                if ( organisationUnit != null )
                {
                    for ( DataSet  dataSet : dataSets )
                    {
                        for ( Period  period : periods )
                        {
                            if ( organisationUnit.getDataSets().contains( dataSet ) )
                            {   
                                deleteLockException( dataSet.getId(), period.getId(), organisationUnit.getId() );
                            }
                        }
                    }
                }
            }
            */
            message = i18n.getString( "lock_exception_successfully_deleted" );
        }
        cacheManager.clearCache();
        System.out.println( " End Time is : " + new Date() );
        
        return SUCCESS;
    }
    
    
    // Supportive methods
    
    
    // get Lock Exception from organisationUnitId,periodId,dataSetId
    public Boolean getLockExceptionByOrgUnitPeriodDataSet( Integer organisationUnitId, Integer periodId, Integer dataSetId )
    {
        Boolean recordExist = false;
        
        try
        {
            String query = "SELECT organisationunitid,periodid,datasetid FROM lockexception WHERE organisationunitid = " + organisationUnitId + " AND "
                + " periodid = " + periodId + " AND datasetid = " + dataSetId;

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                recordExist = true;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        
        return recordExist;
    }   
    
    //delete lock exception
    public void deleteLockException( Integer dataSetId, Integer periodId, Integer orgUnitId )
    {
        try
        {
            String query = "DELETE FROM lockexception WHERE organisationunitid = " + orgUnitId + "  AND periodid  = " + periodId +  " AND datasetid = " + dataSetId +"";
            
            //System.out.println( " query "  + query);
            
            jdbcTemplate.execute( query );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    } 
    
    //batch deletion lock exception
    public void batchDeleteLockException()
    {
        System.out.println( " start delete Time is : " + new Date() );
        Collection<Integer> dataSetIds = new ArrayList<>( getIdentifiers( DataSet.class, dataSets ) );        
        dataSetIdsByComma = getCommaDelimitedString( dataSetIds );
        
        Collection<Integer> periodIds = new ArrayList<>( getIdentifiers( Period.class, periods ) );        
        periodIdsByComma = getCommaDelimitedString( periodIds );

        try
        {
            String query = "DELETE FROM lockexception WHERE organisationunitid IN ( " + organisationUnitIdsByComma + " ) "
                + " AND periodid IN ( " + periodIdsByComma + " ) " + " AND datasetid IN ( " + dataSetIdsByComma + " ) ";
            
            
            //System.out.println( " query "  + query);
            
            jdbcTemplate.execute( query );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    } 
    
    
}
