package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.fileresource.*;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.jclouds.rest.AuthorizationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DataValueController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataValueController
{
    public static final String RESOURCE_PATH = "/dataValues";

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private InputUtils inputUtils;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private AggregateAccessManager accessManager;

    // ---------------------------------------------------------------------
    // POST
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @RequestMapping( method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.CREATED )
    public void saveDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) String comment,
        @RequestParam( required = false ) boolean followUp,
        @RequestParam( required = false ) boolean force, HttpServletResponse response )
        throws WebMessageException
    {
        boolean strictPeriods = (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );
        boolean strictCategoryOptionCombos = (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS );
        boolean strictOrgUnits = (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );
        boolean requireCategoryOptionCombo = (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO );
        FileResourceRetentionStrategy retentionStrategy = (FileResourceRetentionStrategy) systemSettingManager.getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY );
        User currentUser = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = getAndValidateDataElement( currentUser, de );

        CategoryOptionCombo categoryOptionCombo = getAndValidateCategoryOptionCombo( co, requireCategoryOptionCombo );

        CategoryOptionCombo attributeOptionCombo = getAndValidateAttributeOptionCombo( cc, cp );

        Period period = getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = getAndValidateOrganisationUnit( ou );

        validateOrganisationUnitPeriod( organisationUnit, period );

        DataSet dataSet = getAndValidateOptionalDataSet( ds, dataElement );

        validateInvalidFuturePeriod( period, dataElement );

        validateAttributeOptionComboWithOrgUnitAndPeriod( attributeOptionCombo, organisationUnit, period );

        String valueValid = ValidationUtils.dataValueIsValid( value, dataElement );

        if ( valueValid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Invalid value: " + value + ", must match data element type: " + dataElement.getValueType() ) );
        }

        String commentValid = ValidationUtils.commentIsValid( comment );

        if ( commentValid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Invalid comment: " + comment ) );
        }

        OptionSet optionSet = dataElement.getOptionSet();

        if ( !Strings.isNullOrEmpty( value ) && optionSet != null && !optionSet.getOptionCodesAsSet().contains( value ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value is not a valid option of the data element option set: " + dataElement.getUid() ) );
        }

        List<String> categoryOptionComboErrors = accessManager.canWriteCached( currentUser, categoryOptionCombo );

        if ( !categoryOptionComboErrors.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User does not have write access to category option combo: " + co + ", errors: " + categoryOptionComboErrors ) );
        }

        List<String> attributeOptionComboErrors = accessManager.canWriteCached( currentUser, attributeOptionCombo );

        if ( !attributeOptionComboErrors.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User does not have write access to attribute option combo: " + co + ", errors: " + attributeOptionComboErrors ) );
        }

        // ---------------------------------------------------------------------
        // Optional constraints
        // ---------------------------------------------------------------------

        if ( strictPeriods && !dataElement.getPeriodTypes().contains( period.getPeriodType() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Period type of period: " + period.getIsoDate() + " not valid for data element: " + dataElement.getUid() ) );
        }

        if ( strictCategoryOptionCombos && !dataElement.getCategoryOptionCombos().contains( categoryOptionCombo ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Category option combo: " + categoryOptionCombo.getUid() + " must be part of category combo of data element: " + dataElement.getUid() ) );
        }

        if ( strictOrgUnits && !organisationUnit.hasDataElement( dataElement ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: " + organisationUnit.getUid() ) );
        }

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, force ) )
        {
            validateDataSetNotLocked( dataElement, period, dataSet, organisationUnit, attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        validateDataInputPeriodForDataElementAndPeriod( dataElement, period, dataSet );

        // ---------------------------------------------------------------------
        // Assemble and save data value
        // ---------------------------------------------------------------------

        String storedBy = currentUserService.getCurrentUsername();

        Date now = new Date();

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo );

        FileResource fileResource = null;

        if ( dataValue == null )
        {
            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.getValueType().isFile() )
            {
                fileResource = validateAndSetAssigned( value );
            }

            dataValue = new DataValue( dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo,
                StringUtils.trimToNull( value ), storedBy, now, StringUtils.trimToNull( comment ) );

            dataValueService.addDataValue( dataValue );
        }
        else
        {
            if ( value == null && ValueType.TRUE_ONLY.equals( dataElement.getValueType() ) )
            {
                if ( comment == null )
                {
                    dataValueService.deleteDataValue( dataValue );
                    return;
                }
                else
                {
                    value = DataValue.FALSE;
                }
            }

            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.getValueType().isFile() )
            {
                fileResource = validateAndSetAssigned( value );
            }

            if ( dataElement.isFileType() && retentionStrategy == FileResourceRetentionStrategy.NONE )
            {
                try
                {
                    fileResourceService.deleteFileResource( dataValue.getValue() );
                }
                catch ( AuthorizationException exception )
                {
                    // If we fail to delete the fileResource now, mark it as unassigned for removal later
                    fileResourceService.getFileResource( dataValue.getValue() ).setAssigned( false );
                }
                dataValue.setValue( StringUtils.EMPTY );
            }

            // -----------------------------------------------------------------
            // Value and comment are sent individually, so null checks must be 
            // made for each. Empty string is sent for clearing a value.
            // -----------------------------------------------------------------

            if ( value != null )
            {
                dataValue.setValue( StringUtils.trimToNull( value ) );
            }

            if ( comment != null )
            {
                dataValue.setComment( StringUtils.trimToNull( comment ) );
            }

            if ( followUp )
            {
                dataValue.toggleFollowUp();
            }

            dataValue.setLastUpdated( now );
            dataValue.setStoredBy( storedBy );

            dataValueService.updateDataValue( dataValue );
        }

        if ( fileResource != null )
        {
            fileResourceService.updateFileResource( fileResource );
        }
    }

    // ---------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_DELETE')" )
    @RequestMapping( method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) boolean force, HttpServletResponse response )
        throws WebMessageException
    {
        FileResourceRetentionStrategy retentionStrategy = (FileResourceRetentionStrategy) systemSettingManager.getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY );

        User currentUser = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = getAndValidateDataElement( currentUserService.getCurrentUser(), de );

        CategoryOptionCombo categoryOptionCombo = getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = getAndValidateAttributeOptionCombo( cc, cp );

        Period period = getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = getAndValidateOrganisationUnit( ou );

        DataSet dataSet = getAndValidateOptionalDataSet( ds, dataElement );

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, force ) )
        {
            validateDataSetNotLocked( dataElement, period, dataSet, organisationUnit, attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        validateDataInputPeriodForDataElementAndPeriod( dataElement, period, dataSet );

        // ---------------------------------------------------------------------
        // Delete data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value cannot be deleted because it does not exist" ) );
        }

        if ( dataValue.getDataElement().isFileType() && retentionStrategy == FileResourceRetentionStrategy.NONE )
        {
            fileResourceService.deleteFileResource( dataValue.getValue() );
        }


        dataValueService.deleteDataValue( dataValue );
    }

    // ---------------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody List<String> getDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        Model model, HttpServletResponse response )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        DataElement dataElement = getAndValidateDataElement( currentUser, de );

        CategoryOptionCombo categoryOptionCombo = getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = getAndValidateAttributeOptionCombo( cc, cp );

        Period period = getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = getAndValidateOrganisationUnit( ou );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value does not exist" ) );
        }

        // ---------------------------------------------------------------------
        // Data Sharing check
        // ---------------------------------------------------------------------

        List<String> errors = accessManager.canRead( currentUser, dataValue );

        if ( !errors.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.forbidden( errors.toString() ) );
        }

        List<String> value = new ArrayList<>();
        value.add( dataValue.getValue() );

        setNoStore( response );
        return value;
    }

    // ---------------------------------------------------------------------
    // GET file
    // ---------------------------------------------------------------------

    @RequestMapping( value = "/files", method = RequestMethod.GET )
    public void getDataValueFile(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = getAndValidateDataElement( currentUserService.getCurrentUser(), de );

        if ( !dataElement.isFileType() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "DataElement must be of type file" ) );
        }

        CategoryOptionCombo categoryOptionCombo = getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = getAndValidateAttributeOptionCombo( cc, cp );

        Period period = getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = getAndValidateOrganisationUnit( ou );

        validateOrganisationUnitPeriod( organisationUnit, period );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value does not exist" ) );
        }


        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        String uid = dataValue.getValue();

        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "A data value file resource with id " + uid + " does not exist." ) );
        }

        FileResourceStorageStatus storageStatus = fileResource.getStorageStatus();

        if ( storageStatus != FileResourceStorageStatus.STORED )
        {
            // Special case:
            // The FileResource exists and has been tied to this DataValue, however, the underlying file
            // content is still not stored to the (most likely external) file store provider.

            // HTTP 409, for lack of a more suitable status code
            WebMessage webMessage = WebMessageUtils.conflict( "The content is being processed and is not available yet. Try again later.",
                "The content requested is in transit to the file store and will be available at a later time." );
            webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

            throw new WebMessageException( webMessage );
        }

        response.setContentType( fileResource.getContentType() );
        response.setContentLength( new Long( fileResource.getContentLength() ).intValue() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );
        setNoStore( response );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related" ) );
        }
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private DataElement getAndValidateDataElement( User user, String de )
        throws WebMessageException
    {
        DataElement dataElement = idObjectManager.get( DataElement.class, de );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data element not found or not accessible: " + de ) );
        }

        return dataElement;
    }

    private CategoryOptionCombo getAndValidateCategoryOptionCombo( String co, boolean requireCategoryOptionCombo )
        throws WebMessageException
    {
        CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( co );

        if ( categoryOptionCombo == null )
        {
            if ( requireCategoryOptionCombo )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Category option combo is required but is not specified" ) );
            }
            else if ( co != null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Category option combo not found or not accessible: " + co ) );
            }
            else
            {
                categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
            }
        }

        return categoryOptionCombo;
    }

    private CategoryOptionCombo getAndValidateAttributeOptionCombo( String cc, String cp )
        throws WebMessageException
    {
        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( cc, cp, false );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Attribute option combo not found or not accessible: " + cc + " " + cp ) );
        }

        return attributeOptionCombo;
    }

    private Period getAndValidatePeriod( String pe )
        throws WebMessageException
    {
        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
        }

        return period;
    }

    private void validateOrganisationUnitPeriod( OrganisationUnit organisationUnit, Period period ) throws WebMessageException
    {
        Date openingDate = organisationUnit.getOpeningDate();
        Date closedDate = organisationUnit.getClosedDate();
        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        if ( ( closedDate != null && closedDate.before( startDate ) ) || openingDate.after( endDate ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit is closed for the selected period. " ) );
        }
    }

    private OrganisationUnit getAndValidateOrganisationUnit( String ou )
        throws WebMessageException
    {
        OrganisationUnit organisationUnit = idObjectManager.get( OrganisationUnit.class, ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit not found or not accessible: " + ou ) );
        }

        boolean isInHierarchy = organisationUnitService.isInUserHierarchyCached( organisationUnit );

        if ( !isInHierarchy )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit is not in the hierarchy of the current user: " + ou ) );
        }

        return organisationUnit;
    }

    private DataSet getAndValidateOptionalDataSet( String ds, DataElement dataElement )
        throws WebMessageException
    {
        if ( ds == null )
        {
            return null;
        }

        DataSet dataSet = dataSetService.getDataSet( ds );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set not found or not accessible: " + ds ) );
        }

        if ( !dataSet.getDataElements().contains( dataElement ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set: " + ds + " does not contain data element: " + dataElement.getUid() ) );
        }

        return dataSet;
    }

    private void validateInvalidFuturePeriod( Period period, DataElement dataElement )
        throws WebMessageException
    {
        Period latestFuturePeriod = dataElement.getLatestOpenFuturePeriod();

        if ( period.isAfter( latestFuturePeriod ) && calendarService.getSystemCalendar().isIso8601() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period: " +
                period.getIsoDate() + " is after latest open future period: " + latestFuturePeriod.getIsoDate() + " for data element: " + dataElement.getUid() ) );
        }
    }

    private void validateAttributeOptionComboWithOrgUnitAndPeriod( CategoryOptionCombo attributeOptionCombo,
        OrganisationUnit organisationUnit, Period period )
        throws WebMessageException
    {
        for ( CategoryOption option : attributeOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && period.getEndDate().compareTo( option.getStartDate() ) < 0 )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Period " + period.getIsoDate()
                    + " is before start date " + i18nManager.getI18nFormat().formatDate( option.getStartDate() )
                    + " for attributeOption '" + option.getName() + "'" ) );
            }

            if ( option.getEndDate() != null && period.getStartDate().compareTo( option.getEndDate() ) > 0 )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Period " + period.getIsoDate()
                    + " is after end date " + i18nManager.getI18nFormat().formatDate( option.getEndDate() )
                    + " for attributeOption '" + option.getName() + "'" ) );
            }
        }
    }

    private void validateDataSetNotLocked( DataElement dataElement, Period period, DataSet dataSet,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
        throws WebMessageException
    {
        User user = currentUserService.getCurrentUser();

        if ( dataSet == null ? dataSetService.isLocked( user, dataElement, period, organisationUnit, attributeOptionCombo, null )
            : dataSetService.isLocked( user, dataSet, period, organisationUnit, attributeOptionCombo, null) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set is locked" ) );
        }
    }

    private void validateDataInputPeriodForDataElementAndPeriod( DataElement dataElement, Period period, DataSet dataSet )
        throws WebMessageException
    {
        if ( !( dataSet == null ? dataElement.isDataInputAllowedForPeriodAndDate( period, new Date() )
            : dataSet.isDataInputPeriodAndDateAllowed( period, new Date() ) ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period reported is not open in data set" ) );
        }
    }

    private FileResource validateAndSetAssigned( String uid )
        throws WebMessageException
    {
        FileResource fileResource = null;

        if ( uid != null )
        {
            fileResource = fileResourceService.getFileResource( uid );

            if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
            {
                throw new WebMessageException( WebMessageUtils.notFound( FileResource.class, uid ) );
            }

            if ( fileResource.isAssigned() )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "File resource already assigned or linked to another data value" ) );
            }

            fileResource.setAssigned( true );
        }
        else
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Missing parameter 'value'" ) );
        }

        return fileResource;
    }
}
