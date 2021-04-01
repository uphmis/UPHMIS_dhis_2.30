package org.hisp.dhis.reporting.advancelockexception.action;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class LockExceptionManagementFormAction 
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Input/output
    // -------------------------------------------------------------------------

    private Collection<PeriodType> periodTypes;

    public Collection<PeriodType> getPeriodTypes()
    {
        return periodTypes;
    }
    
    private String expireOnDate;
    
    public String getExpireOnDate()
    {
        return expireOnDate;
    }

    private SimpleDateFormat simpleDateFormat;
    String todayDate = "";
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
    {
        periodTypes = periodService.getAllPeriodTypes();
        simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
        Date date = new Date();
        // 10 day after date
        Calendar tenDayAfter = Calendar.getInstance();
        tenDayAfter.setTime( date );
        tenDayAfter.add( Calendar.DATE, 10 );
        Date tenDayAfterDate = tenDayAfter.getTime();
        
        expireOnDate = simpleDateFormat.format( tenDayAfterDate );
        System.out.println( "expireOndate - " + expireOnDate );
        return SUCCESS;
    }
}
