package org.hisp.dhis.excelimport.dataserassigneditor.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.databrowser.MetaValue;
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
public class DefinedMultiAssociationsDataSetAssignEditorAction implements Action
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
    // Parameters
    // -------------------------------------------------------------------------

    private Integer orgUnitId;

    private Integer[] dataSetIds;

    private Boolean[] statuses;

    private boolean checked;

    private OrganisationUnit source;

    private List<MetaValue> metaItems = new ArrayList<>();

    private Map<Integer, String> itemMaps = new HashMap<>();

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public void setOrgUnitId( Integer orgUnitId )
    {
        this.orgUnitId = orgUnitId;
    }

    public void setDataSetIds( Integer[] dataSetIds )
    {
        this.dataSetIds = dataSetIds;
    }

    public void setStatuses( Boolean[] statuses )
    {
        this.statuses = statuses;
    }

    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked( boolean checked )
    {
        this.checked = checked;
    }

    public List<MetaValue> getMetaItems()
    {
        return metaItems;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public Map<Integer, String> getItemMaps()
    {
        return itemMaps;
    }

    public OrganisationUnit getSource()
    {
        return source;
    }

    // -------------------------------------------------------------------------
    // Action implement
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        String title = "";

        if ( checked )
        {
            title = i18n.getString( "assigned" ) + SEPERATE;
        }
        else
        {
            title = i18n.getString( "unassigned" ) + SEPERATE;
        }

        if ( dataSetIds.length == statuses.length )
        {
            source = organisationUnitService.getOrganisationUnit( orgUnitId );

            for ( int i = 0; i < dataSetIds.length; i++ )
            {
                DataSet dataSet = dataSetService.getDataSet( dataSetIds[i] );

                itemMaps.put( i, title + dataSet.getName() + SEPERATE + source.getName() );
                
                metaItems.add( new MetaValue( orgUnitId, dataSet.getId() + "", String.valueOf( checked ) ) );

                if ( (checked && !statuses[i]) )
                {
                    dataSet.getSources().add( source );

                    dataSetService.updateDataSet( dataSet );
                }
                else if ( (!checked && statuses[i]) )
                {
                    dataSet.getSources().remove( source );

                    dataSetService.updateDataSet( dataSet );
                }
            }
        }

        return SUCCESS;
    }
}
