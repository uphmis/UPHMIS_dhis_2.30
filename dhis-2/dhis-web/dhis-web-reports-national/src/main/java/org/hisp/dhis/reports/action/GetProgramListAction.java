package org.hisp.dhis.reports.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.reports.ReportService;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 *
 * @version GetProgramListAction.java Sep 1, 2012 1:25:57 PM	
 */

public class GetProgramListAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private ReportService reportService;

    public void setReportService( ReportService reportService )
    {
        this.reportService = reportService;
    }

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------
    /*
    private String ouId;

    public void setOuId( String ouId )
    {
        this.ouId = ouId;
    }
    */
    private Integer orgUnitId;
    
    public void setOrgUnitId( Integer orgUnitId )
    {
        this.orgUnitId = orgUnitId;
    }

    private List<Program> programList;
    
    public List<Program> getProgramList()
    {
        return programList;
    }

    private String ouName;

    public String getOuName()
    {
        return ouName;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        //System.out.println( " OU ID is  : " + orgUnitId );
        if ( orgUnitId != null )
        {
            try
            {
                //OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( Integer.parseInt( ouId ) );
                OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitId );
                ouName = orgUnit.getShortName();
                //System.out.println( " OU Name is  : " + ouName );
                
                programList = new ArrayList<Program>( reportService.getProgramsByOrgUnit( orgUnit ) );
                
                //Collections.sort( programList, new IdentifiableObjectNameComparator() );
                Collections.sort( programList );
            }
            catch ( Exception e )
            {
                System.out.println( "Exception while getting Program List : " + e.getMessage() );
            }
        }
        //System.out.println( "Size of Program List is   : " + programList.size() );
        /*
        for ( Program program : programList )
        {
            System.out.println( " Program ID is  : " + program.getId() + "--Program Name is " + program.getName() );
        }
        */
        return SUCCESS;
    }
}


