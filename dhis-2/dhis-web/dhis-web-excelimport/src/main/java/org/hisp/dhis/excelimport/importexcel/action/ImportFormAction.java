package org.hisp.dhis.excelimport.importexcel.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

public class ImportFormAction  implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private PeriodService periodService;
    
    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;
    
    @Autowired
    private OrganisationUnitGroupSetStore organisationUnitGroupSetStore;

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    List<String> statusMsg;
    
    private List<Period> periods;

    public List<Period> getPeriods()
    {
        return periods;
    }
    
    private List<OrganisationUnit> orgunits;
    public List<OrganisationUnit> getSource()
    {
    	return orgunits;
    }

    private List<String> yearList;
    
    private List<OrganisationUnitGroup> organisationUnitGroups;
    
    public List<OrganisationUnitGroup> getOrganisationUnitGroups()
    {
        return organisationUnitGroups;
    }

    public List<String> getStatusMsg()
    {
        return statusMsg;
    }

    public void setStatusMsg( List<String> statusMsg )
    {
        this.statusMsg = statusMsg;
    }

    public List<String> getYearList()
    {
        return yearList;
    }


    public void setYearList( String year )
    {
        System.out.println( year );
    }

    private SimpleDateFormat simpleDateFormat;

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        PeriodType periodType = periodService.getPeriodTypeByName( "Yearly" );

        periods = new ArrayList<Period>( periodService.getPeriodsByPeriodType( periodType ) );

        Iterator<Period> periodIterator = periods.iterator();
        while ( periodIterator.hasNext() )
        {
            Period p1 = periodIterator.next();

            if ( p1.getStartDate().compareTo( new Date() ) > 0 )
            {
                periodIterator.remove();
            }

        }

        //Collections.sort( periods, new PeriodComparator() );

        yearList = new ArrayList<String>();
        simpleDateFormat = new SimpleDateFormat( "yyyy" );
        for ( Period p1 : periods )
        {
            yearList.add( simpleDateFormat.format( p1.getStartDate() ) );
        }
	
        /*
        //from YearMonth
        YearMonth ym = YearMonth.of(2019, Month.JANUARY);
        LocalDate d3 = ym.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d3 Jan -- " +  d3);
        
        
        //from YearMonth
        YearMonth ym1 = YearMonth.of(2019, Month.FEBRUARY);
        LocalDate d4 = ym1.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d4 Feb -- " +  d4);
        
        //from YearMonth
        YearMonth ym2 = YearMonth.of(2019, Month.of( 03 ));
        LocalDate d5 = ym2.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d5 march -- " +  d5);
        */
        
        return SUCCESS;
    }

}



