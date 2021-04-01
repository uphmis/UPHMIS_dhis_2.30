package org.hisp.dhis.excelimport.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class CompleteDatasetRegistrationAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CurrentUserService currentUserService;
    
    @Autowired
    private I18nManager i18nManager;
    
    @Autowired
    private CategoryService categoryService;

    
    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    
    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------
    
    private String organisationUnitUid;
    
    public void setOrganisationUnitUid( String organisationUnitUid )
    {
        this.organisationUnitUid = organisationUnitUid;
    }

    private String startDate;

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    private String endDate;

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }
    
    private boolean dataSetCompleteRegistration;
    
    public void setDataSetCompleteRegistration( boolean dataSetCompleteRegistration )
    {
        this.dataSetCompleteRegistration = dataSetCompleteRegistration;
    }

    private Date sDate;

    private Date eDate;
    
    private String weeklyPeriodTypeName;
    
    private String message;

    public String getMessage()
    {
        return message;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute() throws Exception
    {
        
        // ---------------------------------------------------------------------
        // Register as completed data set
        // ---------------------------------------------------------------------
        
        System.out.println(organisationUnitUid +  " : " + startDate +  " : " + endDate );
        
        System.out.println(   " dataSetCompleteRegistration : " + dataSetCompleteRegistration );
        
        message = "";
        // Period Info
        sDate = format.parseDate( startDate );
        eDate = format.parseDate( endDate );
        
        List<Period> periodList = new ArrayList<Period>();
        
        weeklyPeriodTypeName = WeeklyPeriodType.NAME;
        PeriodType periodType = periodService.getPeriodTypeByName( weeklyPeriodTypeName );
        
        //periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates( periodType, sDate, eDate ) );
        periodList = new ArrayList<Period>( periodService.getIntersectingPeriodsByPeriodType( periodType, sDate, eDate ) );
        
        // OrganisationUnit Info
        OrganisationUnit selectedOrgUnit = organisationUnitService.getOrganisationUnit( organisationUnitUid );
        
        Set<OrganisationUnit> children = selectedOrgUnit.getChildren();

        String storedBy = currentUserService.getCurrentUsername();
        
        if( dataSetCompleteRegistration )
        {
            if( selectedOrgUnit != null )
            {
                if( children != null && children.size() > 0 )
                {
                    for ( OrganisationUnit unit : children )
                    {
                        Set<DataSet> dataSets = unit.getDataSets();
                        
                        if( dataSets != null && dataSets.size() > 0  )
                        {
                            for ( DataSet ds : dataSets )
                            {
                                if( periodList != null && periodList.size() > 0 )
                                {
                                    for ( Period period : periodList )
                                    {
                                        System.out.println( "Complete Registration for Children -- " + ds.getId() +  " : " + unit.getId() +  " : " + period.getId() );
                                        registerCompleteDataSet( ds, period, unit, storedBy );
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    Set<DataSet> dataSets = selectedOrgUnit.getDataSets();
                    
                    if( dataSets != null && dataSets.size() > 0  )
                    {
                        for ( DataSet ds : dataSets )
                        {
                            if( periodList != null && periodList.size() > 0 )
                            {
                                for ( Period period : periodList )
                                {
                                    System.out.println( "Complete Registration for selected OrgUnit -- " + ds.getId() +  " : " + selectedOrgUnit.getId() +  " : " + period.getId() );
                                    registerCompleteDataSet( ds, period, selectedOrgUnit, storedBy );
                                }
                            }
                        }
                    }
                }
                message = "Dataset Complete Successfully";
            }
        }
        
        else
        {
            if( selectedOrgUnit != null )
            {
                if( children != null && children.size() > 0 )
                {
                    for ( OrganisationUnit unit : children )
                    {
                        Set<DataSet> dataSets = unit.getDataSets();
                        
                        if( dataSets != null && dataSets.size() > 0  )
                        {
                            for ( DataSet ds : dataSets )
                            {
                                if( periodList != null && periodList.size() > 0 )
                                {
                                    for ( Period period : periodList )
                                    {
                                        System.out.println( "InComplete Registration for Children -- " + ds.getId() +  " : " + unit.getId() +  " : " + period.getId() );
                                        unRegisterCompleteDataSet( ds, period, unit );
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    Set<DataSet> dataSets = selectedOrgUnit.getDataSets();
                    
                    if( dataSets != null && dataSets.size() > 0  )
                    {
                        for ( DataSet ds : dataSets )
                        {
                            if( periodList != null && periodList.size() > 0 )
                            {
                                for ( Period period : periodList )
                                {
                                    System.out.println( "InComplete Registration for selected OrgUnit -- " + ds.getId() +  " : " + selectedOrgUnit.getId() +  " : " + period.getId() );
                                    unRegisterCompleteDataSet( ds, period, selectedOrgUnit );
                                }
                            }
                        }
                    }
                }
                message = "Dataset incomplete Successfully";
            }
        }
        
        return SUCCESS;
    }

    private void registerCompleteDataSet( DataSet dataSet, Period period, OrganisationUnit organisationUnit, String storedBy )
    {
        I18nFormat format = i18nManager.getI18nFormat();
        
        CompleteDataSetRegistration registration = new CompleteDataSetRegistration();
        
        CategoryOptionCombo defaultAttributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        
        
        if ( registrationService.getCompleteDataSetRegistration( dataSet, period, organisationUnit, defaultAttributeOptionCombo ) == null )
        {
            registration.setDataSet( dataSet );
            registration.setPeriod( period );
            registration.setSource( organisationUnit );
            registration.setDate( new Date() );
            registration.setStoredBy( storedBy );

            registration.setPeriodName( format.formatPeriod( registration.getPeriod() ) );

            //registrationService.saveCompleteDataSetRegistration( registration, true );
            
            registrationService.saveCompleteDataSetRegistration( registration );
        }
    }
    
    private void unRegisterCompleteDataSet( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {   
        //DataElementCategoryOptionCombo defaultAttributeOptionCombo = dataElementCategoryService.getDefaultDataElementCategoryOptionCombo();
        CategoryOptionCombo defaultAttributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dataSet, period, organisationUnit, defaultAttributeOptionCombo );
        if ( registration != null )
        {
            registrationService.deleteCompleteDataSetRegistration( registration );
        }
    }
    
    
}
