package org.hisp.dhis.reporting.advancelockexception.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class ValidateLockExceptionAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
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

    private List<Integer> selectedPeriods = new ArrayList<Integer>();

    
    public void setSelectedPeriods( List<Integer> selectedPeriods )
    {
        this.selectedPeriods = selectedPeriods;
    }

    private List<Integer> selectedDataSets = new ArrayList<Integer>();

    public void setSelectedDataSets( List<Integer> selectedDataSets )
    {
        this.selectedDataSets = selectedDataSets;
    }
    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String message;

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
    {
        if ( selectedPeriods == null || selectedPeriods.size() == 0 )
        {
            message = i18n.getString( "period_not_selected" );

            return INPUT;
        }

        if ( selectedDataSets == null || selectedDataSets.size() == 0 )
        {
            message = i18n.getString( "dataset_not_selected" );

            return INPUT;
        }
        
        Collection<OrganisationUnit> selectedUnits = new HashSet<OrganisationUnit>();
        
        selectedUnits = selectionTreeManager.getSelectedOrganisationUnits();

        if ( selectedUnits == null || selectedUnits.size() == 0 )
        {
            message = i18n.getString( "organisation_not_selected" );

            return INPUT;
        }

        return SUCCESS;
    }
}
