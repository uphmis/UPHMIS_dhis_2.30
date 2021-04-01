package org.hisp.dhis.dxf2.events.trackedentity;

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

import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractTrackedEntityInstanceService
    implements TrackedEntityInstanceService
{
    private static final Log log = LogFactory.getLog( AbstractTrackedEntityInstanceService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Autowired
    protected org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    protected TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    protected RelationshipService _relationshipService;

    @Autowired
    protected org.hisp.dhis.dxf2.events.relationship.RelationshipService relationshipService;

    @Autowired
    protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected EnrollmentService enrollmentService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected QueryService queryService;

    @Autowired
    protected ReservedValueService reservedValueService;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    protected TrackerOwnershipManager trackerOwnershipAccessManager;

    @Autowired
    protected Notifier notifier;

    private final CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private final CachingMap<String, Program> programCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntityType> trackedEntityCache = new CachingMap<>();

    private final CachingMap<String, TrackedEntityAttribute> trackedEntityAttributeCache = new CachingMap<>();

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams queryParams,
        TrackedEntityInstanceParams params, boolean skipAccessValidation )
    {
        List<org.hisp.dhis.trackedentity.TrackedEntityInstance> daoTEIs = teiService
            .getTrackedEntityInstances( queryParams, skipAccessValidation );

        List<TrackedEntityInstance> dtoTeis = new ArrayList<>();
        User user = currentUserService.getCurrentUser();

        List<TrackedEntityType> trackedEntityTypes = manager.getAll( TrackedEntityType.class );

        Set<TrackedEntityAttribute> trackedEntityTypeAttributes = trackedEntityTypes.stream().collect( Collectors.toList() )
            .stream().map( TrackedEntityType::getTrackedEntityAttributes ).flatMap( Collection::stream ).collect( Collectors.toSet() );

        Set<TrackedEntityAttribute> attributes;

        if ( queryParams != null && queryParams.isIncludeAllAttributes() )
        {
            List<Program> programs = manager.getAll( Program.class );

            for ( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance : daoTEIs )
            {
                attributes = new HashSet<>( trackedEntityTypeAttributes );

                // check if user can read the TEI
                if ( trackerAccessManager.canRead( user, daoTrackedEntityInstance ).isEmpty() )
                {
                    // pick only those program attributes that user is the owner
                    for ( Program program : programs )
                    {
                        if ( trackerOwnershipAccessManager.hasAccess( user, daoTrackedEntityInstance, program ) )
                        {
                            attributes.addAll( program.getTrackedEntityAttributes() );
                        }
                    }

                    dtoTeis.add( getTei( daoTrackedEntityInstance, attributes, params, user ) );
                }
            }
        }
        else
        {
            attributes = new HashSet<>( trackedEntityTypeAttributes );

            if ( queryParams.hasProgram() )
            {
                attributes.addAll( new HashSet<>( queryParams.getProgram().getTrackedEntityAttributes() ) );
            }

            for ( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance : daoTEIs )
            {
                if ( trackerAccessManager.canRead( user, daoTrackedEntityInstance, queryParams.getProgram(), false ).isEmpty() )
                {
                    dtoTeis.add( getTei( daoTrackedEntityInstance, attributes, params, user ) );
                }
            }
        }

        return dtoTeis;
    }

    @Override
    @Transactional(readOnly = true)
    public int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation )
    {
        return teiService.getTrackedEntityInstanceCount( params, skipAccessValidation, skipSearchScopeValidation );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        return getTrackedEntityInstance( teiService.getTrackedEntityInstance( uid ) );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( String uid, TrackedEntityInstanceParams params )
    {
        return getTrackedEntityInstance( teiService.getTrackedEntityInstance( uid ), params );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance(
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance )
    {
        return getTrackedEntityInstance( daoTrackedEntityInstance, TrackedEntityInstanceParams.TRUE );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance(
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        TrackedEntityInstanceParams params )
    {
        return getTrackedEntityInstance( daoTrackedEntityInstance, params, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional(readOnly = true)
    public TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        TrackedEntityInstanceParams params, User user )
    {
        if ( daoTrackedEntityInstance == null )
        {
            return null;
        }

        List<String> errors = trackerAccessManager.canRead( user, daoTrackedEntityInstance );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        Set<TrackedEntityAttribute> readableAttributes = trackedEntityAttributeService.getAllUserReadableTrackedEntityAttributes();

        return getTei( daoTrackedEntityInstance, readableAttributes, params, user );
    }


    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummaries addTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances,
        ImportOptions importOptions )
    {
        List<List<TrackedEntityInstance>> partitions = Lists.partition( trackedEntityInstances, FLUSH_FREQUENCY );
        importOptions = updateImportOptions( importOptions );
        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<TrackedEntityInstance> _trackedEntityInstances : partitions )
        {
            reloadUser( importOptions );
            prepareCaches( _trackedEntityInstances, importOptions.getUser() );

            for ( TrackedEntityInstance trackedEntityInstance : _trackedEntityInstances )
            {
                importSummaries.addImportSummary( addTrackedEntityInstance( trackedEntityInstance, importOptions ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    @Override
    @Transactional
    public ImportSummaries addTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances,
        ImportOptions importOptions, JobConfiguration jobId )
    {
        notifier.clear( jobId ).notify( jobId, "Importing tracked entities" );
        importOptions = updateImportOptions( importOptions );

        try
        {
            ImportSummaries importSummaries = addTrackedEntityInstances( trackedEntityInstances, importOptions );

            if ( jobId != null )
            {
                notifier.notify( jobId, NotificationLevel.INFO, "Import done", true )
                    .addJobSummary( jobId, importSummaries, ImportSummaries.class );
            }

            return importSummaries;
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( jobId, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummaries().addImportSummary(
                new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() ) );
        }
    }

    @Override
    @Transactional
    public ImportSummary addTrackedEntityInstance( TrackedEntityInstance dtoEntityInstance,
        ImportOptions importOptions )
    {
        importOptions = updateImportOptions( importOptions );

        if ( teiService.trackedEntityInstanceExistsIncludingDeleted( dtoEntityInstance.getTrackedEntityInstance() ) )
        {
            String message = "Tracked entity instance " + dtoEntityInstance.getTrackedEntityInstance() +
                " already exists or was deleted earlier";
            return new ImportSummary( ImportStatus.ERROR, message )
                .setReference( dtoEntityInstance.getTrackedEntityInstance() ).incrementIgnored();
        }

        ImportSummary importSummary = new ImportSummary( dtoEntityInstance.getTrackedEntityInstance() );

        dtoEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkTrackedEntityType( dtoEntityInstance, importOptions ) );
        checkAttributes( dtoEntityInstance, importOptions, importConflicts, false );

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setConflicts( importConflicts );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();
            return importSummary;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = createDAOTrackedEntityInstance(
            dtoEntityInstance, importOptions, importSummary );

        if ( daoEntityInstance == null )
        {
            return importSummary;
        }

        List<String> errors = trackerAccessManager.canWrite( importOptions.getUser(), daoEntityInstance );

        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() )
                .incrementIgnored();
        }

        teiService.addTrackedEntityInstance( daoEntityInstance );

        updateAttributeValues( dtoEntityInstance, daoEntityInstance, null, importOptions.getUser() );

        importSummary.setReference( daoEntityInstance.getUid() );
        importSummary.getImportCount().incrementImported();

        importSummary.setEnrollments( handleEnrollments( dtoEntityInstance, daoEntityInstance, importOptions ) );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummaries updateTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances,
        ImportOptions importOptions )
    {
        sortTrackedEntityInstanceUpdates( trackedEntityInstances );
        List<List<TrackedEntityInstance>> partitions = Lists.partition( trackedEntityInstances, FLUSH_FREQUENCY );
        importOptions = updateImportOptions( importOptions );
        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<TrackedEntityInstance> _trackedEntityInstances : partitions )
        {
            reloadUser( importOptions );
            prepareCaches( _trackedEntityInstances, importOptions.getUser() );

            for ( TrackedEntityInstance trackedEntityInstance : _trackedEntityInstances )
            {
                importSummaries.addImportSummary( updateTrackedEntityInstance( trackedEntityInstance, null, importOptions, false ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    /**
     * Sorts the tracked entity instances according to the tracked entity instance identifier.
     *
     * @param trackedEntityInstances the list of tracked entity instances.
     */
    private void sortTrackedEntityInstanceUpdates( List<TrackedEntityInstance> trackedEntityInstances )
    {
        trackedEntityInstances.sort( ( a, b ) -> a.getTrackedEntityInstance().compareTo( b.getTrackedEntityInstance() ) );
    }

    @Override
    @Transactional
    public ImportSummary updateTrackedEntityInstance( TrackedEntityInstance dtoEntityInstance, String programId,
        ImportOptions importOptions, boolean singleUpdate )
    {
        ImportSummary importSummary = new ImportSummary( dtoEntityInstance.getTrackedEntityInstance() );
        importOptions = updateImportOptions( importOptions );

        dtoEntityInstance.trimValuesToNull();

        Set<ImportConflict> importConflicts = new HashSet<>();
        checkAttributes( dtoEntityInstance, importOptions, importConflicts, true );

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService
            .getTrackedEntityInstance( dtoEntityInstance.getTrackedEntityInstance() );
        List<String> errors = trackerAccessManager.canWrite( importOptions.getUser(), daoEntityInstance );
        OrganisationUnit organisationUnit = getOrganisationUnit( new IdSchemes(), dtoEntityInstance.getOrgUnit() );
        Program program = getProgram( new IdSchemes(), programId );


        if ( daoEntityInstance == null || !errors.isEmpty() || organisationUnit == null || !importConflicts.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();

            if ( daoEntityInstance == null )
            {
                String message =
                    "Tracked entity instance " + dtoEntityInstance.getTrackedEntityInstance() + " does not exist";
                importConflicts.add( new ImportConflict( "TrackedEntityInstance", message ) );
            }
            else if ( !errors.isEmpty() )
            {
                importSummary.setDescription( errors.toString() );
            }
            else if ( organisationUnit == null )
            {
                String message = "Org unit " + dtoEntityInstance.getOrgUnit() + " does not exist";
                importConflicts.add( new ImportConflict( "OrganisationUnit", message ) );
            }

            importSummary.setConflicts( importConflicts );
            return importSummary;
        }

        daoEntityInstance.setOrganisationUnit( organisationUnit );
        daoEntityInstance.setInactive( dtoEntityInstance.isInactive() );
        daoEntityInstance.setFeatureType( dtoEntityInstance.getFeatureType() );
        daoEntityInstance.setCoordinates( dtoEntityInstance.getCoordinates() );

        updateAttributeValues( dtoEntityInstance, daoEntityInstance, program, importOptions.getUser() );

        updateDateFields( dtoEntityInstance, daoEntityInstance );

        teiService.updateTrackedEntityInstance( daoEntityInstance );

        importSummary.setReference( daoEntityInstance.getUid() );
        importSummary.getImportCount().incrementUpdated();

        if ( singleUpdate )
        {
            importSummary.setRelationships( handleRelationships( dtoEntityInstance, daoEntityInstance, importOptions ) );
        }
        importSummary.setEnrollments( handleEnrollments( dtoEntityInstance, daoEntityInstance, importOptions ) );

        return importSummary;
    }

    @Override
    @Transactional
    public void updateTrackedEntityInstancesSyncTimestamp( List<String> entityInstanceUIDs, Date lastSynced )
    {
        teiService.updateTrackedEntityInstancesSyncTimestamp( entityInstanceUIDs, lastSynced );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummary deleteTrackedEntityInstance( String uid )
    {
        return deleteTrackedEntityInstance( uid, null, null );
    }

    @Override
    @Transactional
    public ImportSummaries deleteTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances,
        ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );

        int counter = 0;

        for ( TrackedEntityInstance tei : trackedEntityInstances )
        {
            importSummaries
                .addImportSummary( deleteTrackedEntityInstance( tei.getTrackedEntityInstance(), tei, importOptions ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        clearSession();

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private ImportSummary deleteTrackedEntityInstance( String uid, TrackedEntityInstance dtoEntityInstance,
                                                       ImportOptions importOptions )
    {
        ImportSummary importSummary = new ImportSummary();
        importOptions = updateImportOptions( importOptions );

        boolean teiExists = teiService.trackedEntityInstanceExists( uid );

        if ( teiExists )
        {
            org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = teiService
                    .getTrackedEntityInstance( uid );

            if ( dtoEntityInstance != null )
            {
                importSummary.setReference( uid );
                importSummary
                        .setEnrollments( handleEnrollments( dtoEntityInstance, daoEntityInstance, importOptions ) );
            }

            if ( importOptions.getUser() != null )
            {
                List<ImportConflict> importConflicts = isAllowedToDelete( importOptions.getUser(), daoEntityInstance );

                if ( !importConflicts.isEmpty() )
                {
                    importSummary.setStatus( ImportStatus.ERROR );
                    importSummary.setReference( daoEntityInstance.getUid() );
                    importSummary.getConflicts().addAll( importConflicts );
                    importSummary.incrementIgnored();
                    return importSummary;
                }
            }

            teiService.deleteTrackedEntityInstance( daoEntityInstance );

            importSummary.setStatus( ImportStatus.SUCCESS );
            importSummary.setDescription( "Deletion of tracked entity instance " + uid + " was successful" );
            return importSummary.incrementDeleted();
        }
        else
        {
            importSummary.setStatus( ImportStatus.SUCCESS );
            importSummary.setDescription(
                    "Tracked entity instance " + uid + " cannot be deleted as it is not present in the system" );
            return importSummary.incrementIgnored();
        }
    }

    private org.hisp.dhis.trackedentity.TrackedEntityInstance createDAOTrackedEntityInstance(
            TrackedEntityInstance dtoEntityInstance, ImportOptions importOptions, ImportSummary importSummary)
    {
        if ( StringUtils.isEmpty( dtoEntityInstance.getOrgUnit() ) )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(),
                    "No org unit ID in tracked entity instance object" ) );
            return null;
        }

        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = new org.hisp.dhis.trackedentity.TrackedEntityInstance();

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(),
                dtoEntityInstance.getOrgUnit() );

        if ( organisationUnit == null )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(),
                    "Invalid org unit ID: " + dtoEntityInstance.getOrgUnit() ) );
            return null;
        }

        daoEntityInstance.setOrganisationUnit( organisationUnit );

        TrackedEntityType trackedEntityType = getTrackedEntityType( importOptions.getIdSchemes(),
                dtoEntityInstance.getTrackedEntityType() );

        if ( trackedEntityType == null )
        {
            importSummary.getConflicts().add( new ImportConflict( dtoEntityInstance.getTrackedEntityInstance(),
                    "Invalid tracked entity ID: " + dtoEntityInstance.getTrackedEntityType() ) );
            return null;
        }

        daoEntityInstance.setTrackedEntityType( trackedEntityType );
        daoEntityInstance.setUid( CodeGenerator.isValidUid( dtoEntityInstance.getTrackedEntityInstance() ) ?
                dtoEntityInstance.getTrackedEntityInstance() : CodeGenerator.generateUid() );

        updateDateFields( dtoEntityInstance, daoEntityInstance );

        daoEntityInstance.setFeatureType( dtoEntityInstance.getFeatureType() );
        daoEntityInstance.setCoordinates( dtoEntityInstance.getCoordinates() );

        return daoEntityInstance;
    }

    private ImportSummaries handleRelationships( TrackedEntityInstance dtoEntityInstance,
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        List<Relationship> create = new ArrayList<>();
        List<Relationship> update = new ArrayList<>();
        List<Relationship> delete = new ArrayList<>();
        List<String> relationshipUids = dtoEntityInstance.getRelationships().stream()
            .map( Relationship::getRelationship )
            .collect( Collectors.toList() );
        delete.addAll(
            daoEntityInstance.getRelationshipItems().stream()
                .map( RelationshipItem::getRelationship )
                // Remove items we cant write to
                .filter(
                    relationship -> trackerAccessManager.canWrite( importOptions.getUser(), relationship ).isEmpty() )
                .filter(
                    relationship -> relationship.getFrom().getTrackedEntityInstance().getUid().equals( daoEntityInstance.getUid() )
                )
                .map( org.hisp.dhis.relationship.Relationship::getUid )
                // Remove items we are already referencing
                .filter( ( uid ) -> !relationshipUids.contains( uid ) )
                // Create Relationships for these uids
                .map( uid -> {
                    Relationship relationship = new Relationship();
                    relationship.setRelationship( uid );
                    return relationship;
                } )
                .collect( Collectors.toList() )
        );
        for ( Relationship relationship : dtoEntityInstance.getRelationships() )
        {
            if ( importOptions.getImportStrategy() == ImportStrategy.SYNC && dtoEntityInstance.isDeleted() )
            {
                delete.add( relationship );
            }
            else if ( relationship.getRelationship() == null )
            {
                org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipItem = new org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem();
                relationshipItem.setTrackedEntityInstance( dtoEntityInstance );
                relationship.setFrom( relationshipItem );
                create.add( relationship );
            }
            else
            {
                String fromUid = relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance();
                if ( fromUid.equals( daoEntityInstance.getUid() ) )
                {
                    if ( _relationshipService.relationshipExists( relationship.getRelationship() ))
                    {
                        update.add( relationship );
                    }
                    else
                    {
                        create.add( relationship );
                    }
                }
                else
                {
                    String message = String.format(
                        "Can't update relationship '%s': TrackedEntityInstance '%s' is not the owner of the relationship",
                        relationship.getRelationship(), daoEntityInstance.getUid() );
                    importSummaries.addImportSummary(
                        new ImportSummary( ImportStatus.ERROR, message )
                            .setReference( relationship.getRelationship() )
                            .incrementIgnored()
                    );
                }
            }
        }
        importSummaries.addImportSummaries( relationshipService.addRelationships( create, importOptions ) );
        importSummaries.addImportSummaries( relationshipService.updateRelationships( update, importOptions ) );
        importSummaries.addImportSummaries( relationshipService.deleteRelationships( delete, importOptions ) );
        return importSummaries;
    }

    private ImportSummaries handleEnrollments( TrackedEntityInstance dtoEntityInstance,
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance, ImportOptions importOptions )
    {
        List<Enrollment> create = new ArrayList<>();
        List<Enrollment> update = new ArrayList<>();
        List<Enrollment> delete = new ArrayList<>();

        for ( Enrollment enrollment : dtoEntityInstance.getEnrollments() )
        {
            enrollment.setTrackedEntityType( dtoEntityInstance.getTrackedEntityType() );
            enrollment.setTrackedEntityInstance( daoEntityInstance.getUid() );

            if ( importOptions.getImportStrategy().isSync() && enrollment.isDeleted() )
            {
                delete.add( enrollment );
            }
            else if ( !programInstanceService.programInstanceExists( enrollment.getEnrollment() ) )
            {
                create.add( enrollment );
            }
            else
            {
                update.add( enrollment );
            }
        }

        ImportSummaries importSummaries = new ImportSummaries();

        importSummaries.addImportSummaries( enrollmentService.deleteEnrollments( delete, importOptions, false ) );
        importSummaries.addImportSummaries( enrollmentService.updateEnrollments( update, importOptions, false ) );
        importSummaries.addImportSummaries( enrollmentService.addEnrollments( create, importOptions, daoEntityInstance, false ) );

        return importSummaries;
    }

    private void prepareCaches( List<TrackedEntityInstance> trackedEntityInstances, User user )
    {
        Collection<String> orgUnits = trackedEntityInstances.stream().map( TrackedEntityInstance::getOrgUnit )
            .collect( Collectors.toSet() );

        if ( !orgUnits.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( OrganisationUnit.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", orgUnits ) );
            queryService.query( query )
                .forEach( ou -> organisationUnitCache.put( ou.getUid(), (OrganisationUnit) ou ) );
        }

        Collection<String> trackedEntityAttributes = new HashSet<>();
        trackedEntityInstances
            .forEach( e -> e.getAttributes().forEach( at -> trackedEntityAttributes.add( at.getAttribute() ) ) );

        if ( !trackedEntityAttributes.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( TrackedEntityAttribute.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", trackedEntityAttributes ) );
            queryService.query( query )
                .forEach( tea -> trackedEntityAttributeCache.put( tea.getUid(), (TrackedEntityAttribute) tea ) );
        }
    }

    private void updateAttributeValues( TrackedEntityInstance dtoEntityInstance,
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance, Program program, User user )
    {
        Map<String, TrackedEntityAttributeValue> teiAttributeToValueMap = getTeiAttributeValueMap( trackedEntityAttributeValueService.getTrackedEntityAttributeValues( daoEntityInstance ) );

        Set<String> incomingAttributes = new HashSet<>();

        List<Attribute> attributes = dtoEntityInstance.getAttributes().stream().filter(  ObjectUtils.distinctByKey( att -> att.getAttribute() ) ).collect( Collectors.toList() );

        for ( Attribute dtoAttribute : attributes )
        {
            String storedBy = getStoredBy( dtoAttribute, new ImportSummary(),
                user == null ? "[Unknown]" : user.getUsername() );

            TrackedEntityAttributeValue existingAttributeValue = teiAttributeToValueMap.get( dtoAttribute.getAttribute() );

            incomingAttributes.add( dtoAttribute.getAttribute() );

            if ( existingAttributeValue != null ) // value exists
            {
                if ( !existingAttributeValue.getValue().equals( dtoAttribute.getValue() ) ) // value is changed, do update
                {
                    existingAttributeValue.setStoredBy( storedBy );
                    existingAttributeValue.setValue( dtoAttribute.getValue() );
                    trackedEntityAttributeValueService.updateTrackedEntityAttributeValue( existingAttributeValue );
                }
            }
            else // value is new, do add
            {
                TrackedEntityAttribute daoEntityAttribute = trackedEntityAttributeService
                    .getTrackedEntityAttribute( dtoAttribute.getAttribute() );

                TrackedEntityAttributeValue newAttributeValue = new TrackedEntityAttributeValue();

                newAttributeValue.setStoredBy( storedBy );
                newAttributeValue.setEntityInstance( daoEntityInstance );
                newAttributeValue.setValue( dtoAttribute.getValue() );
                newAttributeValue.setAttribute( daoEntityAttribute );

                daoEntityInstance.getTrackedEntityAttributeValues().add( newAttributeValue );
                trackedEntityAttributeValueService.addTrackedEntityAttributeValue( newAttributeValue );
            }
        }

        if ( program != null )
        {
            for( TrackedEntityAttribute att : program.getTrackedEntityAttributes() )
            {
                TrackedEntityAttributeValue attVal = teiAttributeToValueMap.get( att.getUid() );

                if ( attVal != null && !incomingAttributes.contains( att.getUid() ) )
                {
                    trackedEntityAttributeValueService.deleteTrackedEntityAttributeValue( attVal );
                }
            }
        }
    }

    private OrganisationUnit getOrganisationUnit( IdSchemes idSchemes, String id )
    {
        return organisationUnitCache
            .get( id, () -> manager.getObject( OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id ) );
    }

    private Program getProgram( IdSchemes idSchemes, String id )
    {
        if ( id == null )
        {
            return null;
        }

        return programCache
            .get( id, () -> manager.getObject( Program.class, idSchemes.getProgramIdScheme(), id ) );
    }

    private TrackedEntityType getTrackedEntityType( IdSchemes idSchemes, String id )
    {
        return trackedEntityCache
            .get( id, () -> manager.getObject( TrackedEntityType.class, idSchemes.getTrackedEntityIdScheme(), id ) );
    }

    private TrackedEntityAttribute getTrackedEntityAttribute( IdSchemes idSchemes, String id )
    {
        return trackedEntityAttributeCache.get( id, () -> manager
            .getObject( TrackedEntityAttribute.class, idSchemes.getTrackedEntityAttributeIdScheme(), id ) );
    }

    private Map<String, TrackedEntityAttributeValue> getTeiAttributeValueMap(
        List<TrackedEntityAttributeValue> teiAttributeValues )
    {
        return teiAttributeValues.stream().collect( Collectors.toMap( tav -> tav.getAttribute().getUid(), tav -> tav ) );
    }

    //--------------------------------------------------------------------------
    // VALIDATION
    //--------------------------------------------------------------------------

    private void validateAttributeType( Attribute attribute, ImportOptions importOptions, Set<ImportConflict> importConflicts )
    {
        //Cache is populated. I should hit it.
        TrackedEntityAttribute daoTrackedEntityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(),
            attribute.getAttribute() );

        if ( daoTrackedEntityAttribute == null )
        {
            importConflicts.add( new ImportConflict( "Attribute.attribute", "Does not point to a valid attribute" ) );
        }

        String errorMessage = trackedEntityAttributeService
            .validateValueType( daoTrackedEntityAttribute, attribute.getValue() );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }
    }

    private void validateTextPatternValue( TrackedEntityAttribute attribute, String value, Set<ImportConflict> importConflicts )
    {
        if ( !TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value )
            && !reservedValueService.isReserved( attribute.getTextPattern(), value ) )
        {
            importConflicts
                .add( new ImportConflict( "Attribute.value", "Value does not match the attribute pattern" ) );
        }
    }

    private void checkAttributeUniquenessWithinScope( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance,
        TrackedEntityAttribute trackedEntityAttribute, String value, OrganisationUnit organisationUnit,
        Set<ImportConflict> importConflicts )
    {
        String errorMessage = trackedEntityAttributeService
            .validateAttributeUniquenessWithinScope( trackedEntityAttribute, value, entityInstance,
                organisationUnit );

        if ( errorMessage != null )
        {
            importConflicts.add( new ImportConflict( "Attribute.value", errorMessage ) );
        }
    }

    private void checkAttributes( TrackedEntityInstance dtoEntityInstance, ImportOptions importOptions,
        Set<ImportConflict> importConflicts, boolean teiExistsInDatabase )
    {
        if ( dtoEntityInstance.getAttributes().isEmpty() )
        {
            return;
        }

        List<String> fileValues = new ArrayList<>();
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance = null;

        if ( teiExistsInDatabase )
        {
            daoEntityInstance = teiService.getTrackedEntityInstance( dtoEntityInstance.getTrackedEntityInstance() );

            daoEntityInstance.getTrackedEntityAttributeValues().stream()
                .filter( attrVal -> attrVal.getAttribute().getValueType().isFile() ).forEach( attrVal -> fileValues.add( attrVal.getValue() ) );
        }

        for ( Attribute attribute : dtoEntityInstance.getAttributes() )
        {
            if ( StringUtils.isNotEmpty( attribute.getValue() ) )
            {
                //Cache was populated in prepareCaches, so I should hit the cache
                TrackedEntityAttribute daoEntityAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(),
                    attribute.getAttribute() );

                if ( daoEntityAttribute == null )
                {
                    importConflicts.add( new ImportConflict( "Attribute.attribute", "Invalid attribute " + attribute.getAttribute() ) );
                    continue;
                }

                if ( daoEntityAttribute.isGenerated() && daoEntityAttribute.getTextPattern() != null && !importOptions.isSkipPatternValidation() )
                {
                    validateTextPatternValue( daoEntityAttribute, attribute.getValue(), importConflicts );
                }

                if ( daoEntityAttribute.isUnique() )
                {
                    //Cache was populated in prepareCaches, so I should hit the cache
                    OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(),
                        dtoEntityInstance.getOrgUnit() );
                    checkAttributeUniquenessWithinScope( daoEntityInstance, daoEntityAttribute, attribute.getValue(), organisationUnit, importConflicts );
                }

                validateAttributeType( attribute, importOptions, importConflicts );

                if ( daoEntityAttribute.getValueType().isFile() && checkAssigned( attribute, fileValues ) )
                {
                    importConflicts.add( new ImportConflict( "Attribute.value",
                        String.format( "File resource with uid '%s' has already been assigned to a different object",
                            attribute.getValue() ) ) );
                }
            }
        }
    }

    private List<ImportConflict> checkTrackedEntityType( TrackedEntityInstance entityInstance,
        ImportOptions importOptions )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        if ( entityInstance.getTrackedEntityType() == null )
        {
            importConflicts.add( new ImportConflict( "TrackedEntityInstance.trackedEntityType",
                "Missing required property trackedEntityType" ) );
            return importConflicts;
        }

        TrackedEntityType daoTrackedEntityType = getTrackedEntityType( importOptions.getIdSchemes(),
            entityInstance.getTrackedEntityType() );

        if ( daoTrackedEntityType == null )
        {
            importConflicts
                .add( new ImportConflict( "TrackedEntityInstance.trackedEntityType", "Invalid trackedEntityType" +
                    entityInstance.getTrackedEntityType() ) );
        }

        return importConflicts;
    }

    private void clearSession()
    {
        organisationUnitCache.clear();
        trackedEntityCache.clear();
        trackedEntityAttributeCache.clear();

        dbmsManager.clearSession();
    }

    private void updateDateFields( TrackedEntityInstance dtoEntityInstance,
        org.hisp.dhis.trackedentity.TrackedEntityInstance daoEntityInstance )
    {
        Date createdAtClient = DateUtils.parseDate( dtoEntityInstance.getCreatedAtClient() );

        if ( createdAtClient != null )
        {
            daoEntityInstance.setCreatedAtClient( createdAtClient );
        }

        String lastUpdatedAtClient = dtoEntityInstance.getLastUpdatedAtClient();

        if ( lastUpdatedAtClient != null )
        {
            daoEntityInstance.setLastUpdatedAtClient( DateUtils.parseDate( lastUpdatedAtClient ) );
        }

        daoEntityInstance.setAutoFields();
    }

    private String getStoredBy( Attribute attributeValue, ImportSummary importSummary, String fallbackUsername )
    {
        String storedBy = attributeValue.getStoredBy();

        if ( StringUtils.isEmpty( storedBy ) )
        {
            storedBy = User.getSafeUsername( fallbackUsername );
        }
        else if ( storedBy.length() > UserCredentials.USERNAME_MAX_LENGTH )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "stored by",
                    storedBy + " is more than " + UserCredentials.USERNAME_MAX_LENGTH + " characters, using current username instead" ) );
            }

            storedBy = User.getSafeUsername( fallbackUsername );
        }

        return storedBy;
    }

    private boolean checkAssigned( Attribute attribute, List<String> oldFileValues )
    {
        FileResource fileResource = fileResourceService.getFileResource( attribute.getValue() );
        return fileResource != null && fileResource.isAssigned() && !oldFileValues.contains( attribute.getValue() );
    }

    protected ImportOptions updateImportOptions( ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
            importOptions.setSkipLastUpdated( true );
        }

        if ( importOptions.getUser() == null )
        {
            importOptions.setUser( currentUserService.getCurrentUser() );
        }

        return importOptions;
    }

    private void reloadUser(ImportOptions importOptions)
    {
        if ( importOptions == null || importOptions.getUser() == null )
        {
            return;
        }

        importOptions.setUser( userService.getUser( importOptions.getUser().getUid() ) );
    }

    private List<ImportConflict> isAllowedToDelete( User user, org.hisp.dhis.trackedentity.TrackedEntityInstance tei )
    {
        List<ImportConflict> importConflicts = new ArrayList<>();

        Set<ProgramInstance> programInstances = tei.getProgramInstances().stream()
            .filter( pi -> !pi.isDeleted() )
            .collect( Collectors.toSet() );

        if ( !programInstances.isEmpty() && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
        {
            importConflicts.add( new ImportConflict( tei.getUid(), "Tracked entity instance " + tei.getUid() +
                " cannot be deleted as it has associated enrollments and user does not have authority "
                + Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) );
        }

        List<String> errors = trackerAccessManager.canWrite( user, tei );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> importConflicts.add( new ImportConflict( tei.getUid(), error ) ) );
        }

        return importConflicts;
    }

    private TrackedEntityInstance getTei( org.hisp.dhis.trackedentity.TrackedEntityInstance daoTrackedEntityInstance,
        Set<TrackedEntityAttribute> readableAttributes, TrackedEntityInstanceParams params, User user )
    {
        if ( daoTrackedEntityInstance == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setTrackedEntityInstance( daoTrackedEntityInstance.getUid() );
        trackedEntityInstance.setOrgUnit( daoTrackedEntityInstance.getOrganisationUnit().getUid() );
        trackedEntityInstance.setTrackedEntityType( daoTrackedEntityInstance.getTrackedEntityType().getUid() );
        trackedEntityInstance.setCreated( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getCreated() ) );
        trackedEntityInstance
            .setCreatedAtClient( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdatedAtClient() ) );
        trackedEntityInstance.setLastUpdated( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdated() ) );
        trackedEntityInstance
            .setLastUpdatedAtClient( DateUtils.getIso8601NoTz( daoTrackedEntityInstance.getLastUpdatedAtClient() ) );
        trackedEntityInstance.setInactive( daoTrackedEntityInstance.isInactive() );
        trackedEntityInstance.setFeatureType( daoTrackedEntityInstance.getFeatureType() );
        trackedEntityInstance.setCoordinates( daoTrackedEntityInstance.getCoordinates() );
        trackedEntityInstance.setDeleted( daoTrackedEntityInstance.isDeleted() );

        if ( params.isIncludeRelationships() )
        {
            for ( RelationshipItem relationshipItem : daoTrackedEntityInstance.getRelationshipItems() )
            {
                org.hisp.dhis.relationship.Relationship daoRelationship = relationshipItem.getRelationship();

                if ( trackerAccessManager.canRead( user, daoRelationship ).isEmpty() )
                {
                    Relationship relationship = relationshipService.getRelationship( relationshipItem.getRelationship(),
                        RelationshipParams.FALSE, user );

                    trackedEntityInstance.getRelationships().add( relationship );
                }
            }
        }

        if ( params.isIncludeEnrollments() )
        {
            for ( ProgramInstance programInstance : daoTrackedEntityInstance.getProgramInstances() )
            {
                if ( trackerAccessManager.canRead( user, programInstance, false ).isEmpty() && (params.isIncludeDeleted() || !programInstance.isDeleted()) )
                {
                    trackedEntityInstance.getEnrollments()
                        .add( enrollmentService.getEnrollment( user, programInstance, params, true ) );
                }
            }
        }

        if ( params.isIncludeProgramOwners() )
        {
            for ( TrackedEntityProgramOwner programOwner : daoTrackedEntityInstance.getProgramOwners() )
            {
                trackedEntityInstance.getProgramOwners().add( new ProgramOwner( programOwner ) );
            }
        }

        Set<TrackedEntityAttribute> readableAttributesCopy;

        if ( params.isDataSynchronizationQuery() )
        {
            List<String> programs = trackedEntityInstance.getEnrollments().stream()
                .map( Enrollment::getProgram )
                .collect( Collectors.toList() );

            readableAttributesCopy = readableAttributes.stream()
                .filter( att -> !att.getSkipSynchronization() )
                .collect( Collectors.toSet() ) ;

            IdSchemes idSchemes = new IdSchemes();
            for ( String programUid : programs )
            {
                Program program = getProgram( idSchemes, programUid );

                readableAttributesCopy.addAll(
                    program.getTrackedEntityAttributes().stream()
                        .filter( att -> !att.getSkipSynchronization() )
                        .collect( Collectors.toSet() ) );
            }
        }
        else
        {
            readableAttributesCopy = new HashSet<>( readableAttributes );
        }

        for ( TrackedEntityAttributeValue attributeValue : daoTrackedEntityInstance.getTrackedEntityAttributeValues() )
        {
            if ( readableAttributesCopy.contains( attributeValue.getAttribute() ) )
            {
                Attribute attribute = new Attribute();

                attribute.setCreated( DateUtils.getIso8601NoTz( attributeValue.getCreated() ) );
                attribute.setLastUpdated( DateUtils.getIso8601NoTz( attributeValue.getLastUpdated() ) );
                attribute.setDisplayName( attributeValue.getAttribute().getDisplayName() );
                attribute.setAttribute( attributeValue.getAttribute().getUid() );
                attribute.setValueType( attributeValue.getAttribute().getValueType() );
                attribute.setCode( attributeValue.getAttribute().getCode() );
                attribute.setValue( attributeValue.getValue() );
                attribute.setStoredBy( attributeValue.getStoredBy() );
                attribute.setSkipSynchronization( attributeValue.getAttribute().getSkipSynchronization() );

                trackedEntityInstance.getAttributes().add( attribute );
            }
        }

        return trackedEntityInstance;
    }
}
