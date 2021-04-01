package org.hisp.dhis.reporting.dataentryformstatus.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

//import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GenerateDataEntryStatusFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetService dataSetService;

    // -------------------------------------------------------------------------
    // input / output
    // -------------------------------------------------------------------------

    private List<DataSet> dataSetList;

    public List<DataSet> getDataSetList()
    {
        return dataSetList;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    public String execute()
        throws Exception
    {
        /* DataSet List */

        dataSetList = new ArrayList<DataSet>( dataSetService.getAllDataSets() );
        
        //System.out.println(  "Initial Size : " + dataSetList.size() );
        
        Iterator<DataSet> dataSetListIterator = dataSetList.iterator();

        while ( dataSetListIterator.hasNext() )
        {
            DataSet dataSet = (DataSet) dataSetListIterator.next();

            if ( dataSet.getSources().size() <= 0 )
            {
                dataSetListIterator.remove();
            }
            
            else
            {
                if( dataSet == null || !dataSet.hasDataEntryForm() )
                {
                    //System.out.println(  dataSet.getId() +" : " + dataSet.getName() + " : " + dataSet.hasDataEntryForm() );
                    
                    dataSetListIterator.remove();
                }
            }
        }
        
        //System.out.println(  "Final Size : " + dataSetList.size() );
        
        //Collections.sort( dataSetList, new IdentifiableObjectNameComparator() );
        //Collections.sort( dataSetList, new DataSet );

        return SUCCESS;
    }

}