package org.hisp.dhis.kafka;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "kafka", namespace = DxfNamespaces.DXF_2_0 )
public class Kafka
{
    private String bootstrapServers;

    private String clientId;

    private int retries;

    private int maxPollRecords;

    public Kafka()
    {
    }

    public Kafka( String bootstrapServers, String clientId, int retries, int maxPollRecords )
    {
        this.bootstrapServers = bootstrapServers;
        this.clientId = clientId;
        this.retries = retries;
        this.maxPollRecords = maxPollRecords;
    }

    @JsonProperty( "bootstrap-servers" )
    @JacksonXmlProperty( localName = "bootstrap-servers", namespace = DxfNamespaces.DXF_2_0 )
    public String getBootstrapServers()
    {
        return bootstrapServers;
    }

    public Kafka setBootstrapServers( String bootstrapServers )
    {
        this.bootstrapServers = bootstrapServers;
        return this;
    }

    @JsonProperty( "client-id" )
    @JacksonXmlProperty( localName = "client-id", namespace = DxfNamespaces.DXF_2_0 )
    public String getClientId()
    {
        return clientId;
    }

    public void setClientId( String clientId )
    {
        this.clientId = clientId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getRetries()
    {
        return retries;
    }

    public Kafka setRetries( int retries )
    {
        this.retries = retries;
        return this;
    }

    @JsonProperty( "max-poll-records" )
    @JacksonXmlProperty( localName = "max-poll-records", namespace = DxfNamespaces.DXF_2_0 )
    public int getMaxPollRecords()
    {
        return maxPollRecords;
    }

    public void setMaxPollRecords( int maxPollRecords )
    {
        this.maxPollRecords = maxPollRecords;
    }

    public boolean isValid()
    {
        return bootstrapServers != null;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "bootstrap-servers", bootstrapServers )
            .add( "retries", retries )
            .add( "max-poll-records", maxPollRecords )
            .toString();
    }
}
