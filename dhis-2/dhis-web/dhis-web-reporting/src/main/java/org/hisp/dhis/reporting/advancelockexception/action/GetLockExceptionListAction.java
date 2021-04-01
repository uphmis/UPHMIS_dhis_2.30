package org.hisp.dhis.reporting.advancelockexception.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.comparator.LockExceptionNameComparator;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GetLockExceptionListAction
    extends ActionPagingSupport<LockException>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    @Autowired
    private DataSetService dataSetService;

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<LockException> lockExceptions;

    public List<LockException> getLockExceptions()
    {
        return lockExceptions;
    }

    private boolean usePaging = true;

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        if ( usePaging )
        {
            paging = createPaging( dataSetService.getLockExceptionCount() );
            lockExceptions = new ArrayList<>( dataSetService.getLockExceptionsBetween( paging.getStartPos(),
                paging.getEndPos() ) );
        }
        else
        {
            lockExceptions = new ArrayList<>( dataSetService.getAllLockExceptions() );
        }

        Collections.sort( lockExceptions, new LockExceptionNameComparator() );

        for ( LockException lockException : lockExceptions )
        {
            lockException.getPeriod().setName( format.formatPeriod( lockException.getPeriod() ) );
        }

        return SUCCESS;
    }
}
