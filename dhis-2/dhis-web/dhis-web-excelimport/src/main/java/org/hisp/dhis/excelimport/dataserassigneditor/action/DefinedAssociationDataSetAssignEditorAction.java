package org.hisp.dhis.excelimport.dataserassigneditor.action;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class DefinedAssociationDataSetAssignEditorAction implements Action
{
    private static final String SEPERATE = " - ";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetService dataSetService;
    /*
    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }
    */
    @Autowired
    private OrganisationUnitService organisationUnitService;
    /*
    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }
    */
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

    private Integer orgUnitId;

    public void setOrgUnitId( Integer orgUnitId )
    {
        this.orgUnitId = orgUnitId;
    }

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private boolean assigned;

    public void setAssigned( boolean assigned )
    {
        this.assigned = assigned;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public Integer getOrgUnitId()
    {
        return orgUnitId;
    }

    public Integer getDataSetId()
    {
        return dataSetId;
    }

    public boolean isAssigned()
    {
        return assigned;
    }

    private String title;

    public String getTitle()
    {
        return title;
    }

    // -------------------------------------------------------------------------
    // Action implement
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        DataSet dataSet = dataSetService.getDataSet( dataSetId );
        OrganisationUnit source = organisationUnitService.getOrganisationUnit( orgUnitId );

        title = SEPERATE + dataSet.getName() + SEPERATE + source.getName();

        if ( assigned )
        {
            dataSet.getSources().add( source );
            source.getDataSets().add( dataSet );
            title = i18n.getString( "assigned" ) + SEPERATE + title;
        }
        else
        {
            dataSet.getSources().remove( source );
            source.getDataSets().remove( dataSet );
            title = i18n.getString( "unassigned" ) + SEPERATE + title;
        }

        dataSetService.updateDataSet( dataSet );

        return SUCCESS;
    }

}