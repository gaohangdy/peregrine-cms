package com.peregrine.sitemap.impl;

/*-
 * #%L
 * platform base - Core
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import com.peregrine.sitemap.SiteMapFileContentBuilder;
import com.peregrine.sitemap.SiteMapEntry;
import com.peregrine.sitemap.SiteMapUrlBuilder;
import com.peregrine.sitemap.XMLBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.peregrine.sitemap.SiteMapConstants.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component(service = SiteMapFileContentBuilder.class)
@Designate(ocd = SiteMapFileContentBuilderImplConfig.class)
public final class SiteMapFileContentBuilderImpl implements SiteMapFileContentBuilder,
        SiteMapEntry.PropertiesVisitor<Integer> {

    private static final int BASE_ENTRY_LENGTH = XMLBuilder.getBasicElementLength(URL);
    private static final String EQ = "=";
    private static final Map<String, String> SITE_MAP_INDEX_ATTRIBUTES = new HashMap<>();
    private static final Map<String, String> URL_SET_ATTRIBUTES = new HashMap<>();

    static {
        SITE_MAP_INDEX_ATTRIBUTES.put("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
        URL_SET_ATTRIBUTES.putAll(SITE_MAP_INDEX_ATTRIBUTES);
        URL_SET_ATTRIBUTES.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        URL_SET_ATTRIBUTES.put("xsi:schemaLocation",
                "http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd");
    }

    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
    private final Map<String, String> urlSetAttributes = new HashMap<>();
    private int baseSiteMapLength;

    @Activate
    public void activate(final SiteMapFileContentBuilderImplConfig config) {
        urlSetAttributes.clear();
        urlSetAttributes.putAll(URL_SET_ATTRIBUTES);
        final String[] xmlnsMappings = config.xmlnsMappings();
        if (nonNull(xmlnsMappings)) {
            for (final String mapping : xmlnsMappings) {
                if (StringUtils.contains(mapping, EQ)) {
                    final String key = "xmlns:" + StringUtils.substringBefore(mapping, EQ);
                    final String value = StringUtils.substringAfter(mapping, EQ);
                    urlSetAttributes.put(key, value);
                }
            }
        }

        baseSiteMapLength = XMLBuilder.XML_VERSION.length();
        baseSiteMapLength += XMLBuilder.getBasicElementLength(URL_SET, urlSetAttributes);
    }

    @Override
    public String buildUrlSet(final Collection<SiteMapEntry> entries) {
        final XMLBuilder result = new XMLBuilder();
        result.startElement(URL_SET, urlSetAttributes);
        for (final SiteMapEntry entry : entries) {
            if (!isEmpty(entry)) {
                addEntryAsUrl(entry, result);
            }
        }

        result.endElement();
        return result.toString();
    }

    private boolean isEmpty(final SiteMapEntry entry) {
        return isBlank(entry.getUrl());
    }

    private void addEntryAsUrl(final SiteMapEntry source, final XMLBuilder target) {
        target.startElement(URL);
        for (final Map.Entry<String, Object> e : source.getProperties().entrySet()) {
            target.addElement(e.getKey(), String.valueOf(e.getValue()));
        }

        target.endElement();
    }

    @Override
    public int getSize(final SiteMapEntry entry) {
        if (isEmpty(entry)) {
            return 0;
        }

        return entry.walk(this, BASE_ENTRY_LENGTH);
    }

    @Override
    public Integer visit(final String propertyName, final String propertyValue, final Integer size) {
        return visit(propertyName, size) + propertyValue.length();
    }

    @Override
    public Integer visit(final String mapName, final Integer size) {
        return size + XMLBuilder.getBasicElementLength(mapName);
    }

    @Override
    public int getBaseSiteMapLength() {
        return baseSiteMapLength;
    }

    @Override
    public String buildSiteMapIndex(final Resource root, final SiteMapUrlBuilder urlBuilder, final int numberOfParts) {
        final XMLBuilder result = new XMLBuilder();
        result.startElement(SITE_MAP_INDEX, SITE_MAP_INDEX_ATTRIBUTES);
        for (int part = 1; part <= numberOfParts; part++) {
            final String url = urlBuilder.buildSiteMapUrl(root, part);
            result.startElement(SITE_MAP);
            result.addElement(LOC, url);
            final Date lastModified = new Date(System.currentTimeMillis());
            if (nonNull(lastModified)) {
                result.addElement(LAST_MOD, dateFormat.format(lastModified));
            }

            result.endElement();
        }

        result.endElement();
        return result.toString();
    }

}
