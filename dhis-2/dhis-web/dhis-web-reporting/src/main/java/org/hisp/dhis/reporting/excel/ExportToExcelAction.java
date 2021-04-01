package org.hisp.dhis.reporting.excel;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.opensymphony.xwork2.Action;


/**
 * @author Mithilesh Kumar Thakur
 */
public class ExportToExcelAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------
    
    private InputStream inputStream;

    public InputStream getInputStream()
    {
        return inputStream;
    }
    
    private String fileName;

    public String getFileName()
    {
        return fileName;
    }

    private String htmlCode;
    
    public void setHtmlCode( String htmlCode )
    {
        this.htmlCode = htmlCode;
    }
    
    private String htmlCode1;
    
    public void setHtmlCode1( String htmlCode1 )
    {
        this.htmlCode1 = htmlCode1;
    }
    
    private String reportName;
    
    public void setReportName( String reportName )
    {
        this.reportName = reportName;
    }


    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------


    public String execute() throws Exception
    {                        
        if( reportName != null && reportName.length() > 0  )
        {
            fileName = reportName + ".xls";
        }
        
        else
        {
            fileName = "CustumDataSetReport.xls";
        }
        
        if( htmlCode != null )
        {
            inputStream = new BufferedInputStream( new ByteArrayInputStream( htmlCode.getBytes("UTF-8") ) );
        }
        else 
        {
            inputStream = new BufferedInputStream( new ByteArrayInputStream( htmlCode1.getBytes("UTF-8") ) );
        }
        
        return SUCCESS;
    }

}

