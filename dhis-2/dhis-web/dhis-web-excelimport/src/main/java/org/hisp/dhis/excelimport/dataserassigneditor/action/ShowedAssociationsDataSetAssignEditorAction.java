package org.hisp.dhis.excelimport.dataserassigneditor.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.databrowser.MetaValue;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class ShowedAssociationsDataSetAssignEditorAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SelectionTreeManager selectionTreeManager;
    /*
    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }
    */
    
    @Autowired
    private DataSetService dataSetService;

    /*
    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
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
    // Output
    // -------------------------------------------------------------------------

    private Grid grid = new ListGrid();

    public Grid getGrid()
    {
        return grid;
    }

    public List<Object> getMetaValues()
    {
        return grid.getColumn( 0 );
    }

    public Map<Integer, List<Object>> getMetaValueMaps()
    {
        Map<Integer, List<Object>> maps = new Hashtable<>();

        for ( List<Object> row : grid.getRows() )
        {
            if ( !row.isEmpty() && row.size() > 1 )
            {
                maps.put( ((MetaValue) row.get( 0 )).getId(), row.subList( 1, row.size() ) );
            }
        }

        return maps;
    }

    public List<String> getHeaderIds()
    {
        List<String> ids = new ArrayList<>();

        for ( GridHeader header : grid.getVisibleHeaders() )
        {
            ids.add( header.getColumn() );
        }

        ids.remove( 0 );

        return ids;
    }

    // -------------------------------------------------------------------------
    // Action implement
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        OrganisationUnit parent = selectionTreeManager.getReloadedSelectedOrganisationUnit();

        if ( parent == null )
        {
            return SUCCESS;
        }

        Set<DataSet> assignedDataSets = null;

        List<DataSet> dataSets = new ArrayList<>( dataSetService.getAllDataSets() );

        List<OrganisationUnit> children = new ArrayList<>();

        if ( parent.getChildren() == null || parent.getChildren().isEmpty() )
        {
            children.add( parent );
        }
        else
        {
            children.addAll( parent.getChildren() );
        }

        //Collections.sort( dataSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataSets );

        //Collections.sort( children, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( children );

        grid.addHeader( new GridHeader( i18n.getString( "organisation_units" ), false, true ) );

        for ( DataSet dataSet : dataSets )
        {
            //grid.addHeader( new GridHeader( dataSet.getDisplayShortName(), dataSet.getId() + "", dataSet.getDisplayName(), false, false ) );
        }

        for ( OrganisationUnit child : children )
        {
            assignedDataSets = new HashSet<>( child.getDataSets() );

            grid.addRow().addValue( new MetaValue( child.getId(), child.getDisplayName() ) );

            for ( DataSet dataSet : dataSets )
            {
                if ( assignedDataSets.contains( dataSet ) )
                {
                    grid.addValue( new MetaValue( child.getId(), dataSet.getId() + "", "true" ) );
                }
                else
                {
                    grid.addValue( new MetaValue( child.getId(), dataSet.getId() + "", "false" ) );
                }
            }
        }

        return SUCCESS;
    }
}
