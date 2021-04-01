package org.hisp.dhis.reporting.advancelockexception.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GetDataSetsForPeriodTypeAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Input/output
    // -------------------------------------------------------------------------

    private String periodType;

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
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
    {
        if ( periodType != null && !periodType.isEmpty() )
        {
            dataSets = getAssignedDataSetsByPeriodType( periodService.getPeriodTypeByName( periodType ) );
        }
        
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

