package org.hisp.dhis.dxf2.synch;

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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportSummaryResponseExtractor;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.dxf2.webmessage.utils.WebMessageParseUtils;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.MessageConversationSchemaDescriptor;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.*;

/**
 * @author Lars Helge Overland
 */
public class DefaultSynchronizationManager
    implements SynchronizationManager
{
    private static final Log log = LogFactory.getLog( DefaultSynchronizationManager.class );

    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private MetadataImportService importService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // SynchronizatonManager implementation
    // -------------------------------------------------------------------------

    @Override
    public AvailabilityStatus isRemoteServerAvailable()
    {
        return SyncUtils.isRemoteServerAvailable( systemSettingManager, restTemplate );
    }

    @Override
    public ImportSummary executeDataPush() throws WebMessageParseException
    {
        AvailabilityStatus availability = isRemoteServerAvailable();

        if ( !availability.isAvailable() )
        {
            log.info( "Aborting synch, server not available" );
            return null;
        }

        String url = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + SyncEndpoint.DATA_VALUE_SETS.getPath();
        String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        SystemInstance instance = new SystemInstance( url, username, password );

        return executeDataPush( instance );
    }

    /**
     * Executes a push of data values to the given remote instance.
     *
     * @param instance the remote system instance.
     * @return an ImportSummary.
     */
    private ImportSummary executeDataPush( SystemInstance instance ) throws WebMessageParseException
    {
        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------

        final Date startTime = new Date();
        final Date lastSuccessTime = getLastDataSynchSuccessFallback();
        final Date skipChangedBefore = (Date) systemSettingManager
            .getSystemSetting( SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE );
        final Date lastUpdatedAfter = lastSuccessTime.after( skipChangedBefore ) ? lastSuccessTime : skipChangedBefore;
        final int objectsToSynchronize = dataValueService.getDataValueCountLastUpdatedAfter( lastUpdatedAfter, true );

        log.info( "DataValues last changed before " + skipChangedBefore + " will not be synchronized." );

        if ( objectsToSynchronize == 0 )
        {
            SyncUtils.setLastSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_DATA_SYNC,
                startTime );
            log.debug( "Skipping data values push, no new or updated data values" );

            ImportCount importCount = new ImportCount( 0, 0, 0, 0 );
            return new ImportSummary( ImportStatus.SUCCESS, "No new or updated data values to push.", importCount );
        }

        log.info( "Values: " + objectsToSynchronize + " since last synchronization success: " + lastSuccessTime );
        log.info( "Remote server POST URL: " + instance.getUrl() );

        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );

            dataValueSetService.writeDataValueSetJson( lastUpdatedAfter, request.getBody(), new IdSchemes() );
        };

        ResponseExtractor<ImportSummary> responseExtractor = new ImportSummaryResponseExtractor();
        ImportSummary summary = null;
        try
        {
            summary = restTemplate
                .execute( instance.getUrl(), HttpMethod.POST, requestCallback, responseExtractor );
        }
        catch ( HttpClientErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            summary = WebMessageParseUtils.fromWebMessageResponse( responseBody, ImportSummary.class );
        }
        catch ( HttpServerErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            log.error( "Internal error happened during event data push: " + responseBody, ex );
            throw ex;
        }
        catch ( ResourceAccessException ex )
        {
            log.error( "Exception during event data push: " + ex.getMessage(), ex );
            throw ex;
        }

        log.info( "Synch summary: " + summary );

        if ( summary != null && ImportStatus.SUCCESS.equals( summary.getStatus() ) )
        {
            setLastDataSynchSuccess( startTime );
            log.info( "Synch successful, setting last success time: " + startTime );
        }
        else
        {
            log.warn( "Sync failed: " + summary );
        }

        return summary;
    }

    @Override
    public ImportReport executeMetadataPull( String url )
    {
        User user = currentUserService.getCurrentUser();

        String userUid = user != null ? user.getUid() : null;

        log.info( String.format( "Metadata pull, url: %s, user: %s", url, userUid ) );

        String json = restTemplate.getForObject( url, String.class );

        Metadata metadata;

        try
        {
            metadata = DefaultRenderService.getJsonMapper().readValue( json, Metadata.class );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to parse remote JSON document", ex );
        }

        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setSkipSharing( true );
        importParams.setAtomicMode( AtomicMode.NONE );

        //Remove MessageConversation schema
        List<Schema> metadataSchemas = schemaService.getMetadataSchemas().stream()
            .filter( schema -> !schema.getPlural().equals( MessageConversationSchemaDescriptor.PLURAL ) )
            .collect( Collectors.toList());

        importParams.addMetadata( metadataSchemas, metadata );

        return importService.importMetadata( importParams );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the time of the last successful data synchronization operation. If not set,
     * the current date subtracted by three days is returned.
     */
    private Date getLastDataSynchSuccessFallback()
    {
        Date fallback = new DateTime().minusDays( 3 ).toDate();

        return (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_SYNC, fallback );
    }

    /**
     * Sets the time of the last successful data synchronization operation.
     */
    private void setLastDataSynchSuccess( Date time )
    {
        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_SYNC, time );
    }
}
