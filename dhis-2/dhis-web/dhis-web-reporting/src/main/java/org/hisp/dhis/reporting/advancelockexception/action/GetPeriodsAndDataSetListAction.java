package org.hisp.dhis.reporting.advancelockexception.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
//import org.hisp.dhis.period.comparator.PeriodComparator;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GetPeriodsAndDataSetListAction implements Action
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
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
    
    private List<DataSet> dataSets = new ArrayList<DataSet>();
    
    public List<DataSet> getDataSets()
    {
        return dataSets;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        periods = new ArrayList<Period>();
        
        if ( periodTypeName != null && !periodTypeName.equals( "-1" ) )
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
            
            dataSets = getAssignedDataSetsByPeriodType( periodType );
        }

        for ( Period period : periods )
        {
            period.setName( format.formatPeriod( period ) );
        }

        Collections.sort( periods, new PeriodsComparator() );
        //Collections.sort( dataSets, new IdentifiableObjectNameComparator() );
        Collections.sort( dataSets );
        
        return SUCCESS;
    }
    
    // Supportive method
    public List<DataSet> getAssignedDataSetsByPeriodType( PeriodType periodType )
    {
        List<DataSet> dataSetListByPeriodType = new ArrayList<>( dataSetService.getDataSetsByPeriodType( periodType ) );

        Iterator<DataSet> dataSetIterator = dataSetListByPeriodType.iterator();
        while ( dataSetIterator.hasNext() )
        {
            DataSet dataSet = dataSetIterator.next();
            if ( dataSet.getSources() == null || dataSet.getSources().size() == 0 )
            {
                dataSetIterator.remove();
            }
        }

        return dataSetListByPeriodType;
    }
    
    
}
