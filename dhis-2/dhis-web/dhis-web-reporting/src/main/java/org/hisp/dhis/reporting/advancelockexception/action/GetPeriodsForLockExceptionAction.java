package org.hisp.dhis.reporting.advancelockexception.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
//import org.hisp.dhis.period.comparator.PeriodComparator;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GetPeriodsForLockExceptionAction implements Action
{
    private final static String ALL = "ALL";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------

    private String periodTypeName;

    public String getPeriodTypeName()
    {
        return periodTypeName;
    }

    public void setPeriodTypeName( String periodTypeName )
    {
        this.periodTypeName = periodTypeName;
    }

    private List<Period> periods = new ArrayList<Period>();

    public List<Period> getPeriods()
    {
        return periods;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        periods = new ArrayList<Period>();
        
        if ( periodTypeName == null || periodTypeName.equals( ALL ) )
        {
            Collection<PeriodType> periodTypes = periodService.getAllPeriodTypes();
            
            List<Period> allPeriods = new ArrayList<Period>();
            for ( PeriodType type : periodTypes )
            {
                allPeriods.addAll( periodService.getPeriodsByPeriodType( type ) );
            }
            
            for ( Period p : allPeriods )
            {
                if ( !(p.getStartDate().compareTo( new Date() ) > 0) )
                {
                    periods.add( p );
                }
            }
        }
        else
        {
            PeriodType periodType = periodService.getPeriodTypeByName( periodTypeName );
            
            List<Period> allPeriodsOfSelectedPeriodType = new ArrayList<Period>( periodService.getPeriodsByPeriodType( periodType ) );

            for ( Period p : allPeriodsOfSelectedPeriodType )
            {
                if ( !(p.getStartDate().compareTo( new Date() ) > 0) )
                {
                    periods.add( p );
                }
            }
        }

        for ( Period period : periods )
        {
            period.setName( format.formatPeriod( period ) );
        }

        Collections.sort( periods, new PeriodsComparator() );
        return SUCCESS;
    }

}


