package org.hisp.dhis.reports;


import static org.hisp.dhis.util.ConversionUtils.getIdentifiers;
import static org.hisp.dhis.util.TextUtils.getCommaDelimitedString;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.config.ConfigurationService;
import org.hisp.dhis.config.Configuration_IN;
import org.hisp.dhis.dataelement.DataElement;
//import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
//import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultReportService
    implements ReportService
{
    private static final String NULL_REPLACEMENT = "0";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ReportStore reportStore;

    public void setReportStore( ReportStore reportStore )
    {
        this.reportStore = reportStore;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService( ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    private IndicatorService indicatorService;

    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

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

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    /*
    private DataElementCategoryService dataElementCategoryOptionComboService;

    public void setDataElementCategoryOptionComboService(
        DataElementCategoryService dataElementCategoryOptionComboService )
    {
        this.dataElementCategoryOptionComboService = dataElementCategoryOptionComboService;
    }
    */
    
    @Autowired
    private CategoryService categoryService;
    
    
    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }
    
    private DatabaseInfoProvider databaseInfoProvider;

    public void setDatabaseInfoProvider( DatabaseInfoProvider databaseInfoProvider )
    {
        this.databaseInfoProvider = databaseInfoProvider;
    }

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;

    }

    // -------------------------------------------------------------------------
    // Report_in
    // -------------------------------------------------------------------------

    @Transactional
    public int addReport( Report_in report )
    {
        return reportStore.addReport( report );
    }

    @Transactional
    public void deleteReport( Report_in report )
    {
        reportStore.deleteReport( report );
    }

    @Transactional
    public void updateReport( Report_in report )
    {
        reportStore.updateReport( report );
    }

    @Transactional
    public Collection<Report_in> getAllReports()
    {
        return reportStore.getAllReports();
    }

    @Transactional
    public Report_in getReport( int id )
    {
        return reportStore.getReport( id );
    }

    @Transactional
    public Report_in getReportByName( String name )
    {
        return reportStore.getReportByName( name );
    }

    @Transactional
    public Collection<Report_in> getReportBySource( OrganisationUnit source )
    {
        return reportStore.getReportBySource( source );
    }

    @Transactional
    public Collection<Report_in> getReportsByPeriodAndReportType( PeriodType periodType, String reportType )
    {
        return reportStore.getReportsByPeriodAndReportType( periodType, reportType );
    }

    @Transactional
    public Collection<Report_in> getReportsByPeriodType( PeriodType periodType )
    {
        return reportStore.getReportsByPeriodType( periodType );
    }

    @Transactional
    public Collection<Report_in> getReportsByReportType( String reportType )
    {
        return reportStore.getReportsByReportType( reportType );
    }

    @Transactional
    public Collection<Report_in> getReportsByPeriodSourceAndReportType( PeriodType periodType, OrganisationUnit source,
        String reportType )
    {
        return reportStore.getReportsByPeriodSourceAndReportType( periodType, source, reportType );
    }

    // get Patients List ByOrgUnit
    /*
    @Transactional
    public Collection<Patient> getPatientByOrgUnit( OrganisationUnit organisationUnit )
    {
        return reportStore.getPatientByOrgUnit( organisationUnit );
    }
    */
    
    // get Patients List By OrgUnit and Program
    /*
    @Transactional
    public Collection<Patient> getPatientByOrgUnitAndProgram( OrganisationUnit organisationUnit, Program program )
    {
        return reportStore.getPatientByOrgUnitAndProgram( organisationUnit, program );
    }
    */
    
    // -------------------------------------------------------------------------
    // for Report Result Action input/otput
    // -------------------------------------------------------------------------

    // private String reportModelTB;

    // -------------------------------------------------------------------------
    // Support Methods Defination
    // -------------------------------------------------------------------------

    // get Programs List ByOrgUnit
    public Collection<Program> getProgramsByOrgUnit( OrganisationUnit organisationUnit )
    {
        // System.out.println( "In side  get Programs By OrgUnit Method " );
        List<Program> programList = new ArrayList<Program>();

        // List<Program> allProgramList = new ArrayList<Program>(
        // programService.getAllPrograms() );

        for ( Program program : programService.getAllPrograms() )
        {
            if ( program.getOrganisationUnits().contains( organisationUnit ) )
            {
                programList.add( program );
            }
        }
        // System.out.println( "Size of Program List is   : " +
        // programList.size() );
        return programList;
    }

    // ----------------------------------------------------------------------------------------------------
    // START
    // Fetch Global Decode Configuration
    // ----------------------------------------------------------------------------------------------------

    /**
     * Generates a map of global-to-local ids using the global settings XML
     * file.
     * 
     * @return
     */

    public Map<String, String> mapGlobalValues()
    {
        final String configFileName = "globalsettings.xml";

        Map<String, String> globalValuesMap = new HashMap<String, String>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator + configFileName;

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = null;
            try
            {
                docBuilder = docBuilderFactory.newDocumentBuilder();
            }
            catch ( ParserConfigurationException e )
            {
                e.printStackTrace();
            }
            Document doc = null;
            try
            {
                doc = docBuilder.parse( new File( path ) );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }

            if ( doc == null )
            {
                System.out.println( "Global config XML file not found" );
                return null;
            }

            NodeList listOfConfigCodes = doc.getElementsByTagName( "gconfig" );

            int totalConfigCodes = listOfConfigCodes.getLength();

            for ( int s = 0; s < totalConfigCodes; s++ )
            {
                Element configElement = (Element) listOfConfigCodes.item( s );

                String value = configElement.getAttribute( "dhisid" ).trim();

                String id = configElement.getAttribute( "commonid" ).trim();

                globalValuesMap.put( id, value );

            }

        }
        catch ( SAXException e )
        {
            e.printStackTrace();
        }

        return globalValuesMap;
    }

    /**
     * Replaces global ids with local values
     * 
     * @param expression
     * @return
     */

    public String getGlobalExpression( String expression, Map<String, String> globalValuesMap )
    {
        String result = null;

        Pattern p = Pattern.compile( "\\[(.*?)\\]" );
        Matcher matcher = p.matcher( expression );

        String localValue;

        while ( matcher.find() )
        {
            result = matcher.group( 1 );
            localValue = globalValuesMap.get( result );
            expression = expression.replace( "[" + result + "]", "[" + localValue + "]" );
        }

        result = expression;

        return result;
    }

    // ----------------------------------------------------------------------------------------------------------
    // END
    // Fetch Global Decode Configuration
    // ----------------------------------------------------------------------------------------------------------

    public List<Report_inDesign> getDistrictFeedbackReportDesign( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();
            Map<String, String> globalValuesMap = mapGlobalValues();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();

                // ------------------------replace global
                // values------------------------------------------------

                expression = getGlobalExpression( expression, globalValuesMap );

                // ---------------------------------------------------------------------------------------------

                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    public String getRAFolderName()
    {
        String raFolderName = "ra_national";

        try
        {
            raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue();
        }
        catch ( Exception e )
        {
            System.out.println( "Exception : " + e.getMessage() );
            return null;
        }

        return raFolderName;
    }

    public List<Integer> getLinelistingRecordNos( OrganisationUnit organisationUnit, Period period, String lltype )
    {
        List<Integer> recordNosList = new ArrayList<Integer>();

        String query = "";

        int dataElementid = 1020;

        if ( lltype.equalsIgnoreCase( "lllivebirth-l4DECodes.xml" )
            || lltype.equalsIgnoreCase( "lllivebirth-l5DECodes.xml" )
            || lltype.equalsIgnoreCase( "lllivebirth-l6DECodes.xml" ) )
        {
            dataElementid = 1020;
        }
        else if ( lltype.equalsIgnoreCase( "lldeath-l4DECodes.xml" )
            || lltype.equalsIgnoreCase( "lldeath-l5DECodes.xml" ) || lltype.equalsIgnoreCase( "lldeath-l6DECodes.xml" )
            || lltype.equalsIgnoreCase( "monthly_SCWebPortalDECodes.xml" ) )
        {
            dataElementid = 1027;
        }
        else if ( lltype.equalsIgnoreCase( "llmaternaldeath-l4DECodes.xml" )
            || lltype.equalsIgnoreCase( "llmaternaldeath-l5DECodes.xml" )
            || lltype.equalsIgnoreCase( "llmaternaldeath-l6DECodes.xml" ) )
        {
            dataElementid = 1032;
        }
        else if ( lltype.equalsIgnoreCase( "LL_FP_Status.xml" ) )
        {
            dataElementid = 7271;
        }
        else if ( lltype.equalsIgnoreCase( "LL_Yukti_Status.xml" ) )
        {
            dataElementid = 7280;
        }

        try
        {
            query = "SELECT recordno FROM lldatavalue WHERE dataelementid = " + dataElementid + " AND periodid = "
                + period.getId() + " AND sourceid = " + organisationUnit.getId();

            SqlRowSet rs1 = jdbcTemplate.queryForRowSet( query );

            while ( rs1.next() )
            {
                recordNosList.add( rs1.getInt( 1 ) );
            }

            Collections.sort( recordNosList );
        }
        catch ( Exception e )
        {
            System.out.println( "SQL Exception : " + e.getMessage() );
        }

        return recordNosList;
    }

    public List<Report_inDesign> getReportDesign( Report_in report )
    {
        List<Report_inDesign> deCodes = new ArrayList<Report_inDesign>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator
            + report.getXmlTemplateName();

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );
                int rowMerge = new Integer( deCodeElement.getAttribute( "rowmerge" ) );
                int colMerge = new Integer( deCodeElement.getAttribute( "colmerge" ) );

                Report_inDesign reportDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, rowMerge,
                    colMerge, expression );

                deCodes.add( reportDesign );

            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    /*
     * Returns Previous Month's Period object For ex:- selected period is
     * Aug-2007 it returns the period object corresponding July-2007
     */

    public List<Report_inDesign> getReportDesignForGlobalSetting( Report_in report )
    {
        List<Report_inDesign> deCodes = new ArrayList<Report_inDesign>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator
            + report.getXmlTemplateName();

        // String configFile = "";

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();
            Map<String, String> globalValuesMap = mapGlobalValues();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();

                // ------------------------replace global
                // values------------------------------------------------

                expression = getGlobalExpression( expression, globalValuesMap );

                // ---------------------------------------------------------------------------------------------

                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );
                int rowMerge = new Integer( deCodeElement.getAttribute( "rowmerge" ) );
                int colMerge = new Integer( deCodeElement.getAttribute( "colmerge" ) );

                Report_inDesign reportDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, rowMerge,
                    colMerge, expression );

                deCodes.add( reportDesign );

            }

            // end of for loop with s var
        }

        // try block end

        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    // -------------------------------------------------------------------------
    // Get Aggregated Result for dataelement expression for Global Setting
    // -------------------------------------------------------------------------

    public List<Report_inDesign> getReportDesignForGlobalSetting( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();
            Map<String, String> globalValuesMap = mapGlobalValues();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();

                // ------------------------replace global
                // values------------------------------------------------

                // System.out.println(
                // "\n*INFO :<< CHECKING CONFIG FILE SETUP(2) >>" );
                // System.out.println( "*INFO :Global Value: " + expression );
                expression = getGlobalExpression( expression, globalValuesMap );
                // System.out.println( "*INFO :Local Value: " + expression );
                // System.out.println( "*INFO :<<CHECK FINISHED>>\n" );

                // ---------------------------------------------------------------------------------------------

                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    public List<Report_inDesign> getReportDesignForTracker( Report_in report )
    {
        List<Report_inDesign> deCodes = new ArrayList<Report_inDesign>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator
            + report.getXmlTemplateName();

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );
                int rowMerge = new Integer( deCodeElement.getAttribute( "rowmerge" ) );
                int colMerge = new Integer( deCodeElement.getAttribute( "colmerge" ) );

                Report_inDesign reportDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, rowMerge,
                    colMerge, expression );

                deCodes.add( reportDesign );

            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    // -------------------------------------------------------------------------
    // Get Aggregated Result for dataelement/attribute/tracker expression
    // -------------------------------------------------------------------------

    public List<Report_inDesign> getReportDesignForTracker( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    public List<Report_inDesign> getReportDesignForHeader( String fileName )
    {
        List<Report_inDesign> deCodes = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "header-info" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign reportDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );

                deCodes.add( reportDesign );

            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    public Period getPreviousPeriod( Date startDate, Date endDate )
    {
        Period period = new Period();
        Calendar tempDate = Calendar.getInstance();
        tempDate.setTime( startDate );
        if ( tempDate.get( Calendar.MONTH ) == Calendar.JANUARY )
        {
            tempDate.set( Calendar.MONTH, Calendar.DECEMBER );
            tempDate.roll( Calendar.YEAR, -1 );
        }
        else
        {
            tempDate.roll( Calendar.MONTH, -1 );
        }
        PeriodType periodType = new MonthlyPeriodType();
        period = getPeriodByMonth( tempDate.get( Calendar.MONTH ), tempDate.get( Calendar.YEAR ), periodType );

        return period;
    }

    /*
     * Returns the Period Object of the given date For ex:- if the month is 3,
     * year is 2006 and periodType Object of type Monthly then it returns the
     * corresponding Period Object
     */
    public Period getPeriodByMonth( int month, int year, PeriodType periodType )
    {
        int monthDays[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        Calendar cal = Calendar.getInstance();
        cal.set( year, month, 1, 0, 0, 0 );
        Date firstDay = new Date( cal.getTimeInMillis() );

        if ( periodType.getName().equals( "Monthly" ) )
        {
            cal.set( year, month, 1, 0, 0, 0 );
            if ( year % 4 == 0 )
            {
                cal.set( Calendar.DAY_OF_MONTH, monthDays[month] + 1 );
            }
            else
            {
                cal.set( Calendar.DAY_OF_MONTH, monthDays[month] );
            }
        }
        else if ( periodType.getName().equals( "Yearly" ) )
        {
            cal.set( year, Calendar.DECEMBER, 31 );
        }
        Date lastDay = new Date( cal.getTimeInMillis() );
        // System.out.println( lastDay.toString() );
        Period newPeriod = new Period();
        newPeriod = periodService.getPeriod( firstDay, lastDay, periodType );
        return newPeriod;
    }

    public List<Calendar> getStartingEndingPeriods( String deType, Date startDate, Date endDate )
    {
        List<Calendar> calendarList = new ArrayList<Calendar>();

        Calendar tempStartDate = Calendar.getInstance();
        Calendar tempEndDate = Calendar.getInstance();

        Period previousPeriod = new Period();
        previousPeriod = getPreviousPeriod( startDate, endDate );

        if ( deType.equalsIgnoreCase( ReportDesignPeriodType.RDPT_CCMCY ) )
        {
            tempStartDate.setTime( startDate );
            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
            tempEndDate.setTime( endDate );
        }
        else if ( deType.equalsIgnoreCase( ReportDesignPeriodType.RDPT_CPMCY ) )
        {
            tempStartDate.setTime( previousPeriod.getStartDate() );
            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
            tempEndDate.setTime( previousPeriod.getEndDate() );
        }
        else if ( deType.equalsIgnoreCase( ReportDesignPeriodType.RDPT_CMPY ) )
        {
            tempStartDate.setTime( startDate );
            tempEndDate.setTime( endDate );

            tempStartDate.roll( Calendar.YEAR, -1 );
            tempEndDate.roll( Calendar.YEAR, -1 );
        }
        else if ( deType.equalsIgnoreCase( ReportDesignPeriodType.RDPT_CCMPY ) )
        {
            tempStartDate.setTime( startDate );
            tempEndDate.setTime( endDate );

            tempStartDate.roll( Calendar.YEAR, -1 );
            tempEndDate.roll( Calendar.YEAR, -1 );

            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
        }
        else if ( deType.equalsIgnoreCase( ReportDesignPeriodType.RDPT_PMCY ) )
        {
            tempStartDate.setTime( previousPeriod.getStartDate() );
            tempEndDate.setTime( previousPeriod.getEndDate() );
        }
        else
        {
            tempStartDate.setTime( startDate );
            tempEndDate.setTime( endDate );
        }

        // System.out.print(deType+" -- ");
        calendarList.add( tempStartDate );
        calendarList.add( tempEndDate );

        return calendarList;
    }

    public List<Period> getMonthlyPeriods( Date start, Date end )
    {
        List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates( start, end ) );
        PeriodType monthlyPeriodType = getPeriodTypeObject( "monthly" );

        List<Period> monthlyPeriodList = new ArrayList<Period>();
        Iterator<Period> it = periodList.iterator();
        while ( it.hasNext() )
        {
            Period period = (Period) it.next();
            if ( period.getPeriodType().getId() == monthlyPeriodType.getId() )
            {
                monthlyPeriodList.add( period );
            }
        }
        return monthlyPeriodList;
    }

    /*
     * Returns the PeriodType Object based on the Period Type Name For ex:- if
     * we pass name as Monthly then it returns the PeriodType Object for Monthly
     * PeriodType If there is no such PeriodType returns null
     */
    public PeriodType getPeriodTypeObject( String periodTypeName )
    {
        Collection<PeriodType> periodTypes = periodService.getAllPeriodTypes();
        PeriodType periodType = null;
        Iterator<PeriodType> iter = periodTypes.iterator();
        while ( iter.hasNext() )
        {
            PeriodType tempPeriodType = (PeriodType) iter.next();
            if ( tempPeriodType.getName().toLowerCase().trim().equals( periodTypeName ) )
            {
                periodType = tempPeriodType;
                break;
            }
        }
        if ( periodType == null )
        {
            System.out.println( "No Such PeriodType" );
            return null;
        }
        return periodType;
    }

    /*
     * Returns the child tree of the selected Orgunit
     */
    public List<OrganisationUnit> getAllChildren( OrganisationUnit selecteOU )
    {
        List<OrganisationUnit> ouList = new ArrayList<OrganisationUnit>();
        Iterator<OrganisationUnit> it = selecteOU.getChildren().iterator();
        while ( it.hasNext() )
        {
            OrganisationUnit orgU = (OrganisationUnit) it.next();
            ouList.add( orgU );
        }
        return ouList;
    }

    /*
     * Returns the PeriodType Object for selected DataElement, If no PeriodType
     * is found then by default returns Monthly Period type
     */
    public PeriodType getDataElementPeriodType( DataElement de )
    {
        List<DataSet> dataSetList = new ArrayList<DataSet>( dataSetService.getAllDataSets() );
        Iterator<DataSet> it = dataSetList.iterator();
        while ( it.hasNext() )
        {
            DataSet ds = (DataSet) it.next();
            List<DataElement> dataElementList = new ArrayList<DataElement>( ds.getDataElements() );
            if ( dataElementList.contains( de ) )
            {
                return ds.getPeriodType();
            }
        }

        return null;

    } // getDataElementPeriodType end

    // -------------------------------------------------------------------------
    // Get Aggregated Result for dataelement expression
    // -------------------------------------------------------------------------
    @SuppressWarnings("unused")
	public String getResultDataValue( String formula, Date startDate, Date endDate, OrganisationUnit organisationUnit,
        String reportModelTB )
    {
        int deFlag1 = 0;
        int isAggregated = 0;

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );

                DataElement dataElement = dataElementService.getDataElement( dataElementId );
                //DataElementCategoryOptionCombo optionCombo = dataElementCategoryOptionComboService.getDataElementCategoryOptionCombo( optionComboId );

                CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( optionComboId );
                if ( dataElement == null || optionCombo == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }
                //if ( dataElement.getType().equalsIgnoreCase( "int" ) )
               	//if ( dataElement.getValueType().getValueType().isInteger() )	
               	if ( dataElement.isNumericType() )
               	{
               	    Double aggregatedValue = null;
               	    //Double aggregatedValue = aggregationService.getAggregatedDataValue( dataElement, optionCombo, startDate, endDate, organisationUnit );
                    // System.out.println( dataElement.getId() + " : " +
                    // optionCombo.getId() + " : " + startDate + " : " + endDate
                    // + " : " + organisationUnit + " : " + aggregatedValue);
                    if ( aggregatedValue == null )
                    {
                        replaceString = NULL_REPLACEMENT;
                    }
                    else
                    {
                        replaceString = String.valueOf( aggregatedValue );

                        isAggregated = 1;
                    }
                }
                else
                {
                    deFlag1 = 1;
                    PeriodType dePeriodType = getDataElementPeriodType( dataElement );
                    // List<Period> periodList = new ArrayList<Period>(
                    // periodService.getIntersectingPeriodsByPeriodType(
                    // dePeriodType, startDate, endDate ) );
                    List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates(
                        dePeriodType, startDate, endDate ) );
                    Period tempPeriod = new Period();
                    if ( periodList == null || periodList.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        tempPeriod = (Period) periodList.get( 0 );
                    }

                    //DataValue dataValue = dataValueService.getDataValue( organisationUnit, dataElement, tempPeriod, optionCombo );
                    
                    DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, optionCombo );

                    if ( dataValue != null && dataValue.getValue() != null )
                    {
                        replaceString = dataValue.getValue();
                    }
                    else
                    {
                        replaceString = "";
                    }

                    if ( replaceString == null )
                    {
                        replaceString = "";
                    }
                }
                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                    resultValue = "";
                }
                if ( d == -1 )
                {
                    d = 0.0;
                    resultValue = "";
                }
                else
                {
                    // This is to display financial data as it is like 2.1476838
                    resultValue = "" + d;

                    // These lines are to display financial data that do not
                    // have decimals
                    d = d * 10;
                    if ( d % 10 == 0 )
                    {
                        resultValue = "" + (int) d / 10;
                    }

                    d = d / 10;

                    // These line are to display non financial data that do not
                    // require decimals
                    if ( !(reportModelTB.equalsIgnoreCase( "STATIC-FINANCIAL" )) )
                    {
                        resultValue = "" + (double) d;
                    }
                }

            }
            else
            {
                resultValue = buffer.toString();
            }

            if ( isAggregated == 0 )
            {
                resultValue = " ";
            }

            if ( resultValue.equalsIgnoreCase( "" ) )
            {
                resultValue = " ";
            }

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    // -------------------------------------------------------------------------
    // Get Individual Result for dataelement expression
    // -------------------------------------------------------------------------
    public String getIndividualResultDataValue( String formula, Date startDate, Date endDate,
        OrganisationUnit organisationUnit, String reportModelTB )
    {
        int deFlag1 = 0;

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";
            boolean valueDoesNotExist = true;

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );

                DataElement dataElement = dataElementService.getDataElement( dataElementId );
                //DataElementCategoryOptionCombo optionCombo = dataElementCategoryOptionComboService.getDataElementCategoryOptionCombo( optionComboId );
                CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( optionComboId );
                if ( dataElement == null || optionCombo == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }
                //if ( dataElement.getType().equalsIgnoreCase( "int" ) )
                //if ( dataElement.getValueType().isInteger() )
                if ( dataElement.isNumericType() )
                {

                    PeriodType dePeriodType = getDataElementPeriodType( dataElement );
                    // List<Period> periodList = new ArrayList<Period>(
                    // periodService.getIntersectingPeriodsByPeriodType(
                    // dePeriodType, startDate, endDate ) );
                    List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates(
                        dePeriodType, startDate, endDate ) );

                    if ( periodList == null || periodList.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        double aggregatedValue = 0.0;
                        for ( Period tempPeriod : periodList )
                        {
                            //DataValue dataValue = dataValueService.getDataValue( organisationUnit, dataElement, tempPeriod, optionCombo );
                            DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, optionCombo );

                            if ( dataValue != null && dataValue.getValue() != null )
                            {
                                aggregatedValue += Double.parseDouble( dataValue.getValue() );

                                valueDoesNotExist = false;
                            }
                        }
                        replaceString = String.valueOf( aggregatedValue );
                    }
                }
                else
                {
                    deFlag1 = 1;
                    PeriodType dePeriodType = getDataElementPeriodType( dataElement );
                    // List<Period> periodList = new ArrayList<Period>(
                    // periodService.getIntersectingPeriodsByPeriodType(
                    // dePeriodType, startDate, endDate ) );
                    List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates(
                        dePeriodType, startDate, endDate ) );
                    Period tempPeriod = new Period();
                    if ( periodList == null || periodList.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        tempPeriod = (Period) periodList.get( 0 );
                    }

                    //DataValue dataValue = dataValueService.getDataValue( organisationUnit, dataElement, tempPeriod, optionCombo );
                    DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, optionCombo );
                    
                    if ( dataValue != null )
                    {
                        replaceString = dataValue.getValue();
                        valueDoesNotExist = false;
                    }
                    else
                    {
                        replaceString = "";
                    }

                    if ( replaceString == null )
                    {
                        replaceString = "";
                    }
                }
                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;

                    resultValue = "";
                }
                if ( d == -1 )
                {
                    d = 0.0;

                    resultValue = "";
                }
                else
                {

                    // This is to display financial data as it is like 2.1476838
                    resultValue = "" + d;

                    // These lines are to display financial data that do not
                    // have decimals
                    d = d * 10;

                    if ( d % 10 == 0 )
                    {
                        resultValue = "" + (int) d / 10;
                    }

                    d = d / 10;

                    // These line are to display non financial data that do not
                    // require decimals
                    if ( !(reportModelTB.equalsIgnoreCase( "STATIC-FINANCIAL" )) )
                    {
                        resultValue = "" + (int) d;
                    }
                }
            }
            else
            {
                resultValue = buffer.toString();
            }

            if ( valueDoesNotExist )
            {
                resultValue = " ";
            }

            if ( resultValue.equalsIgnoreCase( "" ) )
            {
                resultValue = " ";
            }

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    // functoin getBooleanDataValue stsrt

    @SuppressWarnings("unused")
	public String getBooleanDataValue( String formula, Date startDate, Date endDate, OrganisationUnit organisationUnit,
        String reportModelTB )
    {
        int deFlag1 = 0;
        int deFlag2 = 0;
        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );

                DataElement dataElement = dataElementService.getDataElement( dataElementId );
                //DataElementCategoryOptionCombo optionCombo = dataElementCategoryOptionComboService.getDataElementCategoryOptionCombo( optionComboId );
                CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( optionComboId );
                if ( dataElement == null || optionCombo == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }
                //if ( dataElement.getType().equalsIgnoreCase( "bool" ) )
                //if ( dataElement.getValueType().isBoolean() )
                if ( dataElement.isNumericType() )
                {
                    deFlag1 = 1;
                    deFlag2 = 0;
                    PeriodType dePeriodType = getDataElementPeriodType( dataElement );
                    // List<Period> periodList = new ArrayList<Period>(
                    // periodService.getIntersectingPeriodsByPeriodType(
                    // dePeriodType, startDate, endDate ) );
                    List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates(
                        dePeriodType, startDate, endDate ) );
                    Period tempPeriod = new Period();
                    if ( periodList == null || periodList.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        tempPeriod = (Period) periodList.get( 0 );
                    }

                    //DataValue dataValue = dataValueService.getDataValue( organisationUnit, dataElement, tempPeriod, optionCombo );
                    DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, optionCombo );

                    if ( dataValue != null )
                    {
                        // Works for both text and boolean data types

                        if ( dataValue.getValue().equalsIgnoreCase( "true" ) )
                        {
                            replaceString = "Yes";
                        }
                        else if ( dataValue.getValue().equalsIgnoreCase( "false" ) )
                        {
                            replaceString = "No";
                        }
                        else
                        {
                            replaceString = dataValue.getValue();
                        }
                    }
                    else
                    {
                        replaceString = "";
                    }
                }
                else
                {
                    //Double aggregatedValue = aggregationService.getAggregatedDataValue( dataElement, optionCombo, startDate, endDate, organisationUnit );
                    Double aggregatedValue = null;
                    if ( aggregatedValue == null )
                    {
                        replaceString = NULL_REPLACEMENT;
                    }
                    else
                    {
                        replaceString = String.valueOf( aggregatedValue );

                        deFlag2 = 1;
                    }
                }
                matcher.appendReplacement( buffer, replaceString );
            }

            matcher.appendTail( buffer );

            String resultValue = "";
            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                }
                if ( d == -1 )
                {
                    d = 0.0;
                }
                else
                {
                    d = Math.round( d * Math.pow( 10, 1 ) ) / Math.pow( 10, 1 );
                    resultValue = "" + (int) d;
                }

                if ( deFlag2 == 0 )
                {
                    resultValue = " ";
                }
            }
            else
            {
                deFlag2 = 0;
                resultValue = buffer.toString();
            }
            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    // functoin getBooleanDataValue end

    // function getStartingEndingPeriods starts

    public List<Calendar> getStartingEndingPeriods( String deType, Period selectedPeriod )
    {
        List<Calendar> calendarList = new ArrayList<Calendar>();

        Calendar tempStartDate = Calendar.getInstance();
        Calendar tempEndDate = Calendar.getInstance();

        Period previousPeriod = new Period();
        previousPeriod = getPreviousPeriod( selectedPeriod );

        // Financial current year
        if ( deType.equalsIgnoreCase( "fcy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempEndDate.setTime( selectedPeriod.getStartDate() );

            int currentYear = tempStartDate.get( Calendar.YEAR );

            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.set( Calendar.YEAR, currentYear - 1 );
                tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
                tempStartDate.set( Calendar.DATE, 1 );

                tempEndDate.set( Calendar.YEAR, currentYear );
                tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
                tempEndDate.set( Calendar.DATE, 31 );
            }
            else
            {
                tempStartDate.set( Calendar.YEAR, currentYear );
                tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
                tempStartDate.set( Calendar.DATE, 1 );

                tempEndDate.set( Calendar.YEAR, currentYear + 1 );
                tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
                tempEndDate.set( Calendar.DATE, 31 );
            }

        }
        // Financial previous year
        else if ( deType.equalsIgnoreCase( "fpy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempEndDate.setTime( selectedPeriod.getEndDate() );

            tempStartDate.roll( Calendar.YEAR, -1 );
            tempEndDate.roll( Calendar.YEAR, -1 );

            int currentYear = tempStartDate.get( Calendar.YEAR );

            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.set( Calendar.YEAR, currentYear - 1 );
                tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
                tempStartDate.set( Calendar.DATE, 1 );

                tempEndDate.set( Calendar.YEAR, currentYear );
                tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
                tempEndDate.set( Calendar.DATE, 31 );
            }
            else
            {
                tempStartDate.set( Calendar.YEAR, currentYear );
                tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
                tempStartDate.set( Calendar.DATE, 1 );

                tempEndDate.set( Calendar.YEAR, currentYear + 1 );
                tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
                tempEndDate.set( Calendar.DATE, 31 );
            }

        }

        // Commutative current month and current year from April
        else if ( deType.equalsIgnoreCase( "ccmcy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
            tempEndDate.setTime( selectedPeriod.getEndDate() );
        }

        else if ( deType.equalsIgnoreCase( "cpmcy" ) )
        {
            tempStartDate.setTime( previousPeriod.getStartDate() );
            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
            tempEndDate.setTime( previousPeriod.getEndDate() );
        }

        // current month previous year
        else if ( deType.equalsIgnoreCase( "cmpy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempEndDate.setTime( selectedPeriod.getEndDate() );

            tempStartDate.roll( Calendar.YEAR, -1 );
            tempEndDate.roll( Calendar.YEAR, -1 );
        }

        // Commutative current month previous year
        else if ( deType.equalsIgnoreCase( "ccmpy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempEndDate.setTime( selectedPeriod.getEndDate() );

            tempStartDate.roll( Calendar.YEAR, -1 );
            tempEndDate.roll( Calendar.YEAR, -1 );

            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.APRIL )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
        }
        // previous month current year
        else if ( deType.equalsIgnoreCase( "pmcy" ) )
        {
            tempStartDate.setTime( previousPeriod.getStartDate() );
            tempEndDate.setTime( previousPeriod.getEndDate() );
        }
        else if ( deType.equalsIgnoreCase( "bccmcy" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            if ( tempStartDate.get( Calendar.MONTH ) < Calendar.JULY )
            {
                tempStartDate.roll( Calendar.YEAR, -1 );
            }
            tempStartDate.set( Calendar.MONTH, Calendar.JULY );
            tempEndDate.setTime( selectedPeriod.getEndDate() );
        }
        else if ( deType.equalsIgnoreCase( "bq1" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.JANUARY );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
            tempEndDate.set( Calendar.DATE, 31 );
        }
        else if ( deType.equalsIgnoreCase( "bq2" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.JUNE );
            tempEndDate.set( Calendar.DATE, 30 );
        }
        else if ( deType.equalsIgnoreCase( "bq3" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.JULY );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.SEPTEMBER );
            tempEndDate.set( Calendar.DATE, 31 );
        }
        else if ( deType.equalsIgnoreCase( "bq4" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.OCTOBER );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.DECEMBER );
            tempEndDate.set( Calendar.DATE, 31 );
        }

        else if ( deType.equalsIgnoreCase( "bfq1" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.JULY );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.SEPTEMBER );
            tempEndDate.set( Calendar.DATE, 30 );
        }
        else if ( deType.equalsIgnoreCase( "bfq2" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.OCTOBER );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.DECEMBER );
            tempEndDate.set( Calendar.DATE, 31 );
        }
        else if ( deType.equalsIgnoreCase( "bfq3" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.JANUARY );
            tempStartDate.roll( Calendar.YEAR, 1 );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.MARCH );
            tempEndDate.set( Calendar.DATE, 31 );
        }
        else if ( deType.equalsIgnoreCase( "bfq4" ) )
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempStartDate.set( Calendar.MONTH, Calendar.APRIL );
            tempStartDate.roll( Calendar.YEAR, 1 );

            tempEndDate.setTime( selectedPeriod.getEndDate() );
            tempEndDate.set( Calendar.MONTH, Calendar.JUNE );
            tempEndDate.set( Calendar.DATE, 30 );
        }
        // CMCY current month current year
        else
        {
            tempStartDate.setTime( selectedPeriod.getStartDate() );
            tempEndDate.setTime( selectedPeriod.getEndDate() );
        }

        calendarList.add( tempStartDate );
        calendarList.add( tempEndDate );

        return calendarList;
    }

    // function getPreviousPeriod starts
    public Period getPreviousPeriod( Period selectedPeriod )
    {
        Period period = new Period();
        Calendar tempDate = Calendar.getInstance();
        tempDate.setTime( selectedPeriod.getStartDate() );
        if ( tempDate.get( Calendar.MONTH ) == Calendar.JANUARY )
        {
            tempDate.set( Calendar.MONTH, Calendar.DECEMBER );
            tempDate.roll( Calendar.YEAR, -1 );

        }
        else
        {
            tempDate.roll( Calendar.MONTH, -1 );
        }
        PeriodType periodType = getPeriodTypeObject( "monthly" );
        period = getPeriodByMonth( tempDate.get( Calendar.MONTH ), tempDate.get( Calendar.YEAR ), periodType );

        return period;
    }

    @SuppressWarnings("unused")
	public String getResultIndicatorValue( String formula, Date startDate, Date endDate,
        OrganisationUnit organisationUnit )
    {
        int deFlag1 = 0;
        int deFlag2 = 0;

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int indicatorId = Integer.parseInt( replaceString );

                Indicator indicator = indicatorService.getIndicator( indicatorId );

                if ( indicator == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }

                //Double aggregatedValue = aggregationService.getAggregatedIndicatorValue( indicator, startDate, endDate, organisationUnit );
                
                Double aggregatedValue = null;
                if ( aggregatedValue == null )
                {
                    replaceString = NULL_REPLACEMENT;
                }
                else
                {
                    replaceString = String.valueOf( aggregatedValue );
                    deFlag2 = 1;
                }
                matcher.appendReplacement( buffer, replaceString );
            }

            matcher.appendTail( buffer );

            String resultValue = "";
            if ( deFlag1 == 0 )
            {
                double d = 0.0;

                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                }
                if ( d == -1 )
                {
                    d = 0.0;
                }
                else

                {
                    d = Math.round( d * Math.pow( 10, 1 ) ) / Math.pow( 10, 1 );
                    resultValue = "" + d;
                }

                if ( deFlag2 == 0 )
                {
                    resultValue = " ";
                }
            }
            else
            {
                resultValue = buffer.toString();
                deFlag2 = 0;
            }

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    public String getIndividualResultIndicatorValue( String formula, Date startDate, Date endDate,
        OrganisationUnit organisationUnit )
    {

        int deFlag1 = 0;
        int deFlag2 = 0;
        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int indicatorId = Integer.parseInt( replaceString );

                Indicator indicator = indicatorService.getIndicator( indicatorId );

                if ( indicator == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }

                String numeratorExp = indicator.getNumerator();
                String denominatorExp = indicator.getDenominator();
                int indicatorFactor = indicator.getIndicatorType().getFactor();
                String reportModelTB = "";
                String numeratorVal = getIndividualResultDataValue( numeratorExp, startDate, endDate, organisationUnit,
                    reportModelTB );
                String denominatorVal = getIndividualResultDataValue( denominatorExp, startDate, endDate,
                    organisationUnit, reportModelTB );

                double numeratorValue;
                try
                {
                    numeratorValue = Double.parseDouble( numeratorVal );
                }
                catch ( Exception e )
                {
                    numeratorValue = 0.0;
                }

                double denominatorValue;
                try
                {
                    denominatorValue = Double.parseDouble( denominatorVal );
                }
                catch ( Exception e )
                {
                    denominatorValue = 1.0;
                }

                double aggregatedValue;
                try
                {
                    aggregatedValue = (numeratorValue / denominatorValue) * indicatorFactor;
                }
                catch ( Exception e )
                {
                    System.out.println( "Exception while calculating Indicator value for Indicaotr "
                        + indicator.getName() );
                    aggregatedValue = 0.0;
                }

                replaceString = String.valueOf( aggregatedValue );
                deFlag2 = 1;

                matcher.appendReplacement( buffer, replaceString );
            }

            matcher.appendTail( buffer );

            String resultValue = "";
            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                }
                if ( d == -1 )
                {
                    d = 0.0;
                }
                else
                {
                    d = Math.round( d * Math.pow( 10, 1 ) ) / Math.pow( 10, 1 );
                    resultValue = "" + d;
                }

                if ( deFlag2 == 0 )
                {
                    resultValue = " ";
                }
            }
            else
            {
                deFlag2 = 0;
                resultValue = buffer.toString();
            }

            return resultValue;

        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }

    }

    // -------------------------------------------------------------------------
    // Get ReportDesign (decode tags) from corresponding xml file
    // -------------------------------------------------------------------------

    public List<Report_inDesign> getReportDesignWithMergeCells( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );
                int rowMerge = new Integer( deCodeElement.getAttribute( "rowmerge" ) );
                int colMerge = new Integer( deCodeElement.getAttribute( "colmerge" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, rowMerge,
                    colMerge, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    // -------------------------------------------------------------------------
    // Get Aggregated Result for dataelement expression
    // -------------------------------------------------------------------------

    public List<Report_inDesign> getReportDesign( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    /*
    public String getResultSurveyValue( String formula, OrganisationUnit organisationUnit )
    {
        try
        {
            int deFlag1 = 0;
            int deFlag2 = 0;
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                String surveyIdString = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int indicatorId = Integer.parseInt( replaceString );

                int surveyId = Integer.parseInt( surveyIdString );

                Indicator indicator = indicatorService.getIndicator( indicatorId );

                Survey survey = surveyService.getSurvey( surveyId );

                if ( indicator == null || survey == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }

                SurveyDataValue surveyDataValue = new SurveyDataValue();

                surveyDataValue = surveyDataValueService.getSurveyDataValue( organisationUnit, survey, indicator );

                if ( surveyDataValue == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }

                Double surveyValue = Double.valueOf( surveyDataValue.getValue() );

                if ( surveyValue == null )
                {
                    replaceString = NULL_REPLACEMENT;
                }
                else
                {
                    replaceString = String.valueOf( surveyValue );
                    deFlag2 = 1;
                }

                matcher.appendReplacement( buffer, replaceString );
            }

            matcher.appendTail( buffer );

            String resultValue = "";
            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                }
                if ( d == -1 )
                    d = 0.0;
                else
                {
                    d = Math.round( d * Math.pow( 10, 1 ) ) / Math.pow( 10, 1 );
                    resultValue = "" + d;
                }

                if ( deFlag2 == 0 )
                {
                    resultValue = " ";
                }
            }
            else
            {
                resultValue = buffer.toString();
            }
            // System.out.println("Result in Survey : "+ resultValue);
            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal Indicator and survey id", ex );
        }
    }
*/
    // -------------------------------------------------------------------------
    // Get Aggregated Result for dataelement expression from Aggregated Table
    // -------------------------------------------------------------------------
    @SuppressWarnings("unused")
	public String getResultDataValueFromAggregateTable( String formula, Collection<Integer> periodIds,
        OrganisationUnit organisationUnit, String reportModelTB )
    {
        int deFlag1 = 0;
        int isAggregated = 0;

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );

                DataElement dataElement = dataElementService.getDataElement( dataElementId );
                //DataElementCategoryOptionCombo optionCombo = dataElementCategoryOptionComboService.getDataElementCategoryOptionCombo( optionComboId );
                CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( optionComboId );
                if ( dataElement == null || optionCombo == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }
                //if ( dataElement.getType().equalsIgnoreCase( "int" ) )
                //if ( dataElement.getValueType().isInteger() )
                if ( dataElement.isNumericType() )
                {
                    //Double aggregatedValue = aggregatedDataValueService.getAggregatedValue( dataElement.getId(), optionCombo.getId(), periodIds, organisationUnit.getId() );
                    Double aggregatedValue = null;
                    if ( aggregatedValue == null )
                    {
                        replaceString = NULL_REPLACEMENT;
                    }
                    else
                    {
                        replaceString = String.valueOf( aggregatedValue );

                        isAggregated = 1;
                    }

                }
                else
                {
                    deFlag1 = 1;

                    Period tempPeriod = new Period();

                    if ( periodIds == null || periodIds.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        tempPeriod = periodService.getPeriod( periodIds.iterator().next() );
                    }

                    //DataValue dataValue = dataValueService.getDataValue( organisationUnit, dataElement, tempPeriod, optionCombo );
                    DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, optionCombo );

                    if ( dataValue != null && dataValue.getValue() != null )
                    {
                        replaceString = dataValue.getValue();
                    }
                    else
                    {
                        replaceString = "";
                    }

                    if ( replaceString == null )
                    {
                        replaceString = "";
                    }
                }
                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {

                    d = MathUtils.calculateExpression( buffer.toString() );

                    d = Math.round( d );

                }
                catch ( Exception e )
                {
                    d = 0.0;
                    resultValue = "";
                }
                if ( d == -1 )
                {
                    d = 0.0;
                    resultValue = "";
                }
                else
                {
                    // This is to display financial data as it is like 2.1476838
                    resultValue = "" + d;

                    // These lines are to display financial data that do not
                    // have decimals
                    d = d * 10;
                    if ( d % 10 == 0 )
                    {
                        resultValue = "" + (int) d / 10;
                    }

                    d = d / 10;

                    // These line are to display non financial data that do not
                    // require decimals
                    if ( !(reportModelTB.equalsIgnoreCase( "STATIC-FINANCIAL" )) )
                    {
                        resultValue = "" + (int) d;
                    }
                }

            }
            else
            {
                resultValue = buffer.toString();
            }

            if ( isAggregated == 0 )
            {
                resultValue = " ";
            }

            if ( resultValue.equalsIgnoreCase( "" ) )
            {
                resultValue = " ";
            }

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    public Map<String, String> getResultDataValueFromAggregateTable( Integer orgunitId, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + " ) AND "
                    + " organisationunitid = "
                    + orgunitId
                    + " AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( value ) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " organisationunitid = "
                    + orgunitId + " AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + " ) AND "
                + " organisationunitid = "
                + orgunitId
                + " AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                Double aggregatedValue = rs.getDouble( 3 );

                if ( aggregatedValue != null )
                {
                    aggDeMap.put( deId + "." + optionComId, "" + Math.round( aggregatedValue ) );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    
    /*
     * SELECT sag.level, sag.parent, sag.name, case when sag1.dataelementid is null then 0 else sag1.dataelementid  end AS DE,
 case when sag1.categoryoptioncomboid is null then 0 else sag1.categoryoptioncomboid end  AS CCI,
SUM(  case when sag1.value is null then 0 else sag1.value::integer  end ) AS total, sag1.de_uid, sag1.coc_uid 
FROM ( SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual 
FROM _orgunitstructure os 
INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid INNER JOIN _orgunitstructure os1 ON os.organisationunitid = 
CASE WHEN os.level=2 THEN os1.idlevel2 WHEN os.level=3 THEN os1.idlevel3 WHEN os.level=4 THEN os1.idlevel4 WHEN os.level=5 THEN os1.idlevel5 WHEN os.level=6 THEN os1.idlevel6 END 
WHERE os.organisationunitid IN (9541, 10176, 10181, 10177, 1345995, 10178, 10179, 10180, 10478) )sag 
INNER JOIN ( SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid FROM datavalue dv 
INNER JOIN period p ON p.periodid=dv.periodid 
INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid 
INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid 
WHERE dv.dataelementid IN (-1,3) AND dv.periodid IN (309, 307, 296, 306, 233, 310, 261, 294))sag1 ON sag.actual = sag1.sourceid 
GROUP BY sag.parent, sag1.dataelementid,sag1.categoryoptioncomboid ORDER BY sag.level,sag.parent
     */
    
    
    public Map<String, String> getResultDataValueFromAggregateTableByPeriodAgg( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + ") AND "
                    + " organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") "
                    + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM(value) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + ") AND "
                    + " organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") "
                    + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            }
            */
            
            query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + ") AND "
                + " organisationunitid IN ("
                + orgUnitIdsByComma
                + ") AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") "
                + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 4 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( ouId + ":" + deId + ":" + optionComId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromAggDataValueTableForOrgUnitWise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + ") AND "
                    + " organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") "
                    + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM(value) FROM aggregateddatavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + ") AND "
                    + " organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") "
                    + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT organisationunitid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue"
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + ") AND "
                + " organisationunitid IN ("
                + orgUnitIdsByComma
                + ") AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") "
                + " GROUP BY organisationunitid,dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 4 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( deId + "." + optionComId + ":" + ouId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getResultDataValueFromAggregateTable( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        try
        {
            String query = "SELECT organisationunitid,dataelementid,categoryoptioncomboid,periodid,value FROM aggregateddatavalue "
                + " WHERE organisationunitid IN ("
                + orgUnitIdsByComma
                + ") AND "
                + " dataelementid IN ("
                + dataElmentIdsByComma + ") AND " + " periodid IN (" + periodIdsByComma + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Integer periodId = rs.getInt( 4 );
                Double aggregatedValue = rs.getDouble( 5 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( orgUnitId + ":" + deId + ":" + optionComId + ":" + periodId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggNonNumberDataFromDataValueTable( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, GROUP_CONCAT(value) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                String aggregatedNonNumberValue = rs.getString( 3 );
                if ( aggregatedNonNumberValue != null )
                {
                    aggDeMap.put( deId + "." + optionComId, aggregatedNonNumberValue );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        //System.out.println( " orgUnitIdsByComma : " + orgUnitIdsByComma + " dataElmentIdsByComma : " + dataElmentIdsByComma + "   periodIdsByComma : " + periodIdsByComma );
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM(value) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";

            }
            */
            
            //System.out.println( " query : " + query  );
            query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                + " WHERE deleted is false AND dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                Double aggregatedValue = rs.getDouble( 3 );
                if ( aggregatedValue != null )
                {
                    aggDeMap.put( deId + "." + optionComId, "" + aggregatedValue );
                    
                    //System.out.println( " deId : " + deId + " optionComId : " + optionComId + "   aggregatedValue : " + aggregatedValue );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getBatchDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();

        try
        {
            String query = "";
            query = "SELECT periodid, value FROM datavalue " + " WHERE dataelementid IN (" + dataElmentIdsByComma
                + " ) AND " + " sourceid IN (" + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer periodId = rs.getInt( 1 );
                // Double aggregatedValue = rs.getDouble( 2 );
                Integer aggregatedValue = rs.getInt( 2 );
                if ( aggregatedValue != null )
                {
                    aggDeMap.put( "" + periodId, "" + aggregatedValue );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public List<Report_inDesign> getHeaderInfo( String fileName )
    {
        List<Report_inDesign> reportDesignList = new ArrayList<Report_inDesign>();

        String path = System.getProperty( "user.home" ) + File.separator + "dhis" + File.separator
            + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
            + File.separator + fileName;
        try
        {
            String newpath = System.getenv( "DHIS2_HOME" );
            if ( newpath != null )
            {
                path = newpath + File.separator
                    + configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER ).getValue()
                    + File.separator + fileName;
            }
        }
        catch ( NullPointerException npe )
        {
            System.out.println( "DHIS2_HOME not set" );
        }

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "There is no DECodes related XML file in the ra folder" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "header-info" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                String stype = deCodeElement.getAttribute( "stype" );
                String ptype = deCodeElement.getAttribute( "type" );
                int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                Report_inDesign report_inDesign = new Report_inDesign( stype, ptype, sheetno, rowno, colno, expression );
                reportDesignList.add( report_inDesign );
            }// end of for loop with s var
        }// try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return reportDesignList;
    }

    // get text data and date from data value table
    public String getTextDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        String textData = "";

        try
        {
            String query = "";

            query = "SELECT  value  FROM datavalue " + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND "
                + " sourceid IN (" + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                String textValue = rs.getString( 1 );

                if ( textValue != null )
                {
                    // textData += "," + textValue;

                    textData += textValue + ",";
                }
            }

            return textData;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    // get count of dataElement from data value table
    public Integer getDataCountFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Integer dataCount = 0;

        try
        {
            String query = "";

            query = "SELECT COUNT( value ) FROM datavalue " + " WHERE dataelementid IN (" + dataElmentIdsByComma
                + " ) AND " + " sourceid IN (" + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                dataCount = rs.getInt( 1 );
            }

            return dataCount;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromDataValueTableByDeAndOrgUnitwise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String startDate, String endDate )
    {
        
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        //int ouMaxLevel = organisationUnitService.getMaxOfOrganisationUnitLevels();
        int ouMaxLevel = organisationUnitService.getNumberOfOrganisationalLevels();
        
        String query = "";
        
        Map<String, String> aggDataMap = new HashMap<String, String>();
        try
        {
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = 
                    "SELECT sag.level, sag.parent, sag.name, case when sag1.dataelementid is null then 0 else sag1.dataelementid end AS DE, case when sag1.categoryoptioncomboid is null then 0 else sag1.categoryoptioncomboid end AS CCI,SUM( case when sag1.value is null then 0 else sag1.value::integer  end ) AS total, sag1.de_uid, sag1.coc_uid "
                        + " FROM "
                        + "( "
                        + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                        + " FROM _orgunitstructure os "
                        + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                        + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                    for ( int i = 2; i <= ouMaxLevel; i++ )
                    {
                        query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                    }
            
                    query += " END" + " WHERE os.organisationunitid IN ("
                        + orgUnitIdsByComma
                        + ") "
                        + ")sag"
                        + " INNER JOIN "
                        + " ( "
                        + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                        + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                        + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                        + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                        + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND p.startdate BETWEEN '"
                        + startDate + "' AND '" + endDate + "'" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                        + " GROUP BY sag.parent, sag1.dataelementid,sag1.categoryoptioncomboid "
                        + " ORDER BY sag.level,sag.parent";

            }
            
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = 
                    "SELECT sag.level, sag.parent, sag.name, IFNULL(sag1.dataelementid,0) AS DE,IFNULL(sag1.categoryoptioncomboid,0) AS CCI,SUM(IFNULL(sag1.value,0)) AS total, sag1.de_uid, sag1.coc_uid "
                        + " FROM "
                        + "( "
                        + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                        + " FROM _orgunitstructure os "
                        + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                        + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                    for ( int i = 2; i <= ouMaxLevel; i++ )
                    {
                        query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                    }

                    query += " END" + " WHERE os.organisationunitid IN ("
                        + orgUnitIdsByComma
                        + ") "
                        + ")sag"
                        + " INNER JOIN "
                        + " ( "
                        + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                        + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                        + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                        + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                        + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND p.startdate BETWEEN '"
                        + startDate + "' AND '" + endDate + "'" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                        + " GROUP BY sag.parent, sag1.dataelementid,sag1.categoryoptioncomboid "
                        + " ORDER BY sag.level,sag.parent";
                
                
            }
            */
            
            /*
             * 
             * "SELECT sag.level, sag.parent, sag.name, IFNULL(sag1.dataelementid,0) AS DE,IFNULL(sag1.categoryoptioncomboid,0) AS CCI,SUM(IFNULL(sag1.value,0)) AS total "
             * + " FROM " + "( " +
             * "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
             * + " FROM _orgunitstructure os " +
             * " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
             * +
             * " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE "
             * ;
             * 
             * for( int i = 2; i <= ouMaxLevel; i++ ) { query +=
             * " WHEN os.level="+i+" THEN os1.idlevel"+i+ " "; }
             * 
             * query += " END" + " WHERE os.organisationunitid IN ("+
             * orgUnitIdsByComma +") " + ")sag" + " INNER JOIN "+ " ( " +
             * " SELECT dv.dataelementid,dv.categoryoptioncomboid,dv.value,dv.sourceid "
             * + " FROM datavalue dv "+
             * " INNER JOIN period p ON p.periodid=dv.periodid "+
             * " WHERE dv.dataelementid IN ("+ dataElmentIdsByComma +") " +
             * " AND p.startdate BETWEEN '"+ startDate +"' AND '"+ endDate +"'"
             * + ")sag1 " + "ON sag.actual = sag1.sourceid " +
             * " GROUP BY sag.parent, sag1.dataelementid,sag1.categoryoptioncomboid "
             * + " ORDER BY sag.level,sag.parent";
             */

            query = 
                "SELECT sag.level, sag.parent, sag.name, case when sag1.dataelementid is null then 0 else sag1.dataelementid end AS DE, case when sag1.categoryoptioncomboid is null then 0 else sag1.categoryoptioncomboid end AS CCI,SUM( case when sag1.value is null then 0 else sag1.value::integer  end ) AS total, sag1.de_uid, sag1.coc_uid "
                    + " FROM "
                    + "( "
                    + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                    + " FROM _orgunitstructure os "
                    + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                    + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                for ( int i = 2; i <= ouMaxLevel; i++ )
                {
                    query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                }
        
                query += " END" + " WHERE os.organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") "
                    + ")sag"
                    + " INNER JOIN "
                    + " ( "
                    + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                    + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                    + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                    + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                    + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND p.startdate BETWEEN '"
                    + startDate + "' AND '" + endDate + "'" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                    + " GROUP BY sag.parent, sag1.dataelementid,sag1.categoryoptioncomboid "
                    + " ORDER BY sag.level,sag.parent";
                
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 2 );
                //Integer deId = rs.getInt( 4 );
                //Integer optionComId = rs.getInt( 5 );
                // Integer periodId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 6 );
                String deUID = rs.getString( 7 );
                String optionComUID = rs.getString( 8 );

                if ( aggregatedValue != null )
                {
                    aggDataMap.put( ouId + ":" + deUID + ":" + optionComUID, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromDataValueTableByDeAndOrgUnitwise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        //int ouMaxLevel = organisationUnitService.getMaxOfOrganisationUnitLevels();
        int ouMaxLevel = organisationUnitService.getNumberOfOrganisationalLevels();

        Map<String, String> aggDataMap = new HashMap<String, String>();
        
        String query = "";
        
        try
        {   /*    
            if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query =

                    "SELECT sag.level, sag.parent, sag.name, IFNULL(sag1.dataelementid,0) AS DE,IFNULL(sag1.categoryoptioncomboid,0) AS CCI,SUM(IFNULL(sag1.value,0)) AS total, sag1.de_uid, sag1.coc_uid "
                        + " FROM "
                        + "( "
                        + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                        + " FROM _orgunitstructure os "
                        + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                        + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                    for ( int i = 2; i <= ouMaxLevel; i++ )
                    {
                        query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                    }

                    query += " END"
                        + " WHERE os.organisationunitid IN ("
                        + orgUnitIdsByComma
                        + ") "
                        + ")sag"
                        + " INNER JOIN "
                        + " ( "
                        + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                        + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                        + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                        + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                        + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND dv.periodid IN ("
                        + periodIdsByComma + ")" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                        + " GROUP BY sag.level, sag.parent, sag.name, sag1.dataelementid, sag1.categoryoptioncomboid, sag1.de_uid, sag1.coc_uid "
                        + " ORDER BY sag.level,sag.parent";
            }
           
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query =

                    "SELECT sag.level, sag.parent, sag.name, case when sag1.dataelementid is null then 0 else sag1.dataelementid  end AS DE, case when sag1.categoryoptioncomboid is null then 0 else sag1.categoryoptioncomboid end  AS CCI, SUM( case when sag1.value is null then 0 else sag1.value::integer  end ) AS total, sag1.de_uid, sag1.coc_uid "
                        + " FROM "
                        + "( "
                        + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                        + " FROM _orgunitstructure os "
                        + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                        + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                    for ( int i = 2; i <= ouMaxLevel; i++ )
                    {
                        query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                    }

                    query += " END"
                        + " WHERE os.organisationunitid IN ("
                        + orgUnitIdsByComma
                        + ") "
                        + ")sag"
                        + " INNER JOIN "
                        + " ( "
                        + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                        + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                        + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                        + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                        + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND dv.periodid IN ("
                        + periodIdsByComma + ")" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                        + " GROUP BY sag.level, sag.parent, sag.name, sag1.dataelementid, sag1.categoryoptioncomboid, sag1.de_uid, sag1.coc_uid "
                        + " ORDER BY sag.level,sag.parent";                
            }
            */
            query =

                "SELECT sag.level, sag.parent, sag.name, case when sag1.dataelementid is null then 0 else sag1.dataelementid  end AS DE, case when sag1.categoryoptioncomboid is null then 0 else sag1.categoryoptioncomboid end  AS CCI, SUM( case when sag1.value is null then 0 else sag1.value::integer  end ) AS total, sag1.de_uid, sag1.coc_uid "
                    + " FROM "
                    + "( "
                    + "SELECT os.level,os.organisationunitid AS parent,ou.name,os1.organisationunitid AS actual "
                    + " FROM _orgunitstructure os "
                    + " INNER JOIN organisationunit ou ON ou.organisationunitid=os.organisationunitid "
                    + " INNER JOIN _orgunitstructure os1 ON os.organisationunitid = CASE ";

                for ( int i = 2; i <= ouMaxLevel; i++ )
                {
                    query += " WHEN os.level=" + i + " THEN os1.idlevel" + i + " ";
                }

                query += " END"
                    + " WHERE os.organisationunitid IN ("
                    + orgUnitIdsByComma
                    + ") "
                    + ")sag"
                    + " INNER JOIN "
                    + " ( "
                    + " SELECT dv.dataelementid, de.uid AS de_uid, dv.categoryoptioncomboid, coc.uid AS coc_uid, dv.value,dv.sourceid "
                    + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                    + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                    + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                    + " WHERE dv.dataelementid IN (" + dataElmentIdsByComma + ") " + " AND dv.periodid IN ("
                    + periodIdsByComma + ")" + ")sag1 " + "ON sag.actual = sag1.sourceid "
                    + " GROUP BY sag.level, sag.parent, sag.name, sag1.dataelementid, sag1.categoryoptioncomboid, sag1.de_uid, sag1.coc_uid "
                    + " ORDER BY sag.level,sag.parent";    
                
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 2 );
                // Integer deId = rs.getInt( 4 );
                // Integer optionComId = rs.getInt( 5 );
                Double aggregatedValue = rs.getDouble( 6 );
                String deUID = rs.getString( 7 );
                String optionComUID = rs.getString( 8 );

                if ( aggregatedValue != null )
                {
                    aggDataMap.put( ouId + ":" + deUID + ":" + optionComUID, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getCapturedDataFromDataValueTableByDeAndOrgUnitwise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        String query = "";
        
        Map<String, String> aggDataMap = new HashMap<String, String>();
        try
        {
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = " SELECT dv.sourceid, de.uid AS de_uid, coc.uid AS coc_uid, SUM(dv.value) "
                    + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                    + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                    + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                    + " WHERE " + " dv.dataelementid IN (" + dataElmentIdsByComma + ") AND " + " dv.periodid IN ("
                    + periodIdsByComma + ") AND " + " dv.sourceid IN (" + orgUnitIdsByComma + ") "
                    + " GROUP BY dv.sourceid, dv.dataelementid, dv.categoryoptioncomboid ";
            }
            
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                
                query = " SELECT dv.sourceid, de.uid AS de_uid, coc.uid AS coc_uid, SUM( cast( value as numeric) ) "
                    + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                    + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                    + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                    + " WHERE " + " dv.dataelementid IN (" + dataElmentIdsByComma + ") AND " + " dv.periodid IN ("
                    + periodIdsByComma + ") AND " + " dv.sourceid IN (" + orgUnitIdsByComma + ") "
                    + " GROUP BY dv.sourceid, de.uid, coc.uid "; 
            }
            */
            query = " SELECT dv.sourceid, de.uid AS de_uid, coc.uid AS coc_uid, SUM( cast( value as numeric) ) "
                + " FROM datavalue dv " + " INNER JOIN period p ON p.periodid=dv.periodid "
                + " INNER JOIN dataelement de ON de.dataelementid=dv.dataelementid "
                + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=dv.categoryoptioncomboid "
                + " WHERE " + " dv.dataelementid IN (" + dataElmentIdsByComma + ") AND " + " dv.periodid IN ("
                + periodIdsByComma + ") AND " + " dv.sourceid IN (" + orgUnitIdsByComma + ") "
                + " GROUP BY dv.sourceid, de.uid, coc.uid "; 
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 1 );
                String deUID = rs.getString( 2 );
                String optionComUID = rs.getString( 3 );
                Double aggregatedValue = rs.getDouble( 4 );

                if ( aggregatedValue != null )
                {
                    aggDataMap.put( ouId + ":" + deUID + ":" + optionComUID, "" + aggregatedValue );
                    
                    //System.out.println( ouId +  " -- " + deUID +  " -- " + optionComUID +  " -- " + aggregatedValue );
                    
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromDataValueTableByDeAndPeriodwise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid,periodid,SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + " ) AND "
                    + " sourceid IN ("
                    + orgUnitIdsByComma
                    + " ) AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid,periodid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid,periodid,SUM( value ) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid,periodid";
            }
            */
            
            query = "SELECT dataelementid,categoryoptioncomboid,periodid,SUM( cast( value as numeric) ) FROM datavalue "
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + " ) AND "
                + " sourceid IN ("
                + orgUnitIdsByComma
                + " ) AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid,periodid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                Integer periodId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 4 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( deId + ":" + optionComId + ":" + periodId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getDataFromDataValueTableByPeriodAgg( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + " ) AND "
                    + " sourceid IN ("
                    + orgUnitIdsByComma
                    + " ) AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM(value) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                + "  WHERE deleted is false AND dataelementid IN ("
                + dataElmentIdsByComma
                + " ) AND "
                + " sourceid IN ("
                + orgUnitIdsByComma
                + " ) AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 4 );
                if ( aggregatedValue != null  )
                {
                    aggDataMap.put( orgUnitId + ":" + deId + ":" + optionComId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        try
        {
            String query = "SELECT sourceid,dataelementid,categoryoptioncomboid,periodid,value FROM datavalue "
                + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN (" + orgUnitIdsByComma
                + " ) AND " + " periodid IN (" + periodIdsByComma + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Integer periodId = rs.getInt( 4 );
                Double aggregatedValue = rs.getDouble( 5 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( orgUnitId + ":" + deId + ":" + optionComId + ":" + periodId, "" + aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public String getResultDataValueFromAggregateTable( String formula, String periodIdsByComma, Integer orgunitId )
    {
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        String query = "";

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );
                /*
                if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
                {
                    query = "SELECT SUM( cast( value as numeric) ) FROM aggregateddatavalue WHERE dataelementid = "
                        + dataElementId + " AND categoryoptioncomboid = " + optionComboId + " AND periodid IN ("
                        + periodIdsByComma + ")" + " AND organisationunitid = " + orgunitId;
                }
                else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
                {
                    query = "SELECT SUM(value) FROM aggregateddatavalue WHERE dataelementid = " + dataElementId
                        + " AND categoryoptioncomboid = " + optionComboId + " AND periodid IN (" + periodIdsByComma
                        + ")" + " AND organisationunitid = " + orgunitId;
                }
                */
                query = "SELECT SUM( cast( value as numeric) ) FROM aggregateddatavalue WHERE dataelementid = "
                    + dataElementId + " AND categoryoptioncomboid = " + optionComboId + " AND periodid IN ("
                    + periodIdsByComma + ")" + " AND organisationunitid = " + orgunitId;
                
                SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

                Double aggregatedValue = null;
                if ( rs.next() )
                {
                    aggregatedValue = rs.getDouble( 1 );
                }

                if ( aggregatedValue == null )
                {
                    replaceString = NULL_REPLACEMENT;
                }
                else
                {
                    replaceString = String.valueOf( aggregatedValue );
                }

                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            double d = 0.0;
            try
            {

                d = MathUtils.calculateExpression( buffer.toString() );

                d = Math.round( d );

            }
            catch ( Exception e )
            {
                d = 0.0;
                resultValue = "";
            }

            resultValue = "" + (double) d;

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    /*
    public String getSurveyDesc( String formula )
    {
        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int surveyId = Integer.parseInt( replaceString );

                Survey survey = surveyService.getSurvey( surveyId );

                if ( survey == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }
                else
                {
                    replaceString = survey.getDescription();
                }

                matcher.appendReplacement( buffer, replaceString );
            }

            matcher.appendTail( buffer );

            String resultValue = buffer.toString();
            ;

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }
	*/
    
    public String getAggCountForTextData( String formula, Date startDate, Date endDate,
        OrganisationUnit organisationUnit )
    {
        String[] partsOfFormula = formula.split( ":" );

        int dataElementId = Integer.parseInt( partsOfFormula[0] );
        int optComboId = Integer.parseInt( partsOfFormula[1] );
        String compareText = partsOfFormula[2];

        Collection<Period> periods = new ArrayList<Period>( periodService.getPeriodsBetweenDates( startDate, endDate ) );
        Collection<OrganisationUnit> orgUnits = new ArrayList<OrganisationUnit>(
            organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getId() ) );

        int recordCount = 0;
        try
        {
            String query = "SELECT COUNT(*) FROM datavalue WHERE dataelementid = " + dataElementId
                + " AND categoryoptioncomboid = " + optComboId + " AND periodid IN ("
                + getCommaDelimitedString( getIdentifiers( Period.class, periods ) ) + ")" + " AND sourceid IN ("
                + getCommaDelimitedString( getIdentifiers( OrganisationUnit.class, orgUnits ) ) + ")"
                + " AND value like '" + compareText + "'";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                recordCount = rs.getInt( 1 );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return "" + recordCount;
    }

    public String getCountForTextData( String formula, Date startDate, Date endDate, OrganisationUnit organisationUnit )
    {
        String[] partsOfFormula = formula.split( ":" );

        int dataElementId = Integer.parseInt( partsOfFormula[0] );
        int optComboId = Integer.parseInt( partsOfFormula[1] );
        String compareText = partsOfFormula[2];

        Collection<Period> periods = new ArrayList<Period>( periodService.getPeriodsBetweenDates( startDate, endDate ) );

        int recordCount = 0;
        try
        {
            String query = "SELECT COUNT(*) FROM datavalue WHERE dataelementid = " + dataElementId
                + " AND categoryoptioncomboid = " + optComboId + " AND periodid IN ("
                + getCommaDelimitedString( getIdentifiers( Period.class, periods ) ) + ")" + " AND sourceid IN ("
                + organisationUnit.getId() + ")" + " AND value like '" + compareText + "'";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                recordCount = rs.getInt( 1 );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return "" + recordCount;
    }

    public String getDataelementIdsAsString( List<Indicator> indicatorList )
    {
    	String dataElmentIdsByComma = null;
        //Set<DataElement> dataElementList = expressionService.getDataElementsInIndicators( indicatorList );
        
        //Collection<Integer> dataElementIds = new ArrayList<Integer>( getIdentifiers( DataElement.class, dataElementList ) );
        //String dataElmentIdsByComma = getCommaDelimitedString( dataElementIds );

        if ( dataElmentIdsByComma == null )
        {
            dataElmentIdsByComma = "-1";
        }

        /*
         * String dataElmentIdsByComma = "-1"; for ( Indicator indicator :
         * indicatorList ) { String formula = indicator.getNumerator() + " + " +
         * indicator.getDenominator(); try { Pattern pattern = Pattern.compile(
         * "(\\[\\d+\\.\\d+\\])" );
         * 
         * Matcher matcher = pattern.matcher( formula ); StringBuffer buffer =
         * new StringBuffer();
         * 
         * while ( matcher.find() ) { String replaceString = matcher.group();
         * 
         * replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
         * replaceString = replaceString.substring( 0, replaceString.indexOf(
         * '.' ) );
         * 
         * int dataElementId = Integer.parseInt( replaceString );
         * dataElmentIdsByComma += "," + dataElementId; replaceString = "";
         * matcher.appendReplacement( buffer, replaceString ); } } catch (
         * Exception e ) {
         * 
         * } }
         */

        return dataElmentIdsByComma;
    }

    public String getDataelementIdsByStype( List<Report_inDesign> reportDesignList, String sType )
    {
        String dataElmentIdsByComma = "-1";
        for ( Report_inDesign report_inDesign : reportDesignList )
        {
            if ( report_inDesign.getStype().equalsIgnoreCase( sType ) )
            {
                String formula = report_inDesign.getExpression();

                try
                {
                    Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

                    Matcher matcher = pattern.matcher( formula );
                    StringBuffer buffer = new StringBuffer();

                    while ( matcher.find() )
                    {
                        String replaceString = matcher.group();

                        replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                        replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                        int dataElementId = Integer.parseInt( replaceString );
                        dataElmentIdsByComma += "," + dataElementId;
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                    }
                }
                catch ( Exception e )
                {

                }
            }
        }

        return dataElmentIdsByComma;
    }

    public String getDataelementIds( List<Report_inDesign> reportDesignList )
    {
        String dataElmentIdsByComma = "-1";
        for ( Report_inDesign report_inDesign : reportDesignList )
        {
            String formula = report_inDesign.getExpression();
            try
            {
                Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

                Matcher matcher = pattern.matcher( formula );
                StringBuffer buffer = new StringBuffer();

                while ( matcher.find() )
                {
                    String replaceString = matcher.group();

                    replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                    replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                    int dataElementId = Integer.parseInt( replaceString );
                    dataElmentIdsByComma += "," + dataElementId;
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                }
            }
            catch ( Exception e )
            {

            }
        }

        return dataElmentIdsByComma;
    }

    public String getAggVal( String expression, Map<String, String> aggDeMap )
    {
        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( expression );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                replaceString = aggDeMap.get( replaceString );

                if ( replaceString == null )
                {
                    replaceString = "0";
                }

                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            double d = 0.0;
            try
            {

                d = MathUtils.calculateExpression( buffer.toString() );

                d = Math.round( d );

            }
            catch ( Exception e )
            {
                d = 0.0;
                resultValue = "";
            }

            resultValue = "" + (double) d;

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    public Map<String, List<String>> getIndicatorDataValueFromAggregateTable( Integer orgunitId,
        String indicatorIdsByComma, Integer periodId )
    {
        Map<String, List<String>> aggIndicatorMap = new HashMap<String, List<String>>();
        try
        {
            String query = "SELECT indicatorid, numeratorvalue, denominatorvalue FROM aggregatedindicatorvalue "
                + " WHERE indicatorid IN (" + indicatorIdsByComma + " ) AND " + " organisationunitid = " + orgunitId
                + " AND " + " periodid = " + periodId;

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer indicatorId = rs.getInt( 1 );
                Double aggregatedIndicatorValue = rs.getDouble( 2 );
                Double aggNumeratorValue = rs.getDouble( 3 );
                Double aggDenominatorValue = rs.getDouble( 4 );

                List<String> tempList = new ArrayList<String>();
                if ( aggregatedIndicatorValue != null )
                {
                    tempList.add( "" + aggregatedIndicatorValue );
                    tempList.add( "" + aggNumeratorValue );
                    tempList.add( "" + aggDenominatorValue );

                    aggIndicatorMap.put( "" + indicatorId, tempList );
                }
            }

            return aggIndicatorMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public double getIndividualIndicatorValue( Indicator indicator, OrganisationUnit orgunit, Date startDate,
        Date endDate )
    {
        String numeratorExp = indicator.getNumerator();
        String denominatorExp = indicator.getDenominator();
        int indicatorFactor = indicator.getIndicatorType().getFactor();
        String reportModelTB = "";
        String numeratorVal = getIndividualResultDataValue( numeratorExp, startDate, endDate, orgunit, reportModelTB );
        String denominatorVal = getIndividualResultDataValue( denominatorExp, startDate, endDate, orgunit,
            reportModelTB );

        double numeratorValue;
        try
        {
            numeratorValue = Double.parseDouble( numeratorVal );
        }
        catch ( Exception e )
        {
            numeratorValue = 0.0;
        }

        double denominatorValue;
        try
        {
            denominatorValue = Double.parseDouble( denominatorVal );
        }
        catch ( Exception e )
        {
            denominatorValue = 1.0;
        }

        double aggregatedValue;
        try
        {
            if ( denominatorValue == 0 )
            {
                aggregatedValue = 0.0;
            }
            else
            {
                aggregatedValue = (numeratorValue / denominatorValue) * indicatorFactor;
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Exception while calculating Indicator value for Indicaotr " + indicator.getName() );
            aggregatedValue = 0.0;
        }

        return aggregatedValue;
    }

    public Map<Integer, Integer> getOrgunitLevelMap()
    {
        Map<Integer, Integer> orgUnitLevelMap = new HashMap<Integer, Integer>();
        try
        {
            String query = "SELECT organisationunitid,level FROM _orgunitstructure";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer level = rs.getInt( 2 );

                orgUnitLevelMap.put( orgUnitId, level );
            }

            return orgUnitLevelMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    //

    public Map<String, String> getLLDeathDataFromLLDataValueTable( Integer orgunitId,
        String dataElmentIdsForLLDeathByComma, String periodIdsByComma, String recordNoByComma )
    {
        Map<String, String> aggDeForLLDeathMap = new HashMap<String, String>();
        // DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            query = "SELECT value,dataelementid,categoryoptioncomboid,recordno FROM lldatavalue"
                + " WHERE dataelementid IN (" + dataElmentIdsForLLDeathByComma + " ) AND " + " sourceid = " + orgunitId
                + " AND " + " periodid IN (" + periodIdsByComma + ") AND recordno IN  (" + recordNoByComma + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            String tempValue = "";

            while ( rs.next() )
            {
                tempValue = rs.getString( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Integer recordNo = rs.getInt( 4 );
                // Double aggregatedValue = rs.getDouble( 3 );
                if ( tempValue != null )
                {
                    aggDeForLLDeathMap.put( deId + "." + optionComId + ":" + recordNo, "" + tempValue );
                }
            }

            return aggDeForLLDeathMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromDataValueTableForOrgUnitWise( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            /*
            if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT sourceid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue"
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + ") AND "
                    + " sourceid IN ("
                    + orgUnitIdsByComma
                    + ") AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") "
                    + " GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT sourceid, dataelementid,categoryoptioncomboid, SUM(value) FROM datavalue"
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + ") AND " + " sourceid IN ("
                    + orgUnitIdsByComma + ") AND " + " periodid IN (" + periodIdsByComma + ") "
                    + " GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            */
            
            query = "SELECT sourceid, dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue"
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + ") AND "
                + " sourceid IN ("
                + orgUnitIdsByComma
                + ") AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") "
                + " GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );
            String tempValue = "";
            while ( rs.next() )
            {
                Integer ouId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                tempValue = rs.getString( 4 );
                // Double aggregatedValue = rs.getDouble( 4 );
                if ( tempValue != null )
                {
                    aggDataMap.put( deId + "." + optionComId + ":" + ouId, "" + tempValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getResultDataValueFromDataValueTable( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDataMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            
            /*if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + " ) AND "
                    + " sourceid IN ("
                    + orgUnitIdsByComma
                    + " ) AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM(value) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            }
            */
            
            query = "SELECT sourceid,dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + " ) AND "
                + " sourceid IN ("
                + orgUnitIdsByComma
                + " ) AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") GROUP BY sourceid,dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                Double aggregatedValue = rs.getDouble( 4 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( deId + "." + optionComId + ":" + orgUnitId, "" + aggregatedValue );

                    // aggDataMap.put( orgUnitId+":"+deId+":"+optionComId,
                    // ""+aggregatedValue );
                }
            }

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Map<String, String> getAggDataFromAggDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        // System.out.println("o,p,d"+orgUnitIdsByComma+periodIdsByComma+dataElmentIdsByComma
        // );
        Map<String, String> aggDeMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            
            /*if ( dataBaseInfo.getType().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue "
                    + " WHERE dataelementid IN ("
                    + dataElmentIdsByComma
                    + " ) AND "
                    + " organisationunitid IN ("
                    + orgUnitIdsByComma
                    + " ) AND "
                    + " periodid IN ("
                    + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM(value) FROM aggregateddatavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " organisationunitid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM aggregateddatavalue "
                + " WHERE dataelementid IN ("
                + dataElmentIdsByComma
                + " ) AND "
                + " organisationunitid IN ("
                + orgUnitIdsByComma
                + " ) AND "
                + " periodid IN ("
                + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                Double aggregatedValue = rs.getDouble( 3 );
                if ( aggregatedValue != null )
                {
                    aggDeMap.put( deId + "." + optionComId, "" + aggregatedValue );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    public Integer getOrgunitCountByOrgunitGroup( String orgunitGroupIdsByComma, Integer orgUnitId )
    {
        Integer totalOrgUnitCount = 0;
        //int maxOULevels = organisationUnitService.getMaxOfOrganisationUnitLevels();
        int maxOULevels = organisationUnitService.getNumberOfOrganisationalLevels();

        try
        {
            String query = "SELECT COUNT(*) FROM orgunitgroupmembers WHERE orgunitgroupid IN ( "
                + orgunitGroupIdsByComma + " ) AND "
                + " organisationunitid IN ( SELECT organisationunitid FROM _orgunitstructure WHERE ";

            for ( int i = 1; i <= maxOULevels; i++ )
            {
                query += " idlevel" + i + " = " + orgUnitId + " OR ";
            }
            query = query.substring( 0, query.length() - 4 );

            query += ")";

            /*
             * " idlevel1 = "+ orgUnitId +" OR idlevel2 = "+ orgUnitId
             * +" OR idlevel3 = "+ orgUnitId +" OR " + " idlevel4 = "+ orgUnitId
             * +" OR idlevel5 = "+ orgUnitId +" OR " + " idlevel6 = "+ orgUnitId
             * +" OR idlevel7 = "+ orgUnitId +" OR idlevel8 = "+ orgUnitId
             * +" )";
             */

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                totalOrgUnitCount = rs.getInt( 1 );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return totalOrgUnitCount;
    }

    public Integer getReportingOrgunitCountByDataset( Integer dataSetId, Integer orgUnitId )
    {
        Integer reportingOrgUnitCount = 0;
        //int maxOULevels = organisationUnitService.getMaxOfOrganisationUnitLevels();
        int maxOULevels = organisationUnitService.getNumberOfOrganisationalLevels();
        try
        {
            String query = "SELECT COUNT(*) FROM datasetsource WHERE datasetid = " + dataSetId + " AND "
                + " sourceid IN ( SELECT organisationunitid FROM _orgunitstructure WHERE ";

            for ( int i = 1; i <= maxOULevels; i++ )
            {
                query += " idlevel" + i + " = " + orgUnitId + " OR ";
            }
            query = query.substring( 0, query.length() - 4 );

            query += ")";

            /*
             * " idlevel1 = "+ orgUnitId +" OR idlevel2 = "+ orgUnitId
             * +" OR idlevel3 = "+ orgUnitId +" OR " + " idlevel4 = "+ orgUnitId
             * +" OR idlevel5 = "+ orgUnitId +" OR " + " idlevel6 = "+ orgUnitId
             * +" OR idlevel7 = "+ orgUnitId +" OR idlevel8 = "+ orgUnitId
             * +" )";
             */

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                reportingOrgUnitCount = rs.getInt( 1 );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return reportingOrgUnitCount;
    }

    public Integer getReportingOrgunitCountByDataset( Integer dataSetId, Integer orgUnitId, Integer periodId )
    {
        Double constValue = 0.0;
        Integer reportingOrgUnitCount = 0;

        List<OrganisationUnit> orgUnitList = new ArrayList<OrganisationUnit>();
        orgUnitList.addAll( organisationUnitService.getOrganisationUnitWithChildren( orgUnitId ) );

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Collection<DataElement> dataElements = new ArrayList<DataElement>( dataSet.getDataElements() );
        int dataSetMemberCount = 0;
        for ( DataElement de : dataElements )
        {
            //dataSetMemberCount += de.getCategoryCombo().getOptionCombos().size();
            dataSetMemberCount += de.getCategoryOptionCombos().size();
        }
        Collection<Integer> dataElementIds = new ArrayList<Integer>( getIdentifiers( DataElement.class, dataElements ) );
        String dataElementIdsByComma = getCommaDelimitedString( dataElementIds );

        List<OrganisationUnit> dataSetOrgunits = new ArrayList<OrganisationUnit>( dataSet.getSources() );
        orgUnitList.retainAll( dataSetOrgunits );

        for ( OrganisationUnit orgUnit : orgUnitList )
        {
            String query = "SELECT COUNT(*) FROM datavalue WHERE dataelementid IN (" + dataElementIdsByComma
                + ") AND sourceid = " + orgUnit.getId() + " AND periodid =" + periodId;

            double dataStatusPercentatge = 0.0;

            SqlRowSet sqlResultSet = jdbcTemplate.queryForRowSet( query );

            if ( sqlResultSet.next() )
            {
                try
                {
                    dataStatusPercentatge = ((double) sqlResultSet.getInt( 1 ) / (double) dataSetMemberCount) * 100.0;
                }
                catch ( Exception e )
                {
                    dataStatusPercentatge = 0.0;
                }
            }
            else
            {
                dataStatusPercentatge = 0.0;
            }

            if ( dataStatusPercentatge > 100.0 )
            {
                dataStatusPercentatge = 100;
            }

            // System.out.println( query + "  : " + dataStatusPercentatge );

            dataStatusPercentatge = Math.round( dataStatusPercentatge * Math.pow( 10, 0 ) ) / Math.pow( 10, 0 );
            if ( dataStatusPercentatge > constValue )
            {
                reportingOrgUnitCount += 1;
            }
        }

        return reportingOrgUnitCount;
    }

    public String getResultDataValueForOrgUnitGroupMember( String formula, String orgUnitIdsByComma, Date startDate,
        Date endDate, String reportModelTB )
    {
        int deFlag1 = 0;
        int isAggregated = 0;

        List<Period> tempPeriodList = new ArrayList<Period>( periodService.getIntersectingPeriods( startDate, endDate ) );
        Collection<Integer> tempPeriodIds = new ArrayList<Integer>( getIdentifiers( Period.class, tempPeriodList ) );
        String periodIdsByComma = getCommaDelimitedString( tempPeriodIds );

        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

            Matcher matcher = pattern.matcher( formula );
            StringBuffer buffer = new StringBuffer();

            String query = "";
            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                String optionComboIdStr = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                int dataElementId = Integer.parseInt( replaceString );
                int optionComboId = Integer.parseInt( optionComboIdStr );

                DataElement dataElement = dataElementService.getDataElement( dataElementId );
                //DataElementCategoryOptionCombo optionCombo = dataElementCategoryOptionComboService.getDataElementCategoryOptionCombo( optionComboId );
                CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( optionComboId );
                if ( dataElement == null || optionCombo == null )
                {
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                    continue;
                }

                //if ( dataElement.getType().equalsIgnoreCase( "int" ) )
                //if ( dataElement.getValueType().isInteger() )
                if ( dataElement.isNumericType() )
                {
                    query = "SELECT SUM(value) FROM datavalue " + " WHERE dataelementid = " + dataElement.getId()
                        + " AND " + " sourceid IN (" + orgUnitIdsByComma + " ) AND " + " periodid IN ("
                        + periodIdsByComma + ")";

                    SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

                    if ( rs.next() )
                    {
                        Double aggregatedValue = rs.getDouble( 1 );

                        if ( aggregatedValue == null )
                        {
                            replaceString = NULL_REPLACEMENT;
                        }
                        else
                        {
                            replaceString = String.valueOf( aggregatedValue );
                            isAggregated = 1;
                        }
                    }
                }

                else
                {
                    deFlag1 = 1;
                    PeriodType dePeriodType = getDataElementPeriodType( dataElement );

                    List<Period> periodList = new ArrayList<Period>( periodService.getPeriodsBetweenDates(
                        dePeriodType, startDate, endDate ) );
                    Period tempPeriod = new Period();
                    if ( periodList == null || periodList.isEmpty() )
                    {
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                        continue;
                    }
                    else
                    {
                        tempPeriod = (Period) periodList.get( 0 );
                    }

                    query = "SELECT CONCAT(value)  FROM datavalue " + " WHERE dataelementid =" + dataElement.getId()
                        + " AND " + " sourceid IN (" + orgUnitIdsByComma + " ) AND " + " periodid = "
                        + tempPeriod.getId() + ")";

                    SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

                    if ( rs.next() )
                    {
                        String value = rs.getString( 1 );

                        if ( value != null )
                        {
                            replaceString = value;
                        }
                        else
                        {
                            replaceString = "";
                        }
                    }

                    if ( replaceString == null )
                    {
                        replaceString = "";
                    }
                }
                matcher.appendReplacement( buffer, replaceString );

                resultValue = replaceString;
            }

            matcher.appendTail( buffer );

            if ( deFlag1 == 0 )
            {
                double d = 0.0;
                try
                {
                    d = MathUtils.calculateExpression( buffer.toString() );
                    d = Math.round( d );
                }
                catch ( Exception e )
                {
                    d = 0.0;
                    resultValue = "";
                }
                if ( d == -1 )
                {
                    d = 0.0;
                    resultValue = "";
                }
                else
                {
                    // This is to display financial data as it is like 2.1476838
                    resultValue = "" + d;

                    // These lines are to display financial data that do not
                    // have decimals
                    d = d * 10;
                    if ( d % 10 == 0 )
                    {
                        resultValue = "" + (int) d / 10;
                    }

                    d = d / 10;

                    // These line are to display non financial data that do not
                    // require decimals
                    if ( !(reportModelTB.equalsIgnoreCase( "STATIC-FINANCIAL" )) )
                    {
                        resultValue = "" + (double) d;
                    }
                }

            }
            else
            {
                resultValue = buffer.toString();
            }

            if ( isAggregated == 0 )
            {
                resultValue = " ";
            }

            if ( resultValue.equalsIgnoreCase( "" ) )
            {
                resultValue = " ";
            }

            return resultValue;
        }

        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

    public Map<String, String> getResultDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        // System.out.println( " Inside Query Start : " + new Date() );

        Map<String, String> aggDataMap = new HashMap<String, String>();

        try
        {
            String query = "SELECT sourceid,dataelementid,categoryoptioncomboid,periodid,value FROM datavalue "
                + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN (" + orgUnitIdsByComma
                + " ) AND " + " periodid IN (" + periodIdsByComma + ")";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer deId = rs.getInt( 2 );
                Integer optionComId = rs.getInt( 3 );
                // Integer periodId = rs.getInt( 4 );
                Double aggregatedValue = rs.getDouble( 5 );
                if ( aggregatedValue != null )
                {
                    aggDataMap.put( deId + "." + optionComId + ":" + orgUnitId, "" + aggregatedValue );
                }
            }

            // System.out.println( " Inside Query End : " + new Date() );

            return aggDataMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    // delete Batch lock exception
    public void deleteLockException( String orgUnitIdsByComma, String periodIdsByComma, String dataSetIdsByComma )
    {
        try
        {
            String query = "DELETE FROM lockexception WHERE organisationunitid IN (" + orgUnitIdsByComma + ")"
                + " AND periodid IN (" + periodIdsByComma + ")" + " AND datasetid IN (" + dataSetIdsByComma + ")";

            jdbcTemplate.execute( query );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    // create batch lock exception
    public void createBatchLockExceptions( String insertQuery )
    {
        try
        {
            jdbcTemplate.execute( insertQuery );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    // get Lock Exception from organisationUnitId,periodId,dataSetId
    public Boolean getLockException( Integer organisationUnitId, Integer periodId, Integer dataSetId )
    {
        Boolean recordExist = false;

        try
        {
            String query = "SELECT organisationunitid,periodid,datasetid FROM lockexception WHERE organisationunitid = "
                + organisationUnitId + " AND " + " periodid = " + periodId + " AND datasetid = " + dataSetId;

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            if ( rs.next() )
            {
                recordExist = true;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return recordExist;
    }

    // Methods for delete Lock Exception
    @Transactional
    public void deleteLockException( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        reportStore.deleteLockException( dataSet, period, organisationUnit );
    }

    // Get Data value for Latest Period
    @Transactional
    public DataValue getLatestDataValue( DataElement dataElement, CategoryOptionCombo categoryOptionCombo,
        OrganisationUnit organisationUnit )
    {
        return reportStore.getLatestDataValue( dataElement, categoryOptionCombo, organisationUnit );
    }

    // methods for getting Data from data value for GOI Monthly Report
    public Map<String, String> getDataFromDataValueTableForGoiMonthly( String orgUnitIdsByComma,
        String dataElmentIdsByComma, String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();
        //DatabaseInfo dataBaseInfo = databaseInfoProvider.getDatabaseInfo();
        try
        {
            String query = "";
            //if ( dataBaseInfo.getType().equalsIgnoreCase( "postgres" ) )
            /*
            if ( dataBaseInfo.getUser().equalsIgnoreCase( "postgresql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                    + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                    + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                    + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            else if ( dataBaseInfo.getType().equalsIgnoreCase( "mysql" ) )
            {
                query = "SELECT dataelementid,categoryoptioncomboid, SUM(value) FROM datavalue " + " WHERE "
                    + " dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid = " + orgUnitIdsByComma
                    + " AND " + " periodid IN (" + periodIdsByComma + ") GROUP BY dataelementid,categoryoptioncomboid";
            }
            */
            query = "SELECT dataelementid,categoryoptioncomboid, SUM( cast( value as numeric) ) FROM datavalue "
                + " WHERE dataelementid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN ("
                + orgUnitIdsByComma + " ) AND " + " periodid IN (" + periodIdsByComma
                + ") GROUP BY dataelementid,categoryoptioncomboid";
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                Integer optionComId = rs.getInt( 2 );
                Double value = rs.getDouble( 3 );
                if ( value != null )
                {
                    aggDeMap.put( deId + "." + optionComId, "" + value );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    /*
    public String getDataelementIds(List<Report_inDesign> reportDesignList)
    {
      String dataElmentIdsByComma = "-1";
      for (Report_inDesign report_inDesign : reportDesignList)
      {
        String formula = report_inDesign.getExpression();
        try
        {
          Pattern pattern = Pattern.compile("(\\[\\d+\\.\\d+\\])");

          Matcher matcher = pattern.matcher(formula);
          StringBuffer buffer = new StringBuffer();

          while (matcher.find())
          {
            String replaceString = matcher.group();

            replaceString = replaceString.replaceAll("[\\[\\]]", "");
            replaceString = replaceString.substring(0, replaceString.indexOf(46));

            int dataElementId = Integer.parseInt(replaceString);
            dataElmentIdsByComma = dataElmentIdsByComma + "," + dataElementId;
            replaceString = "";
            matcher.appendReplacement(buffer, replaceString);
          }
        }
        catch (Exception e)
        {
        }

      }

      return dataElmentIdsByComma;
    }    
    */
    
    //@SuppressWarnings( "unchecked" )
    public List<Integer> getDataElementIds(List<Report_inDesign> reportDesignList)
    {
      List<Integer> dataElmentIdsList = new ArrayList<Integer>();
      for (Report_inDesign report_inDesign : reportDesignList)
      {
        String formula = report_inDesign.getExpression();
        try
        {
          Pattern pattern = Pattern.compile("(\\[\\d+\\.\\d+\\])");

          Matcher matcher = pattern.matcher(formula);
          StringBuffer buffer = new StringBuffer();

          while (matcher.find())
          {
            String replaceString = matcher.group();

            replaceString = replaceString.replaceAll("[\\[\\]]", "");
            replaceString = replaceString.substring(0, replaceString.indexOf(46));

            int dataElementId = Integer.parseInt(replaceString);
            dataElmentIdsList.add(Integer.valueOf(dataElementId));
            replaceString = "";
            matcher.appendReplacement(buffer, replaceString);
          }
        }
        catch (Exception e)
        {
        }
      }
      return dataElmentIdsList;
    }    
    // New Line Listing Report Related methods
    
    //--------------------------------------------------------------------------------
    // get Program Stage Instance Ids
    //--------------------------------------------------------------------------------
    public Set<Integer> getProgramStageInstanceIds( Integer programId, Integer programStageId, Integer organisationUnitId, Period period )
    {
        Set<Integer> programStageInstanceIds = new HashSet<Integer>();
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            String query = "SELECT  programstageinstanceid FROM programstageinstance  " +
                            " INNER JOIN programinstance on programinstance.programinstanceid = programstageinstance.programinstanceid " +  
                            " WHERE " +  
                                " programinstance.programid = "+ programId +"  AND " + 
                                " programstageinstance.programstageid = " + programStageId + " AND " + 
                                " programstageinstance.organisationunitid = "+ organisationUnitId + " AND " +  
                                " programstageinstance.executiondate between '"+ simpleDateFormat.format( period.getStartDate() ) + "'" + " AND '" + simpleDateFormat.format( period.getEndDate() ) + "'"; 
           
            
            /*
            SELECT  programstageinstanceid FROM programstageinstance 
            INNER JOIN programinstance on programinstance.programinstanceid = programstageinstance.programinstanceid
            WHERE  programinstance.programid = 465 AND 
                   programstageinstance.programstageid = 466 AND  
                   programstageinstance.organisationunitid = 50 AND    
                   programstageinstance.executiondate between '2015-07-01' and '2015-07-31';
            */
            
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer programStageInstanceId = rs.getInt( 1 );
               
                
                if ( programStageInstanceId != null  )
                {
                    programStageInstanceIds.add( programStageInstanceId );
                }
            }
            
            return programStageInstanceIds;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal Program Stage Instance Id ", e );
        }
    }    

    // get LineListing data with programStageInstanceId and dataElements from tracker Data Value
    public Map<String, String> getTrackedEntityDataValue( Integer programId, Integer programStageId, String orgUnitIdsByComma, String dataElementIdsByComma, Period period )
    {
        Map<String, String> trackedEntityDataValueMap = new HashMap<String, String>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        try
        {
            
            String query = "SELECT  programstageinstance.organisationunitid,trackedentitydatavalue.programstageinstanceid,dataelementid, value FROM trackedentitydatavalue  " +
                           " INNER JOIN programstageinstance ON programstageinstance.programstageinstanceid = trackedentitydatavalue.programstageinstanceid " +  
                           " INNER JOIN programinstance on programinstance.programinstanceid = programstageinstance.programinstanceid " +      
                           " WHERE " +  
                           " programinstance.programid = "+ programId +"  AND " + 
                           " programstageinstance.programstageid = " + programStageId + " AND " + 
                           " programstageinstance.organisationunitid IN ( "+ orgUnitIdsByComma + " ) AND " + 
                           " trackedentitydatavalue.dataelementid IN ( "+ dataElementIdsByComma + " ) AND " + 
                           " programstageinstance.executiondate between '"+ simpleDateFormat.format( period.getStartDate() ) + "'" + " AND '" + simpleDateFormat.format( period.getEndDate() ) + "'"; 
            
            /*
            SELECT  programstageinstance.organisationunitid,trackedentitydatavalue.programstageinstanceid,dataelementid, value FROM trackedentitydatavalue 
            INNER JOIN programstageinstance ON programstageinstance.programstageinstanceid = trackedentitydatavalue.programstageinstanceid
            INNER JOIN programinstance on programinstance.programinstanceid = programstageinstance.programinstanceid
            WHERE  
                 programinstance.programid = 465 AND 
                 programstageinstance.programstageid = 466 AND  
                 programstageinstance.organisationunitid IN (49,50) AND   
                 trackedentitydatavalue.dataelementid IN ( 468,469,470,471,472,473 ) AND 
                 programstageinstance.executiondate between '2015-08-01' and '2015-08-31';           
            */
            
                        
            //System.out.println( " SQUERY ======  :" + query );
            
            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );
                
            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                Integer programStageInstanceId = rs.getInt( 2 );
                Integer dataElementId = rs.getInt( 3 );
                String trackedDataValue = rs.getString( 4 );
                
                if ( trackedDataValue!= null  )
                {
                    String mapKey = orgUnitId + ":" + programStageInstanceId + ":" + dataElementId;
                    
                    //System.out.println("Map Key : \t" + mapKey + "Map Value : \t" + trackedDataValue );
                    trackedEntityDataValueMap.put( mapKey,  trackedDataValue );
                }
            }
            
                //System.out.println( " Size of tracked Entity DataValue Map  inside service is ======  :" + trackedEntityDataValueMap.size() ); 
                
            return trackedEntityDataValueMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }    
        
    
    
    @Transactional
    public Collection<Report_in> getAllNonSchedulableRports()
    {
        return ((DefaultReportService) reportStore).getAllNonSchedulableRports();
    }

    @Transactional
    public Collection<Report_in> getAllNonScheduledRports()
    {
        return ((DefaultReportService) reportStore).getAllNonScheduledRports();
    }

    @Transactional
    public Collection<Report_in> getAllSchedulableReports()
    {
        return ((DefaultReportService) reportStore).getAllSchedulableReports();
    }

    @Transactional
    public Collection<Report_in> getAllSchedulabledEmailableReports()
    {
        return ((DefaultReportService) reportStore).getAllSchedulabledEmailableReports();
    }

    @Transactional
    public Collection<Report_in> getAllSchedulabledNonEmailableReports()
    {
        return ((DefaultReportService) reportStore).getAllSchedulabledNonEmailableReports();
    }

    @Transactional
    public Collection<Report_in> getAllScheduledReports()
    {
        return ((DefaultReportService) reportStore).getAllScheduledReports();
    }

}
