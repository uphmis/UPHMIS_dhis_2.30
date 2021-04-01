/* Updated by Sunakshi and Mithilesh Kumar Thakur on 10/04/18 */
package org.hisp.dhis.excelimport.export.action;

import static org.hisp.dhis.util.ConversionUtils.getIdentifiers;
import static org.hisp.dhis.util.TextUtils.getCommaDelimitedString;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.config.Configuration_IN;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.excelimport.util.ExcelImport;
import org.hisp.dhis.excelimport.util.ExcelImportService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.opensymphony.xwork2.Action;

public class ExportDataResultAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    private static final String FILE_HEADER = "dataelement,period,orgunit,categoryoptioncombo,attributeoptioncombo,value,storedby,lastupdated,comment,followup";

    private static final String COMMA_DELIMITER = ",";

    private static final String NEW_LINE_SEPARATOR = "\n";

    @Autowired
    private OrganisationUnitGroupSetStore organisationUnitGroupSetStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PeriodService periodService;

    private ExcelImportService excelImportService;

    public void setExcelImportService( ExcelImportService excelImportService )
    {
        this.excelImportService = excelImportService;
    }

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    // -------------------------------------------------------------------------
    // Input/Output
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

    private Integer year;

    private String org;

    public void setorg( String org )
    {
        this.org = org;
    }

    public void setYear( Integer year )
    {
        this.year = year;
    }

    private String orgUnitGroupId;

    public void setOrgUnitGroupId( String orgUnitGroupId )
    {
        this.orgUnitGroupId = orgUnitGroupId;
    }

    private String weeklyPeriodTypeName;

    private String deCodesXMLFileName = "";

    private SimpleDateFormat simpleDateFormat;

    OrganisationUnitGroup orgUnitGroup;

    String attributeValue;

    OrganisationUnitGroup orgUnitGroup1;

    String orgGroup, orgGroupUid, attValue;

    private ArrayList<OrganisationUnitGroup> organisationUnitGroups;

    private Map<String, String> attributeValueMap;

    private Map<String, Map<String, String>> orgGroupDataValueMap;

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    public String execute()
        throws Exception
    {
        OrganisationUnitGroupSet OrganisationUnitGroupSet = organisationUnitGroupSetStore
            .getByCode( "ExcelExportGroupSet" );
        organisationUnitGroups = new ArrayList<OrganisationUnitGroup>(
            OrganisationUnitGroupSet.getOrganisationUnitGroups() );

        System.out.println( "Start Time : " + new Date() );
        simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

        String startDate = "01-01-" + year;
        String endDate = "31-12-" + year;

        String userName = currentUserService.getCurrentUsername();
        List<ExcelImport> excelExportDesignList = new ArrayList<ExcelImport>();

        // collect dataElementIDs by commaSepareted

        // period information
        weeklyPeriodTypeName = WeeklyPeriodType.NAME;
        PeriodType periodType = periodService.getPeriodTypeByName( weeklyPeriodTypeName );

        List<Period> periods = new ArrayList<Period>();

        String periodIdsByComma = "-1";
        if ( year != null )
        {
            String isoPeriodString = year.toString();

            Period period = periodService.reloadIsoPeriod( isoPeriodString );

            if ( period != null )
            {
                periods = new ArrayList<Period>(
                    periodService.getPeriodsBetweenDates( periodType, period.getStartDate(), period.getEndDate() ) );
            }

            Collection<Integer> periodIds = new ArrayList<Integer>( getIdentifiers( Period.class, periods ) );
            periodIdsByComma = getCommaDelimitedString( periodIds );
        }

        Map<String, String> aggDeMap = new HashMap<String, String>();

        attributeValueMap = new HashMap<String, String>( excelImportService.getAttributeValueCode() );

        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        List<String> getUID = new ArrayList<String>();

        List<Map<String, String>> listUid = new ArrayList<Map<String, String>>();

        if ( orgUnitGroupId != null && !orgUnitGroupId.equalsIgnoreCase( "ALL" )
            && !orgUnitGroupId.equalsIgnoreCase( "NA" ) )
        {

            deCodesXMLFileName = "exportData.xml";
            excelExportDesignList = new ArrayList<ExcelImport>(
                excelImportService.getExcelImportDesignDesign( deCodesXMLFileName ) );

            String dataElmentIdsByComma = excelImportService.getDataelementUIds( excelExportDesignList );

            orgUnitGroup = organisationUnitGroupService.getOrganisationUnitGroup( orgUnitGroupId );

            excelImportService.getAttributeValueCode();

            List<OrganisationUnit> groupMember = new ArrayList<OrganisationUnit>( orgUnitGroup.getMembers() );
            List<Integer> orgaUnitIds = new ArrayList<Integer>( getIdentifiers( OrganisationUnit.class, groupMember ) );
            String orgaUnitIdsByComma = getCommaDelimitedString( orgaUnitIds );

            aggDeMap.putAll( excelImportService.getAggDataFromDataValueTable( orgaUnitIdsByComma, dataElmentIdsByComma,
                periodIdsByComma ) );

            orgGroup = orgUnitGroup.getUid();

        }
        else
        {
            deCodesXMLFileName = "exportData.xml";
            excelExportDesignList = new ArrayList<ExcelImport>(
                excelImportService.getExcelImportDesignDesign( deCodesXMLFileName ) );
            String dataElmentIdsByComma = excelImportService.getDataelementUIds( excelExportDesignList );

            // orgUnit Details

            int i = 1;
            orgGroupDataValueMap = new HashMap<String, Map<String, String>>();
            for ( OrganisationUnitGroup x : organisationUnitGroups )
            {
                OrganisationUnitGroup tempOrgUnitGroup = organisationUnitGroupService
                    .getOrganisationUnitGroup( x.getUid() );

                List<OrganisationUnit> groupMember = new ArrayList<OrganisationUnit>( tempOrgUnitGroup.getMembers() );
                List<Integer> orgaUnitIds = new ArrayList<Integer>(
                    getIdentifiers( OrganisationUnit.class, groupMember ) );
                String orgaUnitIdsByComma = getCommaDelimitedString( orgaUnitIds );

                orgGroupDataValueMap.put( tempOrgUnitGroup.getUid(), excelImportService
                    .getAggDataFromDataValueTable( orgaUnitIdsByComma, dataElmentIdsByComma, periodIdsByComma ) );

                i++;

            }

        }

        String outputReportPath = System.getenv( "DHIS2_HOME" ) + File.separator + Configuration_IN.DEFAULT_TEMPFOLDER;
        File file = new File( outputReportPath );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        outputReportPath += File.separator + UUID.randomUUID().toString() + ".csv";

        FileWriter fileWriter = null;
        fileWriter = new FileWriter( outputReportPath );
        // for header
        fileWriter.append( FILE_HEADER.toString() );

        // Add a new line separator after the header
        fileWriter.append( NEW_LINE_SEPARATOR );

        Iterator<ExcelImport> excelExportDesignIterator = excelExportDesignList.iterator();
        while ( excelExportDesignIterator.hasNext() )
        {
            ExcelImport exceEmportDesign = (ExcelImport) excelExportDesignIterator.next();

            String deCodeString = exceEmportDesign.getExpression();
            String tempStr = "", neworgGroup = "";

            try
            {

                if ( orgUnitGroupId != null && !orgUnitGroupId.equalsIgnoreCase( "ALL" )
                    && !orgUnitGroupId.equalsIgnoreCase( "NA" ) )
                {

                    tempStr = getAggVal( deCodeString, aggDeMap );

                    if ( !tempStr.equalsIgnoreCase( "0" ) )
                    {
                        fileWriter.append( exceEmportDesign.getDataelement() );
                        fileWriter.append( COMMA_DELIMITER );
                        fileWriter.append( year.toString() );
                        fileWriter.append( COMMA_DELIMITER );

                        neworgGroup = attributeValueMap.get( orgUnitGroupId );

                        fileWriter.append( neworgGroup );
                        fileWriter.append( COMMA_DELIMITER );

                        fileWriter.append( exceEmportDesign.getCategoryoptioncombo() );
                        fileWriter.append( COMMA_DELIMITER );
                        fileWriter.append( exceEmportDesign.getAttributeoptioncombo() );
                        fileWriter.append( COMMA_DELIMITER );

                        fileWriter.append( tempStr );
                        fileWriter.append( COMMA_DELIMITER );

                        fileWriter.append( userName );
                        fileWriter.append( COMMA_DELIMITER );
                        fileWriter.append( simpleDateFormat.format( new Date() ) );
                        fileWriter.append( COMMA_DELIMITER );
                        fileWriter.append( exceEmportDesign.getComment() );
                        fileWriter.append( COMMA_DELIMITER );
                        fileWriter.append( "" );
                        fileWriter.append( COMMA_DELIMITER );

                        fileWriter.append( NEW_LINE_SEPARATOR );
                    }

                    else
                    {

                    }
                }
                else
                {

                    int i = 0;
                    for ( String orgGroupUid : orgGroupDataValueMap.keySet() )
                    {
                        tempStr = getAggVal( deCodeString, orgGroupDataValueMap.get( orgGroupUid ) );

                        if ( !tempStr.equalsIgnoreCase( "0" ) )
                        {
                            fileWriter.append( exceEmportDesign.getDataelement() );
                            fileWriter.append( COMMA_DELIMITER );
                            fileWriter.append( year.toString() );
                            fileWriter.append( COMMA_DELIMITER );

                            if ( orgGroupUid != null )
                            {
                                neworgGroup = attributeValueMap.get( orgGroupUid );
                            }

                            fileWriter.append( neworgGroup );
                            fileWriter.append( COMMA_DELIMITER );

                            fileWriter.append( exceEmportDesign.getCategoryoptioncombo() );
                            fileWriter.append( COMMA_DELIMITER );
                            fileWriter.append( exceEmportDesign.getAttributeoptioncombo() );
                            fileWriter.append( COMMA_DELIMITER );

                            fileWriter.append( tempStr );
                            fileWriter.append( COMMA_DELIMITER );

                            fileWriter.append( userName );
                            fileWriter.append( COMMA_DELIMITER );
                            fileWriter.append( simpleDateFormat.format( new Date() ) );
                            fileWriter.append( COMMA_DELIMITER );
                            fileWriter.append( exceEmportDesign.getComment() );
                            fileWriter.append( COMMA_DELIMITER );
                            fileWriter.append( "" );
                            fileWriter.append( COMMA_DELIMITER );

                            fileWriter.append( NEW_LINE_SEPARATOR );

                        }

                        else
                        {

                        }

                        i++;
                    }

                }
            }

            catch ( Exception e )
            {

                System.out.println( "Error in CsvFileWriter !!!" );
                e.printStackTrace();
            }

        }

        try
        {
            fileWriter.flush();
            fileWriter.close();

            fileName = "ExportResult.csv";
            File outputReportFile = new File( outputReportPath );
            inputStream = new BufferedInputStream( new FileInputStream( outputReportFile ) );

            outputReportFile.deleteOnExit();

        }
        catch ( IOException e )
        {
            System.out.println( "Error while flushing/closing fileWriter !!!" );
            e.printStackTrace();
        }

        System.out.println( "End Time : " + new Date() );

        return SUCCESS;
    }

    // getting data value using Map
    private String getAggVal( String expression, Map<String, String> aggDeMap )
    {
        try
        {
            Pattern pattern = Pattern.compile( "(\\[\\w+\\.\\w+\\])" );
            Matcher matcher = pattern.matcher( expression );
            StringBuffer buffer = new StringBuffer();

            String resultValue = "";

            while ( matcher.find() )
            {
                String replaceString = matcher.group();

                replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

                String categoryComboUID = replaceString.substring( replaceString.indexOf( '.' ) + 1,
                    replaceString.length() );

                replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                replaceString = replaceString + "." + categoryComboUID;

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
            }
            catch ( Exception e )
            {
                d = 0.0;
                resultValue = "";
            }

            resultValue = "" + (int) d;

            return resultValue;
        }
        catch ( NumberFormatException ex )
        {
            throw new RuntimeException( "Illegal DataElement id", ex );
        }
    }

}