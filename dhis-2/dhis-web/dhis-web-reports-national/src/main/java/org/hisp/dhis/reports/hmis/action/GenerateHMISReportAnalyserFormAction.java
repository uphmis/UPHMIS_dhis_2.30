package org.hisp.dhis.reports.hmis.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.reports.ReportType;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;
/**
 * @author Mithilesh Kumar Thakur
 */
public class GenerateHMISReportAnalyserFormAction implements Action
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    @Autowired
    private PeriodService periodService;

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    private Collection<PeriodType> periodTypes;

    public Collection<PeriodType> getPeriodTypes()
    {
        return periodTypes;
    }

    private String reportTypeName;

    public String getReportTypeName()
    {
        return reportTypeName;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------
    public String execute()
        throws Exception
    {
        reportTypeName = ReportType.RT_HMIS;

        periodTypes = periodService.getAllPeriodTypes();
        //List<String> reportTypes = new ArrayList<>();
        
        // Filtering Periodtypes other than Monthly, Quarterly and Yearly
        Iterator<PeriodType> periodTypeIterator = periodTypes.iterator();
        while ( periodTypeIterator.hasNext() )
        {
            PeriodType type = periodTypeIterator.next();
            if ( type.getName().equalsIgnoreCase( "Monthly" ) || type.getName().equalsIgnoreCase( "quarterly" )
                || type.getName().equalsIgnoreCase( "yearly" ) )
            {
            }
            else
            {
                periodTypeIterator.remove();
            }
        }

        return SUCCESS;
    }
}
